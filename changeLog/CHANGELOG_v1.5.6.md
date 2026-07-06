# KaMenu v1.5.6 更新报告

## 📋 版本信息
- **版本号**: 1.5.6
- **发布日期**: 2026年7月6日

---

## 🐛 问题修复

### 1. 修复 Paper 26.2 环境下 `/km guide` 报错

修复在部分 Paper 26.2 / Adventure API 环境中执行 `/km guide` 时可能出现的运行时异常：

```text
java.lang.NoSuchMethodError:
net.kyori.adventure.text.Component.join(...)
```

该问题发生在菜单按钮 Tooltip 多行文本拼接时。旧实现依赖 `Component.join(Component.newline(), ...)`，该方法在当前运行环境中的方法签名与编译环境不一致，导致菜单打开失败。

现在已改为手动拼接 `Component`：

- 不再调用已废弃的 `Component.join(...)`
- 多行 Tooltip 仍然按换行显示
- 普通按钮、`notice`、`confirmation.confirm`、`confirmation.deny` 的 Tooltip 均已覆盖

---

## 🔧 优化

### 1. 提升 Adventure API 兼容性

按钮 Tooltip 组件拼接逻辑改为更稳定的 `Component.empty().append(...)` 方式，减少不同 Paper / Adventure API 版本之间的方法签名兼容风险。

---

## 📝 兼容说明

- 菜单语法无需修改
- 配置文件版本无需升级
- 不影响现有按钮、Tooltip、`guide.yml`、外部 API 菜单或普通菜单加载逻辑
- 本次为兼容性修复版本，建议所有使用 Paper 26.2 或更高版本测试构建的服务器升级

---

## 🚀 升级指南

### 从 1.5.5 升级到 1.5.6

1. 替换插件 jar 文件为 `KaMenu-1.5.6.jar`
2. 完整重启服务器
3. 执行 `/km guide` 验证向导菜单是否能正常打开

---

# KaMenu v1.5.6 Update Report

## 📋 Version Info
- **Version**: 1.5.6
- **Release Date**: July 6, 2026

---

## 🐛 Bug Fixes

### 1. Fixed `/km guide` crash on Paper 26.2

Fixed a runtime exception that could occur when running `/km guide` on some Paper 26.2 / Adventure API environments:

```text
java.lang.NoSuchMethodError:
net.kyori.adventure.text.Component.join(...)
```

The issue happened while joining multi-line button tooltip text. The previous implementation relied on `Component.join(Component.newline(), ...)`, whose runtime method signature may differ from the compile-time Adventure API signature.

The implementation now manually appends components:

- No longer calls the deprecated `Component.join(...)`
- Multi-line tooltips still render with line breaks
- Covers regular buttons, `notice`, `confirmation.confirm`, and `confirmation.deny` tooltips

---

## 🔧 Improvements

### 1. Improved Adventure API compatibility

Button tooltip component joining now uses the more stable `Component.empty().append(...)` pattern, reducing compatibility risk across different Paper / Adventure API versions.

---

## 📝 Compatibility Notes

- No menu syntax changes are required
- No config version upgrade is required
- Existing buttons, tooltips, `guide.yml`, external API menus, and regular menu loading behavior are unchanged
- This is a compatibility fix release and is recommended for servers using Paper 26.2 or newer test builds

---

## 🚀 Upgrade Guide

### Upgrading from 1.5.5 to 1.5.6

1. Replace the plugin jar with `KaMenu-1.5.6.jar`
2. Fully restart the server
3. Run `/km guide` to verify that the guide menu opens correctly
