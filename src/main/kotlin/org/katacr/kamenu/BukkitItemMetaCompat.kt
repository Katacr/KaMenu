package org.katacr.kamenu

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import java.net.URL
import java.lang.reflect.Method

/**
 * 通过 Bukkit 公共反射入口兼容不同版本公开的物品属性。
 *
 * 这里不调用 NMS，也不在共享代码中静态链接 1.20+ 的 ItemModel、MaxDamage 或 PlayerProfile
 * 方法；旧核心缺少这些方法时返回 false/null，由调用方保留基础物品行为并输出已有配置警告。
 */
object BukkitItemMetaCompat {
    /** 在当前核心支持时设置命名空间物品模型。 */
    fun setItemModel(meta: ItemMeta, key: NamespacedKey): Boolean = runCatching {
        val method = sequenceOf(ItemMeta::class.java, meta.javaClass)
            .mapNotNull { type -> type.findMethod("setItemModel", NamespacedKey::class.java) }
            .firstOrNull()
            ?: return false
        method.isAccessible = true
        method.invoke(meta, key)
        true
    }.getOrDefault(false)

    /** 读取当前物品的最大耐久；旧核心回退到材质默认值。 */
    fun maxDamage(meta: Damageable, fallback: Int): Int {
        val hasMaxDamage = runCatching {
            (meta.javaClass.findMethod("hasMaxDamage")?.invoke(meta) as? Boolean) == true
        }.getOrDefault(false)
        if (!hasMaxDamage) return fallback
        return runCatching {
            (meta.javaClass.findMethod("getMaxDamage")?.invoke(meta) as? Number)?.toInt()
        }.getOrNull()?.takeIf { it > 0 } ?: fallback
    }

    /** 使用公开 PlayerProfile 方法设置头颅纹理；旧核心不支持时返回 false。 */
    fun setSkullTexture(meta: SkullMeta, textureUrl: URL, profileId: java.util.UUID): Boolean = runCatching {
        val createProfile = Bukkit::class.java.methods.firstOrNull {
            it.name == "createPlayerProfile" && it.parameterTypes.contentEquals(
                arrayOf(java.util.UUID::class.java, String::class.java)
            )
        } ?: return false
        val profile = createProfile.invoke(null, profileId, "custom_head") ?: return false
        val textures = profile.javaClass.findMethod("getTextures")?.invoke(profile) ?: return false
        val setSkin = textures.javaClass.findMethod("setSkin", URL::class.java) ?: return false
        setSkin.invoke(textures, textureUrl)
        val setTextures = profile.javaClass.methods.firstOrNull {
            it.name == "setTextures" && it.parameterCount == 1
        } ?: return false
        setTextures.invoke(profile, textures)
        val setOwnerProfile = meta.javaClass.methods.firstOrNull {
            it.name == "setOwnerProfile" && it.parameterCount == 1
        } ?: return false
        setOwnerProfile.invoke(meta, profile)
        true
    }.getOrDefault(false)

    /** 在当前核心支持时使用 Adventure Component 设置展示名；旧核心返回 false。 */
    fun setDisplayNameComponent(meta: ItemMeta, component: Component): Boolean = runCatching {
        val method = sequenceOf(ItemMeta::class.java, meta.javaClass)
            .mapNotNull { type -> type.findMethod("displayName", Component::class.java) }
            .firstOrNull()
            ?: return false
        method.isAccessible = true
        method.invoke(meta, component)
        true
    }.getOrDefault(false)

    /** 在当前核心支持时使用 Adventure Component 列表设置 Lore；旧核心返回 false。 */
    fun setLoreComponent(meta: ItemMeta, components: List<Component>): Boolean = runCatching {
        val method = sequenceOf(ItemMeta::class.java, meta.javaClass)
            .mapNotNull { type -> type.findMethod("lore", List::class.java) }
            .firstOrNull()
            ?: return false
        method.isAccessible = true
        method.invoke(meta, components)
        true
    }.getOrDefault(false)

    /** 在当前核心支持时使用 Adventure Component 创建库存；旧核心返回 null。 */
    fun createInventoryComponent(holder: InventoryHolder?, size: Int, title: Component): Inventory? = runCatching {
        val server = Bukkit.getServer()
        val method = sequenceOf(Bukkit::class.java, server.javaClass)
            .mapNotNull { type ->
                type.findMethod("createInventory", InventoryHolder::class.java, java.lang.Integer.TYPE, Component::class.java)
            }
            .firstOrNull()
            ?: return null
        method.isAccessible = true
        method.invoke(server, holder, size, title) as? Inventory
    }.getOrNull()

    /** 在当前核心支持时使用 Adventure Component 设置库存标题；返回新视图或 null。 */
    fun setInventoryTitleComponent(view: InventoryView, component: Component): InventoryView? = runCatching {
        val method = sequenceOf(InventoryView::class.java, view.javaClass)
            .mapNotNull { type -> type.findMethod("title", Component::class.java) }
            .firstOrNull()
            ?: return null
        method.isAccessible = true
        method.invoke(view, component) as? InventoryView
    }.getOrNull()

    private fun Class<*>.findMethod(name: String, vararg parameterTypes: Class<*>): Method? =
        runCatching { getMethod(name, *parameterTypes) }.getOrNull()
}
