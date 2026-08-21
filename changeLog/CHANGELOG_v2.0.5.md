# KaMenu v2.0.5 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.5
- **发布日期 / Release Date:** 2026年8月21日 / August 21, 2026

---

## 中文

### KaProxy 跨服目标选择器

- 新增 `{cross}` 标记。在动作末尾的目标选择器 `{player: 选择器}` 后追加 `{cross}`，即可通过 KaProxy 把动作转发到其他后端执行。
- 新增 KaMenu `kaproxy.enabled` 配置项。启用后自动注册 `kaproxy:main` 插件消息通道；关闭或卸载时自动注销。
- KaProxy 新增 `modules.kamenu` 模块及 `servers` 配置。列表控制可接收跨服动作的后端；设为 `all` 或 `*` 时转发到所有后端，始终排除来源服。
- 目标后端收到动作后在本地求值选择器，因此 PAPI 变量和 KaMenu 内置条件在各服均可正常使用。
- 跨服执行采用后端白名单保护，仅支持 `tell`、`actionbar`、`title`、`sound`、`hovertext`、`money`、`points`、`data`、`gdata`、`tmpdata`、`meta`、`set-data`、`set-gdata`、`set-meta`。非白名单动作会被拒绝并记录警告。
- `/kaproxy status` 现在显示 Kamenu 模块状态。

### 文档

- 新增 KaProxy Kamenu 模块中英文文档，并补充 KaMenu 中英文动作文档与菜单编写 Skill 的跨服选择器说明。

### 修复

- 修复 Paper 原生 Dialog 的 `Inputs` 中 `type: checkbox` 无论勾选状态如何都返回 `false` 的问题。回调现在按组件声明类型读取响应，checkbox 使用布尔值读取，不再被数值读取的默认 `0` 覆盖。

---

## English

### KaProxy Cross-Server Target Selector

- Added the `{cross}` tag. Append it after an action's `{player: selector}` target selector to forward the action to other backends through KaProxy.
- Added the KaMenu `kaproxy.enabled` setting. When enabled, KaMenu registers the `kaproxy:main` plugin-message channel and unregisters it during shutdown.
- Added KaProxy's `modules.kamenu` module and `servers` setting. The list controls which backends receive cross-server actions; setting it to `all` or `*` forwards to every backend while always excluding the source backend.
- Target backends evaluate selectors locally, so PAPI variables and KaMenu built-in conditions work independently on every backend.
- Cross-server execution is protected by a backend whitelist: `tell`, `actionbar`, `title`, `sound`, `hovertext`, `money`, `points`, `data`, `gdata`, `tmpdata`, `meta`, `set-data`, `set-gdata`, and `set-meta`. Non-whitelisted actions are rejected with a warning.
- `/kaproxy status` now shows the Kamenu module state.

### Documentation

- Added Chinese and English KaProxy Kamenu module documentation, and documented cross-server selectors in KaMenu's Chinese/English action docs and menu-authoring Skill.

### Fixes

- Fixed Paper native Dialog `Inputs` with `type: checkbox` always returning `false` regardless of its selected state. Callbacks now read responses by declared component type, so checkbox booleans are no longer masked by the numeric reader's default `0`.
