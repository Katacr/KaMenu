# KaMenu v1.3.2 更新报告

## 📋 版本信息
- **版本号**: 1.3.2
- **发布日期**: 2026年4月2日

---

## ✨ 新增功能

### 1. 右键玩家监听器

**功能说明:**
新增右键点击玩家打开菜单的监听器，支持配置菜单和潜行条件。

**配置项:**
```yaml
listeners:
  player-click:
    enabled: false          # 启用此监听器
    menu: 'inspect_player'  # 要打开的菜单
    require-sneaking: false # 需要潜行才能触发
```

**功能特性:**
- ✅ 自动设置 `{meta:player}` 为被点击的玩家名
- ✅ 支持普通右键和 Shift+右键
- ✅ 可在菜单中使用槽位引用显示玩家装备

### 2. 物品组件槽位引用

**功能说明:**
item 组件支持引用玩家装备槽位，支持变量解析。

**格式:**
- `[HEAD]` - 当前玩家头盔
- `[HEAD:PlayerName]` - 指定玩家头盔
- `[HEAD:{meta:player}]` - 使用 meta 变量

**支持的槽位:**
- `HEAD` - 头部
- `CHEST` - 胸部
- `LEGGINGS` - 护腿
- `BOOTS` - 靴子
- `MAINHAND` - 主手
- `OFFHAND` - 副手

**空槽位处理:**
- 头部为空：渲染玩家皮肤头颅
- 其他槽位为空：渲染浅灰色玻璃板，名称为"无"

**变量支持:**
- ✅ PAPI 变量：`[HEAD:%player_name%]`
- ✅ 内置变量：`[HEAD:{data:target_player}]`
- ✅ Meta 变量：`[HEAD:{meta:player}]`

### 3. 玩家互动菜单

**文件:** `menus/inspect_player.yml`

**功能:**
- 📦 显示玩家 4 个装备槽位（32x32 尺寸）
- 🤝 6 个互动按钮：
  - 加为好友
  - 传送到玩家
  - 传送玩家到你
  - 私聊
  - 查看背包
  - 踢出玩家
- 📝 打开/关闭事件提示

**使用方式:**
1. 在 `config.yml` 中启用 `player-click` 监听器
2. 右键任意玩家打开互动菜单
3. 查看对方装备并进行互动

### 4. server 动作改进

**功能说明:**
优化 server 动作，支持通过配置选择 BungeeCord 插件消息系统或命令模式。

### 5. 目标选择器（全局功能）

**功能说明:**
所有动作支持目标选择器，可以指定动作作用的目标玩家，支持条件表达式筛选。

**语法:**
```yaml
# 格式: {player: 选择器}
- 'tell: 你好！'  # 未指定，发给执行者本身
- 'tell: 你好！{player: *}'  # 发给所有在线玩家
- 'tell: 你好！{player: %player_level% >= 10}'  # 发给满足条件的玩家
```

**选择器类型:**
| 选择器 | 说明 | 示例 |
|--------|------|------|
| `{player: *}` | 所有在线玩家 | `{player: *}` |
| `{player: all}` | 所有在线玩家 | `{player: all}` |
| `{player: 条件}` | 满足条件的玩家 | `{player: %player_level% >= 10}` |

**支持的动作:**
- ✅ `tell`, `actionbar`, `title`, `toast`, `hovertext`
- ✅ `command`, `chat`, `console`, `sound`
- ✅ `money`, `stock-item`, `item`
- ✅ `data`, `gdata`, `meta`, `js`
- ❌ `open`, `close`, `server`, `actions`（只对当前玩家）

**条件表达式:**
```yaml
# 单条件
{player: %player_level% >= 10}

# 多条件（与）
{player: %player_level% >= 10 && %player_has_permission:vip%}

# 多条件（或）
{player: %player_is_op% || %player_has_permission:admin%}

# 包含检查
{player: %player_world% contains survival}

# 复杂条件
{player: (%player_level% >= 10 && %vault_eco_balance% >= 1000) || %player_is_op%}
```

**功能特性:**
- ✅ 支持所有 ConditionUtils 条件表达式
- ✅ 自动过滤目标玩家，不匹配时记录警告
- ✅ PAPI 变量为每个目标玩家单独解析
- ✅ 性能优化：正则表达式预编译

**使用场景:**
- 服务器公告：`- 'tell: 服务器将在5分钟后重启{player: *}'`
- VIP 玩家专属消息：`- 'tell: VIP专属活动即将开始{player: %player_has_permission:vip%}'`
- 等级奖励：`- 'money: type=add;num=100{player: %player_level% >= 10}'`
- 管理员通知：`- 'actionbar: 服务器维护中{player: %player_is_op%}'`

**新增配置:**
```yaml
# config.yml
bungeecord: true  # 启用 BungeeCord 支持
```

**功能特性:**
- ✅ **BungeeCord 模式**：使用插件消息系统，无需玩家权限
- ✅ **命令模式**：使用 `/server` 命令（需要玩家权限）
- ✅ **自动适配**：根据配置自动选择最合适的传输方式
- ✅ **兼容性**：与 DeluxeMenus 等主流插件保持一致

**使用建议:**
- BungeeCord/Velocity 网络设置 `bungeecord: true`
- 单服务器或其他跨服方案设置 `bungeecord: false`

**工作原理:**
```kotlin
// BungeeCord 模式
val out = ByteStreams.newDataOutput()
out.writeUTF("Connect")
out.writeUTF(serverName)
player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray())

// 命令模式
player.performCommand("server $serverName")
```

---

## 🔨 改进优化

### 1. item 组件优化

**name 属性可选:**
- `name` 现在是可选的
- 不提供时使用物品默认名称
- 提供时覆盖物品名称

**lore 优化:**
- 只有当 lore 不为空时才设置
- 避免空列表覆盖物品原有的 lore

---

## 🐛 问题修复

### 1. 异步指令执行错误

**问题描述:**
`command:` 和 `chat:` 动作在异步线程中执行，触发 "Asynchronous Command Dispatched Async" 错误。

**修复方案:**
将这两个动作包装在主线程执行：
```kotlin
// command: 玩家执行指令
finalCmd.startsWith("command:") -> {
    val cmd = finalCmd.removePrefix("command:").trim()
    Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
        player.performCommand(cmd)
    })
}

// chat: 玩家执行指令
finalCmd.startsWith("chat:") -> {
    val cmd = finalCmd.removePrefix("chat:").trim()
    Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
        player.chat(cmd)
    })
}
```

**修复内容:**
- ✅ `command:` 动作现在在主线程执行
- ✅ `chat:` 动作现在在主线程执行
- ✅ 不再触发异步线程错误

---

## 📝 配置示例

### 示例 1：启用玩家互动

```yaml
# config.yml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: false
```

### 示例 2：显示玩家装备

```yaml
# 菜单配置
Body:
  helmet_display:
    type: item
    material: '[HEAD:{meta:player}]'
    width: 32
    height: 32

  chest_display:
    type: item
    material: '[CHEST:{meta:player}]'
    width: 32
    height: 32
```

### 示例 3：变量解析槽位

```yaml
Body:
  # 使用数据存储的目标玩家
  target_helmet:
    type: item
    material: '[HEAD:{data:target_player}]'
    width: 32

  # 使用 PAPI 变量
  random_player_helmet:
    type: item
    material: '[HEAD:%random_player%]'
    width: 32
```

---

## ⚠️ 注意事项

### 1. 监听器配置

- 右键玩家监听器默认关闭
- 需要在 `config.yml` 中手动启用
- Shift+右键需要设置 `require-sneaking: true`

### 2. 槽位引用

- 槽位引用模式下，`lore`、`item_model` 等属性不生效
- 仍支持 `width`、`height`、`decorations`、`tooltip`、`description`

### 3. 互动功能依赖

玩家互动菜单中的某些功能需要相应插件支持：
- 加为好友：需要好友插件（如 EssentialsX）
- 查看背包：需要相应权限
- 踢出玩家：需要管理员权限

---

## 🚀 升级指南

### 从 1.3.1 升级到 1.3.2

**升级步骤:**

1. **更新插件**
   - 下载最新的 `KaMenu-1.3.2.jar`
   - 替换旧版本 jar 文件
   - 删除旧版本 jar 文件

2. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器
   - 确认无错误

3. **配置监听器（可选）**
   - 编辑 `config.yml`
   - 启用 `player-click` 监听器
   - 设置要打开的菜单

4. **测试功能**
   - 右键测试玩家互动菜单
   - 查看装备显示是否正常
   - 测试各个互动按钮

**兼容性:**
- ✅ **完全兼容**：所有现有菜单无需修改
- ✅ **向后兼容**：旧版本菜单继续正常工作
- ✅ **可选功能**：新功能为可选启用

---

## 📈 版本对比

| 功能 | 1.3.1 | 1.3.2 |
|------|--------|--------|
| 右键玩家监听器 | ❌ | ✅ |
| 槽位引用 | ❌ | ✅ |
| 玩家互动菜单 | ❌ | ✅ |
| item name 可选 | ❌ | ✅ |
| 空槽位处理 | ❌ | ✅ |
| command 主线程修复 | ❌ | ✅ |
| chat 主线程修复 | ❌ | ✅ |

---

**v1.3.2 - 互动体验升级 🎮**

---

---

# KaMenu v1.3.2 Update Report

## 📋 Version Info
- **Version**: 1.3.2
- **Release Date**: April 2, 2026

---

## ✨ New Features

### 1. Player Click Listener

**Description:**
Added a listener for right-clicking players to open menus, with configurable menu and sneaking conditions.

**Configuration:**
```yaml
listeners:
  player-click:
    enabled: false          # Enable this listener
    menu: 'inspect_player'  # Menu file to open
    require-sneaking: false # Require sneaking to trigger
```

**Features:**
- ✅ Automatically sets `{meta:player}` to the clicked player's name
- ✅ Supports both normal right-click and Shift+Right-Click
- ✅ Can display player equipment using slot references in menus

### 2. Item Slot Reference

**Description:**
The item component now supports referencing player equipment slots with variable resolution.

**Format:**
- `[HEAD]` - Current player's helmet
- `[HEAD:PlayerName]` - Specified player's helmet
- `[HEAD:{meta:player}]` - Use meta variable

**Supported Slots:**
- `HEAD` - Helmet
- `CHEST` - Chestplate
- `LEGGINGS` - Leggings
- `BOOTS` - Boots
- `MAINHAND` - Main Hand
- `OFFHAND` - Off Hand

**Empty Slot Handling:**
- Empty helmet slot: Render player skull
- Other empty slots: Render light gray glass pane named "无"

**Variable Support:**
- ✅ PAPI variables: `[HEAD:%player_name%]`
- ✅ Built-in variables: `[HEAD:{data:target_player}]`
- ✅ Meta variables: `[HEAD:{meta:player}]`

### 3. Player Interaction Menu

**File:** `menus/inspect_player.yml`

**Features:**
- 📦 Display 4 player equipment slots (32x32 size)
- 🤝 6 interaction buttons:
  - Add Friend
  - Teleport to Player
  - Teleport Player to You
  - Private Message
  - Inspect Inventory
  - Kick Player
- 📝 Open/close event notifications

**Usage:**
1. Enable the `player-click` listener in `config.yml`
2. Right-click any player to open the interaction menu
3. View their equipment and interact

---

## 🔨 Improvements

### 1. Item Component Enhancement

**Optional Name Property:**
- `name` is now optional
- If not provided, uses the item's default name
- If provided, overrides the item's name

**Lore Optimization:**
- Lore is only set when not empty
- Prevents empty lists from overwriting existing item lore

---

## 🐛 Bug Fixes

### 1. Async Command Dispatch Error

**Issue:**
`command:` and `chat:` actions were executing in async threads, triggering "Asynchronous Command Dispatched Async" errors.

**Fix:**
Wrapped both actions to execute on the main thread:
```kotlin
// command: Player executes command
finalCmd.startsWith("command:") -> {
    val cmd = finalCmd.removePrefix("command:").trim()
    Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
        player.performCommand(cmd)
    })
}

// chat: Player executes chat
finalCmd.startsWith("chat:") -> {
    val cmd = finalCmd.removePrefix("chat:").trim()
    Bukkit.getScheduler().runTask(plugin ?: return, Runnable {
        player.chat(cmd)
    })
}
```

**Fixed:**
- ✅ `command:` action now executes on main thread
- ✅ `chat:` action now executes on main thread
- ✅ No more async thread errors

---

## 📝 Configuration Examples

### Example 1: Enable Player Interaction

```yaml
# config.yml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: false
```

### Example 2: Display Player Equipment

```yaml
# Menu configuration
Body:
  helmet_display:
    type: item
    material: '[HEAD:{meta:player}]'
    width: 32
    height: 32

  chest_display:
    type: item
    material: '[CHEST:{meta:player}]'
    width: 32
    height: 32
```

### Example 3: Variable Resolved Slot

```yaml
Body:
  # Use data-stored target player
  target_helmet:
    type: item
    material: '[HEAD:{data:target_player}]'
    width: 32

  # Use PAPI variable
  random_player_helmet:
    type: item
    material: '[HEAD:%random_player%]'
    width: 32
```

---

## ⚠️ Notes

### 1. Listener Configuration

- Player click listener is disabled by default
- Requires manual enable in `config.yml`
- Shift+Right-Click requires `require-sneaking: true`

### 2. Slot Reference

- In slot reference mode, `lore`, `item_model` and other properties are ignored
- Still supports `width`, `height`, `decorations`, `tooltip`, `description`

### 3. Interaction Dependencies

Some features in the player interaction menu require corresponding plugin support:
- Add friend: requires friend plugin (e.g., EssentialsX)
- Inspect inventory: requires permission
- Kick player: requires admin permission

---

## 🚀 Upgrade Guide

### Upgrade from 1.3.1 to 1.3.2

**Steps:**

1. **Update Plugin**
   - Download the latest `KaMenu-1.3.2.jar`
   - Replace the old version jar file
   - Delete the old version jar file

2. **Restart Server**
   - Stop the server completely
   - Restart the server
   - Confirm no errors

3. **Configure Listener (Optional)**
   - Edit `config.yml`
   - Enable `player-click` listener
   - Set the menu to open

4. **Test Features**
   - Right-click to test player interaction menu
   - Check if equipment display works correctly
   - Test each interaction button

**Compatibility:**
- ✅ **Fully Compatible**: No changes needed for existing menus
- ✅ **Backward Compatible**: Old menus continue to work
- ✅ **Optional Features**: New features can be enabled as needed

---

## 📈 Version Comparison

| Feature | 1.3.1 | 1.3.2 |
|---------|-------|-------|
| Player Click Listener | ❌ | ✅ |
| Slot Reference | ❌ | ✅ |
| Player Interaction Menu | ❌ | ✅ |
| Optional item name | ❌ | ✅ |
| Empty Slot Handling | ❌ | ✅ |
| command main thread fix | ❌ | ✅ |
| chat main thread fix | ❌ | ✅ |

---

**v1.3.2 - Interaction Experience Upgrade 🎮**
