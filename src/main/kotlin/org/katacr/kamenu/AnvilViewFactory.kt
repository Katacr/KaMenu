package org.katacr.kamenu

import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView

/**
 * 铁砧视图创建的公共反射工具。
 *
 * 从 ContainerMenuService 抽出，供容器铁砧菜单与统一输入捕获共用；
 * 通过反射调用新版本 API，保证在 Spigot 1.16.5 编译基线上可加载。
 */
object AnvilViewFactory {

    /** 打开结果：alreadyOpen 为 true 表示视图已随创建直接展示给玩家（旧 Paper 路径）。 */
    class Opened(val view: InventoryView, val alreadyOpen: Boolean)

    /** 依次尝试现代 MenuType 构建与 Paper openAnvil；都不可用时返回 null。 */
    fun openInputAnvil(player: Player, title: String): Opened? {
        createModernView(player, title)?.let { return Opened(it, false) }
        openPaperView(player, title)?.let { return Opened(it, true) }
        return null
    }

    /** 在现代 Bukkit/Paper 上通过 MenuType 构建真实铁砧视图，不静态引用新版本类。 */
    private fun createModernView(player: Player, title: String): InventoryView? {
        return runCatching {
            val menuTypeClass = Class.forName("org.bukkit.inventory.MenuType")
            val anvilType = menuTypeClass.getField("ANVIL").get(null)
            val typedMenuClass = Class.forName("org.bukkit.inventory.MenuType\$Typed")
            val builder = typedMenuClass.getMethod("builder").invoke(anvilType)
            val locationBuilderClass = Class.forName(
                "org.bukkit.inventory.view.builder.LocationInventoryViewBuilder"
            )
            val inventoryBuilderClass = Class.forName("org.bukkit.inventory.view.builder.InventoryViewBuilder")

            locationBuilderClass.getMethod("checkReachable", Boolean::class.javaPrimitiveType)
                .invoke(builder, false)

            val titleMethod = inventoryBuilderClass.methods.firstOrNull {
                it.name == "title" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
            } ?: inventoryBuilderClass.methods.firstOrNull {
                it.name == "title" && it.parameterCount == 1 &&
                    it.parameterTypes[0].name == "net.kyori.adventure.text.Component"
            }
            if (titleMethod != null) {
                val titleValue = if (titleMethod.parameterTypes[0] == String::class.java) {
                    title
                } else {
                    TextParser.parseText(title, player)
                }
                titleMethod.invoke(builder, titleValue)
            }

            inventoryBuilderClass.getMethod("build", org.bukkit.entity.HumanEntity::class.java)
                .invoke(builder, player) as InventoryView
        }.getOrNull()
    }

    /** 在没有 MenuType 的 Paper 版本上调用其扩展 openAnvil API；Spigot 缺少该方法时返回 null。 */
    private fun openPaperView(player: Player, title: String): InventoryView? {
        return runCatching {
            val method = player.javaClass.methods.first {
                it.name == "openAnvil" && it.parameterCount == 2 &&
                    it.parameterTypes[1] == Boolean::class.javaPrimitiveType
            }
            val view = method.invoke(player, null, true) as? InventoryView ?: return@runCatching null
            view.javaClass.methods.firstOrNull {
                it.name == "setTitle" && it.parameterTypes.contentEquals(arrayOf(String::class.java))
            }?.let { runCatching { it.invoke(view, title) } }
            view
        }.getOrNull()
    }
}
