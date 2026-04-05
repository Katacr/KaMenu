# KaMenu v1.3.3 更新报告

## 📋 版本信息
- **版本号**: 1.3.3
- **发布日期**: 2026年4月6日

---

## ✨ 新增功能

### 1. BungeeCord 跨服传输优化

**功能说明:**
优化 server 动作，支持通过配置选择 BungeeCord 插件消息系统或命令模式，提升跨服传输的灵活性和兼容性。

**配置项:**
```yaml
# config.yml
bungeecord: true  # 启用 BungeeCord 插件消息模式
```

**工作原理:**
```kotlin
// BungeeCord 模式（无需玩家权限）
val out = ByteStreams.newDataOutput()
out.writeUTF("Connect")
out.writeUTF(serverName)
player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())

// 命令模式（需要玩家执行 /server 命令的权限）
player.performCommand("server $serverName")
```

**功能特性:**
- ✅ **BungeeCord 模式**：使用插件消息系统，无需玩家权限
- ✅ **命令模式**：使用 `/server` 命令（需要玩家权限）
- ✅ **自动适配**：根据配置自动选择最合适的传输方式
- ✅ **兼容性**：与 DeluxeMenus 等主流插件保持一致
- ✅ **插件.yml 配置**：自动注册 `bungeecord: true`

**使用建议:**
- BungeeCord/Velocity 网络设置 `bungeecord: true`
- 单服务器或其他跨服方案设置 `bungeecord: false`

**优势对比:**
|| BungeeCord 模式 | 命令模式 |
||-----------------|----------|
|| 无需玩家权限 | 需要玩家权限 |
|| 通过插件消息传输 | 通过命令执行 |
|| 与 DeluxeMenus 一致 | 更灵活的权限控制 |

### 2. 目标选择器系统

**功能说明:**
所有动作支持目标选择器，可以指定动作作用的目标玩家，支持条件表达式筛选。这是一个强大的功能，让动作可以灵活地作用于不同的玩家群体。

**语法:**
```yaml
# 格式: {player: 选择器}
- 'tell: 你好！'  # 未指定，发给执行者本身
- 'tell: 你好！{player: *}'  # 发给所有在线玩家
- 'tell: 你好！{player: all}'  # 发给所有在线玩家
- 'tell: 你好！{player: %player_level% >= 10}'  # 发给满足条件的玩家
- 'tell: 你好！{player: hasPerm.user.vip}'  # 发给拥有VIP权限的玩家
```

**选择器类型:**

| 选择器 | 说明 | 示例 |
|--------|------|------|
| `{player: *}` | 所有在线玩家 | `{player: *}` |
| `{player: all}` | 所有在线玩家 | `{player: all}` |
| `{player: 条件}` | 满足条件的玩家 | `{player: %player_level% >= 10}` |

**支持的动作:**

**✅ 支持多目标的动作:**
- `tell`, `actionbar`, `title`, `toast`, `hovertext`
- `command`, `chat`, `console`, `sound`
- `money`, `stock-item`, `item`
- `data`, `gdata`, `meta`, `js`

**❌ 只对当前玩家的动作:**
- `open`, `close`, `server`, `actions`

**条件表达式示例:**
```yaml
# 单条件
{player: %player_level% >= 10}

# 多条件（与）
{player: %player_level% >= 10 && hasPerm.user.vip}

# 多条件（或）
{player: %player_is_op% || %player_has_permission:admin%}

# 内置条件方法
{player: hasPerm.cmi.command.help}
{player: isPosNum.%player_level%}
{player: hasMoney.1000}

# 复杂条件
{player: (%player_level% >= 10 && %vault_eco_balance% >= 1000) || %player_is_op%}

# 世界选择
{player: %player_world% == survival}

# 组合条件
{player: %player_level% >= 5 && %player_world% == lobby}
```

**功能特性:**
- ✅ 支持所有 ConditionUtils 条件表达式
- ✅ 自动过滤目标玩家，不匹配时记录警告
- ✅ PAPI 变量为每个目标玩家单独解析
- ✅ 性能优化：正则表达式预编译
- ✅ 大小写保持：保持原始大小写，避免误判方法名和权限节点

**使用场景:**

1. **服务器公告:**
```yaml
- 'tell: &6════════════════════════{player: *}'
- 'tell: &6     服务器公告{player: *}'
- 'tell: &6════════════════════════{player: *}'
- 'tell: &f服务器将在 5 分钟后进行维护{player: *}'
- 'sound: entity.experience_orb.pickup{player: *}'
```

2. **VIP 玩家专属消息:**
```yaml
- 'tell: &dVIP玩家专属消息{player: hasPerm.user.vip}'
- 'actionbar: &eVIP专属活动即将开始{player: hasPerm.user.vip}'
```

3. **等级奖励:**
```yaml
- 'money: type=add;num=100{player: %player_level% >= 10}'
- 'tell: &a达到10级的玩家：&f恭喜你获得100金币奖励！{player: %player_level% >= 10}'
```

4. **管理员通知:**
```yaml
- 'actionbar: &c服务器维护中{player: %player_is_op%}'
- 'sound: block.note_block.pling{player: %player_is_op%}'
```

5. **世界选择:**
```yaml
- 'tell: &2生存世界的玩家请注意！{player: %player_world% == survival}'
```

6. **奖励发放:**
```yaml
- 'tell: &a正在给VIP玩家发放奖励...{player: hasPerm.user.vip}'
- 'money: type=add;num=1000{player: hasPerm.user.vip}'
- 'stock-item: type=give;name=diamond;amount=5{player: hasPerm.user.vip}'
- 'sound: entity.player.levelup{player: hasPerm.user.vip}'
```

**实现原理:**
```kotlin
// 预编译正则表达式
private val targetSelectorPattern = Regex("\\{player:\\s*([^}]*)\\}", RegexOption.IGNORE_CASE)

// 解析目标选择器
private fun parseTargetSelector(action: String): ParsedAction {
    val match = targetSelectorPattern.find(action)
    return if (match != null) {
        val selector = match.groupValues[1].trim()
        val actionWithoutSelector = action.replace(match.value, "")
        ParsedAction(actionWithoutSelector, selector)
    } else {
        ParsedAction(action, null)
    }
}

// 获取目标玩家列表
private fun getTargetPlayers(player: Player, selector: String?): List<Player> {
    if (selector == null) return listOf(player)
    
    val trimmedSelector = selector.trim()
    
    return when (trimmedSelector.lowercase()) {
        "*", "all" -> Bukkit.getOnlinePlayers().toList()
        else -> {
            val targetPlayers = mutableListOf<Player>()
            for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                if (ConditionUtils.checkCondition(onlinePlayer, trimmedSelector)) {
                    targetPlayers.add(onlinePlayer)
                }
            }
            targetPlayers
        }
    }
}
```

### 3. 英文示例菜单

**新增文件:**
- `menus/exampleEN/target_selector_demo.yml`

**功能:**
提供完整的目标选择器英文示例菜单，包含 12 个测试按钮：
- Default Target（默认目标）
- All Players（所有玩家）
- Admins（管理员）
- VIP Players（VIP 玩家）
- Level 10+（10级以上）
- Survival World（生存世界）
- Complex Conditions（复杂条件）
- Server Broadcast（全服公告）
- Reward Distribution（奖励发放）
- Money Distribution（金币发放）
- Sound Playback（声音播放）
- Multi-Action Combo（多动作组合）

---

## 🐛 问题修复

### 1. 目标选择器大小写敏感问题

**问题描述:**
目标选择器中的内置方法（如 `hasPerm`）和权限节点（如 `cmi.command.help`）被强制转换为小写，导致匹配失败：
- `hasPerm.user.vip` → `hasperm.user.vip`
- `ConditionUtils.evaluateBuiltinCondition` 中 `method = "hasperm"` 无法匹配 `"hasPerm"`

**修复方案:**
修改 `getTargetPlayers` 方法，只对特殊关键字 `"*"` 和 `"all"` 进行不区分大小写的匹配，对条件选择器保持原始大小写：

```kotlin
// 修复前
val trimmedSelector = selector.trim().lowercase()

// 修复后
val trimmedSelector = selector.trim()
return when (trimmedSelector.lowercase()) {
    "*", "all" -> { /* ... */ }
    else -> { /* 保持原始大小写处理 */ }
}
```

**修复内容:**
- ✅ `hasPerm.user.vip` 现在可以正确匹配
- ✅ `hasPerm.cmi.command.help` 可以正确判断权限
- ✅ 所有内置条件方法（isNum、hasMoney 等）保持大小写
- ✅ PAPI 变量正常工作
- ✅ 权限节点不会被错误转换

---

## 📝 配置示例

### 示例 1：启用 BungeeCord 模式

```yaml
# config.yml
bungeecord: true

# 菜单配置
Bottom:
  type: multi
  buttons:
    lobby:
      text: '&a[ 传送至大厅 ]'
      actions:
        - 'server: lobby'  # 使用 BungeeCord 模式传送
```

### 示例 2：服务器公告

```yaml
# 管理员菜单
admin_broadcast:
  text: '&6[ 服务器公告 ]'
  actions:
    - 'tell: &6════════════════════════{player: *}'
    - 'tell: &6     重要通知{player: *}'
    - 'tell: &6════════════════════════{player: *}'
    - 'tell: &f服务器将在 5 分钟后进行维护{player: *}'
    - 'tell: &f请提前保存进度，感谢配合！{player: *}'
    - 'tell: &6════════════════════════{player: *}'
    - 'sound: entity.experience_orb.pickup{player: *}'
```

### 示例 3：VIP 专属活动

```yaml
# VIP 活动
vip_event:
  text: '&d[ VIP 专属活动 ]'
  actions:
    - 'tell: &dVIP 专属活动即将开始！{player: hasPerm.user.vip}'
    - 'actionbar: &e请前往活动大厅参与{player: hasPerm.user.vip}'
    - 'sound: entity.player.levelup{player: hasPerm.user.vip}'
    - 'tell: &a活动奖励：1000 金币 + 5 颗钻石{player: hasPerm.user.vip}'
```

### 示例 4：等级奖励

```yaml
# 等级奖励系统
level_rewards:
  text: '&b[ 等级奖励 ]'
  actions:
    - 'money: type=add;num=100{player: %player_level% >= 10}'
    - 'tell: &a恭喜达到 10 级！获得 100 金币{player: %player_level% >= 10}'
    - 'stock-item: type=give;name=diamond;amount=3{player: %player_level% >= 10}'
    - 'sound: entity.experience_orb.pickup{player: %player_level% >= 10}'
```

### 示例 5：复杂条件选择

```yaml
# 特殊玩家通知
special_notification:
  text: '&6[ 特殊通知 ]'
  actions:
    # 同时满足等级和VIP
    - 'tell: &6高级VIP专属通知{player: %player_level% >= 10 && hasPerm.user.vip}'
    
    # 满足其一（管理员或有钱）
    - 'tell: &c管理员通知{player: (%player_level% >= 10 && %vault_eco_balance% >= 1000) || %player_is_op%}'
    
    # 特定世界
    - 'tell: &2生存世界活动{player: %player_world% == survival}'
```

---

## ⚠️ 注意事项

### 1. BungeeCord 配置

- `plugin.yml` 已自动添加 `bungeecord: true`
- BungeeCord 模式需要服务器已配置 BungeeCord/Velocity
- 命令模式需要玩家拥有 `/server` 命令权限
- 建议在 BungeeCord 网络中使用插件消息模式

### 2. 目标选择器使用

- 只有多目标动作支持目标选择器
- `open`, `close`, `server`, `actions` 等动作忽略目标选择器
- 条件表达式支持所有 ConditionUtils 功能
- 复杂条件建议使用括号明确优先级
- PAPI 变量为每个目标玩家单独解析

### 3. 大小写保持

- 目标选择器中条件部分保持原始大小写
- 只对 `*` 和 `all` 关键字不区分大小写
- 内置方法名（hasPerm、isNum 等）保持大小写
- 权限节点不会被错误转换

### 4. 性能考虑

- 目标选择器会遍历所有在线玩家
- 复杂条件在大服务器上可能影响性能
- 建议使用简单的条件表达式
- 可以使用 `{player: *}` 而不是 `{player: all}`（功能相同，更简洁）

---

## 🚀 升级指南

### 从 1.3.2 升级到 1.3.3

**升级步骤:**

1. **更新插件**
   - 下载最新的 `KaMenu-1.3.3.jar`
   - 替换旧版本 jar 文件
   - 删除旧版本 jar 文件

2. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器
   - 确认无错误

3. **配置 BungeeCord（可选）**
   - 编辑 `config.yml`
   - 根据服务器类型设置 `bungeecord: true/false`
   ```yaml
   # BungeeCord/Velocity 网络
   bungeecord: true
   
   # 单服务器或其他跨服方案
   bungeecord: false
   ```

4. **测试功能**
   - 测试 `server:` 动作是否正常工作
   - 测试目标选择器功能
   - 查看示例菜单 `target_selector_demo.yml`
   - 检查日志是否有警告信息

**兼容性:**
- ✅ **完全兼容**：所有现有菜单无需修改
- ✅ **向后兼容**：旧版本菜单继续正常工作
- ✅ **可选功能**：新功能为可选启用
- ✅ **配置迁移**：自动更新配置版本到 v3

**配置版本更新:**
```kotlin
// ConfigUpdater.kt
private const val CURRENT_CONFIG_VERSION = 3
```

---

## 📈 版本对比

| 功能 | 1.3.2 | 1.3.3 |
|------|-------|-------|
| BungeeCord 插件消息 | ❌ | ✅ |
| 可配置的传输模式 | ❌ | ✅ |
| 目标选择器系统 | ❌ | ✅ |
| 多目标动作支持 | ❌ | ✅ |
| 条件选择器 | ❌ | ✅ |
| 英文示例菜单 | ❌ | ✅ |
| 大小写保持修复 | ❌ | ✅ |
| 预编译正则优化 | ❌ | ✅ |

---

## 📚 相关文档

- **配置文档**: `docs/config/config.md` - BungeeCord 配置说明
- **动作文档**: `docs/menu/actions.md` - 目标选择器完整文档
- **示例菜单**: `menus/example/target_selector_demo.yml` - 中文示例
- **示例菜单**: `menus/exampleEN/target_selector_demo.yml` - 英文示例

---

## 🔍 技术细节

### 动作类型分类

系统根据动作类型智能决定是否支持目标选择器：

```kotlin
private enum class ActionType {
    MULTITARGET,        // 支持多目标的动作
    SINGLE_TARGET_ONLY  // 只对单个玩家有意义的动作
}

private fun getActionType(action: String): ActionType {
    return when {
        action.startsWith("open:") ||
        action.startsWith("close") ||
        action.startsWith("server:") ||
        action.startsWith("actions:") -> {
            ActionType.SINGLE_TARGET_ONLY
        }
        else -> {
            ActionType.MULTITARGET
        }
    }
}
```

### 条件检查流程

1. 解析目标选择器 `{player: 条件}`
2. 根据类型获取目标玩家列表：
   - `*` 或 `all` → 所有在线玩家
   - 条件表达式 → 筛选满足条件的玩家
   - 未指定 → 当前玩家
3. 对每个目标玩家执行动作
4. 失败时记录警告日志

---

**v1.3.3 - 跨服与目标选择 🎯**

---

---

# KaMenu v1.3.3 Update Report

## 📋 Version Info
- **Version**: 1.3.3
- **Release Date**: April 6, 2026

---

## ✨ New Features

### 1. BungeeCord Cross-Server Transfer Optimization

**Description:**
Optimized server action to support configurable BungeeCord plugin messaging system or command mode, improving flexibility and compatibility for cross-server transfers.

**Configuration:**
```yaml
# config.yml
bungeecord: true  # Enable BungeeCord plugin messaging mode
```

**How It Works:**
```kotlin
// BungeeCord Mode (no player permission required)
val out = ByteStreams.newDataOutput()
out.writeUTF("Connect")
out.writeUTF(serverName)
player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())

// Command Mode (requires player to have /server command permission)
player.performCommand("server $serverName")
```

**Features:**
- ✅ **BungeeCord Mode**: Uses plugin messaging system, no player permission required
- ✅ **Command Mode**: Uses `/server` command (requires player permission)
- ✅ **Auto-Adapt**: Automatically selects the best transfer method based on config
- ✅ **Compatibility**: Consistent with mainstream plugins like DeluxeMenus
- ✅ **Plugin.yml Config**: Automatically registers `bungeecord: true`

**Usage Recommendations:**
- Set `bungeecord: true` for BungeeCord/Velocity networks
- Set `bungeecord: false` for single servers or other cross-server solutions

**Comparison:**
|| BungeeCord Mode | Command Mode |
||-----------------|--------------|
|| No player permission needed | Requires player permission |
|| Transfer via plugin messaging | Transfer via command execution |
|| Consistent with DeluxeMenus | More flexible permission control |

### 2. Target Selector System

**Description:**
All actions support target selectors, allowing you to specify which players actions should affect, supporting conditional expression filtering. This is a powerful feature that enables actions to flexibly target different player groups.

**Syntax:**
```yaml
# Format: {player: selector}
- 'tell: Hello!'  # No selector, sends to executor
- 'tell: Hello!{player: *}'  # Sends to all online players
- 'tell: Hello!{player: all}'  # Sends to all online players
- 'tell: Hello!{player: %player_level% >= 10}'  # Sends to players meeting condition
- 'tell: Hello!{player: hasPerm.user.vip}'  # Sends to players with VIP permission
```

**Selector Types:**

| Selector | Description | Example |
|----------|-------------|---------|
| `{player: *}` | All online players | `{player: *}` |
| `{player: all}` | All online players | `{player: all}` |
| `{player: condition}` | Players meeting condition | `{player: %player_level% >= 10}` |

**Supported Actions:**

**✅ Multi-Target Actions:**
- `tell`, `actionbar`, `title`, `toast`, `hovertext`
- `command`, `chat`, `console`, `sound`
- `money`, `stock-item`, `item`
- `data`, `gdata`, `meta`, `js`

**❌ Single-Target Actions:**
- `open`, `close`, `server`, `actions`

**Conditional Expression Examples:**
```yaml
# Single condition
{player: %player_level% >= 10}

# Multiple conditions (AND)
{player: %player_level% >= 10 && hasPerm.user.vip}

# Multiple conditions (OR)
{player: %player_is_op% || %player_has_permission:admin%}

# Built-in condition methods
{player: hasPerm.cmi.command.help}
{player: isPosNum.%player_level%}
{player: hasMoney.1000}

# Complex conditions
{player: (%player_level% >= 10 && %vault_eco_balance% >= 1000) || %player_is_op%}

# World selection
{player: %player_world% == survival}

# Combined conditions
{player: %player_level% >= 5 && %player_world% == lobby}
```

**Features:**
- ✅ Supports all ConditionUtils conditional expressions
- ✅ Automatically filters target players, logs warning if no match
- ✅ PAPI variables resolved separately for each target player
- ✅ Performance optimization: Pre-compiled regex
- ✅ Case preservation: Maintains original case to avoid method/permission misidentification

**Use Cases:**

1. **Server Announcement:**
```yaml
- 'tell: &6════════════════════════{player: *}'
- 'tell: &6     Server Announcement{player: *}'
- 'tell: &6════════════════════════{player: *}'
- 'tell: &fServer will be under maintenance in 5 minutes{player: *}'
- 'sound: entity.experience_orb.pickup{player: *}'
```

2. **VIP Exclusive Messages:**
```yaml
- 'tell: &dVIP player exclusive message{player: hasPerm.user.vip}'
- 'actionbar: &eVIP exclusive event starting soon{player: hasPerm.user.vip}'
```

3. **Level Rewards:**
```yaml
- 'money: type=add;num=100{player: %player_level% >= 10}'
- 'tell: &aPlayers at level 10: &fCongratulations! You get 100 coins!{player: %player_level% >= 10}'
```

4. **Admin Notifications:**
```yaml
- 'actionbar: &cServer maintenance in progress{player: %player_is_op%}'
- 'sound: block.note_block.pling{player: %player_is_op%}'
```

5. **World Selection:**
```yaml
- 'tell: &2Survival world players please note!{player: %player_world% == survival}'
```

6. **Reward Distribution:**
```yaml
- 'tell: &aDistributing rewards to VIP players...{player: hasPerm.user.vip}'
- 'money: type=add;num=1000{player: hasPerm.user.vip}'
- 'stock-item: type=give;name=diamond;amount=5{player: hasPerm.user.vip}'
- 'sound: entity.player.levelup{player: hasPerm.user.vip}'
```

**Implementation:**
```kotlin
// Pre-compiled regex pattern
private val targetSelectorPattern = Regex("\\{player:\\s*([^}]*)\\}", RegexOption.IGNORE_CASE)

// Parse target selector
private fun parseTargetSelector(action: String): ParsedAction {
    val match = targetSelectorPattern.find(action)
    return if (match != null) {
        val selector = match.groupValues[1].trim()
        val actionWithoutSelector = action.replace(match.value, "")
        ParsedAction(actionWithoutSelector, selector)
    } else {
        ParsedAction(action, null)
    }
}

// Get target player list
private fun getTargetPlayers(player: Player, selector: String?): List<Player> {
    if (selector == null) return listOf(player)
    
    val trimmedSelector = selector.trim()
    
    return when (trimmedSelector.lowercase()) {
        "*", "all" -> Bukkit.getOnlinePlayers().toList()
        else -> {
            val targetPlayers = mutableListOf<Player>()
            for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                if (ConditionUtils.checkCondition(onlinePlayer, trimmedSelector)) {
                    targetPlayers.add(onlinePlayer)
                }
            }
            targetPlayers
        }
    }
}
```

### 3. English Example Menu

**New File:**
- `menus/exampleEN/target_selector_demo.yml`

**Features:**
Complete English example menu for target selectors with 12 test buttons:
- Default Target
- All Players
- Admins
- VIP Players
- Level 10+
- Survival World
- Complex Conditions
- Server Broadcast
- Reward Distribution
- Money Distribution
- Sound Playback
- Multi-Action Combo

---

## 🐛 Bug Fixes

### 1. Target Selector Case Sensitivity Issue

**Issue:**
Built-in methods (like `hasPerm`) and permission nodes (like `cmi.command.help`) in target selectors were forced to lowercase, causing matching failures:
- `hasPerm.user.vip` → `hasperm.user.vip`
- In `ConditionUtils.evaluateBuiltinCondition`, `method = "hasperm"` cannot match `"hasPerm"`

**Fix:**
Modified `getTargetPlayers` method to only case-insensitively match special keywords `"*"` and `"all"`, while preserving original case for conditional selectors:

```kotlin
// Before fix
val trimmedSelector = selector.trim().lowercase()

// After fix
val trimmedSelector = selector.trim()
return when (trimmedSelector.lowercase()) {
    "*", "all" -> { /* ... */ }
    else -> { /* Process with original case */ }
}
```

**Fixed:**
- ✅ `hasPerm.user.vip` now matches correctly
- ✅ `hasPerm.cmi.command.help` correctly checks permission
- ✅ All built-in condition methods (isNum, hasMoney, etc.) maintain case
- ✅ PAPI variables work normally
- ✅ Permission nodes are not incorrectly converted

---

## 📝 Configuration Examples

### Example 1: Enable BungeeCord Mode

```yaml
# config.yml
bungeecord: true

# Menu configuration
Bottom:
  type: multi
  buttons:
    lobby:
      text: '&a[ Teleport to Lobby ]'
      actions:
        - 'server: lobby'  # Transfer using BungeeCord mode
```

### Example 2: Server Announcement

```yaml
# Admin menu
admin_broadcast:
  text: '&6[ Server Announcement ]'
  actions:
    - 'tell: &6════════════════════════{player: *}'
    - 'tell: &6     Important Notice{player: *}'
    - 'tell: &6════════════════════════{player: *}'
    - 'tell: &fServer will be under maintenance in 5 minutes{player: *}'
    - 'tell: &fPlease save your progress, thank you!{player: *}'
    - 'tell: &6════════════════════════{player: *}'
    - 'sound: entity.experience_orb.pickup{player: *}'
```

### Example 3: VIP Exclusive Event

```yaml
# VIP event
vip_event:
  text: '&d[ VIP Exclusive Event ]'
  actions:
    - 'tell: &dVIP exclusive event starting soon!{player: hasPerm.user.vip}'
    - 'actionbar: &ePlease go to the event lobby{player: hasPerm.user.vip}'
    - 'sound: entity.player.levelup{player: hasPerm.user.vip}'
    - 'tell: &aEvent rewards: 1000 coins + 5 diamonds{player: hasPerm.user.vip}'
```

### Example 4: Level Rewards

```yaml
# Level reward system
level_rewards:
  text: '&b[ Level Rewards ]'
  actions:
    - 'money: type=add;num=100{player: %player_level% >= 10}'
    - 'tell: &aCongratulations on reaching level 10! Get 100 coins{player: %player_level% >= 10}'
    - 'stock-item: type=give;name=diamond;amount=3{player: %player_level% >= 10}'
    - 'sound: entity.experience_orb.pickup{player: %player_level% >= 10}'
```

### Example 5: Complex Condition Selection

```yaml
# Special player notification
special_notification:
  text: '&6[ Special Notification ]'
  actions:
    # Both level and VIP
    - 'tell: &6Premium VIP exclusive notification{player: %player_level% >= 10 && hasPerm.user.vip}'
    
    # Either admin or rich
    - 'tell: &cAdmin notification{player: (%player_level% >= 10 && %vault_eco_balance% >= 1000) || %player_is_op%}'
    
    # Specific world
    - 'tell: &2Survival world event{player: %player_world% == survival}'
```

---

## ⚠️ Notes

### 1. BungeeCord Configuration

- `plugin.yml` automatically adds `bungeecord: true`
- BungeeCord mode requires server already configured with BungeeCord/Velocity
- Command mode requires player to have `/server` command permission
- Recommended to use plugin messaging mode for BungeeCord/Velocity networks

### 2. Target Selector Usage

- Only multi-target actions support target selectors
- `open`, `close`, `server`, `actions` actions ignore target selectors
- Conditional expressions support all ConditionUtils features
- Complex conditions recommended to use parentheses for priority
- PAPI variables resolved separately for each target player

### 3. Case Preservation

- Condition part in target selector maintains original case
- Only `*` and `all` keywords are case-insensitive
- Built-in method names (hasPerm, isNum, etc.) maintain case
- Permission nodes are not incorrectly converted

### 4. Performance Considerations

- Target selector iterates through all online players
- Complex conditions may affect performance on large servers
- Recommended to use simple conditional expressions
- Can use `{player: *}` instead of `{player: all}` (same functionality, more concise)

---

## 🚀 Upgrade Guide

### Upgrade from 1.3.2 to 1.3.3

**Steps:**

1. **Update Plugin**
   - Download the latest `KaMenu-1.3.3.jar`
   - Replace old version jar file
   - Delete old version jar file

2. **Restart Server**
   - Stop server completely
   - Restart server
   - Confirm no errors

3. **Configure BungeeCord (Optional)**
   - Edit `config.yml`
   - Set `bungeecord: true/false` based on server type
   ```yaml
   # BungeeCord/Velocity network
   bungeecord: true
   
   # Single server or other cross-server solutions
   bungeecord: false
   ```

4. **Test Features**
   - Test `server:` action works correctly
   - Test target selector functionality
   - View example menu `target_selector_demo.yml`
   - Check logs for any warnings

**Compatibility:**
- ✅ **Fully Compatible**: No changes needed for existing menus
- ✅ **Backward Compatible**: Old menus continue to work
- ✅ **Optional Features**: New features can be enabled as needed
- ✅ **Config Migration**: Automatically updates config version to v3

**Config Version Update:**
```kotlin
// ConfigUpdater.kt
private const val CURRENT_CONFIG_VERSION = 3
```

---

## 📈 Version Comparison

| Feature | 1.3.2 | 1.3.3 |
|---------|-------|-------|
| BungeeCord Plugin Messaging | ❌ | ✅ |
| Configurable Transfer Mode | ❌ | ✅ |
| Target Selector System | ❌ | ✅ |
| Multi-Target Action Support | ❌ | ✅ |
| Conditional Selectors | ❌ | ✅ |
| English Example Menu | ❌ | ✅ |
| Case Preservation Fix | ❌ | ✅ |
| Pre-compiled Regex Optimization | ❌ | ✅ |

---

## 📚 Related Documentation

- **Config Docs**: `docs/config/config.md` - BungeeCord configuration guide
- **Action Docs**: `docs/menu/actions.md` - Target selector complete documentation
- **Example Menu**: `menus/example/target_selector_demo.yml` - Chinese examples
- **Example Menu**: `menus/exampleEN/target_selector_demo.yml` - English examples

---

## 🔍 Technical Details

### Action Type Classification

System intelligently determines whether to support target selectors based on action type:

```kotlin
private enum class ActionType {
    MULTITARGET,        // Actions supporting multiple targets
    SINGLE_TARGET_ONLY  // Actions only meaningful for single player
}

private fun getActionType(action: String): ActionType {
    return when {
        action.startsWith("open:") ||
        action.startsWith("close") ||
        action.startsWith("server:") ||
        action.startsWith("actions:") -> {
            ActionType.SINGLE_TARGET_ONLY
        }
        else -> {
            ActionType.MULTITARGET
        }
    }
}
```

### Condition Check Process

1. Parse target selector `{player: condition}`
2. Get target player list based on type:
   - `*` or `all` → All online players
   - Conditional expression → Filter players meeting condition
   - Not specified → Current player
3. Execute action for each target player
4. Log warning on failure

---

**v1.3.3 - Cross-Server & Target Selection 🎯**
