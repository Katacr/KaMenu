# 容器类菜单

容器类菜单使用 Bukkit 虚拟库存渲染箱子、漏斗、发射器、投掷器、熔炉、高炉、烟熏炉和铁砧等界面，是 Minecraft 1.16.5 以及不支持原生 Dialog 核心的主要菜单路径。

## 先判断是否使用容器类菜单

| 需求 | 推荐结构 | 入口 |
|---|---|---|
| 原生对话框、文字、输入框和按钮 | Dialog | [现代 Dialog 菜单](../modern-dialog/README.md) |
| 箱子、漏斗、发射器、投掷器等库存界面 | 容器类菜单 | 本组文档 |
| 熔炉火焰/箭头或铁砧重命名界面 | 容器类菜单 | [Properties](properties.md) |
| 低于原生 Dialog 支持版本的服务器 | 容器类菜单 | 本组文档 |

容器类菜单的配置顺序通常是：先选择 `Type`，再设置 `Title` 和 `Settings`，用 `Layout` 安排槽位，用 `Buttons` 定义每个按钮，最后按需添加 `Properties`、`Events` 和刷新周期。

## 文档导航

- [创建容器类菜单教程](creating_menu.md)：从创建文件到重载打开的完整新手流程。
- [容器类菜单文件结构](structure.md)：顶层键总览和完整骨架。
- [Type](type.md)：容器类型、尺寸和版本限制。
- [Title](title.md)：容器标题和动态解析。
- [Settings](settings.md)：依赖检查、参数传递和防频繁点击。
- [Layout](layout.md)：库存行、槽位、空格和多字符按钮 ID。
- [自由槽位](free-slots.md)：接收真实物品、条件、预览、原子消费和异常返还。
- [Buttons](buttons.md)：物品显示、显示条件、点击动作和 `variants`。
- [Properties](properties.md)：熔炉进度、铁砧输入和容器专属属性。
- [刷新机制](refresh.md)：整体、标题、按钮和进度刷新。
- [Events](events.md)：容器类菜单与通用生命周期事件的差异。
- [菜单迁移总览](migration.md)：从 DeluxeMenus 或 TrMenu 迁移菜单，并解析迁移报告。

## 与 Dialog 的边界

容器类菜单文件使用 `Type`、`Layout`、`Buttons` 和可选的 `Properties`，不能同时配置 Dialog 专用的 `Body`、`Inputs` 或 `Bottom`。`Events`、动作、条件、变量、PlaceholderAPI 和 JavaScript 仍复用 KaMenu 的通用运行时。

Paper/Folia 和支持适配器的 Spigot 核心可以使用 Dialog；低版本核心会禁用 Dialog，但容器类菜单仍可用。`toast` 依赖 Paper/Folia，跨平台菜单应使用 `actionbar` 或 `title` 反馈。需要接收玩家真实物品时使用 `Free-Slots`，不要把普通展示按钮当作可取出的物品。

## 最小示例

```yaml
Type: CHEST
Title: '&8商店'

Layout:
  - '         '
  - '    `shop`    '
  - '         '

Buttons:
  shop:
    display:
      material: DIAMOND
      name: '&b钻石'
    actions:
      left:
        - 'tell: &a点击了钻石'
        - 'close'
```

菜单 ID、重载方式和通用 YAML 规则参见[现代 Dialog 菜单文件结构](../modern-dialog/structure.md)。

## 从哪里开始

1. 先阅读[文件结构](structure.md)确认顶层键和缩进层级。
2. 普通箱子菜单阅读[Type](type.md)、[Layout](layout.md)和[Buttons](buttons.md)。
3. 需要防连点或菜单传参时补充阅读[Settings](settings.md)。
4. 需要动态标题、周期刷新、熔炉进度或铁砧输入时，再阅读[Title](title.md)、[刷新机制](refresh.md)和[Properties](properties.md)。
5. 最后为打开、关闭和按钮动作添加[Events](events.md)。
