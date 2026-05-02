# KaMenu v1.3.7 更新报告

## 📋 版本信息
- **版本号**: 1.3.7

---

## ✨ 新增功能

### 1. 多层递归条件判断支持

**功能说明:**
现在菜单内大部分文本配置支持多层递归条件判断，可以实现更复杂的条件逻辑。

**功能特性:**
- ✅ **多层嵌套**：支持任意深度的条件嵌套
- ✅ **自动递归解析**：系统自动递归解析所有条件
- ✅ **广泛适用**：适用于 tooltip、lore、name、description 等大部分文本配置

**使用场景:**

**多层嵌套 tooltip 示例：**
```yaml
tooltip:
  - condition: '{data:sign_day} >= 1'
    allow: '&7已领取'
    deny:
      - condition: "{data:last_date} == %server_time%"
        allow:
          - condition: 'hasPerm.admin'
            allow: '&c管理员今日已签到'
            deny: '&c玩家今日已签到'
        deny: '&a可领取奖励'
```

**逻辑解析：**
1. 第一层：检查 `sign_day >= 1`
   - 满足 → 显示 "&7已领取"
   - 不满足 → 进入第二层判断
2. 第二层：检查 `last_date == server_time`
   - 满足 → 进入第三层判断（检查权限）
   - 不满足 → 显示 "&a可领取奖励"

---

### 2. return 动作中断动作列表

**功能说明:**
`return` 动作现在会中断当前动作列表的执行，实现条件性的动作分组。

**功能特性:**
- ✅ **立即中断**：遇到 return 时立即停止后续动作执行
- ✅ **条件分组**：配合条件判断实现动作的条件执行
- ✅ **简化逻辑**：避免使用复杂的条件嵌套

**使用场景:**

**签到系统示例：**
```yaml
actions:
  # 已领取
  - condition: '{data:sign_day} >= 1'
    allow:
      - 'actions: already_done'
      - 'return'
  # 未到第1日（不可能，但保险）
  - condition: '{data:sign_day} < 0'
    allow:
      - 'actions: not_yet'
      - 'return'
  # 今日已签到
  - condition: "{data:sign_last_date} == %server_time_YYYYMMdd%"
    allow:
      - 'actions: already_today'
      - 'return'
  # 正常签到
  - 'data: type=set;key=sign_last_date;var=`%server_time_YYYYMMdd%`'
  - 'data: type=set;key=sign_day;var=`1`'
  - 'money: type=add;num=100'
  - 'item: type=give;mats=DIAMOND;amount=1'
```

**逻辑解析：**
1. 检查 `sign_day >= 1` → 满足则执行 `already_done` 动作并 return 中断
2. 检查 `sign_day < 0` → 满足则执行 `not_yet` 动作并 return 中断
3. 检查 `sign_last_date == 今天` → 满足则执行 `already_today` 动作并 return 中断
4. 以上都不满足 → 执行正常签到逻辑

---

## 📝 配置示例

### 示例 1：多层条件 lore

```yaml
Body:
  quest_item:
    type: 'item'
    material: 'BOOK'
    name: '&e&l每日任务'
    lore:
      - condition: '{data:quest_complete} == true'
        allow:
          - '&a✅ 任务已完成'
          - '&7奖励已发放'
        deny:
          - condition: '{data:quest_progress} >= 50'
            allow:
              - '&e⏳ 任务进度: {data:quest_progress}%'
              - '&a点击领取奖励'
            deny:
              - '&c⛔ 任务未完成'
              - '&7进度: {data:quest_progress}%'
    width: 16
    height: 16
```

### 示例 2：多层条件 name

```yaml
Body:
  player_status:
    type: 'item'
    material: 'PLAYER_HEAD'
    name:
      - condition: '%vault_rank% == VIP'
        allow: '&5[VIP] {player}'
        deny:
          - condition: '%vault_rank% == SVIP'
            allow: '&6[SVIP] {player}'
            deny: '&7{player}'
    width: 16
    height: 16
```

### 示例 3：多层条件 description

```yaml
Body:
  info_panel:
    type: 'item'
    material: 'PAPER'
    name: '&f信息面板'
    description:
      - condition: 'hasPerm.admin'
        allow:
          - '&c【管理员面板】'
          - '&7金币: {gdata:total_coins}'
          - '&7玩家: {gdata:total_players}'
        deny:
          - condition: '%vault_rank% == VIP'
            allow:
              - '&6【VIP 专属】'
              - '&7欢迎回来，{player}！'
              - '&e剩余次数: {data:vip_uses}'
            deny:
              - '&7【免费用户】'
              - '&7欢迎，{player}'
    width: 16
    height: 16
```

---

## ⚠️ 注意事项

- 多层条件支持理论上无深度限制，但建议保持逻辑清晰
- return 动作只中断当前动作列表，不会影响其他组件
- 条件判断支持 PAPI 变量、内置变量 `{data:}`, `{gdata:}`, `{meta:}`
- 内置条件方法：`hasPerm`, `hasMoney`, `hasItem`, `hasStockItem`, `isNum`, `isInt` 等

---

## 🚀 升级指南

### 从 1.3.6 升级到 1.3.7

**升级步骤:**

1. **更新插件**
   - 下载最新的 `KaMenu-1.3.7.jar`
   - 替换旧版本 jar 文件
   - 删除旧版本 jar 文件

2. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器
   - 确认无错误

3. **测试新功能**
   - 测试多层条件判断配置
   - 测试 return 中断动作功能
   - 检查日志是否有警告信息

**兼容性:**
- ✅ **完全兼容**：现有配置无需任何修改
- ✅ **可选功能**：新功能为可选添加
- ✅ **平滑升级**：无需配置迁移

---

## 📈 版本对比

| 功能 | 1.3.6 | 1.3.7 |
|------|-------|-------|
| 基础条件判断 | ✅ | ✅ |
| 多层递归条件 | ❌ | ✅ |
| return 中断动作 | ❌ | ✅ |
| 条件内动作列表 | ❌ | ✅ |

---

## 📚 相关文档

- **配置文档**: `docs/menu/body.md` - item 组件完整文档
- **示例菜单**: `menus/example/` - 中文示例
- **示例菜单**: `menus/exampleEN/` - 英文示例

---

## 🔍 技术细节

### 修改文件

**文件:** `src/main/kotlin/org/katacr/kamenu/ConditionUtils.kt`

**新增/修改方法:**
- `resolveConditionValueToList` - 添加 String 类型处理
- `getStringList` - 添加字符串转列表逻辑
- `resolveConditionValueToString` - 保持字符串原始值
- `getFirstMatch` - 修复 isNonEmpty 检查逻辑

### 数据流

```
配置读取 → 类型检测
         ├─ String → 包装为列表
         ├─ Map → 条件判断处理
         └─ List → 递归解析每个元素
```


---

# KaMenu v1.3.7 Update Report

## 📋 Version Info
- **Version**: 1.3.7

---

## ✨ New Features

### 1. Multi-level Recursive Conditional Support

**Description:**
Now most text configurations in menus support multi-level recursive conditional evaluation, enabling more complex conditional logic.

**Features:**
- ✅ **Multi-level Nesting**: Supports arbitrary nesting depth
- ✅ **Auto Recursive Parsing**: System automatically recursively parses all conditions
- ✅ **Wide Applicability**: Applicable to tooltip, lore, name, description and most other text configurations

**Use Cases:**

**Multi-level Nested Tooltip Example:**
```yaml
tooltip:
  - condition: '{data:sign_day} >= 1'
    allow: '&7Already claimed'
    deny:
      - condition: "{data:last_date} == %server_time%"
        allow:
          - condition: 'hasPerm.admin'
            allow: '&cAdmin signed in today'
            deny: '&cPlayer signed in today'
        deny: '&aCan claim reward'
```

**Logic Analysis:**
1. Level 1: Check `sign_day >= 1`
   - Satisfied → Display "&7Already claimed"
   - Not satisfied → Go to level 2
2. Level 2: Check `last_date == server_time`
   - Satisfied → Go to level 3 (check permission)
   - Not satisfied → Display "&aCan claim reward"

---

### 2. return Action Interrupts Action List

**Description:**
The `return` action now interrupts the execution of the current action list, enabling conditional action grouping.

**Features:**
- ✅ **Immediate Interrupt**: Stops subsequent action execution immediately when return is encountered
- ✅ **Conditional Grouping**: Implement conditional action execution with conditionals
- ✅ **Simplified Logic**: Avoid complex conditional nesting

**Use Cases:**

**Daily Sign System Example:**
```yaml
actions:
  # Already claimed
  - condition: '{data:sign_day} >= 1'
    allow:
      - 'actions: already_done'
      - 'return'
  # Not yet day 1 (impossible, but for safety)
  - condition: '{data:sign_day} < 0'
    allow:
      - 'actions: not_yet'
      - 'return'
  # Signed in today
  - condition: "{data:sign_last_date} == %server_time_YYYYMMdd%"
    allow:
      - 'actions: already_today'
      - 'return'
  # Normal sign in
  - 'data: type=set;key=sign_last_date;var=`%server_time_YYYYMMdd%`'
  - 'data: type=set;key=sign_day;var=`1`'
  - 'money: type=add;num=100'
  - 'item: type=give;mats=DIAMOND;amount=1'
```

**Logic Analysis:**
1. Check `sign_day >= 1` → If satisfied, execute `already_done` action and return
2. Check `sign_day < 0` → If satisfied, execute `not_yet` action and return
3. Check `sign_last_date == today` → If satisfied, execute `already_today` action and return
4. None satisfied → Execute normal sign-in logic

---

## 📝 Configuration Examples

### Example 1: Multi-level Conditional Lore

```yaml
Body:
  quest_item:
    type: 'item'
    material: 'BOOK'
    name: '&e&lDaily Quest'
    lore:
      - condition: '{data:quest_complete} == true'
        allow:
          - '&a✅ Quest completed'
          - '&7Reward distributed'
        deny:
          - condition: '{data:quest_progress} >= 50'
            allow:
              - '&e⏳ Quest progress: {data:quest_progress}%'
              - '&aClick to claim reward'
            deny:
              - '&c⛔ Quest not completed'
              - '&7Progress: {data:quest_progress}%'
    width: 16
    height: 16
```

### Example 2: Multi-level Conditional Name

```yaml
Body:
  player_status:
    type: 'item'
    material: 'PLAYER_HEAD'
    name:
      - condition: '%vault_rank% == VIP'
        allow: '&5[VIP] {player}'
        deny:
          - condition: '%vault_rank% == SVIP'
            allow: '&6[SVIP] {player}'
            deny: '&7{player}'
    width: 16
    height: 16
```

### Example 3: Multi-level Conditional Description

```yaml
Body:
  info_panel:
    type: 'item'
    material: 'PAPER'
    name: '&fInfo Panel'
    description:
      - condition: 'hasPerm.admin'
        allow:
          - '&c【Admin Panel】'
          - '&7Coins: {gdata:total_coins}'
          - '&7Players: {gdata:total_players}'
        deny:
          - condition: '%vault_rank% == VIP'
            allow:
              - '&6【VIP Exclusive】'
              - '&7Welcome back, {player}!'
              - '&eRemaining uses: {data:vip_uses}'
            deny:
              - '&7【Free User】'
              - '&7Welcome, {player}'
    width: 16
    height: 16
```

---

## ⚠️ Notes

- Multi-level conditions theoretically have no depth limit, but keep logic clear
- return action only interrupts current action list, won't affect other components
- Conditionals support PAPI variables, built-in variables `{data:}`, `{gdata:}`, `{meta:}`
- Built-in condition methods: `hasPerm`, `hasMoney`, `hasItem`, `hasStockItem`, `isNum`, `isInt`, etc.

---

## 🚀 Upgrade Guide

### Upgrade from 1.3.6 to 1.3.7

**Steps:**

1. **Update Plugin**
   - Download latest `KaMenu-1.3.7.jar`
   - Replace old version jar file
   - Delete old version jar file

2. **Restart Server**
   - Stop server completely
   - Restart server
   - Confirm no errors

3. **Test New Features**
   - Test multi-level conditional configuration
   - Test return interrupt action feature
   - Check logs for any warning messages

**Compatibility:**
- ✅ **Fully Compatible**: Existing configurations need no modifications
- ✅ **Optional Features**: New features are optional additions
- ✅ **Smooth Upgrade**: No config migration required

---

## 📈 Version Comparison

| Feature | 1.3.6 | 1.3.7 |
|---------|-------|-------|
| Basic conditional support | ✅ | ✅ |
| Multi-level recursive conditionals | ❌ | ✅ |
| return action interrupt | ❌ | ✅ |
| Conditional action lists | ❌ | ✅ |

---

## 📚 Related Documentation

- **Config Docs**: `docs/menu/body.md` - Complete item component documentation
- **Example Menu**: `menus/example/` - Chinese examples
- **Example Menu**: `menus/exampleEN/` - English examples

---

## 🔍 Technical Details

### Modified Files

**File:** `src/main/kotlin/org/katacr/kamenu/ConditionUtils.kt`

**New/Modified Methods:**
- `resolveConditionValueToList` - Added String type handling
- `getStringList` - Added string to list conversion logic
- `resolveConditionValueToString` - Keep string original value
- `getFirstMatch` - Fixed isNonEmpty check logic

### Data Flow

```
Config Reading → Type Detection
              ├─ String → Wrap to list
              ├─ Map → Conditional processing
              └─ List → Recursively parse each element
```

---

