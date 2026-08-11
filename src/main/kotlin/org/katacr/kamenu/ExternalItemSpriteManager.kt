package org.katacr.kamenu

import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.Plugin
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 管理 `&item:[...]` 的手动 Sprite 覆盖与第三方物品二维纹理缓存。
 */
class ExternalItemSpriteManager(private val plugin: KaMenu) {
    private val overrides = ConcurrentHashMap<String, ItemSpriteReference>()
    private val resolved = ConcurrentHashMap<String, ItemSpriteReference>()
    private val unresolved = ConcurrentHashMap.newKeySet<String>()
    private val listener = object : Listener {}
    private var reloadHooksRegistered = false

    /** 释放默认配置、读取覆盖项并注册可选插件的数据重载回调。 */
    fun init() {
        val file = File(plugin.dataFolder, FILE_NAME)
        if (!file.exists()) plugin.saveResource(FILE_NAME, false)
        reload()
        if (MinecraftFeatures.supportsSpriteObjects()) {
            registerReloadHooks()
        }
    }

    /** 重新读取手动覆盖并清空自动解析结果。 */
    fun reload(): Int {
        overrides.clear()
        clearAutoCache()
        val file = File(plugin.dataFolder, FILE_NAME)
        if (!file.isFile) return 0
        val yaml = YamlConfiguration.loadConfiguration(file)
        val section = yaml.getConfigurationSection("sprites") ?: return 0
        for (key in section.getKeys(false)) {
            val reference = parseReference(section, key)
            if (reference == null) {
                plugin.logger.warning("Invalid item sprite mapping '$key' in $FILE_NAME")
                continue
            }
            overrides[key.trim().lowercase()] = reference
        }
        return overrides.size
    }

    /** 将原版材质、第三方物品 ID 或手动别名转换为 MiniMessage Sprite 标签。 */
    fun resolveTag(raw: String): String? {
        val key = raw.trim().lowercase()
        overrides[key]?.let { return it.toMiniMessageTag() }

        val externalId = ExternalItemAdapter.normalizeId(raw)
            ?: return MaterialUtils.getSpriteTag(raw)
        resolved[externalId]?.let { return it.toMiniMessageTag() }
        if (externalId in unresolved) return fallbackTag(raw)

        val reference = ExternalItemAdapter.sprite(raw)
        if (reference != null) {
            resolved[externalId] = reference
            return reference.toMiniMessageTag()
        }
        unresolved += externalId
        return fallbackTag(raw)
    }

    /** 清空第三方 API 解析结果；材质包插件重载后会自动调用。 */
    fun clearAutoCache() {
        resolved.clear()
        unresolved.clear()
    }

    /** 释放当前管理器持有的映射。 */
    fun shutdown() {
        overrides.clear()
        clearAutoCache()
    }

    private fun fallbackTag(raw: String): String? {
        val item = ExternalItemAdapter.create(raw) ?: return null
        return MaterialUtils.getSpriteTag(item.type.name)
    }

    private fun parseReference(section: ConfigurationSection, key: String): ItemSpriteReference? {
        section.getString(key)?.let { return ItemSpriteReference.parse(it) }
        val value = section.getConfigurationSection(key) ?: return null
        val atlas = value.getString("atlas") ?: return null
        val sprite = value.getString("sprite") ?: return null
        return ItemSpriteReference.of(atlas, sprite)
    }

    private fun registerReloadHooks() {
        if (reloadHooksRegistered) return
        reloadHooksRegistered = true
        registerOptionalEvent("ItemsAdder", "dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent")
        registerOptionalEvent("Oraxen", "io.th0rgal.oraxen.api.events.OraxenItemsLoadedEvent")
        registerOptionalEvent("Oraxen", "io.th0rgal.oraxen.api.events.OraxenPackGeneratedEvent")
        registerOptionalEvent("CraftEngine", "net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent")
        registerOptionalEvent("CraftEngine", "net.momirealms.craftengine.bukkit.api.event.AsyncResourcePackGenerateEvent")
    }

    @Suppress("UNCHECKED_CAST")
    private fun registerOptionalEvent(pluginName: String, className: String) {
        val dependency = plugin.server.pluginManager.getPlugin(pluginName) ?: return
        val eventClass = runCatching {
            dependency.javaClass.classLoader.loadClass(className) as Class<out Event>
        }.getOrNull() ?: return
        plugin.server.pluginManager.registerEvent(
            eventClass,
            listener,
            EventPriority.MONITOR,
            EventExecutor { _, _ -> clearAutoCache() },
            plugin,
            true
        )
    }

    companion object {
        private const val FILE_NAME = "item_sprites.yml"
    }
}
