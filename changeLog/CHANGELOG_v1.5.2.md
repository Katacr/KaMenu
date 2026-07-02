# KaMenu v1.5.2 更新报告

## 📋 版本信息
- **版本号**: 1.5.2
- **发布日期**: 2026年7月1日

---

## ✨ 新增功能

### 1. 自定义绑定指令支持 actions 动作列表

`config.yml` 的 `custom-commands` 现在除了原有的“指令直接打开菜单”写法，也支持直接配置 actions 动作队列。

旧写法保持兼容：

```yaml
custom-commands:
  zcd: 'example/main_menu'
```

新增写法：

```yaml
custom-commands:
  test:
    actions:
      - "tell: 嘿，你输入了/test指令"
      - "sound: entity.experience_orb.pickup;volume=1.0;pitch=1.3"
      - "tell: 你想测试什么内容呢？"
```

动作队列与按钮 actions 使用同一套执行逻辑，支持：

- 普通动作
- 条件判断与 `allow` / `deny`
- 嵌套动作列表
- `wait` 延迟
- `return` 中断
- 目标选择器
- 复杂条件表达式

### 2. 自定义指令动作支持命令参数变量

自定义指令动作队列可以读取玩家输入的参数：

- `{arg:0}`：第 1 个参数
- `{arg:1}`：第 2 个参数
- `{args}`：完整参数文本
- `{arg_count}`：参数数量
- `{command}`：实际触发的指令标签

示例：

```yaml
custom-commands:
  greet:
    actions:
      - "tell: &a你好 {arg:0}，欢迎来到 {arg:1}"
```

---

## 🔧 修复与优化

### 1. 修复目标选择器解析错误

修复动作目标选择器在复杂条件中可能被错误截断的问题。

受影响的场景包括：

```yaml
- "tell: 服务器公告{player: hasPerm.vip || hasPerm.admin}"
- "toast: type=task;msg=VIP;icon=diamond{player: %player_level% >= 10}"
```

现在选择器会从 `{player:` 开始正确匹配到对应的闭合 `}`，避免条件表达式中包含变量、JS 或复杂逻辑时被提前截断。

### 2. 改进自定义指令配置容错日志

- 菜单型自定义指令指向不存在的菜单时会输出明确 warning
- actions 型自定义指令配置为空或格式错误时会跳过注册并提示配置项名称
- 注册失败时会记录对应指令名与错误原因

---

## 📝 兼容说明

- 旧的 `custom-commands: 指令名: 菜单ID` 写法完全兼容
- 新的 actions 写法不需要创建菜单文件，适合轻量功能指令
- 自定义指令 actions 没有当前菜单上下文，因此不建议使用依赖菜单配置的 `reset` 或 `actions: Events.Click动作包`
- 需要从指令中打开菜单时，请使用 `open: 菜单ID`

---

## 🚀 升级指南

### 从 1.5.0 / 1.5.1 升级到 1.5.2

1. 替换插件 jar 文件为 `KaMenu-1.5.2.jar`
2. 完整重启服务器，或在确认无任务残留后执行 `/km reload`
3. 可选：在 `config.yml` 的 `custom-commands` 中添加 actions 型指令
4. 如使用复杂 `{player: ...}` 目标选择器，建议升级到本版本

---

# KaMenu v1.5.2 Update Report

## 📋 Version Info
- **Version**: 1.5.2
- **Release Date**: July 1, 2026

---

## ✨ New Features

### 1. Custom bound commands now support actions lists

`custom-commands` in `config.yml` now supports running an actions list directly, in addition to the existing command-to-menu shortcut format.

The legacy format remains compatible:

```yaml
custom-commands:
  zcd: 'example/main_menu'
```

New actions format:

```yaml
custom-commands:
  test:
    actions:
      - "tell: Hey, you ran /test"
      - "sound: entity.experience_orb.pickup;volume=1.0;pitch=1.3"
      - "tell: What would you like to test?"
```

The action queue uses the same execution logic as button actions and supports:

- Normal actions
- Conditional branches with `allow` / `deny`
- Nested action lists
- `wait` delays
- `return` interruption
- Target selectors
- Complex condition expressions

### 2. Command argument variables for custom command actions

Custom command actions can read the arguments entered by the player:

- `{arg:0}`: first argument
- `{arg:1}`: second argument
- `{args}`: full argument text
- `{arg_count}`: argument count
- `{command}`: the command label used by the player

Example:

```yaml
custom-commands:
  greet:
    actions:
      - "tell: &aHello {arg:0}, welcome to {arg:1}"
```

---

## 🔧 Fixes and Improvements

### 1. Fixed target selector parsing

Fixed an issue where action target selectors could be truncated incorrectly when they contained complex conditions.

Affected examples include:

```yaml
- "tell: Server announcement{player: hasPerm.vip || hasPerm.admin}"
- "toast: type=task;msg=VIP;icon=diamond{player: %player_level% >= 10}"
```

Selectors are now matched correctly from `{player:` to the corresponding closing `}`, preventing early truncation when the selector contains variables, JavaScript, or complex logic.

### 2. Improved custom command config diagnostics

- Menu commands pointing to missing menus now log a clear warning
- Empty or invalid actions command configs are skipped with the config key name
- Command registration failures now include the command name and error reason

---

## 📝 Compatibility Notes

- The legacy `custom-commands: command: menuId` format remains fully compatible
- The new actions format does not require a menu file and is suitable for lightweight feature commands
- Custom command actions do not have a current menu context, so `reset` and `actions: Events.ClickAction` are not recommended there
- Use `open: menuId` when a command should open a menu

---

## 🚀 Upgrade Guide

### Upgrading from 1.5.0 / 1.5.1 to 1.5.2

1. Replace the plugin jar with `KaMenu-1.5.2.jar`
2. Fully restart the server, or run `/km reload` after confirming no menu tasks are left running
3. Optionally add actions-based custom commands in `config.yml`
4. Upgrade to this version if you use complex `{player: ...}` target selectors
