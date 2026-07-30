package org.katacr.kamenu.dialog.bedrock

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.katacr.kamenu.KaMenu

/**
 * 可选的基岩版表单传输层。
 *
 * 该接口不引用 Floodgate 类型，使未安装 Floodgate 的服务器不会在加载 KaMenu 共享类时解析外部 API。
 */
interface BedrockFormAdapter {
    /** 初始化可选依赖和表单会话状态。 */
    fun initialize(plugin: KaMenu)

    /**
     * 尝试接管一份菜单配置。
     *
     * 返回 true 表示本适配器已经负责后续打开或回退流程，调用方不得再次打开原生 Dialog。
     */
    fun tryOpen(
        player: Player,
        config: YamlConfiguration,
        contextId: String,
        runOpenEvent: Boolean,
        fallback: () -> Unit
    ): Boolean

    /** 关闭当前由本适配器发送的表单；没有活动表单时返回 false。 */
    fun close(player: Player): Boolean

    /** 玩家离线时丢弃服务端会话，不再向客户端发送关闭请求。 */
    fun discard(player: Player)

    /** 插件关闭时释放全部表单会话。 */
    fun shutdown()
}
