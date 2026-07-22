package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.permissions.Permissible
import java.lang.reflect.Method

/**
 * 可选的 ItemsAdder 字形文本适配器。
 *
 * ItemsAdder 不会自动处理 Paper Dialog 中的 Adventure 文本，因此这里在 KaMenu
 * 创建组件前把 `:font_image:` 和 `:offset_N:` 标签转换为实际字形字符。
 */
object ItemsAdderTextAdapter {
    private const val PLUGIN_NAME = "ItemsAdder"
    private const val FONT_IMAGE_WRAPPER_CLASS = "dev.lone.itemsadder.api.FontImages.FontImageWrapper"

    @Volatile
    private var cachedClassLoader: ClassLoader? = null

    @Volatile
    private var replaceMethod: Method? = null

    @Volatile
    private var replaceWithPermissibleMethod: Method? = null

    /**
     * 解析 ItemsAdder 内置字形标签；插件缺失、未启用或 API 不兼容时保留原文本。
     */
    fun replace(text: String): String = replace(text, null)

    /** 使用玩家权限上下文解析 ItemsAdder 字形，旧版 API 不支持时回退到普通替换。 */
    fun replace(text: String, permissible: Permissible?): String {
        if (':' !in text) return text

        val itemsAdder = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME)
        if (itemsAdder == null || !itemsAdder.isEnabled) return text

        return try {
            val method = resolveReplaceMethod(itemsAdder.javaClass.classLoader) ?: return text
            val result = if (permissible != null && replaceWithPermissibleMethod != null) {
                replaceWithPermissibleMethod?.invoke(null, permissible, text)
            } else {
                method.invoke(null, text)
            }
            result as? String ?: text
        } catch (_: ReflectiveOperationException) {
            text
        } catch (_: LinkageError) {
            text
        }
    }

    /** 查找并缓存当前 ItemsAdder 类加载器中的字符串字形替换方法。 */
    @Synchronized
    private fun resolveReplaceMethod(classLoader: ClassLoader): Method? {
        if (cachedClassLoader === classLoader) return replaceMethod

        replaceMethod = try {
            val wrapperClass = classLoader.loadClass(FONT_IMAGE_WRAPPER_CLASS)
            replaceWithPermissibleMethod = try {
                wrapperClass.getMethod("replaceFontImages", Permissible::class.java, String::class.java)
            } catch (_: ReflectiveOperationException) {
                null
            }
            wrapperClass.getMethod("replaceFontImages", String::class.java)
        } catch (_: ReflectiveOperationException) {
            replaceWithPermissibleMethod = null
            null
        } catch (_: LinkageError) {
            replaceWithPermissibleMethod = null
            null
        }
        cachedClassLoader = classLoader
        return replaceMethod
    }
}
