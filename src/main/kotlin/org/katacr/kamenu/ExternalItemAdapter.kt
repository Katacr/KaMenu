package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method

/**
 * 可选资源包插件的自定义物品适配器。
 *
 * 使用反射保持 ItemsAdder、Oraxen、CraftEngine 都是软依赖，并统一处理自定义物品的
 * 创建、ID 识别和匹配。配置 ID 必须带插件前缀，避免多个插件使用相同命名空间时误匹配。
 */
object ExternalItemAdapter {
    private val providers = listOf(ItemsAdderProvider, OraxenProvider, CraftEngineProvider)

    /** 根据带插件前缀的 ID 创建自定义物品；非外部物品格式或解析失败时返回 null。 */
    fun create(rawId: String, amount: Int = 1, player: Player? = null): ItemStack? {
        val itemId = parseId(rawId) ?: return null
        return itemId.provider.create(itemId.value, player)?.also {
            it.amount = amount.coerceAtLeast(1)
        }
    }

    /** 判断物品是否属于配置的外部物品 ID。 */
    fun matches(item: ItemStack, rawId: String): Boolean {
        val itemId = parseId(rawId) ?: return false
        return itemId.provider.idOf(item)?.equals(itemId.value, ignoreCase = true) == true
    }

    /** 判断配置值是否使用受支持的外部插件前缀。 */
    fun isExternalId(rawId: String): Boolean = parseId(rawId) != null

    /** 返回规范化的外部物品 ID，例如 `itemsadder:namespace:item`。 */
    fun identify(item: ItemStack): String? {
        for (provider in providers) {
            val id = provider.idOf(item)
            if (!id.isNullOrBlank()) return "${provider.prefix}:$id"
        }
        return null
    }

    /** 返回物品所属插件名称；原版物品返回 null。 */
    fun providerName(item: ItemStack): String? {
        return providers.firstOrNull { !it.idOf(item).isNullOrBlank() }?.pluginName
    }

    /** 返回插件自身使用的物品 ID，不包含 KaMenu 的提供方前缀。 */
    fun nativeId(item: ItemStack): String? {
        for (provider in providers) {
            val id = provider.idOf(item)
            if (!id.isNullOrBlank()) return id
        }
        return null
    }

    /** 解析显式插件前缀，支持 `ia:` 与 `ce:` 短别名。 */
    private fun parseId(rawId: String): ExternalItemId? {
        val trimmed = rawId.trim()
        val separator = trimmed.indexOf(':')
        if (separator <= 0 || separator == trimmed.lastIndex) return null

        val provider = when (trimmed.substring(0, separator).lowercase()) {
            "itemsadder", "ia" -> ItemsAdderProvider
            "oraxen" -> OraxenProvider
            "craftengine", "ce" -> CraftEngineProvider
            else -> null
        } ?: return null
        return ExternalItemId(provider, trimmed.substring(separator + 1).trim())
            .takeIf { it.value.isNotEmpty() }
    }

    private data class ExternalItemId(val provider: Provider, val value: String)

    private interface Provider {
        val pluginName: String
        val prefix: String
        fun create(id: String, player: Player?): ItemStack?
        fun idOf(item: ItemStack): String?
    }

    /** 获取已启用插件，避免在插件缺失或禁用期间触发其 API。 */
    private fun enabledPlugin(name: String): Plugin? {
        return Bukkit.getPluginManager().getPlugin(name)?.takeIf(Plugin::isEnabled)
    }

    private object ItemsAdderProvider : Provider {
        override val pluginName = "ItemsAdder"
        override val prefix = "itemsadder"
        private const val CUSTOM_STACK_CLASS = "dev.lone.itemsadder.api.CustomStack"

        private var classLoader: ClassLoader? = null
        private var byIdMethod: Method? = null
        private var byItemMethod: Method? = null
        private var itemStackMethod: Method? = null
        private var namespacedIdMethod: Method? = null

        override fun create(id: String, player: Player?): ItemStack? = invokeSafely {
            val plugin = enabledPlugin(pluginName) ?: return@invokeSafely null
            resolve(plugin)
            val customStack = byIdMethod?.invoke(null, id) ?: return@invokeSafely null
            (itemStackMethod?.invoke(customStack) as? ItemStack)?.clone()
        }

        override fun idOf(item: ItemStack): String? = invokeSafely {
            val plugin = enabledPlugin(pluginName) ?: return@invokeSafely null
            resolve(plugin)
            val customStack = byItemMethod?.invoke(null, item) ?: return@invokeSafely null
            namespacedIdMethod?.invoke(customStack) as? String
        }

        /** 查找并缓存 ItemsAdder CustomStack API。 */
        @Synchronized
        private fun resolve(plugin: Plugin) {
            val loader = plugin.javaClass.classLoader
            if (classLoader === loader) return
            val customStackClass = loader.loadClass(CUSTOM_STACK_CLASS)
            byIdMethod = customStackClass.getMethod("getInstance", String::class.java)
            byItemMethod = try {
                customStackClass.getMethod("byItemStack", ItemStack::class.java)
            } catch (_: NoSuchMethodException) {
                customStackClass.getMethod("getInstance", ItemStack::class.java)
            }
            itemStackMethod = customStackClass.getMethod("getItemStack")
            namespacedIdMethod = customStackClass.getMethod("getNamespacedID")
            classLoader = loader
        }
    }

    private object OraxenProvider : Provider {
        override val pluginName = "Oraxen"
        override val prefix = "oraxen"
        private const val ORAXEN_ITEMS_CLASS = "io.th0rgal.oraxen.api.OraxenItems"

        private var classLoader: ClassLoader? = null
        private var byIdMethod: Method? = null
        private var idByItemMethod: Method? = null
        private var buildMethod: Method? = null

        override fun create(id: String, player: Player?): ItemStack? = invokeSafely {
            val plugin = enabledPlugin(pluginName) ?: return@invokeSafely null
            resolve(plugin)
            val builder = byIdMethod?.invoke(null, id) ?: return@invokeSafely null
            (buildMethod?.invoke(builder) as? ItemStack)?.clone()
        }

        override fun idOf(item: ItemStack): String? = invokeSafely {
            val plugin = enabledPlugin(pluginName) ?: return@invokeSafely null
            resolve(plugin)
            idByItemMethod?.invoke(null, item) as? String
        }

        /** 查找并缓存 OraxenItems 与 ItemBuilder API。 */
        @Synchronized
        private fun resolve(plugin: Plugin) {
            val loader = plugin.javaClass.classLoader
            if (classLoader === loader) return
            val itemsClass = loader.loadClass(ORAXEN_ITEMS_CLASS)
            byIdMethod = itemsClass.getMethod("getItemById", String::class.java)
            idByItemMethod = itemsClass.getMethod("getIdByItem", ItemStack::class.java)
            buildMethod = byIdMethod!!.returnType.getMethod("build")
            classLoader = loader
        }
    }

    private object CraftEngineProvider : Provider {
        override val pluginName = "CraftEngine"
        override val prefix = "craftengine"
        private const val CRAFT_ENGINE_ITEMS_CLASS = "net.momirealms.craftengine.bukkit.api.CraftEngineItems"

        private var classLoader: ClassLoader? = null
        private var byIdMethod: Method? = null
        private var idByItemMethod: Method? = null
        private var buildWithPlayerMethod: Method? = null
        private var buildMethod: Method? = null

        override fun create(id: String, player: Player?): ItemStack? = invokeSafely {
            val plugin = enabledPlugin(pluginName) ?: return@invokeSafely null
            resolve(plugin)
            val definition = byIdMethod?.invoke(null, id) ?: return@invokeSafely null
            val result = if (player != null && buildWithPlayerMethod != null) {
                buildWithPlayerMethod?.invoke(definition, player)
            } else {
                buildMethod?.invoke(definition)
            }
            (result as? ItemStack)?.clone()
        }

        override fun idOf(item: ItemStack): String? = invokeSafely {
            val plugin = enabledPlugin(pluginName) ?: return@invokeSafely null
            resolve(plugin)
            idByItemMethod?.invoke(null, item)?.toString()
        }

        /** 查找并缓存 CraftEngineItems 与 BukkitItemDefinition API。 */
        @Synchronized
        private fun resolve(plugin: Plugin) {
            val loader = plugin.javaClass.classLoader
            if (classLoader === loader) return
            val itemsClass = loader.loadClass(CRAFT_ENGINE_ITEMS_CLASS)
            byIdMethod = itemsClass.getMethod("byId", String::class.java)
            idByItemMethod = itemsClass.getMethod("getCustomItemId", ItemStack::class.java)
            val definitionClass = byIdMethod!!.returnType
            buildWithPlayerMethod = definitionClass.getMethod("buildBukkitItem", Player::class.java)
            buildMethod = definitionClass.getMethod("buildBukkitItem")
            classLoader = loader
        }
    }

    /** 将可选插件的反射/API 异常降级为未匹配，避免影响菜单主流程。 */
    private inline fun <T> invokeSafely(block: () -> T?): T? {
        return try {
            block()
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: LinkageError) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }
}
