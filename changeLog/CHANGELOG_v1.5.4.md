# KaMenu v1.5.4 更新报告

## 📋 版本信息
- **版本号**: 1.5.4
- **发布日期**: 2026年7月4日

---

## ✨ 新增功能

### 1. 新增 `isNull` 条件方法

用于判断变量、PAPI 或输入值是否为空。

```yaml
condition: "isNull.%some_placeholder%"
condition: "isNull.{data:nickname}"
condition: "isNull.$(nickname)"
condition: "!isNull.$(nickname)"
```

`isNull` 会将以下值视为空：

- 空字符串
- 仅包含空格的字符串
- `null` 字符串（不区分大小写）

适合用于表单校验、数据是否存在判断、PAPI 返回值兜底等场景。

### 2. 新增 `isPass` 条件方法

用于判断值是否为“绝对空字符串”。

```yaml
condition: "isPass.$(nickname)"
condition: "!isPass.$(nickname)"
```

`isPass` 只有在字符串长度为 `0` 时才通过；如果玩家输入一个或多个空格，则不会通过。需要将空格也视为空时，请使用 `isNull`。

### 3. 新增 `isTrue` 条件方法

用于判断变量是否为真值。

```yaml
condition: "isTrue.$(agree)"
condition: "isTrue.{data:enabled}"
condition: "isTrue.%some_placeholder%"
condition: "!isTrue.$(agree)"
```

`isTrue` 接受：

- `true`（不区分大小写）
- `yes`（不区分大小写）
- `1`

其他值、空值和空格字符串都视为 false。

### 4. 新增 `getLength` 字符串长度函数

`getLength` 用于输出字符串长度，通常配合比较运算符使用。

```yaml
condition: "getLength.%player_name% >= 3"
condition: "getLength.$(nickname) <= 12"
condition: "getLength.{data:nickname} == 0"
```

`getLength.xxx` 会先解析变量，再计算解析后字符串的长度。适合用于限制昵称、留言、搜索关键字等输入长度。

---

## 🔧 优化与加固

### 1. 条件方法参数解析增强

条件引擎现在会保护 `isNull`、`isPass`、`isTrue` 和 `getLength` 的参数内容，避免空字符串或空格字符串在表达式解析阶段被丢失。

这使以下场景可以按预期工作：

```yaml
condition: "isPass.$(nickname)"
condition: "isNull.$(nickname)"
condition: "getLength.$(nickname) > 12"
```

### 2. 明确条件变量解析顺序

文档补充了条件判断的运行顺序：KaMenu 会先解析 `{data:*}`、`{gdata:*}`、`{meta:*}`、`%papi%`、`$(input)`、`{arg:*}` 等变量，再将解析后的结果交给条件表达式引擎和内置方法判断。

### 3. 条件表达式注入防护

运行时变量值现在会作为完整字符串参与条件判断，不会被重新解释为 `||`、`&&`、比较符、括号等表达式语法。

这可以避免玩家通过输入内容改变条件逻辑，例如：

```yaml
condition: "$(amount) <= 100"
```

如果玩家输入 `1 || true`，该输入会作为普通字符串值处理，不会让条件表达式被改写为逻辑或判断。

---

## 📝 文档更新

- 更新中文条件判断文档：`docs/menu/conditions.md`
- 更新英文条件判断文档：`docs-en/menu/conditions.md`
- 更新 KaMenu 菜单编写 skill 条件参考

---

## 📝 兼容说明

- 旧菜单语法无需修改
- 本次新增功能只扩展条件表达式能力，不改变现有 `isNum`、`hasPerm`、`inList` 等条件方法行为
- `getLength` 是值函数，推荐搭配 `==`、`!=`、`>`、`>=`、`<`、`<=` 使用

---

## 🚀 升级指南

### 从 1.5.3 升级到 1.5.4

1. 替换插件 jar 文件为 `KaMenu-1.5.4.jar`
2. 完整重启服务器
3. 可选：将原本通过复杂比较实现的空值判断改为 `isNull` / `isPass`
4. 可选：使用 `isTrue` 校验复选框、开关变量或外部 PAPI 返回值
5. 可选：使用 `getLength` 对输入框内容进行长度限制

---

# KaMenu v1.5.4 Update Report

## 📋 Version Info
- **Version**: 1.5.4
- **Release Date**: July 4, 2026

---

## ✨ New Features

### 1. Added `isNull` condition method

Checks whether a variable, PAPI placeholder, or input value is empty.

```yaml
condition: "isNull.%some_placeholder%"
condition: "isNull.{data:nickname}"
condition: "isNull.$(nickname)"
condition: "!isNull.$(nickname)"
```

`isNull` treats the following values as empty:

- Empty strings
- Whitespace-only strings
- The string `null`, case-insensitive

This is useful for form validation, data-existence checks, and PlaceholderAPI fallback logic.

### 2. Added `isPass` condition method

Checks whether a value is an absolute empty string.

```yaml
condition: "isPass.$(nickname)"
condition: "!isPass.$(nickname)"
```

`isPass` only passes when the string length is `0`. One or more spaces do not pass. Use `isNull` when whitespace should count as empty.

### 3. Added `isTrue` condition method

Checks whether a variable is truthy.

```yaml
condition: "isTrue.$(agree)"
condition: "isTrue.{data:enabled}"
condition: "isTrue.%some_placeholder%"
condition: "!isTrue.$(agree)"
```

`isTrue` accepts:

- `true`, case-insensitive
- `yes`, case-insensitive
- `1`

Other values, empty values, and whitespace-only strings are treated as false.

### 4. Added `getLength` string length function

`getLength` outputs the length of a string and is usually used with comparison operators.

```yaml
condition: "getLength.%player_name% >= 3"
condition: "getLength.$(nickname) <= 12"
condition: "getLength.{data:nickname} == 0"
```

`getLength.xxx` resolves variables first, then calculates the length of the resolved string. It is useful for limiting nicknames, messages, search keywords, and similar input values.

---

## 🔧 Improvements

### 1. Enhanced condition method argument parsing

The condition engine now preserves argument content for `isNull`, `isPass`, `isTrue`, and `getLength`, so empty strings and whitespace strings are not lost during expression parsing.

These cases now work as expected:

```yaml
condition: "isPass.$(nickname)"
condition: "isNull.$(nickname)"
condition: "getLength.$(nickname) > 12"
```

### 2. Documented condition variable resolution order

The documentation now explains that KaMenu resolves `{data:*}`, `{gdata:*}`, `{meta:*}`, `%papi%`, `$(input)`, `{arg:*}`, and similar variables before passing the result to the condition expression engine and built-in methods.

### 3. Condition expression injection protection

Runtime variable values are now treated as complete string values in condition checks. They are not reinterpreted as `||`, `&&`, comparison operators, parentheses, or other expression syntax.

This prevents player input from changing condition logic. For example:

```yaml
condition: "$(amount) <= 100"
```

If the player enters `1 || true`, the input is handled as ordinary string content and does not rewrite the condition into an OR expression.

---

## 📝 Documentation

- Updated Chinese condition documentation: `docs/menu/conditions.md`
- Updated English condition documentation: `docs-en/menu/conditions.md`
- Updated the KaMenu menu author skill condition reference

---

## 📝 Compatibility Notes

- Existing menu syntax does not need changes
- This release only extends condition expressions and does not change existing behavior for `isNum`, `hasPerm`, `inList`, or other condition methods
- `getLength` is a value function and should usually be used with `==`, `!=`, `>`, `>=`, `<`, or `<=`

---

## 🚀 Upgrade Guide

### Upgrading from 1.5.3 to 1.5.4

1. Replace the plugin jar with `KaMenu-1.5.4.jar`
2. Fully restart the server
3. Optionally replace complex empty-value checks with `isNull` / `isPass`
4. Optionally use `isTrue` for checkboxes, switch-like variables, or external PAPI return values
5. Optionally use `getLength` to limit input field length
