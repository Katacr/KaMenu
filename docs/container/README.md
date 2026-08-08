# Container 菜单

Container 菜单使用 Bukkit 虚拟库存渲染箱子、漏斗、发射器、投掷器、熔炉、高炉、烟熏炉和铁砧等界面，是 Minecraft 1.16.5 以及不支持原生 Dialog 核心的主要菜单路径。

## 文档导航

- [Container 文件结构](structure.md)：顶层键总览和完整骨架。
- [Type](type.md)：容器类型、尺寸和版本限制。
- [Title](title.md)：容器标题和动态解析。
- [Settings](settings.md)：依赖检查、参数传递和防频繁点击。
- [Layout](layout.md)：库存行、槽位、空格和多字符按钮 ID。
- [Buttons](buttons.md)：物品显示、显示条件、点击动作和 `variants`。
- [Properties](properties.md)：熔炉进度、铁砧输入和容器专属属性。
- [刷新机制](refresh.md)：整体、标题、按钮和进度刷新。
- [Events](events.md)：Container 与通用生命周期事件的差异。

## 与 Dialog 的边界

Container 文件使用 `Type`、`Layout`、`Buttons` 和可选的 `Properties`，不能同时配置 Dialog 专用的 `Body`、`Inputs` 或 `Bottom`。`Events`、动作、条件、变量、PlaceholderAPI 和 JavaScript 仍复用 KaMenu 的通用运行时。

Paper/Folia 和支持适配器的 Spigot 核心可以使用 Dialog；低版本核心会禁用 Dialog，但 Container 仍可用。`toast` 依赖 Paper/Folia，跨平台菜单应使用 `actionbar` 或 `title` 反馈。

## 最小示例

```yaml
Type: CHEST
Title: '&8商店'

Layout:
  - '#########'
  - '####`shop`####'
  - '#########'

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
