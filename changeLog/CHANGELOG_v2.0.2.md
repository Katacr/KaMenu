# KaMenu v2.0.2 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.2
- **发布日期 / Release Date:** 2026年8月10日 / August 10, 2026

---

## 中文

### 容器类菜单文档重构

- 重构中英文容器类菜单文档，为 `Type`、`Title`、`Settings`、`Layout`、`Buttons`、`Properties`、刷新机制和 `Events` 增加字段一览表、逐项说明和实际案例。
- 新增从文件创建、布局设计、按钮定义到重载打开的容器类菜单新手教程，并补充常见解析错误和动态 `variants` 示例。
- 完善容器类菜单文档导航、顶层配置流程、跨版本边界和 Dialog 与容器类菜单的结构差异说明。
- 将原 TrMenu 迁移页面重构为统一迁移总览，新增 DeluxeMenus 快速迁移教程、不兼容内容列表及迁移报告和 `TRM_*` 错误代码解析。
- 补充容器类菜单 `Title` 的字符串列表轮播模式，说明 `Title-Update` 每次推进一项，以及手动刷新只重新解析当前项的区别。

### 条件列表解析加固

- 支持在 Dialog 消息文本、Body 物品 Lore、按钮悬浮文字和容器物品 Lore 中按 YAML 顺序混写静态字符串与条件分支。
- 条件分支可在原位置插入一行或多行内容；完全由条件 Map 组成的旧配置仍保持首个非空候选语义。
- 新增行尾 `{condition: 条件}` 快捷条件，可用于上述字符串列表和所有字符串动作；条件不成立时跳过整行或整条动作。
- 统一单行动作修饰符为 `{chance: 数值}`、`{wait: tick}` 和 `{condition: 条件}`，不再使用别名、尖括号或等号格式。
- TrMenu 迁移器可将源 Lore 的行尾条件和动作概率/延迟转换为 KaMenu 标准修饰符。

### 迁移日志输出

- `/km migrate dm` 和 `/km migrate trmenu` 的逐文件结果、完整诊断与冲突明细改为写入 `plugins/KaMenu/logs/migration/` 下的独立日志文件。
- 聊天框仅保留迁移、入口合并、运行时重载摘要和日志路径；日志写入失败时完整报告会回退到服务器控制台。

### 菜单引用与 TrMenu node 迁移

- 新增菜单级 `References`，支持 `{ref:path}` 公共引用、`{config:path}` 当前配置引用，以及 Dialog 组件和容器按钮中的 `{self:id}` / `{self:path}` 自身引用。
- 新增 `{ref:[path;arg0;arg1]}` 参数模板与 `{refarg:n}` 占位符；支持嵌套引用、大小写不敏感路径、简单列表换行展开，并检测缺失路径、结构引用、循环和超过 16 层的递归。
- `{ref:*}`、`{config:*}` 和 `{self:*}` 可安全用于条件表达式；修复 `{self:path}`，现会返回当前组件或按钮的完整配置路径。
- TrMenu 迁移器现可将静态 `{node:*}` / `{nodes:*}` / `{n:*}` 转换为 KaMenu 引用，只按需复制实际使用的节点到 `References.trmenu`。
- 支持 TrMenu node 模板参数和图标上下文 `@iconId@`，动态路径或不可嵌入的结构会生成明确的 `TRM_NODE_*` 诊断，不会猜测转换。

---

## English

### Container Documentation Rework

- Reworked the Chinese and English Container references with field overview tables, detailed explanations, and practical examples for `Type`, `Title`, `Settings`, `Layout`, `Buttons`, `Properties`, refresh behaviour, and `Events`.
- Added a beginner Container tutorial covering file creation, layout design, button definitions, reload/open commands, common parser errors, and dynamic `variants`.
- Improved Container navigation, top-level authoring flow, cross-version boundaries, and Dialog/Container structure guidance.
- Reworked the former TrMenu-only page into a shared migration overview with a DeluxeMenus quick-start tutorial, incompatibility lists, report interpretation, and common `TRM_*` diagnostic codes.
- Documented Container `Title` string-list rotation, including one-frame advancement by `Title-Update` and the non-advancing behavior of manual title refreshes.

### Conditional List Parsing

- Added ordered mixing of static strings and conditional branches for Dialog message text, Body item lore, button tooltips, and Container item lore.
- A selected branch can insert one or several lines at its original position, while existing all-condition lists retain first-non-empty candidate semantics.
- Added the `{condition: expression}` line suffix for those string lists and all string actions. A false condition skips the complete line or action.
- Standardized per-action modifiers as `{chance: value}`, `{wait: ticks}`, and `{condition: expression}`; aliases, angle brackets, and equals-sign forms are no longer runtime syntax.
- The TrMenu migrator now converts source Lore conditions and action chance/delay modifiers into KaMenu's standard forms.

### Migration Log Output

- Moved per-file results, complete diagnostics, and conflict details from `/km migrate dm` and `/km migrate trmenu` into separate report files under `plugins/KaMenu/logs/migration/`.
- Chat now keeps only migration, entry-merge, runtime-reload summaries, and the report path. A failed file write falls back to the server console for the complete report.

### Menu References And TrMenu node Migration

- Added menu-level `References` with `{ref:path}` shared references, `{config:path}` current-config references, and `{self:id}` / `{self:path}` component references in Dialog components and Container buttons.
- Added `{ref:[path;arg0;arg1]}` templates with `{refarg:n}` placeholders, nested expansion, case-insensitive paths, newline-joined simple lists, and explicit protection against missing paths, structural values, cycles, and recursion beyond 16 levels.
- `{ref:*}`, `{config:*}`, and `{self:*}` now resolve safely in condition expressions. Fixed `{self:path}` so it returns the full path of the current component or button.
- The TrMenu migrator now converts static `{node:*}`, `{nodes:*}`, and `{n:*}` expressions into KaMenu references and copies only the used source nodes under `References.trmenu`.
- Added TrMenu node template arguments and icon-context `@iconId@` support. Dynamic paths and non-text structures produce explicit `TRM_NODE_*` diagnostics instead of guessed output.
