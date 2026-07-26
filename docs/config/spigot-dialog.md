# 🧩 Spigot Dialog 兼容

KaMenu v1.7.0 使用同一个插件 JAR 支持 Paper、Folia 与 Spigot。插件启动时会自动探测当前核心，只加载对应的 Dialog 与调度适配器，无需安装额外兼容插件。

## 版本要求

| 核心 | 最低版本 | 支持范围 |
|------|----------|----------|
| Paper / Folia | 1.21.7+ | 完整功能 |
| Spigot | 1.21.6+ | 除下方平台边界外的完整菜单运行时 |

同一个 `KaMenu-1.7.0.jar` 已在 Paper 26.2 与 Spigot 26.2 完成启动、菜单渲染、输入回调、动作执行和关闭验证。

## Spigot 已支持

- 菜单文件、`/km open`、自定义指令和内存 YAML API
- `Events.Open`、`Events.Close`、`Events.Click` 与 `Events.Tasks`
- 完整 actions 队列、条件、`wait`、`return`、菜单跳转和任务控制
- JavaScript、全局 JS 包、全局 actions 包和 PlaceholderAPI
- `data`、`gdata`、`list`、`glist`、物品、经济等通用动作与条件
- 外部 `KaMenuAPI` 菜单和 action handler
- Inputs：`input`、`slider`、`dropdown`、`checkbox`，输入值会提交给 actions
- Bottom：`notice`、`confirmation`、`multi` 与 `repeat`
- `Body.item` 公开视觉属性：名称、Lore、附魔、耐久、模型、光效、Tooltip、皮革颜色与玩家头颅
- 正文 `<text ...;actions=...>` 服务端回调和动作包参数
- `Settings.can_escape`、`pause`、`after_action`、`lifetime`
- `tell`、`actionbar`、`title`、`sound`、`command`、`console`、`open`、`close` 等通用动作

Spigot 按钮和正文 `actions=` 使用服务端保存的一次性回调。客户端只提交当前菜单声明过的输入字段，KaMenu 会校验文本长度、滑块范围、单选项 ID 和复选框值，再执行服务端保存的可信动作路径。

## 平台边界

以下能力依赖 Paper API，或 Spigot 当前没有等价的公开接口：

- `toast:`：Spigot 不创建 Advancement toast，而是向玩家发送本地化的不支持提示。
- ESC 暂停菜单入口：`/km pause` 及 `pause_menu.yml` 数据包入口仅支持 Paper/Folia。
- 可点击文本 `hover_item` 与 `Body.item` 复用同一套 Bukkit 公共属性映射，支持主副手、背包槽位、护甲、保存物品、基础材质和外部物品来源。
- `Body.sprite` 和 `Body.player_head` 仍需 Paper/Folia。
- 单动作 `url:` 与 `copy:` 由客户端直接执行，不会向服务端回传点击；若它们配合 `after_action: CLOSE` 关闭菜单，任务和 Close 生命周期只能由 `Settings.lifetime` 兜底清理。

原生 Dialog 没有通用的“玩家刚刚按 ESC 静默关闭”服务端事件。若业务必须立即、可靠地执行退出逻辑，请使用 `can_escape: false` 和明确的 `close` 按钮；`Settings.lifetime` 始终会主动关闭过期菜单并清理生命周期。

Spigot 的 `Body.item` 仅使用 Bukkit/Spigot 公共 API 显式映射属性，不调用 CraftBukkit 或 NMS。当前支持自定义名称、固定物品名、Lore、普通/储存附魔、耐久、最大耐久、不可破坏、自定义模型数据、`item_model`、附魔光效、稀有度、Tooltip 样式与隐藏项、最大堆叠、皮革颜色和玩家头颅 profile。

槽位引用和外部物品会保留公共 API 能读取的上述视觉属性。PDC、插件私有组件以及 Bukkit 尚未公开的数据组件不会写入 Dialog；这些属性通常不影响物品图标，但对应的特殊 Tooltip 内容可能缺失。映射异常时仅回退该组件的基础材质与数量，并输出一次本地化警告。

## 动作与输入示例

```yaml
Inputs:
  target:
    type: input
    text: '&f玩家名'
    max_length: 16

Bottom:
  type: multi
  columns: 3
  buttons:
    execute:
      text: '&a执行'
      actions:
        - condition: 'isPass.$(target)'
          deny:
            - 'console: say $(target) 从 Spigot Dialog 提交了输入'
            - 'sound: entity.experience_orb.pickup;volume=1;pitch=1.2'
        - 'reset'
    website:
      text: '&b官网'
      actions:
        - 'url: https://example.com'
    copy:
      text: '&e复制地址'
      actions:
        - 'copy: play.example.com'
```

`command:` 是服务端 KaMenu 动作，支持变量、条件和玩家上下文；`url:`、`copy:` 只有在按钮中各自作为唯一动作时才使用客户端静态行为。
