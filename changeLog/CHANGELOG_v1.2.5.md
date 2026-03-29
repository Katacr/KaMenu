# KaMenu v1.2.5 更新报告

## 📋 版本信息
|- **版本号**: 1.2.5
|- **发布日期**: 2026年3月30日

---

## ✨ 新增功能

### 1. 灵活的物品材质名匹配

为物品材质名称匹配添加了智能规范化功能，支持多种格式的材质名称。此改进适用于：

- Body 组件的 item 物品展示
- condition 条件判断中的 hasItem 判断
- condition 条件判断中的 hasStockItem 判断
- actions 动作中的 item 操作
- actions 动作中的 stock-item 操作
- config 配置中的 item-lore 监听器

**支持的格式：**
- 标准格式：`DIAMOND_SWORD`
- 小写：`diamond_sword`
- 混合大小写：`DiAMond swORd`
- 短杠：`Diamond-Sword`
- 空格：`diamond sword`

**规范化规则：**
1. 大小写忽略：自动转换为标准大写
2. 分隔符统一：将短杠 `-` 和空格 ` ` 替换为下划线 `_`
3. 多余分隔符合并：多个连续的下划线会被合并为一个
4. 首尾清理：自动去除首尾空白

---

### 2. MiniMessage 文本格式支持

引入了 Minecraft 现代化的文本格式系统 MiniMessage，插件同时支持两种文本格式：

1. **Legacy 颜色代码**：`&a绿色文本`
2. **MiniMessage 格式**：`<green>绿色文本</green>`

**自动检测机制：**
- 检测到 MiniMessage 标签 `<...>` → 使用 MiniMessage 解析
- 未检测到 MiniMessage 标签 → 使用 Legacy 颜色代码解析
- 两种格式可以混合使用

**MiniMessage 优势：**
- 更现代的文本格式，支持更多样式（加粗、斜体、下划线等）
- 更清晰的标签结构，易于维护
- 与 Minecraft 官方 Adventure API 完全兼容
- 更好的国际化支持

---

### 3. 配置文件和语言文件自动更新机制

实现了智能的配置文件和语言文件管理机制。

**config.yml 自动更新：**
- 版本检测：检查当前配置文件的 `config-version` 字段
- 旧版本兜底：如果配置文件中没有 `config-version` 字段，自动设为 `0`
- 更新流程：
  1. 提取用户当前配置的所有键和值
  2. 备份旧配置文件
  3. 从插件内部复制新的默认配置文件
  4. 将用户配置的值写入新的配置文件

**语言文件自动更新：**
- 缺失检测：当调用某个语言键但文件中不存在时自动触发
- 自动补全：从插件内部加载对应的语言文件，如果插件内部没有该语言，则使用英语作为兜底
- 自动写入：将缺失的键自动写入用户的语言文件

---

### 4. 新增 chat 动作

添加了新的 `chat` 动作，让玩家在聊天框中发送消息。

**与其他动作的对比：**

| 动作 | 执行方式 | 权限要求 | 适用场景 |
|------|---------|---------|---------|
| `command` | 直接执行指令 | 需要玩家权限 | 执行插件指令（如 `/spawn`）|
| `chat` | 模拟玩家在聊天框输入 | 不需要特殊权限 | 发送聊天消息、执行需要玩家权限的指令 |
| `console` | 以控制台权限执行 | 不需要玩家权限 | 执行需要 OP 权限的指令 |

**使用场景：**
- 广播消息：让玩家发送系统公告或通知
- 聊天互动：在菜单中快速发送聊天消息
- 指令执行：执行需要玩家身份的指令
- 社交功能：发送私聊、公聊等

---

## 🔧 优化改进

### 1. 文档结构优化

- 将 `structure.md` 文档拆分为两页：
  - `structure.md` - 文件夹结构和节点说明
  - `creating_menu.md` - 创建菜单教程
- 添加了完整的入门教程，帮助新用户快速上手
- 优化了文档导航和索引

### 2. 文档内容完善

- 添加了物品材质名格式的详细说明
- 添加了 MiniMessage 格式的使用指南
- 添加了配置文件自动更新机制说明
- 添加了 `chat` 动作的完整文档

### 3. 示例菜单优化

- 更新了所有示例菜单，使用更清晰的格式
- 添加了更多实用示例
- 优化了注释和说明

---

## 🐛 Bug 修复

### 1. 修复 checkbox 的 on_true 和 on_false 不生效的问题

**问题描述：**
当用户为 checkbox 配置自定义的 `on_true` 和 `on_false` 值时（如 `on_true: "是"`, `on_false: "否"`），系统没有正确返回这些值，而是返回了 `"1"` 或 `"0"`。

**原因分析：**
原代码逻辑只在用户使用默认值 `on_true: "true"` 和 `on_false: "false"` 时才返回布尔字符串。

**修复方案：**
修改了变量提取逻辑，现在无论用户设置什么值，都会根据 checkbox 的选中状态返回对应的 `onTrue` 或 `onFalse` 文本。

---

## 📚 文档更新

### 新增文档

- `docs/menu/creating_menu.md` - 创建菜单教程
  - 从零开始创建菜单的完整教程
  - 包含 6 个步骤，逐步引导用户
  - 提供完整示例和测试指南
  - 常见问题解答

### 更新文档

- `docs/menu/structure.md` - 优化结构说明
  - 保留文件夹结构和节点说明
  - 简化内容，更易于理解
  - 添加下一步指引

- `docs/menu/body.md` - 添加文本格式支持
  - MiniMessage 和 Legacy 颜色代码说明
  - 自动检测机制介绍
  - 物品材质名格式支持说明

- `docs/menu/conditions_item.md` - 更新物品判断文档
  - 添加材质名格式支持说明
  - 更新注意事项（不区分大小写）

- `docs/menu/actions.md` - 添加 chat 动作文档
  - chat 动作详细说明
  - 与 command 动作的对比
  - 使用示例和场景说明

- `docs/config/config.md` - 添加配置文件自动更新说明
  - config.yml 自动更新机制
  - 语言文件自动更新机制
  - 兜底设计说明

- `docs/SUMMARY.md` - 更新目录索引
  - 添加 creating_menu.md 链接

---

## 🎯 兼容性说明

- ✅ 本版本完全兼容旧版本配置
- ✅ 新增功能不影响现有菜单的正常运行
- ✅ 旧格式（如 `DIAMOND_SWORD`）仍然完全支持
- ✅ Legacy 颜色代码仍然完全支持
- ✅ 建议更新后测试现有菜单，确保功能正常

---

## 📝 下一步计划

- 继续优化监听器功能
- 添加更多条件判断方法
- 扩展物品操作功能
- 优化用户界面体验

---

**感谢您使用 KaMenu！如有问题或建议，欢迎反馈。**

---

---

# KaMenu v1.2.5 Update Notes

## 📋 Version Information
|- **Version**: 1.2.5
|- **Release Date**: March 30, 2026

---

## ✨ New Features

### 1. Flexible Material Name Matching

Added intelligent normalization for material name matching, supporting multiple format variations. This improvement applies to:

- Body component item displays
- hasItem condition checks
- hasStockItem condition checks
- item actions
- stock-item actions
- item-lore listeners in config

**Supported Formats:**
- Standard: `DIAMOND_SWORD`
- Lowercase: `diamond_sword`
- Mixed case: `DiAMond swORd`
- Hyphen: `Diamond-Sword`
- Space: `diamond sword`

**Normalization Rules:**
1. Case-insensitive: Auto-convert to standard uppercase
2. Separator unification: Replace hyphens `-` and spaces ` ` with underscores `_`
3. Multi-separator merging: Multiple consecutive underscores are merged
4. Leading/trailing cleanup: Auto-remove leading and trailing whitespace

---

### 2. MiniMessage Text Format Support

Introduced Minecraft's modern text formatting system, MiniMessage, supporting both text formats simultaneously:

1. **Legacy Color Codes**: `&aGreen text`
2. **MiniMessage Format**: `<green>Green text</green>`

**Auto-Detection Mechanism:**
- MiniMessage tags `<...>` detected → Use MiniMessage parsing
- No MiniMessage tags detected → Use Legacy color code parsing
- Both formats can be mixed

**MiniMessage Advantages:**
- More modern text formatting with additional styles (bold, italic, underline, etc.)
- Clearer tag structure, easier to maintain
- Full compatibility with Minecraft's official Adventure API
- Better internationalization support

---

### 3. Config and Language File Auto-Update Mechanism

Implemented intelligent config and language file management.

**config.yml Auto-Update:**
- Version detection: Check `config-version` field in current config
- Fallback for old versions: Auto-set to `0` if `config-version` is missing
- Update process:
  1. Extract all keys and values from user's current config
  2. Backup old config file
  3. Copy new default config from plugin internals
  4. Write user config values to new config file

**Language File Auto-Update:**
- Missing detection: Auto-trigger when a language key is called but doesn't exist in file
- Auto-completion: Load corresponding language file from plugin internals, use English as fallback if not available
- Auto-write: Automatically write missing keys to user's language file

---

### 4. New chat Action

Added new `chat` action to let players send messages in the chat box.

**Comparison with Other Actions:**

| Action | Execution Method | Permission Requirement | Use Case |
|--------|------------------|----------------------|------------|
| `command` | Direct command execution | Requires player permission | Execute plugin commands (e.g., `/spawn`) |
| `chat` | Simulate player chat input | No special permission required | Send chat messages, execute player-permission commands |
| `console` | Execute with console permission | No player permission required | Execute OP-required commands |

**Use Cases:**
- Broadcasting: Allow players to send system announcements or notifications
- Chat interaction: Quick chat message sending from menu
- Command execution: Execute commands requiring player identity
- Social features: Send private chat, public chat, etc.

---

## 🔧 Optimizations

### 1. Documentation Structure Optimization

- Split `structure.md` into two pages:
  - `structure.md` - Folder structure and node descriptions
  - `creating_menu.md` - Menu creation tutorial
- Added comprehensive onboarding tutorial for new users
- Optimized documentation navigation and indexing

### 2. Documentation Content Enhancement

- Added detailed material name format explanations
- Added MiniMessage format usage guide
- Added config file auto-update mechanism description
- Added complete documentation for `chat` action

### 3. Example Menu Optimization

- Updated all example menus with clearer formatting
- Added more practical examples
- Optimized comments and explanations

---

## 🐛 Bug Fixes

### 1. Fixed checkbox on_true and on_false Not Working

**Issue Description:**
When users configured custom `on_true` and `on_false` values for checkbox (e.g., `on_true: "yes"`, `on_false: "no"`), the system didn't return these values correctly, instead returning `"1"` or `"0"`.

**Root Cause Analysis:**
Original code logic only returned boolean strings when users used default values `on_true: "true"` and `on_false: "false"`.

**Fix:**
Modified variable extraction logic. Now, regardless of what values users set, the system returns the corresponding `onTrue` or `onFalse` text based on checkbox's selected state.

---

## 📚 Documentation Updates

### New Documentation

- `docs/menu/creating_menu.md` - Menu creation tutorial
  - Complete tutorial starting from zero
  - 6 steps to guide users progressively
  - Full examples and testing guide
  - FAQ section

### Updated Documentation

- `docs/menu/structure.md` - Optimized structure explanation
  - Retained folder structure and node descriptions
  - Simplified content for better understanding
  - Added next steps guidance

- `docs/menu/body.md` - Added text format support
  - MiniMessage and Legacy color code explanations
  - Auto-detection mechanism introduction
  - Material name format support description

- `docs/menu/conditions_item.md` - Updated item condition documentation
  - Added material name format support explanation
  - Updated notes (case-insensitive)

- `docs/menu/actions.md` - Added chat action documentation
  - Detailed chat action explanation
  - Comparison with command action
  - Usage examples and scenario descriptions

- `docs/config/config.md` - Added config file auto-update explanation
  - config.yml auto-update mechanism
  - Language file auto-update mechanism
  - Fallback design explanation

- `docs/SUMMARY.md` - Updated table of contents
  - Added creating_menu.md link

---

## 🎯 Compatibility Notes

- ✅ Fully backward compatible with older configurations
- ✅ New features don't affect existing menu functionality
- ✅ Old formats (e.g., `DIAMOND_SWORD`) still fully supported
- ✅ Legacy color codes still fully supported
- ✅ Recommended to test existing menus after update

---

## 📝 Next Steps

- Continue optimizing listener functionality
- Add more condition checking methods
- Expand item operation features
- Optimize user interface experience

---

**Thank you for using KaMenu! If you have any questions or suggestions, please feel free to provide feedback.**

---
