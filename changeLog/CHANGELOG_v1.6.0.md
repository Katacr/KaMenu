# KaMenu v1.6.0 更新报告

## 📋 版本信息
- **版本号**: 1.6.0
- **发布日期**: 2026年7月7日

---

## ✨ 新增功能

### 1. 新增全局 actions 动作包

KaMenu 现在支持在 `plugins/KaMenu/actions/` 目录中放置全局 actions 动作包。一个 `.yml` 文件就是一个动作包，包 ID 为相对路径去掉 `.yml` 后缀。

```text
plugins/KaMenu/actions/reward/daily.yml -> reward/daily
```

动作包文件格式：

```yaml
actions:
  - 'toast: type=task;msg=领取成功;icon=emerald'
  - 'money: type=add;num={arg:0}'
```

任意菜单动作列表、可点击文本动作、周期任务和自定义注册指令都可以调用：

```yaml
- 'actions: reward/daily,100'
```

查找优先级：

1. 当前菜单内的 `Events.Click.<动作列表名称>`
2. 全局动作包 `plugins/KaMenu/actions/<动作列表名称>.yml`

因此菜单内 `Events.Click` 与全局动作包同名时，会优先执行菜单内动作列表。

### 2. 新增全局 JavaScript 包

KaMenu 现在支持在 `plugins/KaMenu/js/` 目录中放置全局 JavaScript 包。一个 `.js` 文件就是一个包，包 ID 为相对路径去掉 `.js` 后缀。

```text
plugins/KaMenu/js/reward/message.js -> reward/message
```

包文件内容就是 JavaScript 代码：

```javascript
var amount = args[0] || "0";
var target = args[1] || name;
"恭喜 " + target + " 获得 " + amount + " 金币";
```

可在动作、文本、条件中调用：

```yaml
- 'js: [reward/message],100,Steve'
text: '提示：{js:[reward/message],100,Steve}'
condition: '{js:[check/vip]} == true'
```

查找优先级：

1. 当前菜单内的 `JavaScript.<包名>`
2. 全局 JavaScript 包 `plugins/KaMenu/js/<包名>.js`

因此菜单内 `JavaScript` 与全局 JS 包同名时，会优先执行菜单内脚本。

### 3. 自定义注册指令支持参数 Tab 补全

`config.yml` 的 `custom-commands` 对象写法现在支持 `args` 键，用于给玩家输入指令参数时提供 Tab 补全候选项：

```yaml
custom-commands:
  test2:
    args:
      0: "[tp, tphere]"
      1: "%kamenu_online_players%"
    actions:
      - condition: "{arg:0} == tp"
        allow:
          - "tell: 你将传送到 {arg:1}"
```

- `args` 使用从 `0` 开始的索引，与动作变量 `{arg:0}`、`{arg:1}` 保持一致
- 候选项支持 YAML 列表、逗号分隔字符串、`[a, b]` 简易列表
- 支持实时解析 PAPI，例如 `%kamenu_online_players%`
- 支持实时解析 KaMenu 内置变量，例如 `{list:friends}`、`{glist:warps}`
- 打开菜单的对象写法也支持 `menu: 菜单ID` + `args`

---

## 🔧 优化与加固

### 1. 统一 actions 与 JavaScript 包参数解析

动作包和 JavaScript 包调用的参数现在统一支持英文逗号或空格分隔：

```yaml
- 'actions: reward/daily,100,vip'
- 'actions: reward/daily 100 vip'
- 'js: [reward_message],100,Steve'
- 'js: [reward_message] 100 Steve'
```

当参数自身包含空格或逗号时，可以使用单引号、双引号或反引号包裹：

```yaml
- 'actions: reward/daily,"100 coins",`vip,plus`'
- 'js: [reward_message],"100 coins",`Steve,Alex`'
```

### 2. 加固 JavaScript 包调用识别

`js:` 与 `{js:...}` 现在只有在内容以合法 `[包名]` 开头时，才会进入包调用解析。否则会完整作为 JavaScript 表达式或代码执行。

```yaml
# 包调用
- 'js: [reward_message],100,Steve'

# 普通 JavaScript 代码，不会因为逗号或空格被拆分
- 'js: player.sendMessage("hello, Steve")'
```

这可以避免普通 JavaScript 表达式因为逗号或空格被错误拆分。

### 3. 移除 js 动作的自动结果提示

`js:` 动作不再要求 JavaScript 必须输出值，也不会再自动向玩家发送 `JS Result` 提示。

- `js:` 动作：只负责执行脚本，有无返回值都可以
- `{js:...}` 文本占位：若脚本无返回值，会解析为空字符串

### 4. JavaScript 执行上下文隔离

每次 `js:`、`{js:...}` 和 JavaScript 包调用都会使用独立执行上下文，避免脚本创建的全局变量、函数或未声明变量污染其他玩家、菜单或包。

如果旧菜单把同一段逻辑拆成多条 `js:` 动作，并依赖上一条动作中定义的变量或函数，需要改为单条 `js:`，或放入菜单 `JavaScript` / 全局 JS 包中：

```yaml
# 旧写法：隔离后第二条读不到 random
- 'js: var random = Math.floor(Math.random() * 100);'
- 'js: player.sendMessage("随机数: " + random);'

# 新写法
- 'js: var random = Math.floor(Math.random() * 100); player.sendMessage("随机数: " + random);'
```

### 5. 防止动作列表直接调用自身

为了避免直接递归，菜单内 `Events.Click` 动作列表和全局 actions 包都不能直接调用自身。

```yaml
Events:
  Click:
    test:
      - 'actions: test'
```

检测到直接自调用时，KaMenu 会跳过这一次 `actions:` 调用，并继续执行后续动作。

### 6. 加固自定义指令取消注册逻辑

自定义指令 reload 时现在只会移除 KaMenu 自己注册的 `Command` 实例，避免与其他插件存在同名指令时误删其他插件的命令映射。

### 7. 启动信息新增包数量

服务器启动时的 KaMenu 信息面板现在会显示已加载的全局 actions 动作包数量和 JavaScript 包数量，便于确认包是否被正确加载。

### 8. 首次启动释放全局包示例

当服务器首次创建 `plugins/KaMenu/actions/` 或 `plugins/KaMenu/js/` 文件夹时，KaMenu 会释放内置示例包，方便用户快速理解包结构：

```text
plugins/KaMenu/actions/example/welcome.yml -> example/welcome
plugins/KaMenu/js/example/message.js -> example/message
```

若对应文件夹已存在，则不会自动写入示例文件，避免影响现有服务器文件。

### 9. 加固全局包加载规则与错误提示

全局 actions 包和 JavaScript 包现在会统一校验包 ID 与文件大小：

- 包 ID 只能使用英文、数字、`_`、`-`、`.`、`/`
- 包 ID 不能包含 `..`，不能以 `/` 开头或结尾，不能包含连续的 `//`
- 单个包文件大小上限为 `1 MiB`
- 非法包名、重复包名、文件过大、加载失败等提示已接入中英文 i18n

### 10. reload 指令支持指定模块

`/km reload` 不填写目标时仍默认重载全部模块，同时新增以下目标用于精确重载：

```bash
/km reload all
/km reload menu
/km reload actions
/km reload js
/km reload lang
/km reload config
```

其中 `lang` 可单独重载当前语言文件；`config` 会重载 `config.yml`、语言文件和自定义指令；`actions` 与 `js` 可分别只重载全局动作包和全局 JavaScript 包。

每个重载目标都会返回总数、成功数、失败数和耗时 ms；不填写目标或使用 `all` 时会依次输出各模块的重载结果。

### 11. 放开自定义语言文件

`/km language` 不再硬编码限制为 `zh_CN` 和 `en_US`。用户可以在 `plugins/KaMenu/lang/` 下添加自己的语言文件，例如：

```text
plugins/KaMenu/lang/de_DE.yml -> /km language de_DE
```

语言 ID 允许英文、数字、`_`、`-`，Tab 补全会自动读取当前 `lang` 文件夹下的 `.yml` 文件。

---

## 📝 文档更新

- 更新中文 actions 文档，补充全局 actions 包、内置示例包、查找优先级、参数规则和自调用行为
- 更新英文 actions 文档
- 新增中文/英文 `actions` 文件夹与 `js` 文件夹独立配置页
- 更新中文/英文 JavaScript 文档，补充全局 JS 包、内置示例包、返回值规则、包参数规则和执行上下文隔离说明
- 更新中文/英文自定义指令文档，补充 `args` Tab 补全
- 更新全局 actions / JavaScript 包文档，补充包 ID 规则、`1 MiB` 文件大小限制和本地化错误提示
- 更新命令文档，补充 `/km reload [all|menu|actions|js|lang|config]` 和自定义语言文件说明
- 更新 KaMenu 菜单编写 skill，加入全局 actions 包、全局 JavaScript 包、内置示例包、新参数解析规则和包安全限制

---

## 📝 兼容说明

- 旧版 `custom-commands` 字符串写法保持兼容
- 旧版 `actions: name,arg0,arg1` 写法保持兼容
- 菜单内 `Events.Click` 优先级高于全局 actions 包
- 菜单内 `JavaScript` 优先级高于全局 JavaScript 包
- `js:` / `{js:...}` / JavaScript 包调用之间不再共享临时变量或函数；依赖连续多条 `js:` 共享变量的旧菜单需要合并为单条脚本或迁移到 JavaScript 包
- 配置文件版本无需升级；需要使用参数补全时，可手动为对象写法指令添加 `args`
- 全局 actions 包仅加载 `.yml` 文件，不加载 `.yaml`
- 全局 JavaScript 包仅加载 `.js` 文件
- 单个全局包文件大小不能超过 `1 MiB`
- 内置全局包示例只会在对应运行目录不存在时释放，不会覆盖已有目录或文件

---

## 🚀 升级指南

### 从 1.5.6 升级到 1.6.0

1. 替换插件 jar 文件为 `KaMenu-1.6.0.jar`
2. 完整重启服务器
3. 如需使用全局 actions 包，在 `plugins/KaMenu/actions/` 下创建 `.yml` 文件
4. 如需使用全局 JavaScript 包，在 `plugins/KaMenu/js/` 下创建 `.js` 文件
5. 如需使用自定义指令参数补全，在 `custom-commands` 的对象写法中添加 `args`

---

# KaMenu v1.6.0 Update Report

## 📋 Version Info
- **Version**: 1.6.0
- **Release Date**: July 7, 2026

---

## ✨ New Features

### 1. Added global actions packages

KaMenu now supports global actions packages under `plugins/KaMenu/actions/`. One `.yml` file is one package, and the package ID is the relative path without the `.yml` suffix.

```text
plugins/KaMenu/actions/reward/daily.yml -> reward/daily
```

Package file format:

```yaml
actions:
  - 'toast: type=task;msg=Claimed;icon=emerald'
  - 'money: type=add;num={arg:0}'
```

Global packages can be called from any menu action list, clickable text action, periodic task, or custom command action list:

```yaml
- 'actions: reward/daily,100'
```

Lookup priority:

1. `Events.Click.<action_list_name>` in the current menu
2. Global package `plugins/KaMenu/actions/<action_list_name>.yml`

If a menu `Events.Click` entry and a global package use the same name, the menu action list has priority.

### 2. Added global JavaScript packages

KaMenu now supports global JavaScript packages under `plugins/KaMenu/js/`. One `.js` file is one package, and the package ID is the relative path without the `.js` suffix.

```text
plugins/KaMenu/js/reward/message.js -> reward/message
```

The package file content is JavaScript code:

```javascript
var amount = args[0] || "0";
var target = args[1] || name;
"Congratulations, " + target + " received " + amount + " coins";
```

Global JS packages can be called from actions, text, and conditions:

```yaml
- 'js: [reward/message],100,Steve'
text: 'Message: {js:[reward/message],100,Steve}'
condition: '{js:[check/vip]} == true'
```

Lookup priority:

1. `JavaScript.<package_name>` in the current menu
2. Global JavaScript package `plugins/KaMenu/js/<package_name>.js`

If a menu `JavaScript` entry and a global JS package use the same name, the menu script has priority.

### 3. Custom command argument Tab completion

Object-form `custom-commands` entries in `config.yml` now support an `args` key for command argument Tab completion:

```yaml
custom-commands:
  test2:
    args:
      0: "[tp, tphere]"
      1: "%kamenu_online_players%"
    actions:
      - condition: "{arg:0} == tp"
        allow:
          - "tell: You will teleport to {arg:1}"
```

- `args` uses 0-based indexes, matching `{arg:0}`, `{arg:1}`, and other action variables
- Candidates support YAML lists, comma-separated strings, and simple bracket lists such as `[a, b]`
- PAPI placeholders such as `%kamenu_online_players%` are resolved in real time when Tab is pressed
- KaMenu built-in variables such as `{list:friends}` and `{glist:warps}` are also resolved in real time
- Object-form menu commands may use `menu: menuId` together with `args`

---

## 🔧 Improvements And Hardening

### 1. Unified argument parsing for actions and JavaScript packages

Action-list calls and JavaScript package calls now support both comma-separated and space-separated arguments:

```yaml
- 'actions: reward/daily,100,vip'
- 'actions: reward/daily 100 vip'
- 'js: [reward_message],100,Steve'
- 'js: [reward_message] 100 Steve'
```

When an argument contains a space or comma, wrap it with single quotes, double quotes, or backticks:

```yaml
- 'actions: reward/daily,"100 coins",`vip,plus`'
- 'js: [reward_message],"100 coins",`Steve,Alex`'
```

### 2. Hardened JavaScript package-call detection

`js:` and `{js:...}` now enter package-call parsing only when the content starts with a valid `[package_name]` prefix. Otherwise, the entire content is executed as JavaScript code or expression.

```yaml
# Package call
- 'js: [reward_message],100,Steve'

# Plain JavaScript code, not split by commas or spaces
- 'js: player.sendMessage("hello, Steve")'
```

This prevents plain JavaScript expressions from being split incorrectly because they contain commas or spaces.

### 3. Removed automatic result messages from js actions

`js:` actions no longer require JavaScript to output a value, and KaMenu no longer automatically sends `JS Result` messages to players.

- `js:` actions simply execute scripts, with or without a return value
- `{js:...}` text placeholders resolve to an empty string when the script has no return value

### 4. JavaScript execution context isolation

Each `js:`, `{js:...}`, and JavaScript package call now uses an isolated execution context. This prevents global variables, functions, or undeclared variables created by one script from leaking into other players, menus, or packages.

If an old menu splits one logical script into multiple `js:` actions and relies on variables or functions from a previous action, merge it into one `js:` action or move it into a menu `JavaScript` / global JS package:

```yaml
# Old style: after isolation, the second action cannot read random
- 'js: var random = Math.floor(Math.random() * 100);'
- 'js: player.sendMessage("Random number: " + random);'

# New style
- 'js: var random = Math.floor(Math.random() * 100); player.sendMessage("Random number: " + random);'
```

### 5. Prevented direct action-list self-calls

To avoid direct recursion, both menu `Events.Click` action lists and global actions packages cannot directly call themselves.

```yaml
Events:
  Click:
    test:
      - 'actions: test'
```

When a direct self-call is detected, KaMenu skips that `actions:` call and continues with the following actions.

### 6. Hardened custom command unregistration

During reload, KaMenu now removes only the `Command` instances it registered itself. This avoids accidentally removing another plugin's command mapping when command names conflict.

### 7. Startup information now shows package counts

KaMenu's startup information panel now displays the number of loaded global actions packages and JavaScript packages, making it easier to confirm whether packages were loaded correctly.

### 8. Built-in package examples on first startup

When KaMenu creates `plugins/KaMenu/actions/` or `plugins/KaMenu/js/` for the first time, it releases built-in example packages to help users understand the package structure:

```text
plugins/KaMenu/actions/example/welcome.yml -> example/welcome
plugins/KaMenu/js/example/message.js -> example/message
```

If the corresponding folder already exists, KaMenu does not write example files automatically, avoiding changes to existing server files.

### 9. Hardened global package loading and error messages

Global actions packages and JavaScript packages now use unified package ID and file size validation:

- Package IDs may only use letters, numbers, `_`, `-`, `.`, and `/`
- Package IDs cannot contain `..`, cannot start or end with `/`, and cannot contain consecutive `//`
- Each package file is limited to `1 MiB`
- Invalid package names, duplicate IDs, oversized files, and load failures now use Chinese/English i18n messages

### 10. reload command supports selected modules

`/km reload` still reloads all modules when no target is provided. You can now specify a target for more precise reloads:

```bash
/km reload all
/km reload menu
/km reload actions
/km reload js
/km reload lang
/km reload config
```

`lang` reloads only the current language file. `config` reloads `config.yml`, language files, and custom commands. `actions` and `js` reload only global action packages and global JavaScript packages.

Each reload target returns total, success, failed, and elapsed ms. When no target is provided, or when `all` is used, KaMenu prints each module's reload result in sequence.

### 11. Custom language files

`/km language` is no longer hardcoded to `zh_CN` and `en_US`. Users may add their own language files under `plugins/KaMenu/lang/`, for example:

```text
plugins/KaMenu/lang/de_DE.yml -> /km language de_DE
```

Language IDs may use letters, numbers, `_`, and `-`. Tab completion reads `.yml` files from the current `lang` folder.

---

## 📝 Documentation Updates

- Updated Chinese actions documentation with global actions packages, built-in examples, lookup priority, argument rules, and self-call behavior
- Updated English actions documentation
- Added standalone Chinese/English configuration pages for the `actions` folder and `js` folder
- Updated Chinese/English JavaScript documentation with global JS packages, built-in examples, return-value rules, package argument rules, and execution context isolation notes
- Updated Chinese/English custom command documentation with `args` Tab completion
- Updated global actions / JavaScript package documentation with package ID rules, the `1 MiB` file size limit, and localized error messages
- Updated command documentation with `/km reload [all|menu|actions|js|lang|config]` and custom language file notes
- Updated the KaMenu menu-author skill with global actions packages, global JavaScript packages, built-in examples, the new argument parsing rules, and package safety limits

---

## 📝 Compatibility Notes

- Legacy string-form `custom-commands` entries remain compatible
- Legacy `actions: name,arg0,arg1` syntax remains compatible
- Menu `Events.Click` entries have priority over global actions packages
- Menu `JavaScript` entries have priority over global JavaScript packages
- `js:` / `{js:...}` / JavaScript package calls no longer share temporary variables or functions; old menus that rely on consecutive `js:` actions sharing variables should merge them into one script or move them into a JavaScript package
- No config version upgrade is required; add `args` manually to object-form commands when argument completion is needed
- Global actions packages only load `.yml` files, not `.yaml`
- Global JavaScript packages only load `.js` files
- Each global package file must be no larger than `1 MiB`
- Built-in global package examples are released only when the corresponding runtime folder does not exist; existing folders or files are not overwritten

---

## 🚀 Upgrade Guide

### Upgrading from 1.5.6 to 1.6.0

1. Replace the plugin jar with `KaMenu-1.6.0.jar`
2. Fully restart the server
3. To use global actions packages, create `.yml` files under `plugins/KaMenu/actions/`
4. To use global JavaScript packages, create `.js` files under `plugins/KaMenu/js/`
5. To use custom command argument completion, add `args` to object-form `custom-commands` entries
