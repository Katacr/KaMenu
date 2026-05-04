# KaMenu v1.4.0 更新报告

## 📋 版本信息
- **版本号**: 1.4.0

---

## ✨ 新增功能

### 1. Tab 补全模糊匹配

**功能说明:**
Tab 补全现在支持模糊检索，不再仅限于首字母/前缀匹配。输入任意关键字即可匹配包含该关键字的候选项。

**功能特性:**
- ✅ **包含匹配**：输入 `menu` 可匹配 `test_menu`、`mymenulist`、`menu-shop` 等
- ✅ **全部场景**：`/km open`、`/km action`、`/km item` 等所有补全场景均支持
- ✅ **大小写不敏感**：输入大小写均可匹配

**对比:**

| 场景 | 旧行为 | 新行为 |
|------|--------|--------|
| `/km open a` | 展示所有菜单 | 只展示包含 `a` 的菜单 |
| `/km open menu` | 展示所有菜单 | 只展示包含 `menu` 的菜单 |
| `/km item give 神` | 展示所有物品 | 只展示包含 `神` 的物品 |

---

### 2. tppos 坐标传送动作

**功能说明:**
新增 `tppos` 动作，可将玩家传送到指定坐标位置。

**格式:** `tppos: <世界名称>,<x>,<y>,<z>[,<yaw>,<pitch>]`

**参数说明:**

| 参数 | 说明 | 必需 |
|------|------|------|
| 世界名称 | 目标世界的名称 | ✅ |
| x | X 坐标 | ✅ |
| y | Y 坐标 | ✅ |
| z | Z 坐标 | ✅ |
| yaw | 水平朝向角度 | ❌（默认保留玩家当前朝向）|
| pitch | 垂直朝向角度 | ❌（默认保留玩家当前朝向）|

**示例:**
```yaml
# 仅坐标，保持当前朝向
- 'tppos: world,100,64,200'

# 完整坐标 + 朝向
- 'tppos: world,100,64,200,90,0'

# 配合变量使用
- 'tppos: {data:target_world},{data:target_x},{data:target_y},{data:target_z}'
```

---

## 🔧 优化改进

### 3. item 组件变量解析修复

**问题:** Body 的 `item` 组件中，`material`、`name`、`description`、`item_model` 字段未解析 `{data:key}`、`{gdata:key}`、`%papi_var%` 等变量。

**修复:** 现已统一处理，item 组件所有文本字段均支持变量解析。

### 4. item 组件 tooltip 键名更名

`tooltip` 布尔开关更名为 `show_tooltip`，避免与按钮悬浮提示文本的 `tooltip` 产生歧义。旧字段 `tooltip` 已移除。

```yaml
# 1.4.0（新写法）
item:
  type: 'item'
  material: 'DIAMOND'
  show_tooltip: false
```

> ⚠️ **注意**: 如果你在 1.3.7 或更早版本中使用了 `tooltip: false` 来隐藏物品提示，升级后需改为 `show_tooltip: false`，否则将恢复默认显示。

---

## 📝 配置示例

### Tab 补全示例

```bash
# 输入 /km open menu 按 Tab
# 旧行为：展示所有菜单 ID
# 新行为：只展示包含 "menu" 的菜单，如：
#   main_menu
#   test_menu
#   menu-shop
#   mymenulist
```

### tppos 动作示例

```yaml
Bottom:
  type: 'multi'
  buttons:
    spawn:
      text: '&a返回主城'
      actions:
        - 'tppos: world,0,64,0,90,0'
        - 'tell: &a已传送至主城！'

    hub:
      text: '&a前往活动大厅'
      actions:
        - 'tppos: world_nether,100,70,200'
```

---

## ⚠️ 注意事项

- `tppos` 指定的世界名称必须存在，否则传送不会执行
- Tab 补全模糊匹配为独立功能，不影响现有配置
- `show_tooltip` 替代旧字段 `tooltip`，旧字段已移除

---

## 🚀 升级指南

### 从 1.3.7 升级到 1.4.0

**升级步骤:**

1. **更新插件**
   - 下载最新的 `KaMenu-1.4.0.jar`
   - 替换旧版本 jar 文件

2. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器

3. **（重要）更新配置**
   - 如使用了 item 组件 `tooltip: true/false`，必须改为 `show_tooltip: true/false`

**兼容性:**
- ⚠️ **不兼容变更**：item 组件的 `tooltip` 字段更名为 `show_tooltip`
- ✅ **其他配置**：无需修改

---

## 📈 版本对比

| 功能 | 1.3.7 | 1.4.0 |
|------|-------|-------|
| Tab 补全（前缀匹配） | ✅ | ✅ |
| Tab 补全（模糊包含匹配） | ❌ | ✅ |
| server 动作 | ✅ | ✅ |
| tppos 动作 | ❌ | ✅ |
| item 变量解析 | 部分 | ✅ 全部字段 |

---

## 📚 相关文档

- **tppos 文档**: `docs/menu/actions.md` - 动作系统完整文档
- **示例菜单**: `menus/example/` - 中文示例
- **示例菜单**: `menus/exampleEN/` - 英文示例

---

# KaMenu v1.4.0 Update Report

## 📋 Version Info
- **Version**: 1.4.0

---

## ✨ New Features

### 1. Tab Completion Fuzzy Matching

**Description:**
Tab completion now supports fuzzy keyword matching instead of prefix-only matching. Type any keyword to match all candidates containing it.

**Features:**
- ✅ **Contains Matching**: Typing `menu` matches `test_menu`, `mymenulist`, `menu-shop`, etc.
- ✅ **All Scenarios**: `/km open`, `/km action`, `/km item` and all other completion scenarios
- ✅ **Case Insensitive**: Matching ignores case

**Comparison:**

| Scenario | Old | New |
|----------|-----|-----|
| `/km open a` | Shows all menus | Only menus containing `a` |
| `/km open menu` | Shows all menus | Only menus containing `menu` |
| `/km item give st` | Shows all items | Only items containing `st` |

---

### 2. tppos Coordinate Teleport Action

**Description:**
New `tppos` action to teleport players to specified coordinates.

**Format:** `tppos: <world>,<x>,<y>,<z>[,<yaw>,<pitch>]`

**Parameters:**

| Parameter | Description | Required |
|-----------|-------------|----------|
| world | Target world name | ✅ |
| x | X coordinate | ✅ |
| y | Y coordinate | ✅ |
| z | Z coordinate | ✅ |
| yaw | Horizontal rotation | ❌ (defaults to player's current) |
| pitch | Vertical rotation | ❌ (defaults to player's current) |

**Example:**
```yaml
# Coordinates only, keep current facing direction
- 'tppos: world,100,64,200'

# Full coordinates + rotation
- 'tppos: world,100,64,200,90,0'

# With variables
- 'tppos: {data:target_world},{data:target_x},{data:target_y},{data:target_z}'
```

---

## 🔧 Improvements

### 3. Item Component Variable Resolution Fix

**Issue:** Body `item` component fields (`material`, `name`, `description`, `item_model`) did not resolve `{data:key}`, `{gdata:key}`, `%papi_var%` variables.

**Fix:** All item component text fields now properly resolve variables.

### 4. Item Component tooltip Key Renamed

`tooltip` boolean toggle renamed to `show_tooltip` to avoid ambiguity with button hover tooltip text. Old `tooltip` key has been removed.

```yaml
# 1.4.0 (new)
item:
  type: 'item'
  material: 'DIAMOND'
  show_tooltip: false
```

> ⚠️ **Note**: If you used `tooltip: false` in 1.3.7 or earlier to hide item tooltips, update to `show_tooltip: false` after upgrading, otherwise the default (shown) behavior will apply.

---

## 📝 Configuration Examples

### tppos Action Example

```yaml
Bottom:
  type: 'multi'
  buttons:
    spawn:
      text: '&aReturn to Spawn'
      actions:
        - 'tppos: world,0,64,0,90,0'
        - 'tell: &aTeleported to spawn!'

    hub:
      text: '&aGo to Event Hub'
      actions:
        - 'tppos: world_nether,100,70,200'
```

---

## ⚠️ Notes

- `tppos` requires the target world to exist, otherwise teleport is silently skipped
- Tab completion fuzzy matching is standalone, no config changes needed
- `show_tooltip` replaces old `tooltip` key, old key has been removed

---

## 🚀 Upgrade Guide

### Upgrade from 1.3.7 to 1.4.0

**Steps:**

1. **Update Plugin**
   - Download latest `KaMenu-1.4.0.jar`
   - Replace old version jar file

2. **Restart Server**
   - Stop server completely
   - Restart server

3. **(Important) Update Configs**
   - If using item component `tooltip: true/false`, must update to `show_tooltip: true/false`

**Compatibility:**
- ⚠️ **Breaking Change**: Item component `tooltip` field renamed to `show_tooltip`
- ✅ **Other Configs**: No changes needed

---

## 📈 Version Comparison

| Feature | 1.3.7 | 1.4.0 |
|---------|-------|-------|
| Tab completion (prefix) | ✅ | ✅ |
| Tab completion (fuzzy contains) | ❌ | ✅ |
| server action | ✅ | ✅ |
| tppos action | ❌ | ✅ |
| item variable resolution | Partial | ✅ All fields |

---

## 📚 Related Documentation

- **tppos docs**: `docs/menu/actions.md` - Complete action system documentation
- **Example Menu**: `menus/example/` - Chinese examples
- **Example Menu**: `menus/exampleEN/` - English examples
