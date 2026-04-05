# ⛳ 配置文件: config.yml

`config.yml` 是 KaMenu 的全局配置文件，位于 `plugins/KaMenu/config.yml`。

---

## 📋 完整示例

```yaml
# KaMenu 全局配置文件

# 插件语言 (对应 lang/ 文件夹下的文件名)
language: 'zh_CN'

# BungeeCord 支持（用于 server: 动作）
bungeecord: true

# 数据库配置
storage:
  # 可选: sqlite, mysql
  type: 'sqlite'
  host: 'localhost'
  port: 3306
  db: 'minecraft'
  user: 'root'
  password: ''

# 快捷键/动作监听配置
listeners:
  # 切换副手 (默认按键 F) 触发
  swap-hand:
    enabled: true
    # 触发时打开的菜单文件名
    menu: 'main_menu'
    # 是否需要潜行时才触发
    require-sneaking: true

  # 右键玩家监听器（右键玩家触发）
  player-click:
    # 启用此监听器
    enabled: true
    # 要打开的菜单文件路径
    menu: 'inspect_player'
    # 需要潜行才能触发（Shift+右键）
    require-sneaking: true 
    
  # 右键物品 Lore 触发（支持多个配置）
  item-lore:
    main-menu:  # 配置名称（自定义）
      enabled: true
      # 物品材质（必须匹配）
      material: 'CLOCK'
      # 目标 Lore 文本（包含该文本即匹配）
      target-lore: '菜单'
      # 触发时打开的菜单文件名
      menu: 'main_menu'
      # 是否需要潜行时才触发
      require-sneaking: false
    # 可以添加更多配置...
    # shop-menu:
    #   enabled: true
    #   material: 'COMPASS'
    #   target-lore: '商店'
    #   menu: 'server_shop'
    #   require-sneaking: false

# 自定义指令注册
# 格式: 指令名: 菜单文件名
custom-commands:
  zcd: 'main_menu'
  shop: 'server_shop'
  menu: 'main_menu'
```

---

## 🔧 配置项详解

### language - 插件语言

设置插件的显示语言，对应 `plugins/KaMenu/lang/` 目录下的语言文件名（不含 `.yml` 扩展名）。

**类型：** `String`

**内置可选值：**

| 值 | 语言 |
|----|------|
| `zh_CN` | 简体中文（默认）|
| `en_US` | English |

**示例：**

```yaml
language: 'en_US'
```

---

### bungeecord - BungeeCord 支持

配置插件是否启用 BungeeCord/Velocity 代理支持，用于 `server:` 动作将玩家传送到其他服务器。

**类型：** `Boolean`

**默认值：** `false`

**字段说明：**

| 值 | 说明 |
|----|------|
| `true` | 启用 BungeeCord 支持，`server:` 动作使用插件消息系统，无需玩家权限 |
| `false` | 禁用 BungeeCord 支持，`server:` 动作使用 `/server` 命令（需要玩家有相应权限） |

**使用 BungeeCord 模式（推荐）：**

```yaml
bungeecord: true
```

此模式使用 BungeeCord 插件消息系统直接与代理服务器通信，具有以下优势：

- ✅ **无需玩家权限**：不需要玩家拥有 `/server` 命令权限
- ✅ **更加可靠**：不依赖命令系统，兼容性更好
- ✅ **性能更优**：避免了命令解析和权限检查的开销
- ✅ **标准化实现**：与 DeluxeMenus 等主流插件保持一致

**使用命令模式（非代理服务器）：**

```yaml
bungeecord: false
```

适用于单服务器或通过其他方式实现跨服传送的情况。

{% hint style="info" %}
**使用建议：**
- 如果您的服务器运行在 BungeeCord/Velocity 代理后面，建议设置为 `true`
- 如果是单服务器或使用其他跨服方案，设置为 `false` 即可
- 启用 BungeeCord 模式后，`server:` 动作会自动使用插件消息系统
{% endhint %}

---

### storage - 数据库配置

配置 KaMenu 的持久化存储后端，用于保存玩家数据和全局数据。

**类型：** `type` 字段决定使用哪种数据库

| 字段 | 说明 | 默认值 |
|------|------|--------|
| `type` | 数据库类型，可选 `sqlite` 或 `mysql` | `sqlite` |
| `host` | MySQL 主机地址 | `localhost` |
| `port` | MySQL 端口 | `3306` |
| `db` | MySQL 数据库名 | `minecraft` |
| `user` | MySQL 用户名 | `root` |
| `password` | MySQL 密码 | _(空)_ |

**使用 SQLite（推荐单服务器）：**

```yaml
storage:
  type: 'sqlite'
```

数据将存储在 `plugins/KaMenu/storage.db` 文件中，无需额外配置。

**使用 MySQL（推荐多服务器/大型服务器）：**

```yaml
storage:
  type: 'mysql'
  host: '127.0.0.1'
  port: 3306
  db: 'kamenu_db'
  user: 'mc_user'
  password: 'your_password'
```

---

### listeners - 快捷键监听

配置通过玩家特定操作自动打开菜单的快捷方式。

#### swap-hand - 切换副手触发

玩家按下切换副手键（默认 `F`）时触发打开菜单。

| 字段 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| `enabled` | 是否启用此监听 | `Boolean` | `true` |
| `menu` | 触发时打开的菜单 ID | `String` | `main_menu` |
| `require-sneaking` | 是否需要同时按住潜行键（Shift）才触发 | `Boolean` | `true` |

**示例：**

```yaml
listeners:
  swap-hand:
    enabled: true
    menu: 'server_menu'
    require-sneaking: true   # Shift + F 才触发，避免误操作
```

{% hint style="info" %}
启用 `require-sneaking` 可以防止玩家在普通游戏过程中意外触发菜单，推荐保持开启。
{% endhint %}

#### item-lore - 右键物品 Lore 触发

玩家右键持有指定材质且包含特定 Lore 文本的物品时触发打开菜单。

**配置格式：**

```yaml
listeners:
  item-lore:
    配置名称:        # 自定义名称，用于区分不同配置
      enabled: true  # 是否启用此配置
      material: 'CLOCK'              # 物品材质（必须匹配）
      target-lore: '菜单'            # 目标 Lore 文本
      menu: 'main_menu'              # 触发时打开的菜单 ID
      require-sneaking: false        # 是否需要潜行才触发
```

**字段说明：**

| 字段 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| `enabled` | 是否启用此监听配置 | `Boolean` | `true` |
| `material` | 物品材质（Material 枚举值，必须匹配） | `String` | 无 |
| `target-lore` | 物品 Lore 中包含的文本（包含即匹配） | `String` | 无 |
| `menu` | 触发时打开的菜单 ID | `String` | 无 |
| `require-sneaking` | 是否需要同时按住潜行键（Shift）才触发 | `Boolean` | `false` |

**基础示例：**

```yaml
listeners:
  item-lore:
    server-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: '服务器菜单'
      menu: 'server_menu'
      require-sneaking: false
```

**多配置示例：**

```yaml
listeners:
  item-lore:
    # 主菜单：时钟物品，包含"菜单"文本
    main-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: '菜单'
      menu: 'main_menu'
      require-sneaking: false

    # 商店菜单：指南针物品，包含"商店"文本
    shop:
      enabled: true
      material: 'COMPASS'
      target-lore: '商店'
      menu: 'server_shop'
      require-sneaking: false

    # 传送菜单：玩家头部物品，需要潜行
    teleport:
      enabled: true
      material: 'PLAYER_HEAD'
      target-lore: '传送'
      menu: 'teleport_menu'
      require-sneaking: true
```

**使用场景：**

1. **主菜单物品** - 新玩家登录时给予一个特殊物品，右键打开主菜单
2. **功能菜单** - 时钟/指南针等物品，右键打开对应功能菜单
3. **特殊工具** - 特定功能的物品，右键打开相关菜单

{% hint style="info" %}
- 支持配置多个 item-lore 监听器，每个监听器可以设置不同的物品和菜单
- `target-lore` 是模糊匹配，只要物品 Lore 中包含该文本就会触发
- 推荐为功能物品设置独特的 Lore 文本，避免与其他物品冲突
{% endhint %}

{% hint style="warning" %}
**注意事项：**
- 物品 Lore 的颜色代码会被忽略进行匹配（原始文本匹配）
- 确保物品 Lore 文本足够独特，避免误触发
{% endhint %}

#### player-click - 右键玩家触发

玩家右键点击其他玩家时触发打开菜单，支持普通右键和 Shift+右键。

**配置格式：**

```yaml
listeners:
  player-click:
    enabled: false              # 是否启用此监听
    menu: 'inspect_player'      # 触发时打开的菜单 ID
    require-sneaking: false      # 是否需要潜行时才触发
```

**字段说明：**

| 字段 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| `enabled` | 是否启用此监听 | `Boolean` | `false` |
| `menu` | 触发时打开的菜单 ID | `String` | 无 |
| `require-sneaking` | 是否需要同时按住潜行键（Shift）才触发 | `Boolean` | `false` |

**基础示例：**

```yaml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: false
```

**Shift+右键示例：**

```yaml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: true   # 只有 Shift + 右键才触发
```

**Meta 数据设置：**

当触发 `player-click` 监听器时，系统会自动设置一个 meta 数据：

- **Meta 键名**：`player`
- **Meta 值**：被点击玩家的名称
- **使用方式**：在菜单中可以通过 `{meta:player}` 引用被点击玩家

**配合槽位引用示例：**

```yaml
# config.yml
listeners:
  player-click:
    enabled: true
    menu: 'inspect_player'
    require-sneaking: false

# menus/inspect_player.yml
Body:
  helmet:
    type: 'item'
    material: '[HEAD:{meta:player}]'  # 显示被点击玩家的头盔
    width: 32
    height: 32

  chestplate:
    type: 'item'
    material: '[CHEST:{meta:player}]'  # 显示被点击玩家的胸甲
    width: 32
    height: 32
```

**使用场景：**

1. **玩家互动菜单** - 右键玩家查看装备并进行互动（私聊、传送、加好友等）
2. **管理员工具** - 右键玩家快速打开管理菜单（查看信息、封禁、传送等）
3. **角色扮演服务器** - 右键玩家查看角色信息和进行互动

{% hint style="info" %}
- 默认关闭（`enabled: false`），需要手动启用
- `{meta:player}` 只在通过 player-click 监听器打开的菜单中可用
- 配合槽位引用功能可以显示被点击玩家的装备
{% endhint %}

{% hint style="warning" %}
**注意事项：**
- 如果玩家不存在或已下线，右键不会触发菜单
- 需要配合槽位引用功能（如 `[HEAD:{meta:player}]`）才能显示被点击玩家的装备
- 右键事件会被取消，不会触发其他插件的右键玩家事件
{% endhint %}

---

### custom-commands - 自定义指令

将简短的自定义指令注册为打开指定菜单的快捷方式，无需额外权限配置。

**格式：** `指令名: 菜单ID`

**示例：**

```yaml
custom-commands:
  shop: 'server_shop'       # /shop -> 打开 server_shop 菜单
  menu: 'main_menu'         # /menu -> 打开 main_menu 菜单
  hub: 'hub/main'           # /hub  -> 打开 hub/main 菜单（子文件夹）
```
想要了解自定义指令的用法和优势，点击此处 [⌨️ 自定义指令](customCommands.md)
