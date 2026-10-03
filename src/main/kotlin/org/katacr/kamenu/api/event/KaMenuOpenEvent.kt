package org.katacr.kamenu.api.event

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * 玩家打开 KaMenu 菜单时触发。
 *
 * 该事件在菜单真正打开（容器菜单或 Dialog 菜单均可）前发出，
 * 携带菜单 ID（如 "主菜单/主菜单"）与触发方式。
 *
 * 供其他插件（如 EcoQuests）监听以驱动任务/统计等逻辑。
 * 事件在服务端主线程同步抛出，监听器可直接调用 Bukkit API。
 *
 * 注意：必须为同步事件（`Event(false)`）。触发点均在主线程，
 * 若声明为异步（`Event(true)`），Paper 会拒绝在主线程抛出：
 * `IllegalStateException: KaMenuOpenEvent may only be triggered asynchronously.`
 */
class KaMenuOpenEvent(
    val player: Player,
    val menuId: String,
    val source: Source,
    val arguments: List<String>
) : Event(false) {

    /** 菜单打开来源 */
    enum class Source {
        COMMAND,        // /km open 或自定义指令
        BUTTON,         // 菜单内按钮动作 open:
        API,            // KaMenuAPI.openMenu / openYaml / openConfig
        OTHER
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