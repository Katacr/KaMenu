package org.katacr.kamenu

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.util.*

/**
 * 数据库管理器。
 *
 * 负责 KaMenu 内置持久化：玩家 data/list、全局 gdata/glist、以及保存的物品。
 * SQLite 使用单连接池以降低锁冲突，MySQL 使用较大的连接池。
 *
 * 菜单动作默认会异步执行写操作，避免主线程等待数据库。
 */
class DatabaseManager(val plugin: KaMenu) {
    var dataSource: HikariDataSource? = null
    private val listMutationLock = Any()

    /**
     * 初始化数据库连接池并创建表结构。
     */
    fun setup() {
        val config = HikariConfig()
        val dbType = plugin.config.getString("storage.type", "SQLite") ?: "SQLite"

        if (dbType.equals("MySQL", ignoreCase = true)) {
            config.jdbcUrl = "jdbc:mysql://${plugin.config.getString("storage.host")}:${plugin.config.getInt("storage.port")}/${plugin.config.getString("storage.db")}"
            config.username = plugin.config.getString("storage.user")
            config.password = plugin.config.getString("storage.password")
            config.driverClassName = "com.mysql.cj.jdbc.Driver"
        } else {
            val file = plugin.dataFolder.resolve("storage.db")
            config.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
            config.driverClassName = "org.sqlite.JDBC"
        }

        config.maximumPoolSize = 10
        if (!dbType.equals("MySQL", ignoreCase = true)) config.maximumPoolSize = 1

        dataSource = HikariDataSource(config)
        createTables()
    }

    /**
     * 获取数据库连接。
     *
     * 调用方必须使用 `use` 关闭连接，避免连接池泄漏。
     */
    val connection: Connection
        get() = dataSource!!.connection

    /**
     * 创建数据库表
     */
    private fun createTables() {
        val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
        val isMySQL = dbType.equals("mysql", ignoreCase = true)
        val autoIncrement = if (isMySQL) "AUTO_INCREMENT" else ""
        val uniqueConstraint = if (isMySQL) "UNIQUE KEY" else "UNIQUE"

        connection.use { conn ->
            val statement = conn.createStatement()

            // 玩家数据表
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    id INTEGER PRIMARY KEY $autoIncrement,
                    player_uuid VARCHAR(36) NOT NULL,
                    data_key VARCHAR(64) NOT NULL,
                    data_value TEXT,
                    update_time BIGINT,
                    $uniqueConstraint(player_uuid, data_key)
                )
            """)

            // 全局数据表
            statement.execute("""
                CREATE TABLE IF NOT EXISTS global_data (
                    id INTEGER PRIMARY KEY $autoIncrement,
                    data_key VARCHAR(64) NOT NULL UNIQUE,
                    data_value TEXT,
                    update_time BIGINT
                )
            """)

            // 保存的物品表
            statement.execute("""
                CREATE TABLE IF NOT EXISTS saved_items (
                    id INTEGER PRIMARY KEY $autoIncrement,
                    item_name VARCHAR(64) NOT NULL UNIQUE,
                    item_data TEXT NOT NULL,
                    saved_by VARCHAR(36),
                    update_time BIGINT
                )
            """)

            // Container 自由槽位托管账本；物品恢复记录不能与普通 data/list 混用。
            statement.execute("""
                CREATE TABLE IF NOT EXISTS free_slot_escrow (
                    session_id VARCHAR(36) NOT NULL,
                    player_uuid VARCHAR(36) NOT NULL,
                    menu_id VARCHAR(255) NOT NULL,
                    menu_generation BIGINT NOT NULL,
                    free_slot_id VARCHAR(64) NOT NULL,
                    inventory_slot INTEGER NOT NULL,
                    item_data TEXT NOT NULL,
                    state VARCHAR(32) NOT NULL,
                    update_time BIGINT NOT NULL,
                    PRIMARY KEY (session_id, inventory_slot)
                )
            """)

            // 玩家临时键值对；expire_at 为到期毫秒时间戳，0 表示永不过期。
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_tmpdata (
                    id INTEGER PRIMARY KEY $autoIncrement,
                    player_uuid VARCHAR(36) NOT NULL,
                    data_key VARCHAR(64) NOT NULL,
                    data_value TEXT,
                    expire_at BIGINT NOT NULL,
                    update_time BIGINT,
                    $uniqueConstraint(player_uuid, data_key)
                )
            """)
        }
    }

    /**
     * 设置玩家数据
     */
    fun setPlayerData(playerUuid: UUID, key: String, value: String) {
        connection.use { conn ->
            val currentTime = System.currentTimeMillis()
            val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
            val isMySQL = dbType.equals("mysql", ignoreCase = true)

            val sql = if (isMySQL) {
                """
                INSERT INTO player_data (player_uuid, data_key, data_value, update_time)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    data_value = VALUES(data_value),
                    update_time = VALUES(update_time)
            """.trimIndent()
            } else {
                """
                INSERT INTO player_data (player_uuid, data_key, data_value, update_time)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(player_uuid, data_key) DO UPDATE SET
                    data_value = excluded.data_value,
                    update_time = excluded.update_time
            """.trimIndent()
            }

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.setString(3, value)
                stmt.setLong(4, currentTime)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 获取玩家数据
     */
    fun getPlayerData(playerUuid: UUID, key: String): String? {
        connection.use { conn ->
            val sql = "SELECT data_value FROM player_data WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getString("data_value") else null
                }
            }
        }
    }

    /**
     * 删除玩家数据
     */
    fun deletePlayerData(playerUuid: UUID, key: String): Boolean {
        connection.use { conn ->
            val sql = "DELETE FROM player_data WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                return stmt.executeUpdate() > 0
            }
        }
    }

    // ==================== 玩家临时数据 tmpdata ====================

    /**
     * 设置玩家临时数据（带到期时间）。
     *
     * @param expireAtMillis 到期毫秒时间戳；0 表示永不过期。
     */
    fun setPlayerTmpData(playerUuid: UUID, key: String, value: String, expireAtMillis: Long) {
        connection.use { conn ->
            val currentTime = System.currentTimeMillis()
            val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
            val isMySQL = dbType.equals("mysql", ignoreCase = true)

            val sql = if (isMySQL) {
                """
                INSERT INTO player_tmpdata (player_uuid, data_key, data_value, expire_at, update_time)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    data_value = VALUES(data_value),
                    expire_at = VALUES(expire_at),
                    update_time = VALUES(update_time)
            """.trimIndent()
            } else {
                """
                INSERT INTO player_tmpdata (player_uuid, data_key, data_value, expire_at, update_time)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(player_uuid, data_key) DO UPDATE SET
                    data_value = excluded.data_value,
                    expire_at = excluded.expire_at,
                    update_time = excluded.update_time
            """.trimIndent()
            }

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.setString(3, value)
                stmt.setLong(4, expireAtMillis)
                stmt.setLong(5, currentTime)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 获取玩家临时数据。
     *
     * 读取时惰性过期：到期时间已过的行返回 null，但不在读路径写库（物理删除交给周期清理）。
     */
    fun getPlayerTmpData(playerUuid: UUID, key: String): String? {
        connection.use { conn ->
            val now = System.currentTimeMillis()
            val sql = "SELECT data_value, expire_at FROM player_tmpdata WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val expireAt = rs.getLong("expire_at")
                        if (expireAt > 0 && expireAt <= now) return null
                        return rs.getString("data_value")
                    }
                    return null
                }
            }
        }
    }

    /**
     * 获取玩家临时数据的到期时间戳。
     *
     * 不存在或已过期的键返回 null，方便剩余时间计算与条件判断。
     */
    fun getPlayerTmpDataExpireAt(playerUuid: UUID, key: String): Long? {
        connection.use { conn ->
            val now = System.currentTimeMillis()
            val sql = "SELECT expire_at FROM player_tmpdata WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val expireAt = rs.getLong("expire_at")
                        if (expireAt > 0 && expireAt <= now) return null
                        return expireAt
                    }
                    return null
                }
            }
        }
    }

    /**
     * 修改玩家临时数据的数值（加或减）。
     *
     * - 键有效存在：只改动数值；提供了 [ttlMillis] 时同时重置到期时间（续期），否则保留原到期时间。
     * - 键不存在或已过期：提供 [ttlMillis] 时按新键写入（值为 delta），否则跳过并告警。
     *
     * @param ttlMillis 到期持续时间（毫秒）；null 表示不重置到期时间。
     */
    fun modifyPlayerTmpData(playerUuid: UUID, key: String, delta: String, ttlMillis: Long?) {
        val numDelta = delta.toDoubleOrNull()
        if (numDelta == null) {
            plugin.logger.warning("玩家临时数据修改失败: 变化量 '$delta' 不是数字")
            return
        }

        connection.use { conn ->
            val now = System.currentTimeMillis()
            val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
            val isMySQL = dbType.equals("mysql", ignoreCase = true)

            val selectSql = "SELECT data_value, expire_at FROM player_tmpdata WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(selectSql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val expireAt = rs.getLong("expire_at")
                        if (expireAt > 0 && expireAt <= now) {
                            // 已过期视为不存在，走新建分支
                            insertMissingTmpData(conn, isMySQL, playerUuid, key, numDelta, ttlMillis, now)
                            return
                        }
                        val currentValue = rs.getString("data_value")
                        val currentNum = currentValue.toDoubleOrNull()
                        if (currentNum == null) {
                            plugin.logger.warning("玩家临时数据修改失败: 键 '$key' 的当前值 '$currentValue' 不是数字")
                            return
                        }
                        val newValueDouble = currentNum + numDelta
                        val newValue = formatNumber(newValueDouble)
                        val newExpireAt = if (ttlMillis != null) {
                            saturatingAdd(now, ttlMillis)
                        } else {
                            expireAt
                        }
                        val updateSql = "UPDATE player_tmpdata SET data_value = ?, expire_at = ?, update_time = ? WHERE player_uuid = ? AND data_key = ?"
                        conn.prepareStatement(updateSql).use { updateStmt ->
                            updateStmt.setString(1, newValue)
                            updateStmt.setLong(2, newExpireAt)
                            updateStmt.setLong(3, now)
                            updateStmt.setString(4, playerUuid.toString())
                            updateStmt.setString(5, key)
                            updateStmt.executeUpdate()
                        }
                    } else {
                        insertMissingTmpData(conn, isMySQL, playerUuid, key, numDelta, ttlMillis, now)
                    }
                }
            }
        }
    }

    /** modifyPlayerTmpData 内部：在键缺失或已过期时按新键写入（无 ttl 则跳过并告警）。 */
    private fun insertMissingTmpData(
        conn: Connection,
        isMySQL: Boolean,
        playerUuid: UUID,
        key: String,
        delta: Double,
        ttlMillis: Long?,
        now: Long
    ) {
        if (ttlMillis == null) {
            plugin.logger.warning("玩家临时数据修改失败: 键 '$key' 不存在或已过期，add/take 需要 ttl 参数")
            return
        }
        val insertValue = formatNumber(delta)
        val expireAt = saturatingAdd(now, ttlMillis)
        val sql = if (isMySQL) {
            """
            INSERT INTO player_tmpdata (player_uuid, data_key, data_value, expire_at, update_time)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                data_value = VALUES(data_value),
                expire_at = VALUES(expire_at),
                update_time = VALUES(update_time)
        """.trimIndent()
        } else {
            """
            INSERT INTO player_tmpdata (player_uuid, data_key, data_value, expire_at, update_time)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, data_key) DO UPDATE SET
                data_value = excluded.data_value,
                expire_at = excluded.expire_at,
                update_time = excluded.update_time
        """.trimIndent()
        }
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, key)
            stmt.setString(3, insertValue)
            stmt.setLong(4, expireAt)
            stmt.setLong(5, now)
            stmt.executeUpdate()
        }
    }

    /**
     * 平移玩家临时数据的到期时间（加时间或扣时间）。
     *
     * - 键不存在或已过期：跳过并返回 false。
     * - 平移后的到期时间小于等于当前时间（扣时间溢出）：直接删除该键并返回 true。
     *
     * @param deltaMillis 平移毫秒数，正数续期、负数提前到期。
     * @return 键存在且已执行（含溢出删除）时返回 true。
     */
    fun shiftPlayerTmpDataExpiry(playerUuid: UUID, key: String, deltaMillis: Long): Boolean {
        connection.use { conn ->
            val now = System.currentTimeMillis()
            val selectSql = "SELECT expire_at FROM player_tmpdata WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(selectSql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return false
                    val expireAt = rs.getLong("expire_at")
                    if (expireAt <= 0 || expireAt <= now) {
                        deleteTmpDataOnConnection(conn, playerUuid, key)
                        return false
                    }
                    val newExpireAt = saturatingAdd(expireAt, deltaMillis)
                    if (newExpireAt <= now) {
                        deleteTmpDataOnConnection(conn, playerUuid, key)
                        return true
                    }
                    val updateSql = "UPDATE player_tmpdata SET expire_at = ?, update_time = ? WHERE player_uuid = ? AND data_key = ?"
                    conn.prepareStatement(updateSql).use { updateStmt ->
                        updateStmt.setLong(1, newExpireAt)
                        updateStmt.setLong(2, now)
                        updateStmt.setString(3, playerUuid.toString())
                        updateStmt.setString(4, key)
                        updateStmt.executeUpdate()
                    }
                    return true
                }
            }
        }
    }

    /**
     * 重置玩家临时数据的到期时间（绝对续期）。
     *
     * 键不存在或已过期时跳过并返回 false。
     */
    fun refreshPlayerTmpDataExpiry(playerUuid: UUID, key: String, ttlMillis: Long): Boolean {
        connection.use { conn ->
            val now = System.currentTimeMillis()
            val selectSql = "SELECT expire_at FROM player_tmpdata WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(selectSql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.executeQuery().use { rs ->
                    if (!rs.next()) return false
                    val expireAt = rs.getLong("expire_at")
                    if (expireAt <= 0 || expireAt <= now) {
                        deleteTmpDataOnConnection(conn, playerUuid, key)
                        return false
                    }
                    val newExpireAt = saturatingAdd(now, ttlMillis)
                    val updateSql = "UPDATE player_tmpdata SET expire_at = ?, update_time = ? WHERE player_uuid = ? AND data_key = ?"
                    conn.prepareStatement(updateSql).use { updateStmt ->
                        updateStmt.setLong(1, newExpireAt)
                        updateStmt.setLong(2, now)
                        updateStmt.setString(3, playerUuid.toString())
                        updateStmt.setString(4, key)
                        updateStmt.executeUpdate()
                    }
                    return true
                }
            }
        }
    }

    /**
     * 删除玩家临时数据。
     */
    fun deletePlayerTmpData(playerUuid: UUID, key: String): Boolean {
        connection.use { conn ->
            return deleteTmpDataOnConnection(conn, playerUuid, key)
        }
    }

    /** 在当前连接上删除一条玩家临时数据。 */
    private fun deleteTmpDataOnConnection(conn: Connection, playerUuid: UUID, key: String): Boolean {
        val sql = "DELETE FROM player_tmpdata WHERE player_uuid = ? AND data_key = ?"
        return conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, playerUuid.toString())
            stmt.setString(2, key)
            stmt.executeUpdate() > 0
        }
    }

    /**
     * 批量清理已过期的玩家临时数据。
     *
     * 由周期任务调用；过期行会在此被物理删除，避免表膨胀。
     */
    fun purgeExpiredTmpData(): Int {
        connection.use { conn ->
            val now = System.currentTimeMillis()
            val sql = "DELETE FROM player_tmpdata WHERE expire_at > 0 AND expire_at <= ?"
            return conn.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, now)
                stmt.executeUpdate()
            }
        }
    }

    /** 把数字格式化为去尾 .0 的字符串（与 modifyPlayerData 保持一致）。 */
    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    /** 饱和加法，避免到期时间戳在续期/扣期时溢出 Long。 */
    private fun saturatingAdd(base: Long, delta: Long): Long {
        return when {
            delta > 0 && Long.MAX_VALUE - base < delta -> Long.MAX_VALUE
            delta < 0 && Long.MIN_VALUE - base > delta -> Long.MIN_VALUE
            else -> base + delta
        }
    }

    fun getPlayerList(playerUuid: UUID, key: String): List<String> {
        return decodeStringList(getPlayerData(playerUuid, key))
    }

    fun getPlayerListJson(playerUuid: UUID, key: String): String {
        return encodeStringList(getPlayerList(playerUuid, key))
    }

    fun setPlayerList(playerUuid: UUID, key: String, values: List<String>) {
        setPlayerData(playerUuid, key, encodeStringList(values))
    }

    fun addPlayerListValues(playerUuid: UUID, key: String, values: List<String>, unique: Boolean = true) {
        if (values.isEmpty()) return
        synchronized(listMutationLock) {
            val current = getPlayerList(playerUuid, key).toMutableList()
            values.forEach { value ->
                if (!unique || !current.contains(value)) {
                    current.add(value)
                }
            }
            setPlayerList(playerUuid, key, current)
        }
    }

    fun removePlayerListValues(playerUuid: UUID, key: String, values: List<String>) {
        if (values.isEmpty()) return
        synchronized(listMutationLock) {
            val removing = values.toSet()
            val current = getPlayerList(playerUuid, key).filterNot { it in removing }
            setPlayerList(playerUuid, key, current)
        }
    }

    fun clearPlayerList(playerUuid: UUID, key: String) {
        setPlayerList(playerUuid, key, emptyList())
    }

    /**
     * 修改玩家数据（增加或减少数值）
     * @param playerUuid 玩家 UUID
     * @param key 数据键
     * @param delta 变化量（正数增加，负数减少，String 类型）
     */
    fun modifyPlayerData(playerUuid: UUID, key: String, delta: String) {
        val numDelta = delta.toDoubleOrNull()
        if (numDelta == null) {
            plugin.logger.warning("玩家数据修改失败: 变化量 '$delta' 不是数字")
            return
        }

        connection.use { conn ->
            // 获取当前值
            val sql = "SELECT data_value FROM player_data WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val currentValue = rs.getString("data_value")
                        val currentNum = currentValue.toDoubleOrNull()
                        if (currentNum != null) {
                            // 当前值是数字，可以进行加减操作
                            val newValueDouble = currentNum + numDelta
                            // 如果结果是整数，去掉小数点后的 .0
                            val newValue = if (newValueDouble == newValueDouble.toLong().toDouble()) {
                                newValueDouble.toLong().toString()
                            } else {
                                newValueDouble.toString()
                            }
                            // 直接执行更新，而不是调用 setPlayerData
                            val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
                            val isMySQL = dbType.equals("mysql", ignoreCase = true)

                            val updateSql = if (isMySQL) {
                                """
                                UPDATE player_data
                                SET data_value = ?, update_time = ?
                                WHERE player_uuid = ? AND data_key = ?
                            """.trimIndent()
                            } else {
                                """
                                UPDATE player_data
                                SET data_value = ?, update_time = ?
                                WHERE player_uuid = ? AND data_key = ?
                            """.trimIndent()
                            }

                            conn.prepareStatement(updateSql).use { updateStmt ->
                                val currentTime = System.currentTimeMillis()
                                updateStmt.setString(1, newValue)
                                updateStmt.setLong(2, currentTime)
                                updateStmt.setString(3, playerUuid.toString())
                                updateStmt.setString(4, key)
                                updateStmt.executeUpdate()
                            }
                        } else {
                            // 当前值不是数字，无法进行加减操作
                            plugin.logger.warning("玩家数据修改失败: 键 '$key' 的当前值 '$currentValue' 不是数字")
                        }
                    } else {
                        // 键不存在，直接插入 delta 的值
                        // 如果 delta 是整数，去掉小数点后的 .0
                        val insertValue = if (numDelta == numDelta.toLong().toDouble()) {
                            numDelta.toLong().toString()
                        } else {
                            numDelta.toString()
                        }

                        val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
                        val isMySQL = dbType.equals("mysql", ignoreCase = true)

                        val insertSql = if (isMySQL) {
                            """
                            INSERT INTO player_data (player_uuid, data_key, data_value, update_time)
                            VALUES (?, ?, ?, ?)
                        """.trimIndent()
                        } else {
                            """
                            INSERT INTO player_data (player_uuid, data_key, data_value, update_time)
                            VALUES (?, ?, ?, ?)
                        """.trimIndent()
                        }

                        conn.prepareStatement(insertSql).use { insertStmt ->
                            val currentTime = System.currentTimeMillis()
                            insertStmt.setString(1, playerUuid.toString())
                            insertStmt.setString(2, key)
                            insertStmt.setString(3, insertValue)
                            insertStmt.setLong(4, currentTime)
                            insertStmt.executeUpdate()
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置全局数据
     */
    fun setGlobalData(key: String, value: String) {
        connection.use { conn ->
            val currentTime = System.currentTimeMillis()
            val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
            val isMySQL = dbType.equals("mysql", ignoreCase = true)

            val sql = if (isMySQL) {
                """
                INSERT INTO global_data (data_key, data_value, update_time)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    data_value = VALUES(data_value),
                    update_time = VALUES(update_time)
            """.trimIndent()
            } else {
                """
                INSERT INTO global_data (data_key, data_value, update_time)
                VALUES (?, ?, ?)
                ON CONFLICT(data_key) DO UPDATE SET
                    data_value = excluded.data_value,
                    update_time = excluded.update_time
            """.trimIndent()
            }

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                stmt.setString(2, value)
                stmt.setLong(3, currentTime)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 获取全局数据
     */
    fun getGlobalData(key: String): String? {
        connection.use { conn ->
            val sql = "SELECT data_value FROM global_data WHERE data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getString("data_value") else null
                }
            }
        }
    }

    /**
     * 删除全局数据
     */
    fun deleteGlobalData(key: String): Boolean {
        connection.use { conn ->
            val sql = "DELETE FROM global_data WHERE data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                return stmt.executeUpdate() > 0
            }
        }
    }

    fun getGlobalList(key: String): List<String> {
        return decodeStringList(getGlobalData(key))
    }

    fun getGlobalListJson(key: String): String {
        return encodeStringList(getGlobalList(key))
    }

    fun setGlobalList(key: String, values: List<String>) {
        setGlobalData(key, encodeStringList(values))
    }

    fun addGlobalListValues(key: String, values: List<String>, unique: Boolean = true) {
        if (values.isEmpty()) return
        synchronized(listMutationLock) {
            val current = getGlobalList(key).toMutableList()
            values.forEach { value ->
                if (!unique || !current.contains(value)) {
                    current.add(value)
                }
            }
            setGlobalList(key, current)
        }
    }

    fun removeGlobalListValues(key: String, values: List<String>) {
        if (values.isEmpty()) return
        synchronized(listMutationLock) {
            val removing = values.toSet()
            val current = getGlobalList(key).filterNot { it in removing }
            setGlobalList(key, current)
        }
    }

    fun clearGlobalList(key: String) {
        setGlobalList(key, emptyList())
    }

    /**
     * 修改全局数据（增加或减少数值）
     * @param key 数据键
     * @param delta 变化量（正数增加，负数减少，String 类型）
     */
    fun modifyGlobalData(key: String, delta: String) {
        val numDelta = delta.toDoubleOrNull()
        if (numDelta == null) {
            plugin.logger.warning("全局数据修改失败: 变化量 '$delta' 不是数字")
            return
        }

        connection.use { conn ->
            // 获取当前值
            val sql = "SELECT data_value FROM global_data WHERE data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val currentValue = rs.getString("data_value")
                        val currentNum = currentValue.toDoubleOrNull()
                        if (currentNum != null) {
                            // 当前值是数字，可以进行加减操作
                            val newValueDouble = currentNum + numDelta
                            // 如果结果是整数，去掉小数点后的 .0
                            val newValue = if (newValueDouble == newValueDouble.toLong().toDouble()) {
                                newValueDouble.toLong().toString()
                            } else {
                                newValueDouble.toString()
                            }
                            // 直接执行更新，而不是调用 setGlobalData
                            val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
                            val isMySQL = dbType.equals("mysql", ignoreCase = true)

                            val updateSql = if (isMySQL) {
                                """
                                UPDATE global_data
                                SET data_value = ?, update_time = ?
                                WHERE data_key = ?
                            """.trimIndent()
                            } else {
                                """
                                UPDATE global_data
                                SET data_value = ?, update_time = ?
                                WHERE data_key = ?
                            """.trimIndent()
                            }

                            conn.prepareStatement(updateSql).use { updateStmt ->
                                val currentTime = System.currentTimeMillis()
                                updateStmt.setString(1, newValue)
                                updateStmt.setLong(2, currentTime)
                                updateStmt.setString(3, key)
                                updateStmt.executeUpdate()
                            }
                        } else {
                            // 当前值不是数字，无法进行加减操作
                            plugin.logger.warning("全局数据修改失败: 键 '$key' 的当前值 '$currentValue' 不是数字")
                        }
                    } else {
                        // 键不存在，直接插入 delta 的值
                        // 如果 delta 是整数，去掉小数点后的 .0
                        val insertValue = if (numDelta == numDelta.toLong().toDouble()) {
                            numDelta.toLong().toString()
                        } else {
                            numDelta.toString()
                        }

                        val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
                        val isMySQL = dbType.equals("mysql", ignoreCase = true)

                        val insertSql = if (isMySQL) {
                            """
                            INSERT INTO global_data (data_key, data_value, update_time)
                            VALUES (?, ?, ?)
                        """.trimIndent()
                        } else {
                            """
                            INSERT INTO global_data (data_key, data_value, update_time)
                            VALUES (?, ?, ?)
                        """.trimIndent()
                        }

                        conn.prepareStatement(insertSql).use { insertStmt ->
                            val currentTime = System.currentTimeMillis()
                            insertStmt.setString(1, key)
                            insertStmt.setString(2, insertValue)
                            insertStmt.setLong(3, currentTime)
                            insertStmt.executeUpdate()
                        }
                    }
                }
            }
        }
    }

    /**
     * 关闭数据源
     */
    fun close() {
        dataSource?.close()
        dataSource = null
    }

    companion object {
        fun encodeStringList(values: List<String>): String {
            return values.joinToString(prefix = "[", postfix = "]") { value ->
                buildString {
                    append('"')
                    value.forEach { char ->
                        when (char) {
                            '\\' -> append("\\\\")
                            '"' -> append("\\\"")
                            '\n' -> append("\\n")
                            '\r' -> append("\\r")
                            '\t' -> append("\\t")
                            else -> append(char)
                        }
                    }
                    append('"')
                }
            }
        }

        fun decodeStringList(raw: String?): List<String> {
            val text = raw?.trim() ?: return emptyList()
            if (text.isEmpty()) return emptyList()
            if (!text.startsWith("[") || !text.endsWith("]")) {
                return decodeLegacyList(text)
            }

            val result = mutableListOf<String>()
            var index = 1
            fun skipWhitespace() {
                while (index < text.lastIndex && text[index].isWhitespace()) index++
            }

            try {
                while (index < text.lastIndex) {
                    skipWhitespace()
                    if (index >= text.lastIndex) break

                    when (text[index]) {
                        ',' -> {
                            index++
                            continue
                        }
                        '"' -> {
                            index++
                            val value = StringBuilder()
                            while (index < text.length) {
                                val char = text[index++]
                                if (char == '"') break
                                if (char == '\\' && index < text.length) {
                                    val escaped = text[index++]
                                    when (escaped) {
                                        '"' -> value.append('"')
                                        '\\' -> value.append('\\')
                                        '/' -> value.append('/')
                                        'b' -> value.append('\b')
                                        'f' -> value.append('\u000C')
                                        'n' -> value.append('\n')
                                        'r' -> value.append('\r')
                                        't' -> value.append('\t')
                                        'u' -> {
                                            if (index + 4 <= text.length) {
                                                val hex = text.substring(index, index + 4)
                                                value.append(hex.toIntOrNull(16)?.toChar() ?: "\\u$hex")
                                                index += 4
                                            } else {
                                                value.append("\\u")
                                            }
                                        }
                                        else -> value.append(escaped)
                                    }
                                } else {
                                    value.append(char)
                                }
                            }
                            result.add(value.toString())
                        }
                        ']' -> break
                        else -> {
                            val start = index
                            while (index < text.lastIndex && text[index] != ',') index++
                            val token = text.substring(start, index).trim()
                            if (token.isNotEmpty() && token != "null") {
                                result.add(token)
                            }
                        }
                    }
                }
                return result
            } catch (_: Exception) {
                return decodeLegacyList(text)
            }
        }

        private fun decodeLegacyList(text: String): List<String> {
            return when {
                text.contains('\n') -> text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                text.contains(',') -> text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                else -> listOf(text)
            }
        }
    }
}
