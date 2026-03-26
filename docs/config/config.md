# ⛳ 配置文件: config.yml

`config.yml` 是 KaMenu 的全局配置文件，位于 `plugins/KaMenu/config.yml`。

---

## 📋 完整示例

```yaml
# KaMenu 全局配置文件

# 插件语言 (对应 lang/ 文件夹下的文件名)
language: 'zh_CN'

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
