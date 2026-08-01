# KaMenu v1.7.2 更新日志

## 版本信息

- **版本号**: 1.7.2
- **发布日期**: 2026年7月31日

---

## 中文

### Geyser/Floodgate 基岩版按钮图标

- `Bottom` 普通按钮和 `repeat.item` 动态按钮新增可选的 `icon` 配置。
- 支持 `url` 网络图片和 `path` 基岩版资源包路径，URL 也可直接使用字符串简写。
- `repeat.item.icon` 支持 `{item.xxx}`、PlaceholderAPI 和 KaMenu 内置变量，可为每个动态项目生成不同图片。
- Floodgate 玩家打开无输入组件且至少含一个有效图标的菜单时，KaMenu 会发送带图标的基岩版 `SimpleForm`。
- Java 玩家、没有图标的菜单及带 `Inputs` 的菜单保持原有 Java Dialog/Geyser 转换路径，不改变现有行为。

### 生命周期与兼容性

- 基岩版按钮继续执行原有 KaMenu actions、条件、变量、动作包、Open/Close 事件、Tasks、repeat 分页及菜单刷新动作。
- 表单回调重新调度到玩家所属线程，兼容 Paper、Folia 和 Spigot 的执行模型。
- 表单会话保持一次性回调、`Settings.lifetime`、`can_escape`、玩家退出及插件关闭清理逻辑。
- `url:` 和 `copy:` 客户端静态按钮动作会自动回退到原有 Java Dialog 转换，避免动作失效。
- 对 URL 协议、长度、控制字符和资源包路径穿越进行校验；无效图标会被忽略并输出本地化错误。
- 修复 repeat 简单字符串配置了 `split` 时仍先尝试 JSON 解析并输出无意义警告的问题。
- 基岩版 `SimpleForm` 不支持 Java Dialog 的按钮矩阵、宽度和悬停提示，`columns`、`width`、`tooltip` 及 repeat 补位按钮不会影响基岩版显示。

---

## English

## Version Information

- **Version**: 1.7.2
- **Release Date**: July 31, 2026

---

### Geyser/Floodgate Bedrock Button Icons

- Normal `Bottom` buttons and dynamic `repeat.item` buttons now accept an optional `icon` setting.
- Supports remote `url` images and Bedrock resource-pack `path` values, with a scalar shorthand for URL values.
- `repeat.item.icon` supports `{item.xxx}`, PlaceholderAPI placeholders, and built-in KaMenu variables, allowing each generated item to use a different image.
- When a Floodgate player opens a menu without input components and at least one valid icon is present, KaMenu sends an icon-enabled Bedrock `SimpleForm`.
- Java players, menus without icons, and menus with `Inputs` keep using the existing Java Dialog/Geyser conversion path.

### Lifecycle and Compatibility

- Bedrock buttons continue to execute existing KaMenu actions, conditions, variables, action packages, Open/Close events, Tasks, repeat pagination, and menu refresh actions.
- Form callbacks return to the player's scheduler context for Paper, Folia, and Spigot compatibility.
- Form sessions retain one-use callbacks, `Settings.lifetime`, `can_escape`, player-quit cleanup, and plugin-shutdown cleanup.
- Client-side `url:` and `copy:` button actions automatically fall back to the existing Java Dialog conversion so their behavior is preserved.
- URL schemes, lengths, control characters, and resource-pack path traversal are validated; invalid icons are ignored with localized diagnostics.
- Fixed unnecessary JSON parsing warnings when a repeat simple-string source already defines `split`.
- Bedrock `SimpleForm` has no equivalents for Java Dialog button grids, widths, or tooltips, so `columns`, `width`, `tooltip`, and repeat padding buttons do not affect its layout.

---

## Upgrade Guide

### Upgrading from 1.7.1 to 1.7.2

1. Replace the plugin JAR with `KaMenu-1.7.2.jar`.
2. Install compatible Geyser and Floodgate builds to use Bedrock button icons.
3. Fully restart the server so the optional dependency order is applied.
4. Existing menus require no configuration changes.
