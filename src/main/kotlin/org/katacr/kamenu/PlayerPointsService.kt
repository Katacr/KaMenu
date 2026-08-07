package org.katacr.kamenu

import org.black_ixx.playerpoints.PlayerPoints
import org.black_ixx.playerpoints.PlayerPointsAPI
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * KaMenu 点券动作使用的最小服务接口。
 *
 * 核心动作处理器只依赖该接口，PlayerPoints 未安装时不会加载其 API 实现类。
 */
internal interface PointsService {
    /** 读取玩家当前点券余额。 */
    fun balance(playerId: UUID): Int

    /** 增加点券并返回 PlayerPoints 是否接受该操作。 */
    fun add(playerId: UUID, amount: Int): Boolean

    /** 扣除点券并返回 PlayerPoints 是否接受该操作。 */
    fun take(playerId: UUID, amount: Int): Boolean
}

/**
 * 基于 PlayerPoints 公共 API 的点券服务实现。
 *
 * 该类只会在 Bukkit 已确认 PlayerPoints 存在并启用后创建，保持第三方依赖可选。
 */
internal class PlayerPointsService private constructor(
    private val api: PlayerPointsAPI
) : PointsService {

    override fun balance(playerId: UUID): Int = api.look(playerId)

    override fun add(playerId: UUID, amount: Int): Boolean = api.give(playerId, amount)

    override fun take(playerId: UUID, amount: Int): Boolean = api.take(playerId, amount)

    companion object {
        /**
         * 从已启用的 Bukkit 插件创建服务；插件类型不匹配时返回 null。
         */
        fun create(plugin: Plugin): PlayerPointsService? {
            val playerPoints = plugin as? PlayerPoints ?: return null
            return PlayerPointsService(playerPoints.api)
        }
    }
}
