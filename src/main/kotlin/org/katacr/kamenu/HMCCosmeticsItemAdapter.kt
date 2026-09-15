package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Method
import java.util.UUID

/** 通过软依赖 API 读取玩家当前 HMCCosmetics 槽位的最终物品。 */
object HMCCosmeticsItemAdapter {
    private const val PLUGIN_NAME = "HMCCosmetics"
    private const val API_CLASS = "com.hibiscusmc.hmccosmetics.api.HMCCosmeticsAPI"
    private const val USER_CLASS = "com.hibiscusmc.hmccosmetics.user.CosmeticUser"
    private const val SLOT_CLASS = "com.hibiscusmc.hmccosmetics.cosmetic.CosmeticSlot"

    private var classLoader: ClassLoader? = null
    private var getUserMethod: Method? = null
    private var getSlotMethod: Method? = null
    private var getItemMethod: Method? = null

    /** 将 KaMenu 的 HMCC 槽位引用名映射为 HMCCosmetics 槽位名。 */
    internal fun slotName(reference: String): String? = when (reference.uppercase()) {
        "HMCC_HAT" -> "HELMET"
        "HMCC_BACKPACK" -> "BACKPACK"
        "HMCC_OFFHAND" -> "OFFHAND"
        "HMCC_BALLOON" -> "BALLOON"
        else -> null
    }

    /** 获取包含玩家染色和完整物品组件的 HMCCosmetics ItemStack。 */
    fun getItem(player: Player, slotName: String): ItemStack? = invokeSafely {
        val plugin = Bukkit.getPluginManager().getPlugin(PLUGIN_NAME)?.takeIf { it.isEnabled }
            ?: return@invokeSafely null
        resolve(plugin.javaClass.classLoader)
        val user = getUserMethod?.invoke(null, player.uniqueId) ?: return@invokeSafely null
        val slot = getSlotMethod?.invoke(null, slotName) ?: return@invokeSafely null
        (getItemMethod?.invoke(user, slot) as? ItemStack)?.clone()
    }

    /** 按当前插件 ClassLoader 缓存 HMCCosmetics 的公开 API 方法。 */
    @Synchronized
    private fun resolve(loader: ClassLoader) {
        if (classLoader === loader) return
        val apiClass = loader.loadClass(API_CLASS)
        val userClass = loader.loadClass(USER_CLASS)
        val slotClass = loader.loadClass(SLOT_CLASS)
        getUserMethod = apiClass.getMethod("getUser", UUID::class.java)
        getSlotMethod = slotClass.getMethod("valueOf", String::class.java)
        getItemMethod = userClass.getMethod("getUserCosmeticItem", slotClass)
        classLoader = loader
    }

    /** 将缺少软依赖或 API 版本不匹配降级为空槽位。 */
    private inline fun <T> invokeSafely(block: () -> T?): T? = try {
        block()
    } catch (_: ReflectiveOperationException) {
        null
    } catch (_: LinkageError) {
        null
    }
}
