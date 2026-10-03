package org.katacr.kamenu.api.event

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.Event

/**
 * KaMenu 数据（data / gdata / list / glist / meta / tmp）发生变化时触发。
 *
 * 用于监听 KaMenu 存储系统的写入、修改、增减等操作。
 * - [scope] 数据作用域（PLAYER_DATA / GLOBAL_DATA / PLAYER_LIST / GLOBAL_LIST / META / PLAYER_TMP）
 * - [action] 操作类型（SET / MODIFY / ADD / REMOVE / CLEAR）
 * - [player] 相关玩家，全局作用域或未关联玩家时为 null
 * - [key] 数据键
 * - [value] 写入/追加的新值；删除类操作可能为 null
 *
 * 供其他插件（如 EcoQuests）监听以驱动任务/统计等逻辑。
 *
 * 注意：必须为同步事件（`Event(false)`）。
 * 触发点通过 `KaScheduler.runGlobal` 回到主线程后抛出，若声明为异步（`Event(true)`），
 * Paper 会拒绝在主线程抛出：`IllegalStateException: ... may only be triggered asynchronously.`
 */
class KaMenuDataChangeEvent(
    val scope: Scope,
    val action: Action,
    val player: Player?,
    val key: String,
    val value: String?
) : Event(false) {

    enum class Scope {
        PLAYER_DATA,    // data
        GLOBAL_DATA,    // gdata
        PLAYER_LIST,    // list
        GLOBAL_LIST,    // glist
        META,           // meta
        PLAYER_TMP      // 临时数据
    }

    enum class Action {
        SET,            // 覆盖写入
        MODIFY,         // 数值增减
        ADD,            // 列表追加
        REMOVE,         // 列表移除 / 删除键
        CLEAR           // 清空
    }

    override fun getHandlers(): HandlerList = HANDLERS

    companion object {
        /**
         * 事件处理器列表。
         *
         * 注意：属性名**不能**叫 `handlers`。
         * 若配合 `@JvmStatic` 会生成静态 `getHandlers()`，与 `override fun getHandlers()`
         * 的 JVM 签名相同，编译器会合并二者，导致 `getHandlers()` 调用自身而无限递归
         * （`StackOverflowError`）。
         */
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }
}