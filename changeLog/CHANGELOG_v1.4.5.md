# KaMenu v1.4.5 更新报告

## 📋 版本信息
- **版本号**: 1.4.5
- **发布日期**: 2026年6月17日

---

## 🔧 修复与优化

### 1. 优化动作执行链

- 统一 `wait`、条件判断和嵌套 `actions` 的执行顺序
- 修复 `Events.Open` 在包含延迟动作时可能提前打开菜单的问题
- 保证 `return` 可在嵌套动作链中正确中断后续执行

### 2. 新增 `{js:...}` 内置变量

- 支持在任意文本位置使用 `{js:xxx}`
- 可用于条件判断和普通文本拼接
- 仅接受有返回值的 JS 表达式，返回空值时按空串处理

### 3. 优化条件与变量解析

- 保持 `allow/deny` 条件分支语法不变
- 支持输入组件变量在嵌套 `actions` 和 `condition` 内继续传递
- 统一文本解析入口，减少各处重复处理

---

## 📝 兼容说明

- 不需要修改旧菜单语法
- 现有 `wait`、条件嵌套、`actions` 跳转写法可继续使用

---

# KaMenu v1.4.5 Update Report

## 📋 Version Info
- **Version**: 1.4.5
- **Release Date**: June 17, 2026

---

## 🔧 Fixes and Improvements

### 1. Improved action execution chain

- Unified the execution order of `wait`, conditions, and nested `actions`
- Fixed menus opening too early when `Events.Open` contains delayed actions
- Ensured `return` correctly stops later actions in nested chains

### 2. Added `{js:...}` built-in placeholder

- Supports `{js:xxx}` in any text position
- Usable in conditions and normal text concatenation
- Only the returned value is used; empty or missing results become an empty string

### 3. Improved condition and variable resolution

- Kept the `allow/deny` condition syntax unchanged
- Preserved input component variables inside nested `actions` and `condition`
- Unified the text resolution entry to reduce duplicated handling

---

## 📝 Compatibility Notes

- No old menu syntax changes are required
- Existing `wait`, nested conditions, and `actions` jumps continue to work

---

## 🚀 Upgrade Guide

### Upgrading from 1.4.4 to 1.4.5

**Steps:**

1. **Update the plugin**
   - Download the latest `KaMenu-1.4.5.jar`
   - Replace the old jar file

2. **Restart the server**
   - Fully stop the server
   - Start it again

3. **(Optional) Use new features**
   - Add `{js:...}` to text or condition expressions

**Compatibility:**
- ✅ Existing menu configs remain fully compatible
- ✅ No config changes are required if you do not use the new placeholder

---

## 📈 Version Comparison

| Feature | 1.4.4 | 1.4.5 |
|---------|-------|-------|
| Unified action chain execution | ❌ | ✅ |
| `{js:...}` built-in placeholder | ❌ | ✅ |
| `Events.Open` waits for full completion | ❌ | ✅ |
| Input variables preserved in nested `actions` | ❌ | ✅ |
