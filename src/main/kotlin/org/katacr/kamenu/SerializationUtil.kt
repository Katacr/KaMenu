package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 序列化工具类
 * 用于将物品序列化为Base64存储
 */
object SerializationUtil {

    /**
     * 序列化物品数组 -> Base64字符串
     */
    fun itemsToBase64(items: Array<ItemStack?>): String {
        try {
            val outputStream = ByteArrayOutputStream()
            BukkitObjectOutputStream(outputStream).use { dataOutput ->
                dataOutput.writeInt(items.size)
                for (item in items) {
                    // writeObject 可以处理 null，但显式处理更安全
                    dataOutput.writeObject(item)
                }
            }
            return Base64Coder.encodeLines(outputStream.toByteArray())
        } catch (e: Exception) {
            throw IllegalStateException("无法序列化物品栏", e)
        }
    }

    /**
     * 反序列化：Base64字符串 -> 物品数组
     */
    fun itemStackArrayFromBase64(data: String): Array<ItemStack?> {
        val inputStream = ByteArrayInputStream(Base64Coder.decodeLines(data))
        BukkitObjectInputStream(inputStream).use { dataInput ->
            val size = dataInput.readInt()
            val items = arrayOfNulls<ItemStack>(size)
            for (i in 0 until size) {
                items[i] = dataInput.readObject() as ItemStack?
            }
            return items
        }
    }

    /**
     * 序列化单个物品 -> Base64字符串
     */
    fun itemToBase64(item: ItemStack?): String {
        if (item == null) return ""
        return itemsToBase64(arrayOf(item))
    }

    /**
     * 反序列化：Base64字符串 -> 单个物品
     */
    fun itemFromBase64(data: String): ItemStack? {
        if (data.isEmpty()) return null
        val items = itemStackArrayFromBase64(data)
        return items.firstOrNull()
    }

    /**
     * Location 序列化 (格式: world,x,y,z,yaw,pitch)
     */
    fun serializeLocation(loc: Location?): String? {
        if (loc == null) return null
        return "${loc.world?.name},${loc.x},${loc.y},${loc.z},${loc.yaw},${loc.pitch}"
    }

    /**
     * Location 反序列化
     */
    fun deserializeLocation(s: String?): Location? {
        if (s == null || s.isEmpty()) return null
        val parts = s.split(",")
        if (parts.size < 4) return null

        val world = Bukkit.getWorld(parts[0]) ?: return null
        val x = parts[1].toDouble()
        val y = parts[2].toDouble()
        val z = parts[3].toDouble()
        val yaw = if (parts.size >= 5) parts[4].toFloat() else 0.0f
        val pitch = if (parts.size >= 6) parts[5].toFloat() else 0.0f

        return Location(world, x, y, z, yaw, pitch)
    }
}
