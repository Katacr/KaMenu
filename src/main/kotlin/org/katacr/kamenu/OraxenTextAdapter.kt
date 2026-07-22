package org.katacr.kamenu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * 可选的 Oraxen MiniMessage 字形适配器。
 *
 * 只在文本含 `<glyph:...>` 或 `<g:...>` 时启用，并把同段 `<shift:...>` 一并解析；
 * 这样不会抢先处理仅供 CraftEngine 数据包拦截器使用的 shift 标签。
 */
object OraxenTextAdapter {
    private const val PLUGIN_NAME = "Oraxen"
    private const val GLYPH_TAG_CLASS = "io.th0rgal.oraxen.glyphs.GlyphTag"
    private const val SHIFT_TAG_CLASS = "io.th0rgal.oraxen.glyphs.ShiftTag"
    private val glyphPattern = Regex("<(?:glyph|g):", RegexOption.IGNORE_CASE)

    private var classLoader: ClassLoader? = null
    private var glyphResolverField: Field? = null
    private var playerResolverMethod: Method? = null
    private var shiftResolverField: Field? = null

    /** 判断完整文本是否使用 Oraxen 内部字形标签。 */
    fun containsGlyphTag(text: String): Boolean = glyphPattern.containsMatchIn(text)

    /**
     * 解析 Oraxen 字形文本；插件不可用或 API 不兼容时返回 null 交由标准解析器处理。
     * forceResolver 用于同一行被可点击文本拆分后仍需解析的 shift-only 片段。
     */
    fun parse(text: String, player: Player?, forceResolver: Boolean = false): Component? {
        if (!forceResolver && !containsGlyphTag(text)) return null
        val oraxen = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME)
        if (oraxen == null || !oraxen.isEnabled) return null

        return try {
            resolve(oraxen.javaClass.classLoader)
            val glyphResolver = if (player == null) {
                glyphResolverField?.get(null)
            } else {
                playerResolverMethod?.invoke(null, player)
            } as? TagResolver ?: return null
            val shiftResolver = shiftResolverField?.get(null) as? TagResolver ?: return null
            val resolver = TagResolver.resolver(TagResolver.standard(), glyphResolver, shiftResolver)
            MiniMessage.builder().tags(resolver).build().deserialize(text)
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: LinkageError) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    /** 查找并缓存 Oraxen 的玩家字形与像素偏移 Resolver。 */
    @Synchronized
    private fun resolve(loader: ClassLoader) {
        if (classLoader === loader) return
        val glyphClass = loader.loadClass(GLYPH_TAG_CLASS)
        val shiftClass = loader.loadClass(SHIFT_TAG_CLASS)
        glyphResolverField = glyphClass.getField("RESOLVER")
        playerResolverMethod = glyphClass.getMethod("getResolverForPlayer", Player::class.java)
        shiftResolverField = shiftClass.getField("RESOLVER")
        classLoader = loader
    }
}
