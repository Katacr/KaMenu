# KaMenu v1.5.5 更新报告

## 📋 版本信息
- **版本号**: 1.5.5
- **发布日期**: 2026年7月6日

---

## ✨ 新增功能

### 1. 新增输入捕获全局空格处理配置

`config.yml` 新增 `input-capture.trim-edge-spaces`：

```yaml
input-capture:
  trim-edge-spaces: false
```

开启后，玩家提交文本输入框时，KaMenu 会在写入 `$(输入键名)` 前自动移除内容前后的空格。

适合玩家名、指令参数、数字、ID、权限节点等对首尾空格敏感的输入场景。

### 2. 文本输入框新增 `remove_chars`

`Inputs` 的 `type: input` 文本输入框现在可以配置 `remove_chars`，用于删除玩家输入中的指定字符。

```yaml
Inputs:
  player_name:
    type: 'input'
    text: '&a请输入玩家名'
    remove_chars: '&_'
```

也支持列表写法：

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&a请输入参数'
    remove_chars:
      - '&'
      - '_'
      - '"'
```

### 3. `remove_chars` 支持特殊转义字符

为了处理不可见字符，`remove_chars` 现在支持以下转义写法：

| 写法 | 含义 |
|------|------|
| `\s` | 普通空格 |
| `\n` | 换行 |
| `\r` | 回车 |
| `\t` | Tab |
| `\\` | 反斜杠 `\` |

例如移除颜色符号、下划线、空格和换行：

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&a请输入参数'
    remove_chars:
      - '&'
      - '_'
      - '\s'
      - '\n'
```

### 4. 新增全局字符移除列表

`config.yml` 的 `input-capture` 下新增 `remove-char-lists`，可以集中定义常用输入过滤规则：

```yaml
input-capture:
  remove-char-lists:
    global:
      - '&'
      - '_'
      - '\s'
      - '\n'
```

菜单内可直接引用预设名：

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&a请输入参数'
    remove_chars: global
```

如果 `remove_chars` 的字符串值匹配全局预设名，则使用该预设；如果没有匹配到预设名，则继续按旧规则作为字面字符集合处理。

---

## 🔧 优化与加固

### 1. 输入清洗统一进入变量上下文

输入清洗发生在 Paper Dialog 回调捕获输入后、写入 `variables` 前。

因此后续读取同一个 `$(key)` 的位置都会拿到一致的处理结果，包括：

- 动作列表
- 条件判断
- JavaScript 参数
- 动作包参数
- 外部 action handler 收到的 variables

### 2. 保持旧菜单兼容

`input-capture.trim-edge-spaces` 默认关闭，旧菜单不会因为升级自动改变输入内容。

未配置 `remove_chars` 的文本输入框也会保持原样输入。

---

## 📝 文档更新

- 更新中文输入组件文档：`docs/menu/inputs.md`
- 更新英文输入组件文档：`docs-en/menu/inputs.md`
- 更新中文配置文件文档：`docs/config/config.md`
- 更新英文配置文件文档：`docs-en/config/config.md`

---

## 📝 兼容说明

- 旧菜单无需修改
- `remove_chars` 只作用于 `type: input` 文本输入框
- 不影响 `slider`、`checkbox`、`dropdown`
- 默认不自动移除首尾空格，需要在 `config.yml` 手动开启
- 多行输入中的换行会原样存储；如需移除，请配置 `remove_chars: ['\n']`

---

## 🚀 升级指南

### 从 1.5.4 升级到 1.5.5

1. 替换插件 jar 文件为 `KaMenu-1.5.5.jar`
2. 完整重启服务器
3. 插件会自动将 `config-version` 升级到 `5` 并补充 `input-capture` 配置
4. 如果希望所有文本输入自动移除前后空格，可设置：

```yaml
input-capture:
  trim-edge-spaces: true
```

5. 对高风险输入字段按需配置 `remove_chars`
6. 如果多个菜单需要相同过滤规则，可在 `input-capture.remove-char-lists` 中定义预设，并在菜单中使用 `remove_chars: global`

---

# KaMenu v1.5.5 Update Report

## 📋 Version Info
- **Version**: 1.5.5
- **Release Date**: July 6, 2026

---

## ✨ New Features

### 1. Added global input capture trimming option

`config.yml` now includes `input-capture.trim-edge-spaces`:

```yaml
input-capture:
  trim-edge-spaces: false
```

When enabled, KaMenu trims leading and trailing spaces from submitted text input values before writing them into `$(input_key)`.

This is useful for player names, command arguments, numbers, IDs, permission nodes, and other inputs where accidental edge spaces can break later logic.

### 2. Added `remove_chars` for text input fields

`type: input` fields under `Inputs` can now define `remove_chars` to remove specific characters from player input.

```yaml
Inputs:
  player_name:
    type: 'input'
    text: '&aEnter player name'
    remove_chars: '&_'
```

List form is also supported:

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&aEnter argument'
    remove_chars:
      - '&'
      - '_'
      - '"'
```

### 3. `remove_chars` supports escape sequences

To handle invisible characters, `remove_chars` now supports:

| Syntax | Meaning |
|--------|---------|
| `\s` | Normal space |
| `\n` | Newline |
| `\r` | Carriage return |
| `\t` | Tab |
| `\\` | Backslash `\` |

Example: remove color symbols, underscores, spaces, and newlines:

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&aEnter argument'
    remove_chars:
      - '&'
      - '_'
      - '\s'
      - '\n'
```

### 4. Added global character removal lists

`config.yml` now supports `remove-char-lists` under `input-capture`, allowing common input filtering rules to be maintained in one place:

```yaml
input-capture:
  remove-char-lists:
    global:
      - '&'
      - '_'
      - '\s'
      - '\n'
```

Menus can reference the preset by name:

```yaml
Inputs:
  command_arg:
    type: 'input'
    text: '&aEnter argument'
    remove_chars: global
```

If the string value of `remove_chars` matches a global preset name, KaMenu uses that preset. If no preset matches, the value keeps the legacy behavior and is treated as the literal set of characters to remove.

---

## 🔧 Improvements

### 1. Input cleanup is applied before variables are written

Input cleanup runs after Paper Dialog captures the submitted value and before KaMenu writes it into the variable context.

As a result, every later use of the same `$(key)` receives the same processed value, including:

- Action lists
- Conditions
- JavaScript arguments
- Action package arguments
- Variables passed to external action handlers

### 2. Existing menus remain compatible

`input-capture.trim-edge-spaces` is disabled by default, so upgrading does not automatically change existing input values.

Text inputs without `remove_chars` also keep their original submitted value.

---

## 📝 Documentation

- Updated Chinese input component documentation: `docs/menu/inputs.md`
- Updated English input component documentation: `docs-en/menu/inputs.md`
- Updated Chinese config documentation: `docs/config/config.md`
- Updated English config documentation: `docs-en/config/config.md`

---

## 📝 Compatibility Notes

- Existing menus do not need changes
- `remove_chars` only applies to `type: input` text fields
- It does not affect `slider`, `checkbox`, or `dropdown`
- Leading/trailing spaces are not trimmed by default; enable it manually in `config.yml`
- Multiline input stores newlines as-is; configure `remove_chars: ['\n']` if newlines should be removed

---

## 🚀 Upgrade Guide

### Upgrading from 1.5.4 to 1.5.5

1. Replace the plugin jar with `KaMenu-1.5.5.jar`
2. Fully restart the server
3. KaMenu will automatically upgrade `config-version` to `5` and add the `input-capture` section
4. To trim edge spaces for all text input captures, set:

```yaml
input-capture:
  trim-edge-spaces: true
```

5. Add `remove_chars` to high-risk input fields as needed
6. If multiple menus need the same filtering rule, define a preset under `input-capture.remove-char-lists` and use `remove_chars: global`
