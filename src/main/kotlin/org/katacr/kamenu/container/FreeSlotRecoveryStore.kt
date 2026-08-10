package org.katacr.kamenu.container

import org.katacr.kamenu.DatabaseManager
import org.katacr.kamenu.KaScheduler
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 自由槽位持久化托管仓库。
 *
 * 调用方在玩家线程完成 ItemStack 序列化，本类只在异步线程执行 SQL，避免数据库延迟阻塞库存事件。
 */
class FreeSlotRecoveryStore(private val databaseManager: DatabaseManager) {
    private val pendingOperations = ConcurrentHashMap.newKeySet<CompletableFuture<*>>()
    private val sessionTails = mutableMapOf<UUID, CompletableFuture<Unit>>()
    private val sessionQueueLock = Any()

    /** 一条无需访问 Bukkit API 的序列化托管记录。 */
    data class Record(
        val sessionId: UUID,
        val playerId: UUID,
        val menuId: String,
        val generation: Long,
        val freeSlotId: String,
        val inventorySlot: Int,
        val itemData: String,
        val state: State,
        val updateTime: Long
    )

    enum class State {
        PREPARED,
        HELD,
        CONSUMED,
        RETURN_PENDING,
        RETURNED
    }

    /** 原子替换一次会话中指定物理槽位的托管快照。 */
    fun replaceSlotsAsync(
        sessionId: UUID,
        playerId: UUID,
        menuId: String,
        generation: Long,
        slotRecords: Map<Int, Pair<String, String>>,
        affectedSlots: Set<Int>,
        state: State = State.PREPARED
    ): CompletableFuture<Unit> = enqueue(sessionId) {
        databaseManager.connection.use { connection ->
            connection.inTransaction {
                deleteSlots(connection, sessionId, affectedSlots)
                if (slotRecords.isNotEmpty()) {
                    insertRecords(
                        connection,
                        sessionId,
                        playerId,
                        menuId,
                        generation,
                        slotRecords,
                        state
                    )
                }
            }
        }
    }

    /** 将一次已提交库存事务的记录切换为 HELD。 */
    fun markHeldAsync(sessionId: UUID, slots: Set<Int>): CompletableFuture<Unit> = enqueue(sessionId) {
        updateState(sessionId, slots, State.HELD)
    }

    /** 删除已经取出、消费或成功返还的物理槽位记录。 */
    fun deleteSlotsAsync(sessionId: UUID, slots: Set<Int>): CompletableFuture<Unit> = enqueue(sessionId) {
        databaseManager.connection.use { connection -> deleteSlots(connection, sessionId, slots) }
    }

    /** 将背包无法接收的托管记录保留为待领取状态。 */
    fun markReturnPendingAsync(sessionId: UUID, slots: Set<Int>): CompletableFuture<Unit> = enqueue(sessionId) {
        updateState(sessionId, slots, State.RETURN_PENDING)
    }

    /** 加载玩家所有仍需恢复的托管记录。 */
    fun loadRecoverableAsync(playerId: UUID): CompletableFuture<List<Record>> = async {
        databaseManager.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT session_id, player_uuid, menu_id, menu_generation, free_slot_id,
                       inventory_slot, item_data, state, update_time
                FROM free_slot_escrow
                WHERE player_uuid = ? AND state IN (?, ?, ?)
                ORDER BY update_time ASC, inventory_slot ASC
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, playerId.toString())
                statement.setString(2, State.PREPARED.name)
                statement.setString(3, State.HELD.name)
                statement.setString(4, State.RETURN_PENDING.name)
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) {
                            add(
                                Record(
                                    sessionId = UUID.fromString(result.getString("session_id")),
                                    playerId = UUID.fromString(result.getString("player_uuid")),
                                    menuId = result.getString("menu_id"),
                                    generation = result.getLong("menu_generation"),
                                    freeSlotId = result.getString("free_slot_id"),
                                    inventorySlot = result.getInt("inventory_slot"),
                                    itemData = result.getString("item_data"),
                                    state = State.valueOf(result.getString("state")),
                                    updateTime = result.getLong("update_time")
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    /** 批量更新指定会话物理槽位的托管状态。 */
    private fun updateState(sessionId: UUID, slots: Set<Int>, state: State) {
        if (slots.isEmpty()) return
        databaseManager.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE free_slot_escrow SET state = ?, update_time = ? WHERE session_id = ? AND inventory_slot = ?"
            ).use { statement ->
                val now = System.currentTimeMillis()
                slots.forEach { slot ->
                    statement.setString(1, state.name)
                    statement.setLong(2, now)
                    statement.setString(3, sessionId.toString())
                    statement.setInt(4, slot)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    /** 在调用方事务中批量插入序列化托管记录。 */
    private fun insertRecords(
        connection: Connection,
        sessionId: UUID,
        playerId: UUID,
        menuId: String,
        generation: Long,
        slotRecords: Map<Int, Pair<String, String>>,
        state: State
    ) {
        connection.prepareStatement(
            """
            INSERT INTO free_slot_escrow (
                session_id, player_uuid, menu_id, menu_generation, free_slot_id,
                inventory_slot, item_data, state, update_time
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            val now = System.currentTimeMillis()
            slotRecords.forEach { (slot, data) ->
                statement.setString(1, sessionId.toString())
                statement.setString(2, playerId.toString())
                statement.setString(3, menuId)
                statement.setLong(4, generation)
                statement.setString(5, data.first)
                statement.setInt(6, slot)
                statement.setString(7, data.second)
                statement.setString(8, state.name)
                statement.setLong(9, now)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    /** 使用已有连接批量删除指定会话槽位记录。 */
    private fun deleteSlots(connection: Connection, sessionId: UUID, slots: Set<Int>) {
        if (slots.isEmpty()) return
        connection.prepareStatement(
            "DELETE FROM free_slot_escrow WHERE session_id = ? AND inventory_slot = ?"
        ).use { statement ->
            slots.forEach { slot ->
                statement.setString(1, sessionId.toString())
                statement.setInt(2, slot)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun Connection.inTransaction(block: () -> Unit) {
        val previousAutoCommit = autoCommit
        autoCommit = false
        try {
            block()
            commit()
        } catch (error: Throwable) {
            rollback()
            throw error
        } finally {
            autoCommit = previousAutoCommit
        }
    }

    /** 调度不需要会话写入排序的异步数据库读取。 */
    private fun <T> async(block: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        track(future)
        KaScheduler.runAsync(Runnable {
            runCatching(block).fold(future::complete, future::completeExceptionally)
        })
        return future
    }

    /** 将同一会话的 SQL 状态迁移串行执行，避免 MySQL 多连接下后写先完成。 */
    private fun enqueue(sessionId: UUID, block: () -> Unit): CompletableFuture<Unit> {
        val result = CompletableFuture<Unit>()
        val predecessor = synchronized(sessionQueueLock) {
            val previous = sessionTails[sessionId] ?: CompletableFuture.completedFuture(Unit)
            sessionTails[sessionId] = result
            previous
        }
        track(result)
        predecessor.whenComplete { _, _ ->
            KaScheduler.runAsync(Runnable {
                runCatching(block).fold(result::complete, result::completeExceptionally)
            })
        }
        result.whenComplete { _, _ ->
            synchronized(sessionQueueLock) {
                if (sessionTails[sessionId] === result) sessionTails.remove(sessionId)
            }
        }
        return result
    }

    /** 跟踪停服前仍需等待的数据库 Future。 */
    private fun track(future: CompletableFuture<*>) {
        pendingOperations += future
        future.whenComplete { _, _ -> pendingOperations -= future }
    }

    /** 停服前等待已提交的 SQL 操作结束，避免先关闭连接池再写托管状态。 */
    fun awaitPending(timeoutMillis: Long): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (pendingOperations.isNotEmpty()) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0L) return false
            val snapshot = pendingOperations.toTypedArray()
            runCatching {
                val settled = snapshot.map { future -> future.handle { _, _ -> Unit } }.toTypedArray()
                CompletableFuture.allOf(*settled).get(remaining, TimeUnit.NANOSECONDS)
            }.getOrElse { return false }
        }
        return true
    }
}
