package org.katacr.kamenu

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

/**
 * 检查运行核心提供的 Adventure API 是否足以运行当前 MiniMessage。
 *
 * Paper 1.16.5 会优先提供 Adventure 4.7.0，而 KaMenu 通过 Libby 挂载的
 * MiniMessage 使用了 4.10.0 才加入的 `Component.compact()`。由于插件类加载器
 * 不能替换已经由核心加载的 Adventure 类，这里必须在首次创建 MiniMessage 前
 * 做能力检测，避免旧核心在插件启用阶段抛出 NoSuchMethodError。
 */
object AdventureCompatibility {
    private const val REQUIRED_COMPONENT_METHOD = "compact"

    @Volatile
    private var miniMessageAvailable: Boolean? = null

    /** 判断当前实际加载的 Adventure Component 是否支持新版 MiniMessage。 */
    fun supportsMiniMessage(): Boolean {
        miniMessageAvailable?.let { return it }

        val supported = try {
            Component::class.java.getMethod(REQUIRED_COMPONENT_METHOD)
            true
        } catch (_: ReflectiveOperationException) {
            false
        } catch (_: LinkageError) {
            false
        }

        miniMessageAvailable = supported
        return supported
    }

    /** 创建新版 MiniMessage；旧核心或依赖不完整时返回 null。 */
    fun createMiniMessage(): MiniMessage? {
        if (!supportsMiniMessage()) return null

        return try {
            MiniMessage.miniMessage()
        } catch (_: LinkageError) {
            miniMessageAvailable = false
            null
        } catch (_: RuntimeException) {
            miniMessageAvailable = false
            null
        }
    }
}
