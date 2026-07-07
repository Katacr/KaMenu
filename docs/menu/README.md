# 🏞️ 菜单系统

KaMenu 的菜单系统基于 YAML 文件配置，支持多层文件夹结构和丰富的组件组合。每个 `.yml` 文件即为一个独立的菜单。

## 功能导航

- [菜单文件结构](structure.md)：了解菜单 ID、文件目录、顶层节点和基础 YAML 结构。
- [全局设置](setting.md)：配置 ESC 关闭、按钮动作后的客户端行为和菜单所需 PlaceholderAPI 扩展。
- [JavaScript 功能](javascript.md)：定义可复用脚本，并通过 `{js:...}` 在文本、条件和动作中输出动态值。
- [actions 文件夹](../config/actions-packages.md)、[js 文件夹](../config/javascript-packages.md)：管理跨菜单复用的全局动作包和 JavaScript 包。
- [事件](events.md)：配置 `Open`、`Close`、`Click` 和 `Tasks`，包括打开前校验、可复用动作组和周期任务。
- [内容组件](body.md)、[输入组件](inputs.md)、[底部按钮](bottom.md)：组合 Dialog 主体、输入控件和按钮布局。
- [动作](actions.md)：使用消息、指令、菜单跳转、数据读写、`wait`、`return`、嵌套 `actions` 等动作。
- [条件判断](conditions.md)：在文本和动作中使用多层条件，配合 PlaceholderAPI、数据变量和 JavaScript 表达式。
