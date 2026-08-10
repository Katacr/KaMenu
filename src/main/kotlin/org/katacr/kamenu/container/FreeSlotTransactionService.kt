@file:Suppress("DEPRECATION")

package org.katacr.kamenu.container

import org.bukkit.Material
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.katacr.kamenu.ConditionUtils
import org.katacr.kamenu.KaMenu
import org.katacr.kamenu.KaScheduler
import org.katacr.kamenu.MenuActions
import org.katacr.kamenu.SerializationUtil
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 执行自由槽位点击、Shift 和拖拽事务。
 *
 * 所有 Bukkit 快照和库存修改均发生在玩家所属线程；数据库写入由 [FreeSlotRecoveryStore] 异步完成。
 */
class FreeSlotTransactionService(
    private val plugin: KaMenu,
    private val recoveryStore: FreeSlotRecoveryStore
) {
    data class SessionContext(
        val sessionId: UUID,
        val playerId: UUID,
        val menuId: String,
        val generation: Long,
        val config: YamlConfiguration,
        val freeSlots: ContainerFreeSlotsDefinition,
        val inventory: Inventory,
        val transactionRunning: AtomicBoolean
    )

    data class ClickRequest(
        val rawSlot: Int,
        val localSlot: Int,
        val clickedTop: Boolean,
        val actionName: String,
        val rightClick: Boolean?,
        val currentItem: ItemStack?
    )

    data class DragRequest(
        val oldCursor: ItemStack?,
        val newCursor: ItemStack?,
        val oldItems: Map<Int, ItemStack?>,
        val newItems: Map<Int, ItemStack>
    )

    enum class Result {
        NOT_HANDLED,
        HANDLED,
        ALLOW_VANILLA
    }

    /** 处理一次库存点击；普通按钮和无自由槽菜单返回 NOT_HANDLED。 */
    fun handleClick(
        player: Player,
        session: SessionContext,
        request: ClickRequest,
        isCurrent: () -> Boolean,
        onChanged: () -> Unit
    ): Result {
        if (session.freeSlots.byId.isEmpty()) return Result.NOT_HANDLED
        if (session.transactionRunning.get()) return Result.HANDLED

        if (!request.clickedTop) {
            return if (request.actionName == "MOVE_TO_OTHER_INVENTORY") {
                handleShiftIntoFreeSlot(player, session, request, isCurrent, onChanged)
                Result.HANDLED
            } else if (request.actionName in SAFE_BOTTOM_ACTIONS) {
                Result.ALLOW_VANILLA
            } else {
                Result.HANDLED
            }
        }

        val freeSlot = session.freeSlots.at(request.rawSlot) ?: return Result.NOT_HANDLED
        if (request.actionName == "MOVE_TO_OTHER_INVENTORY") {
            handleShiftOutOfFreeSlot(player, session, freeSlot, request, isCurrent, onChanged)
            return Result.HANDLED
        }

        val plan = planLiveCursorClick(
            session.inventory.getItem(request.rawSlot),
            player.itemOnCursor,
            request.rightClick
        ) ?: return Result.HANDLED
        val variables = operationVariables(session, freeSlot, request.rawSlot, plan)
        if (plan.place && !allows(player, session, freeSlot.place, variables)) {
            executeEvent(player, session, freeSlot.events.denyPlace, variables)
            return Result.HANDLED
        }
        if (plan.take && !allows(player, session, freeSlot.take, variables)) {
            executeEvent(player, session, freeSlot.events.denyTake, variables)
            return Result.HANDLED
        }

        val commit = commit@{
            if (!sameItem(session.inventory.getItem(request.rawSlot), plan.topBefore) ||
                !sameItem(player.itemOnCursor, plan.cursorBefore)
            ) {
                return@commit false
            } else {
                session.inventory.setItem(request.rawSlot, plan.topAfter?.clone())
                player.setItemOnCursor(plan.cursorAfter?.clone())
                true
            }
        }
        submitTransaction(
            player,
            session,
            affectedSlots = setOf(request.rawSlot),
            postItems = mapOf(request.rawSlot to plan.topAfter),
            persistBeforeCommit = plan.place,
            isCurrent = isCurrent,
            commit = commit,
            afterCommit = {
                completeTransaction(
                    player,
                    session,
                    request.rawSlot,
                    plan,
                    freeSlot,
                    onChanged
                )
            }
        )
        return Result.HANDLED
    }

    /** 执行 `free-slot` 消费、主动返还或刷新动作；返回 true 时中断后续动作链。 */
    fun executeAction(
        player: Player,
        session: SessionContext,
        rawArguments: String,
        isCurrent: () -> Boolean,
        onChanged: () -> Unit
    ): CompletableFuture<Boolean> {
        val arguments = FreeSlotActionParser.arguments(rawArguments)
        return when (arguments["type"]?.lowercase()) {
            "consume" -> executeConsumeAction(player, session, arguments, isCurrent, onChanged)
            "return" -> executeReturnAction(player, session, arguments, isCurrent, onChanged)
            "refresh" -> runPlayerFuture(player) {
                if (!isCurrent()) return@runPlayerFuture true
                val id = arguments["id"]?.takeUnless { it == "*" }
                if (id != null && id !in session.freeSlots.byId) {
                    warnInvalidAction(session, "unknown free-slot id '$id'")
                    true
                } else {
                    onChanged()
                    false
                }
            }
            else -> {
                warnInvalidAction(session, "missing or unsupported type")
                CompletableFuture.completedFuture(true)
            }
        }
    }

    /** 规划并原子扣除单个或多个逻辑自由槽中的指定数量。 */
    private fun executeConsumeAction(
        player: Player,
        session: SessionContext,
        arguments: Map<String, String>,
        isCurrent: () -> Boolean,
        onChanged: () -> Unit
    ): CompletableFuture<Boolean> {
        val requirements = FreeSlotActionParser.consumeRequirements(arguments)
        if (requirements == null) {
            warnInvalidAction(session, "consume requires positive id/amount or items=id:amount,...")
            return CompletableFuture.completedFuture(true)
        }
        val unknownId = requirements.keys.firstOrNull { it !in session.freeSlots.byId }
        if (unknownId != null) {
            warnInvalidAction(session, "unknown free-slot id '$unknownId'")
            return CompletableFuture.completedFuture(true)
        }
        return submitActionMutation(player, session, isCurrent, onChanged) {
            val postItems = linkedMapOf<Int, ItemStack?>()
            requirements.forEach { (id, requiredAmount) ->
                val freeSlot = session.freeSlots.byId[id] ?: return@submitActionMutation null
                var remaining = requiredAmount
                freeSlot.slots.forEach { slot ->
                    val before = present(session.inventory.getItem(slot)) ?: return@forEach
                    if (remaining <= 0) return@forEach
                    val taken = minOf(before.amount, remaining)
                    postItems[slot] = before.clone().apply { amount = before.amount - taken }
                        .takeIf { it.amount > 0 }
                    remaining -= taken
                }
                if (remaining > 0) return@submitActionMutation null
            }
            ActionMutation(postItems) {
                postItems.forEach { (slot, item) -> session.inventory.setItem(slot, item?.clone()) }
            }
        }
    }

    /** 在背包能完整接收时主动返还一个或全部逻辑自由槽。 */
    private fun executeReturnAction(
        player: Player,
        session: SessionContext,
        arguments: Map<String, String>,
        isCurrent: () -> Boolean,
        onChanged: () -> Unit
    ): CompletableFuture<Boolean> {
        val rawId = arguments["id"] ?: "*"
        val groups = if (rawId == "*") {
            session.freeSlots.byId.values.toList()
        } else {
            listOf(session.freeSlots.byId[rawId] ?: run {
                warnInvalidAction(session, "unknown free-slot id '$rawId'")
                return CompletableFuture.completedFuture(true)
            })
        }
        return submitActionMutation(player, session, isCurrent, onChanged) {
            val storageAfter = cloneItems(player.inventory.storageContents)
            val postItems = linkedMapOf<Int, ItemStack?>()
            groups.flatMap { it.slots }.forEach { slot ->
                val item = present(session.inventory.getItem(slot)) ?: return@forEach
                if (addToStorage(storageAfter, item) != null) return@submitActionMutation null
                postItems[slot] = null
            }
            ActionMutation(postItems) {
                player.inventory.storageContents = storageAfter
                postItems.keys.forEach { session.inventory.setItem(it, null) }
            }
        }
    }

    /** 串行提交动作触发的库存变化，并在托管快照完成后恢复动作链。 */
    private fun submitActionMutation(
        player: Player,
        session: SessionContext,
        isCurrent: () -> Boolean,
        onChanged: () -> Unit,
        planner: () -> ActionMutation?
    ): CompletableFuture<Boolean> {
        if (!session.transactionRunning.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(true)
        }
        val result = CompletableFuture<Boolean>()
        runPlayerFuture(player) {
            if (!isCurrent()) return@runPlayerFuture null
            val mutation = planner() ?: return@runPlayerFuture null
            mutation.commit()
            onChanged()
            player.updateInventory()
            mutation
        }.thenCompose { mutation ->
            if (mutation == null) {
                session.transactionRunning.set(false)
                result.complete(true)
                return@thenCompose CompletableFuture.completedFuture(Unit)
            }
            replaceSnapshot(session, mutation.postItems.keys, mutation.postItems)
                .thenCompose { recoveryStore.markHeldAsync(session.sessionId, mutation.postItems.keys) }
                .whenComplete { _, error ->
                session.transactionRunning.set(false)
                if (error != null) {
                    plugin.logger.severe("Free-slot action failed for ${session.menuId}: ${error.message}")
                    result.complete(true)
                } else {
                    result.complete(false)
                }
                }
        }.exceptionally { error ->
            session.transactionRunning.set(false)
            plugin.logger.severe("Free-slot action failed for ${session.menuId}: ${error.message}")
            result.complete(true)
            null
        }
        return result
    }

    /** 输出包含菜单 ID 的自由槽动作配置警告。 */
    private fun warnInvalidAction(session: SessionContext, reason: String) {
        plugin.logger.warning("Invalid free-slot action in ${session.menuId}: $reason")
    }

    private data class ActionMutation(
        val postItems: Map<Int, ItemStack?>,
        val commit: () -> Unit
    )

    /** 处理拖拽；只影响玩家背包时允许原版处理，涉及顶部时由 KaMenu 原子提交。 */
    fun handleDrag(
        player: Player,
        session: SessionContext,
        request: DragRequest,
        isCurrent: () -> Boolean,
        onChanged: () -> Unit
    ): Result {
        if (session.freeSlots.byId.isEmpty()) return Result.NOT_HANDLED
        if (session.transactionRunning.get()) return Result.HANDLED
        val topSlots = request.newItems.keys.filter { it in 0 until session.inventory.size }.toSet()
        if (topSlots.isEmpty()) return Result.ALLOW_VANILLA
        if (topSlots.any { session.freeSlots.at(it) == null }) return Result.HANDLED

        val operationContexts = linkedMapOf<String, OperationEventContext>()
        topSlots.forEach { slot ->
            val freeSlot = session.freeSlots.at(slot) ?: return Result.HANDLED
            val before = request.oldItems[slot]
            val after = request.newItems[slot]
            val incoming = incomingDifference(before, after) ?: return@forEach
            val plan = ClickPlan(before, after, request.oldCursor, request.newCursor, incoming, before, after, true, false)
            val variables = operationVariables(session, freeSlot, slot, plan)
            if (!allows(player, session, freeSlot.place, variables)) {
                executeEvent(player, session, freeSlot.events.denyPlace, variables)
                return Result.HANDLED
            }
            operationContexts[freeSlot.id] = OperationEventContext(freeSlot, slot, plan)
        }

        val commit = commit@{
            if (!sameItem(player.itemOnCursor, request.oldCursor) || request.oldItems.any { (rawSlot, oldItem) ->
                    !sameItem(player.openInventory.getItem(rawSlot), oldItem)
                }
            ) {
                return@commit false
            }
            request.newItems.forEach { (rawSlot, item) -> player.openInventory.setItem(rawSlot, item.clone()) }
            player.setItemOnCursor(request.newCursor?.clone())
            true
        }
        submitTransaction(
            player,
            session,
            topSlots,
            topSlots.associateWith(request.newItems::get),
            persistBeforeCommit = true,
            isCurrent = isCurrent,
            commit = commit,
            afterCommit = {
                operationContexts.values.forEach { eventContext ->
                    executeEvent(
                        player,
                        session,
                        eventContext.freeSlot.events.place,
                        operationVariables(session, eventContext.freeSlot, eventContext.slot, eventContext.plan)
                    )
                }
                onChanged()
                player.updateInventory()
            }
        )
        return Result.HANDLED
    }

    /** 将玩家背包中的 Shift 来源物品放入第一个允许且有容量的自由槽组。 */
    private fun handleShiftIntoFreeSlot(
        player: Player,
        session: SessionContext,
        request: ClickRequest,
        isCurrent: () -> Boolean,
        onChanged: () -> Unit
    ) {
        val source = present(request.currentItem) ?: return
        var denied: Pair<ContainerFreeSlotDefinition, Map<String, String>>? = null
        val candidate = session.freeSlots.byId.values.firstNotNullOfOrNull { freeSlot ->
            val allocation = allocateIntoGroup(session.inventory, freeSlot, source) ?: return@firstNotNullOfOrNull null
            val plan = ClickPlan(
                topBefore = allocation.firstBefore,
                topAfter = allocation.firstAfter,
                cursorBefore = null,
                cursorAfter = null,
                incoming = source.clone().apply { amount = allocation.movedAmount },
                stored = allocation.firstBefore,
                result = allocation.firstAfter,
                place = true,
                take = false
            )
            val variables = operationVariables(session, freeSlot, allocation.changedSlots.first(), plan)
            if (allows(player, session, freeSlot.place, variables)) {
                ShiftCandidate(freeSlot, allocation, plan)
            } else {
                if (denied == null) denied = freeSlot to variables
                null
            }
        }
        if (candidate == null) {
            denied?.let { (freeSlot, variables) ->
                executeEvent(player, session, freeSlot.events.denyPlace, variables)
            }
            return
        }
        val (freeSlot, allocation, plan) = candidate
        val sourceAfter = source.clone().apply { amount = source.amount - allocation.movedAmount }.takeIf { it.amount > 0 }
        val commit = commit@{
            if (!sameItem(player.inventory.getItem(request.localSlot), source)) {
                return@commit false
            }
            allocation.afterItems.forEach { (slot, item) -> session.inventory.setItem(slot, item?.clone()) }
            player.inventory.setItem(request.localSlot, sourceAfter)
            true
        }
        submitTransaction(
            player,
            session,
            allocation.changedSlots,
            allocation.afterItems,
            persistBeforeCommit = true,
            isCurrent = isCurrent,
            commit = commit,
            afterCommit = {
                executeEvent(
                    player,
                    session,
                    freeSlot.events.place,
                    operationVariables(session, freeSlot, allocation.changedSlots.first(), plan)
                )
                onChanged()
                player.updateInventory()
            }
        )
    }

    /** 将自由槽物品尽可能 Shift 转移到玩家存储栏。 */
    private fun handleShiftOutOfFreeSlot(
        player: Player,
        session: SessionContext,
        freeSlot: ContainerFreeSlotDefinition,
        request: ClickRequest,
        isCurrent: () -> Boolean,
        onChanged: () -> Unit
    ) {
        val source = present(request.currentItem) ?: return
        val storageBefore = cloneItems(player.inventory.storageContents)
        val storageAfter = cloneItems(storageBefore)
        val leftover = addToStorage(storageAfter, source)
        val moved = source.amount - (leftover?.amount ?: 0)
        if (moved <= 0) return
        val topAfter = leftover?.clone()
        val plan = ClickPlan(source, topAfter, null, null, null, source, topAfter, false, true)
        val variables = operationVariables(session, freeSlot, request.rawSlot, plan)
        if (!allows(player, session, freeSlot.take, variables)) {
            executeEvent(player, session, freeSlot.events.denyTake, variables)
            return
        }

        val commit = commit@{
            if (!sameItem(session.inventory.getItem(request.rawSlot), source) ||
                !sameItems(player.inventory.storageContents, storageBefore)
            ) {
                return@commit false
            }
            player.inventory.storageContents = storageAfter
            session.inventory.setItem(request.rawSlot, topAfter)
            true
        }
        submitTransaction(
            player,
            session,
            setOf(request.rawSlot),
            mapOf(request.rawSlot to topAfter),
            persistBeforeCommit = false,
            isCurrent = isCurrent,
            commit = commit,
            afterCommit = {
                executeEvent(
                    player,
                    session,
                    freeSlot.events.take,
                    operationVariables(session, freeSlot, request.rawSlot, plan)
                )
                onChanged()
                player.updateInventory()
            }
        )
    }

    /** 编排预写托管、同步库存提交、最终 HELD 状态和成功事件。 */
    private fun submitTransaction(
        player: Player,
        session: SessionContext,
        affectedSlots: Set<Int>,
        postItems: Map<Int, ItemStack?>,
        persistBeforeCommit: Boolean,
        isCurrent: () -> Boolean,
        commit: () -> Boolean,
        afterCommit: () -> Unit
    ) {
        if (!session.transactionRunning.compareAndSet(false, true)) return
        val preparedSnapshot = try {
            if (persistBeforeCommit) {
                replaceSnapshot(session, affectedSlots, postItems)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        } catch (error: Throwable) {
            session.transactionRunning.set(false)
            plugin.logger.severe("Free-slot transaction preparation failed for ${session.menuId}: ${error.message}")
            player.updateInventory()
            return
        }

        // 库存事件线程中立即写入最终状态，使取消事件的客户端校正包直接携带结果，避免先回滚再播放一次。
        val commitFuture = if (KaScheduler.isPlayerThread(player)) {
            // Bukkit-compatible cores need the final cursor state before the
            // cancelled click returns; persistence continues in the future chain.
            try {
                CompletableFuture.completedFuture(if (!isCurrent()) false else commit())
            } catch (error: Throwable) {
                CompletableFuture<Boolean>().apply { completeExceptionally(error) }
            }
        } else {
            val commitReady = if (persistBeforeCommit) {
                preparedSnapshot
            } else {
                CompletableFuture.completedFuture(Unit)
            }
            commitReady.thenCompose {
                runPlayerFuture(player) {
                    if (!isCurrent()) false else commit()
                }
            }
        }
        commitFuture.thenCompose { committed ->
            val snapshot = if (committed) {
                if (persistBeforeCommit) preparedSnapshot
                else replaceSnapshot(session, affectedSlots, postItems)
            } else {
                currentSnapshotOnPlayer(player, session, affectedSlots)
            }
            snapshot.thenCompose { recoveryStore.markHeldAsync(session.sessionId, affectedSlots) }
                .thenApply { committed }
        }.whenComplete { committed, error ->
            session.transactionRunning.set(false)
            if (error != null) {
                plugin.logger.severe("Free-slot transaction failed for ${session.menuId}: ${error.message}")
                KaScheduler.runPlayer(player, Runnable {
                    // UI 已同步提交但托管失败时关闭窗口，交由统一 Close 流程优先返还玩家资产。
                    if (isCurrent()) player.closeInventory() else player.updateInventory()
                })
            } else if (committed == true) {
                KaScheduler.runPlayer(player, Runnable {
                    if (isCurrent()) afterCommit()
                })
            }
        }
    }

    /** 使用提交后的最终会话快照执行一次 take/place 事件并刷新界面。 */
    private fun completeTransaction(
        player: Player,
        session: SessionContext,
        slot: Int,
        plan: ClickPlan,
        freeSlot: ContainerFreeSlotDefinition,
        onChanged: () -> Unit
    ) {
        val variables = operationVariables(session, freeSlot, slot, plan)
        if (plan.take) executeEvent(player, session, freeSlot.events.take, variables)
        if (plan.place) executeEvent(player, session, freeSlot.events.place, variables)
        onChanged()
        player.updateInventory()
    }

    /** 序列化并替换指定物理槽位的持久化托管快照。 */
    private fun replaceSnapshot(
        session: SessionContext,
        affectedSlots: Set<Int>,
        postItems: Map<Int, ItemStack?>
    ) = recoveryStore.replaceSlotsAsync(
        sessionId = session.sessionId,
        playerId = session.playerId,
        menuId = session.menuId,
        generation = session.generation,
        slotRecords = postItems.mapNotNull { (slot, item) ->
            present(item)?.let {
                val freeSlotId = session.freeSlots.idBySlot[slot] ?: return@let null
                slot to (freeSlotId to SerializationUtil.itemToBase64(it.clone()))
            }
        }.toMap(),
        affectedSlots = affectedSlots
    )

    private fun currentSnapshotOnPlayer(
        player: Player,
        session: SessionContext,
        affectedSlots: Set<Int>
    ): CompletableFuture<Unit> {
        return runPlayerFuture(player) {
            affectedSlots.associateWith { slot -> session.inventory.getItem(slot)?.clone() }
        }.thenCompose { currentItems -> replaceSnapshot(session, affectedSlots, currentItems) }
    }

    /** 在玩家所属线程执行库存读取或修改，并把结果接回异步事务链。 */
    private fun <T> runPlayerFuture(player: Player, action: () -> T): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        KaScheduler.runPlayer(player, Runnable {
            runCatching(action).fold(future::complete, future::completeExceptionally)
        })
        return future
    }

    /** 使用本次事务物品变量检查放入或取出规则。 */
    private fun allows(
        player: Player,
        session: SessionContext,
        rule: ContainerFreeSlotRuleDefinition,
        variables: Map<String, String>
    ): Boolean {
        return rule.enabled && ConditionUtils.checkCondition(
            player,
            rule.condition,
            variables,
            session.config
        ) { null }
    }

    /** 复用菜单动作执行器运行自由槽事务事件。 */
    private fun executeEvent(
        player: Player,
        session: SessionContext,
        actions: List<Any>,
        variables: Map<String, String>
    ) {
        if (actions.isEmpty()) return
        MenuActions.executeActionGroup(
            player,
            session.config,
            actions,
            variables,
            contextId = session.menuId
        )
    }

    /** 合并会话最终状态与本次 incoming/stored/result 快照。 */
    private fun operationVariables(
        session: SessionContext,
        freeSlot: ContainerFreeSlotDefinition,
        slot: Int,
        plan: ClickPlan
    ): Map<String, String> {
        return FreeSlotItemContext.sessionVariables(session.freeSlots, session.inventory) +
            FreeSlotItemContext.itemVariables("free:incoming", plan.incoming) +
            FreeSlotItemContext.itemVariables("free:stored", plan.stored) +
            FreeSlotItemContext.itemVariables("free:result", plan.result) +
            mapOf("free:id" to freeSlot.id, "free:slot" to slot.toString())
    }

    /** 使用 Bukkit 当前槽位与光标重放一次左右键点击，避免依赖核心事件快照。 */
    private fun planLiveCursorClick(top: ItemStack?, cursor: ItemStack?, rightClick: Boolean?): ClickPlan? {
        rightClick ?: return null
        val plan = FreeSlotCursorPlanner.plan(top, cursor, rightClick) ?: return null
        return ClickPlan(
            topBefore = plan.slotBefore,
            topAfter = plan.slotAfter,
            cursorBefore = plan.cursorBefore,
            cursorAfter = plan.cursorAfter,
            incoming = incomingDifference(plan.slotBefore, plan.slotAfter),
            stored = plan.slotBefore,
            result = plan.slotAfter,
            place = plan.place,
            take = plan.take
        )
    }

    /** 按声明顺序为 Shift 放入计算同类合并和空槽分配。 */
    private fun allocateIntoGroup(
        inventory: Inventory,
        freeSlot: ContainerFreeSlotDefinition,
        source: ItemStack
    ): GroupAllocation? {
        var remaining = source.amount
        val before = freeSlot.slots.associateWith { inventory.getItem(it)?.clone() }
        val after = before.mapValuesTo(linkedMapOf()) { (_, item) -> item?.clone() }
        freeSlot.slots.forEach { slot ->
            val current = present(after[slot]) ?: return@forEach
            if (!current.isSimilar(source)) return@forEach
            val moved = minOf(remaining, current.maxStackSize - current.amount)
            if (moved > 0) {
                current.amount += moved
                after[slot] = current
                remaining -= moved
            }
        }
        freeSlot.slots.forEach { slot ->
            if (remaining <= 0) return@forEach
            if (present(after[slot]) != null) return@forEach
            val moved = minOf(remaining, source.maxStackSize)
            after[slot] = source.clone().apply { amount = moved }
            remaining -= moved
        }
        val changed = freeSlot.slots.filter { !sameItem(before[it], after[it]) }.toSet()
        if (changed.isEmpty()) return null
        val firstChanged = changed.first()
        return GroupAllocation(
            changedSlots = changed,
            afterItems = changed.associateWith(after::get),
            movedAmount = source.amount - remaining,
            firstBefore = before[firstChanged],
            firstAfter = after[firstChanged]
        )
    }

    /** 在克隆的玩家存储栏中模拟加入物品，并返回无法容纳的剩余部分。 */
    private fun addToStorage(storage: Array<ItemStack?>, source: ItemStack): ItemStack? {
        var remaining = source.amount
        storage.indices.forEach { index ->
            val current = present(storage[index]) ?: return@forEach
            if (!current.isSimilar(source)) return@forEach
            val moved = minOf(remaining, current.maxStackSize - current.amount)
            if (moved > 0) {
                current.amount += moved
                storage[index] = current
                remaining -= moved
            }
        }
        storage.indices.forEach { index ->
            if (remaining <= 0) return@forEach
            if (present(storage[index]) != null) return@forEach
            val moved = minOf(remaining, source.maxStackSize)
            storage[index] = source.clone().apply { amount = moved }
            remaining -= moved
        }
        return source.clone().apply { amount = remaining }.takeIf { it.amount > 0 }
    }

    private fun incomingDifference(before: ItemStack?, after: ItemStack?): ItemStack? {
        val next = present(after) ?: return null
        val previous = present(before)
        if (previous != null && !previous.isSimilar(next)) return next.clone()
        val amount = next.amount - (previous?.amount ?: 0)
        return next.clone().apply { this.amount = amount }.takeIf { it.amount > 0 }
    }

    private fun cloneItems(items: Array<ItemStack?>): Array<ItemStack?> = items.map { it?.clone() }.toTypedArray()

    private fun sameItems(left: Array<ItemStack?>, right: Array<ItemStack?>): Boolean {
        return left.size == right.size && left.indices.all { sameItem(left[it], right[it]) }
    }

    private fun sameItem(left: ItemStack?, right: ItemStack?): Boolean {
        val normalizedLeft = present(left)
        val normalizedRight = present(right)
        return normalizedLeft == normalizedRight
    }

    private fun present(item: ItemStack?): ItemStack? = item?.takeIf { it.type != Material.AIR && it.amount > 0 }

    private data class ClickPlan(
        val topBefore: ItemStack?,
        val topAfter: ItemStack?,
        val cursorBefore: ItemStack?,
        val cursorAfter: ItemStack?,
        val incoming: ItemStack?,
        val stored: ItemStack?,
        val result: ItemStack?,
        val place: Boolean,
        val take: Boolean
    )

    private data class GroupAllocation(
        val changedSlots: Set<Int>,
        val afterItems: Map<Int, ItemStack?>,
        val movedAmount: Int,
        val firstBefore: ItemStack?,
        val firstAfter: ItemStack?
    )

    private data class OperationEventContext(
        val freeSlot: ContainerFreeSlotDefinition,
        val slot: Int,
        val plan: ClickPlan
    )

    private data class ShiftCandidate(
        val freeSlot: ContainerFreeSlotDefinition,
        val allocation: GroupAllocation,
        val plan: ClickPlan
    )

    private companion object {
        val SAFE_BOTTOM_ACTIONS = setOf(
            "NOTHING",
            "PICKUP_ALL",
            "PICKUP_SOME",
            "PICKUP_HALF",
            "PICKUP_ONE",
            "PLACE_ALL",
            "PLACE_SOME",
            "PLACE_ONE",
            "SWAP_WITH_CURSOR",
            "DROP_ALL_CURSOR",
            "DROP_ONE_CURSOR",
            "DROP_ALL_SLOT",
            "DROP_ONE_SLOT"
        )
    }
}
