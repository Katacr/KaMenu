package org.katacr.kamenu.api

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

/**
 * 外部插件动作处理接口。
 *
 * KaMenu 在执行动作字符串时会先检查 `namespace:payload` 的 namespace 是否已注册。
 * 注册方式示例：
 *
 * `KaMenuAPI.registerActionHandler("kgc") { player, action, variables, config -> true }`
 *
 * 返回 `true` 表示动作已被外部插件消费，KaMenu 不再继续按内置动作解析；
 * 返回 `false` 表示未处理，KaMenu 会回退到原有动作逻辑。
 */
fun interface KaMenuActionHandler {
    /**
     * 执行外部动作。
     *
     * @param player 触发动作的玩家。
     * @param action 完整动作文本，例如 `kgc:open lobby`。
     * @param variables 当前动作上下文变量，包含输入框值、动作包参数 `{arg:0}` 等。
     * @param rawConfig 当前菜单配置；外部内存菜单或文件菜单都会尽量传入，独立动作场景可能为空。
     * @return 是否已处理该动作。
     */
    fun execute(
        player: Player,
        action: String,
        variables: Map<String, String>,
        rawConfig: YamlConfiguration?
    ): Boolean
}
