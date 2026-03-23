package org.katacr.kamenu

import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 元数据管理器
 * 用于管理临时的内存缓存数据，无需持久化到数据库
 * 玩家退出时自动清理该玩家缓存，插件重载/关服时清理全部缓存
 */
class MetaDataManager {
    // 玩家元数据缓存: UUID -> (key -> value)
    private val playerMetaData = ConcurrentHashMap<UUID, MutableMap<String, String>>()

    /**
     * 设置玩家元数据
     */
    fun setPlayerMeta(playerUuid: UUID, key: String, value: String) {
        val playerData = playerMetaData.getOrPut(playerUuid) { mutableMapOf() }
        playerData[key] = value
    }

    /**
     * 获取玩家元数据
     * @return 元数据值，如果不存在则返回 "null"
     */
    fun getPlayerMeta(playerUuid: UUID, key: String): String {
        val playerData = playerMetaData[playerUuid]
        return playerData?.get(key) ?: "null"
    }

    /**
     * 检查玩家元数据是否存在
     */
    fun hasPlayerMeta(playerUuid: UUID, key: String): Boolean {
        val playerData = playerMetaData[playerUuid]
        return playerData?.containsKey(key) == true
    }

    /**
     * 清理指定玩家的元数据
     */
    fun clearPlayerMeta(playerUuid: UUID) {
        playerMetaData.remove(playerUuid)
    }

    /**
     * 清理所有元数据
     */
    fun clearAll() {
        playerMetaData.clear()
    }
}
