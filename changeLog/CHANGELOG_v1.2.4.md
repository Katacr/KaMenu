# KaMenu v1.2.4 更新报告

## 📋 版本信息
- **版本号**: 1.2.4
- **发布日期**: 2026年3月29日

---

## ✨ 新增功能

### 1. 右键物品 Lore 打开菜单 (item-lore 监听器)

#### 功能说明
添加了新的监听器功能，允许玩家通过右键持有特定材质且包含指定 Lore 文本的物品打开菜单。此功能支持：
- 物品材质（Material）匹配
- 物品 Lore（描述）文本匹配（模糊匹配）
- 可选的潜行（Shift）触发
- 支持配置多个监听器（不同物品打开不同菜单）

#### 配置格式
在 `config.yml` 的 `listeners` 节点下添加 `item-lore` 配置：

```yaml
listeners:
  item-lore:
    # 主菜单配置
    main-menu:
      enabled: true
      material: 'CLOCK'           # 物品材质
      target-lore: '菜单'         # Lore 包含的文本
      menu: 'main_menu'          # 打开的菜单
      require-sneaking: false     # 是否需要潜行

    # 商店菜单配置
    shop:
      enabled: true
      material: 'COMPASS'
      target-lore: '商店'
      menu: 'server_shop'
      require-sneaking: false

    # 传送菜单配置
    teleport:
      enabled: true
      material: 'PLAYER_HEAD'
      target-lore: '传送'
      menu: 'teleport_menu'
      require-sneaking: true       # 需要潜行才触发
```

#### 字段说明

| 字段 | 说明 | 类型 |
|------|------|------|
| `enabled` | 是否启用此配置 | `Boolean` |
| `material` | 物品材质（Material 枚举值） | `String` |
| `target-lore` | 物品 Lore 包含的文本（模糊匹配） | `String` |
| `menu` | 触发时打开的菜单 ID | `String` |
| `require-sneaking` | 是否需要潜行才触发 | `Boolean` |

#### 使用场景
- **新手引导物品** - 新玩家登录时给予一个时钟/指南针，右键打开主菜单
- **功能菜单物品** - 特殊物品（时钟、指南针等）右键打开对应功能菜单
- **多菜单系统** - 不同物品打开不同菜单（商店、传送、设置等）

#### 物品示例
```yaml
# 给玩家时钟物品
give %player_name% clock{display:{Name:'&6服务器菜单',Lore:['&7右键打开服务器菜单']}}
```

---

### 2. PlaceholderAPI 扩展依赖检查 (need_placeholder 配置)

#### 功能说明
为菜单添加了 `need_placeholder` 配置项，用于检查菜单所需的 PlaceholderAPI 扩展是否已加载。在打开菜单前会自动验证：
- **管理员玩家（有 kamenu.admin 权限）**：显示详细提示，包含缺失的扩展列表和可点击的下载按钮
- **普通玩家**：显示简化提示信息

此功能确保菜单中的占位符变量能正常工作，避免因扩展缺失导致的显示错误。

#### 配置格式
在菜单文件的 `Settings` 节点下添加 `need_placeholder` 配置：

```yaml
Settings:
  need_placeholder:
    - 'player'     # Player 扩展
    - 'server'     # Server 扩展
    - 'vault'      # Vault 扩展
```

#### 配置示例

**基础示例：**
```yaml
Title: '&8» &6&l玩家信息 &8«'

Settings:
  need_placeholder:
    - 'player'
    - 'vault'

Body:
  message:
    type: 'message'
    text: |
      &a玩家名称: %player_name%
      &a玩家余额: %vault_eco_balance%
      &a在线时间: %player_time_played%

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 确定 ]'
    actions:
      - 'close'
```

#### 管理员提示
当管理员（有 kamenu.admin 权限）尝试打开缺少依赖的菜单时，会显示类似以下信息：

```
§c该菜单需要以下PlaceholderAPI扩展：§e[player]，§e[server]
```

每个扩展名称（如 `[player]`）都是可点击的：
- **点击扩展**：自动执行 `/papi ecloud download <扩展名>` 命令
- **悬停显示**：显示将要执行的具体下载命令

#### 普通玩家提示
普通玩家会看到简化提示：
```
§c该菜单缺少必要的依赖文件，请联系管理员。
```

#### 常用扩展列表

| 扩展标识 | 说明 | 常用占位符 |
|---------|------|-----------|
| `player` | 玩家信息 | `%player_name%`, `%player_uuid%`, `%player_time_played%` |
| `server` | 服务器信息 | `%server_name%`, `%server_online%`, `%server_tps%` |
| `vault` | 经济/权限 | `%vault_eco_balance%`, `%vault_rank%` |
| `luckperms` | LuckPerms 权限 | `%luckperms_prefix%`, `%luckperms_suffix%` |

---

## 🔧 优化改进

### 1. 用户体验优化
- 扩展缺失提示支持国际化（中英文自动切换）
- 管理员可以直接点击下载所需的 PlaceholderAPI 扩展
- 普通玩家不会看到技术细节，只看到友好的提示信息

### 2. 性能优化
- 扩展检查仅在打开菜单时执行一次，不影响性能
- 检查过程快速高效，无感知延迟

### 3. 安全性增强
- 物品 Lore 匹配忽略颜色代码，避免配置复杂性
- 支持精确的材质匹配，防止误触发

---

## 📚 文档更新

### 新增文档
- `docs/config/config.md` - 添加 `item-lore` 监听器详细文档
  - 配置格式和字段说明
  - 基础示例和多配置示例
  - 使用场景和注意事项

### 更新文档
- `docs/menu/setting.md` - 添加 `need_placeholder` 配置项说明
  - 功能说明和配置格式
  - 管理员和普通玩家提示说明
  - 常用扩展列表和完整示例

### 更新语言文件
- `src/main/resources/lang/zh_CN.yml` - 添加相关中文消息
  - `menu.missing_papi_extensions_prefix`
  - `menu.missing_dependencies`

- `src/main/resources/lang/en_US.yml` - 添加相关英文消息
  - `menu.missing_papi_extensions_prefix`
  - `menu.missing_dependencies`

---

## 📌 使用示例

### 示例 1：右键物品打开主菜单

**config.yml 配置：**
```yaml
listeners:
  item-lore:
    main-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: '菜单'
      menu: 'main_menu'
      require-sneaking: false
```

**给玩家物品：**
```yaml
# 在新手礼包或其他地方执行
give %player_name% clock{display:{Name:'&6服务器菜单',Lore:['&7右键打开服务器菜单','&7包含所有功能入口']}}
```

### 示例 2：多物品打开不同菜单

**config.yml 配置：**
```yaml
listeners:
  item-lore:
    main-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: '菜单'
      menu: 'main_menu'
      require-sneaking: false

    shop:
      enabled: true
      material: 'COMPASS'
      target-lore: '商店'
      menu: 'server_shop'
      require-sneaking: false

    teleport:
      enabled: true
      material: 'PLAYER_HEAD'
      target-lore: '传送'
      menu: 'teleport_menu'
      require-sneaking: true  # 需要潜行才触发
```

### 示例 3：菜单配置 PlaceholderAPI 依赖

**菜单文件配置：**
```yaml
Title: '&8» &6&l玩家信息 &8«'

Settings:
  can_escape: true
  after_action: NONE
  pause: false
  need_placeholder:    # 所需扩展
    - 'player'
    - 'vault'

Body:
  message:
    type: 'message'
    text: |
      &a欢迎来到服务器！
      &7当前余额: &e%vault_eco_balance% &7金币
      &7在线时间: &e%player_time_played%
      &7玩家名称: &e%player_name%

Bottom:
  type: 'multi'
  buttons:
    check_balance:
      text: '&a[ 查看余额 ]'
      actions:
        - 'tell: &a当前余额: %vault_eco_balance% 金币'
    check_time:
      text: '&b[ 查看时间 ]'
      actions:
        - 'tell: &b在线时间: %player_time_played%'
    exit:
      text: '&c[ 退出 ]'
      actions:
        - 'close'
```

---

## 🎯 兼容性说明

- 本版本完全兼容旧版本配置
- 新增功能不影响现有菜单的正常运行
- 建议更新后测试现有菜单，确保功能正常

---

## 🐛 Bug 修复
- 修复了 PlaceholderAPI 扩展检查时的 API 调用错误
- 使用正确的静态方法 `PlaceholderAPI.isRegistered()` 替代错误的实例调用

---

## 📝 注意事项

### item-lore 监听器
1. 确保 Lore 文本足够独特，避免与其他物品冲突
2. `target-lore` 是模糊匹配，只要包含该文本就会触发
3. 推荐为功能物品设置独特的 Lore 文本
4. 管理员权限玩家（kamenu.admin）会看到详细提示信息
5. 使用 `require-sneaking: true` 可以避免误操作

### need_placeholder 配置
1. 确保所有扩展都已安装并启用
2. 如果扩展未安装，玩家将无法打开菜单
3. 管理员可以看到缺失的扩展列表和下载提示
4. 使用 `/papi list` 查看已安装的扩展
5. 下载扩展后需要执行 `/papi reload` 重载

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

# KaMenu v1.2.4 Update Notes

## 📋 Version Information
- **Version**: 1.2.4
- **Release Date**: March 29, 2026

---

## ✨ New Features

### 1. Right-Click Item Lore to Open Menu (item-lore Listener)

#### Feature Description
Added new listener functionality allowing players to open menus by right-clicking items with specific material and lore text. This feature supports:
- Item material (Material) matching
- Item lore (description) text matching (fuzzy matching)
- Optional sneaking (Shift) trigger
- Supports configuring multiple listeners (different items open different menus)

#### Configuration Format
Add `item-lore` configuration under the `listeners` node in `config.yml`:

```yaml
listeners:
  item-lore:
    # Main menu configuration
    main-menu:
      enabled: true
      material: 'CLOCK'           # Item material
      target-lore: 'menu'         # Text contained in lore
      menu: 'main_menu'          # Menu to open
      require-sneaking: false     # Whether sneaking is required

    # Shop menu configuration
    shop:
      enabled: true
      material: 'COMPASS'
      target-lore: 'shop'
      menu: 'server_shop'
      require-sneaking: false

    # Teleport menu configuration
    teleport:
      enabled: true
      material: 'PLAYER_HEAD'
      target-lore: 'teleport'
      menu: 'teleport_menu'
      require-sneaking: true      # Requires sneaking to trigger
```

#### Field Description

| Field | Description | Type |
|------|-------------|------|
| `enabled` | Whether this configuration is enabled | `Boolean` |
| `material` | Item material (Material enum value) | `String` |
| `target-lore` | Text contained in item lore (fuzzy match) | `String` |
| `menu` | Menu ID to open when triggered | `String` |
| `require-sneaking` | Whether sneaking is required to trigger | `Boolean` |

#### Use Cases
- **New Player Guide Items** - Give new players a clock/compass on login that opens the main menu
- **Function Menu Items** - Special items (clock, compass, etc.) open corresponding function menus
- **Multi-Menu System** - Different items open different menus (shop, teleport, settings, etc.)

#### Item Example
```yaml
# Give player a clock item
give %player_name% clock{display:{Name:'&6Server Menu',Lore:['&7Right-click to open server menu']}}
```

---

### 2. PlaceholderAPI Extension Dependency Check (need_placeholder Configuration)

#### Feature Description
Added `need_placeholder` configuration option for menus to check if required PlaceholderAPI extensions are loaded. Before opening a menu, it automatically verifies:
- **Admin players (with kamenu.admin permission)**: Display detailed prompt with missing extension list and clickable download button
- **Regular players**: Display simplified prompt message

This feature ensures placeholder variables in menus work correctly, avoiding display errors due to missing extensions.

#### Configuration Format
Add `need_placeholder` configuration under the `Settings` node in the menu file:

```yaml
Settings:
  need_placeholder:
    - 'player'     # Player extension
    - 'server'     # Server extension
    - 'vault'      # Vault extension
```

#### Configuration Example

**Basic Example:**
```yaml
Title: '&8» &6&lPlayer Info &8«'

Settings:
  need_placeholder:
    - 'player'
    - 'vault'

Body:
  message:
    type: 'message'
    text: |
      &aPlayer Name: %player_name%
      &aPlayer Balance: %vault_eco_balance%
      &aPlay Time: %player_time_played%

Bottom:
  type: 'notice'
  confirm:
    text: '&a[ Confirm ]'
    actions:
      - 'close'
```

#### Admin Prompt
When admins (with kamenu.admin permission) try to open a menu with missing dependencies, they will see a message similar to:

```
§cThis menu requires the following PlaceholderAPI extensions: §e[player], §e[server]
```

Each extension name (like `[player]`) is clickable:
- **Click extension**: Automatically executes `/papi ecloud download <extension_name>` command
- **Hover display**: Shows the specific download command to be executed

#### Regular Player Prompt
Regular players will see a simplified prompt:
```
§cThis menu is missing required dependencies, please contact an administrator.
```

#### Common Extension List

| Extension ID | Description | Common Placeholders |
|--------------|-------------|--------------------|
| `player` | Player information | `%player_name%`, `%player_uuid%`, `%player_time_played%` |
| `server` | Server information | `%server_name%`, `%server_online%`, `%server_tps%` |
| `vault` | Economy/Permissions | `%vault_eco_balance%`, `%vault_rank%` |
| `luckperms` | LuckPerms permissions | `%luckperms_prefix%`, `%luckperms_suffix%` |

---

## 🔧 Optimizations

### 1. User Experience Improvements
- Extension missing prompt supports internationalization (auto Chinese/English switching)
- Admins can directly click to download required PlaceholderAPI extensions
- Regular players don't see technical details, only friendly prompts

### 2. Performance Optimization
- Extension check only executes once when opening menu, no performance impact
- Check process is fast and efficient, no perceptible delay

### 3. Security Enhancements
- Item lore matching ignores color codes, reducing configuration complexity
- Supports precise material matching to prevent accidental triggers

---

## 📚 Documentation Updates

### New Documentation
- `docs/config/config.md` - Added detailed `item-lore` listener documentation
  - Configuration format and field descriptions
  - Basic and multi-configuration examples
  - Use cases and notes

### Updated Documentation
- `docs/menu/setting.md` - Added `need_placeholder` configuration option documentation
  - Feature description and configuration format
  - Admin and regular player prompt descriptions
  - Common extension list and complete examples

### Updated Language Files
- `src/main/resources/lang/zh_CN.yml` - Added related Chinese messages
  - `menu.missing_papi_extensions_prefix`
  - `menu.missing_dependencies`

- `src/main/resources/lang/en_US.yml` - Added related English messages
  - `menu.missing_papi_extensions_prefix`
  - `menu.missing_dependencies`

---

## 📌 Usage Examples

### Example 1: Right-Click Item to Open Main Menu

**config.yml Configuration:**
```yaml
listeners:
  item-lore:
    main-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: 'menu'
      menu: 'main_menu'
      require-sneaking: false
```

**Give Player Item:**
```yaml
# Execute in starter kit or other places
give %player_name% clock{display:{Name:'&6Server Menu',Lore:['&7Right-click to open server menu','&7Contains all function entries']}}
```

### Example 2: Multiple Items Open Different Menus

**config.yml Configuration:**
```yaml
listeners:
  item-lore:
    main-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: 'menu'
      menu: 'main_menu'
      require-sneaking: false

    shop:
      enabled: true
      material: 'COMPASS'
      target-lore: 'shop'
      menu: 'server_shop'
      require-sneaking: false

    teleport:
      enabled: true
      material: 'PLAYER_HEAD'
      target-lore: 'teleport'
      menu: 'teleport_menu'
      require-sneaking: true  # Requires sneaking to trigger
```

### Example 3: Menu Configuration with PlaceholderAPI Dependencies

**Menu File Configuration:**
```yaml
Title: '&8» &6&lPlayer Info &8«'

Settings:
  can_escape: true
  after_action: NONE
  pause: false
  need_placeholder:    # Required extensions
    - 'player'
    - 'vault'

Body:
  message:
    type: 'message'
    text: |
      &aWelcome to the server!
      &7Current Balance: &e%vault_eco_balance% &7coins
      &7Play Time: &e%player_time_played%
      &7Player Name: &e%player_name%

Bottom:
  type: 'multi'
  buttons:
    check_balance:
      text: '&a[ Check Balance ]'
      actions:
        - 'tell: &aCurrent balance: %vault_eco_balance% coins'
    check_time:
      text: '&b[ Check Time ]'
      actions:
        - 'tell: &bPlay time: %player_time_played%'
    exit:
      text: '&c[ Exit ]'
      actions:
        - 'close'
```

---

## 🎯 Compatibility Notes
- This version is fully backward compatible with older configurations
- New features do not affect existing menu functionality
- Recommended to test existing menus after update to ensure proper operation

---

## 🐛 Bug Fixes
- Fixed API call error when checking PlaceholderAPI extensions
- Used correct static method `PlaceholderAPI.isRegistered()` instead of incorrect instance call

---

## 📝 Notes

### item-lore Listener
1. Ensure lore text is distinctive enough to avoid conflicts with other items
2. `target-lore` is fuzzy matching - it triggers if the text is contained
3. Recommended to set unique lore text for function items
4. Admin permission players (kamenu.admin) will see detailed prompt information
5. Using `require-sneaking: true` can avoid accidental operations

### need_placeholder Configuration
1. Ensure all extensions are installed and enabled
2. If extensions are not installed, players will not be able to open the menu
3. Admins can see the list of missing extensions and download prompts
4. Use `/papi list` to view installed extensions
5. After downloading extensions, need to execute `/papi reload` to reload

---

## 📝 Next Steps
- Continue optimizing listener functionality
- Add more condition checking methods
- Expand item operation features
- Optimize user interface experience

---

**Thank you for using KaMenu! If you have any questions or suggestions, please feel free to provide feedback.**
