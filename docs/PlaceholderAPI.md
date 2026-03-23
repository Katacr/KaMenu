# 🔖 PlaceholderAPI

KaMenu 内置了 PlaceholderAPI (PAPI) 扩展，将插件内的数据暴露为可在其他插件（如记分板、聊天插件）中使用的占位符变量。

---

## 前提条件

需要安装 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) 插件，KaMenu 启动时会自动注册扩展，无需额外操作。

---

## 提供的变量

### 玩家数据变量

读取特定玩家的个人持久化数据（由 `set-data` 动作写入）。

**格式：** `%kamenu_data_<键名>%`

**示例：**

| 变量 | 说明 |
|------|------|
| `%kamenu_data_vip_level%` | 读取玩家的 `vip_level` 数据 |
| `%kamenu_data_nickname%` | 读取玩家的 `nickname` 数据 |
| `%kamenu_data_sign_count%` | 读取玩家的 `sign_count` 数据 |

**在其他插件中使用（例如 CMI 记分板）：**

```yaml
# 记分板配置示例
scoreboard:
  - '&6VIP 等级: &f%kamenu_data_vip_level%'
  - '&7昵称: &f%kamenu_data_nickname%'
```

---

### 全局数据变量

读取服务器级别的全局共享数据（由 `set-gdata` 动作写入）。

**格式：** `%kamenu_gdata_<键名>%`

**示例：**

| 变量 | 说明 |
|------|------|
| `%kamenu_gdata_server_event%` | 读取全局的 `server_event` 数据 |
| `%kamenu_gdata_event_winner%` | 读取全局的 `event_winner` 数据 |

**在其他插件中使用：**

```yaml
# 公告栏配置示例
announcements:
  - '当前活动状态: %kamenu_gdata_server_event%'
  - '上次活动获胜者: %kamenu_gdata_event_winner%'
```

---

## 在 KaMenu 菜单内使用 PAPI 变量

KaMenu 也完整支持**解析来自其他 PAPI 扩展的变量**，可以在菜单的任意文本字段和条件判断中使用：

**菜单标题：**

```yaml
Title: '&a欢迎，&f%player_name%！'
```

**组件文字：**

```yaml
Body:
  stats:
    type: 'message'
    text: '&7等级: &f%player_level% &7| 血量: &c%player_health%'
```

**条件判断：**

```yaml
condition: "%player_level% >= 10 && %player_balance% >= 500"
```

**动作中：**

```yaml
actions:
  - 'tell: &a你好，%player_name%！你现在在 %player_world%。'
  - 'console: give %player_name% diamond 1'
```

---

## 数据未找到的提示

当指定键名对应的数据不存在时，PAPI 变量将返回一个提示信息（由语言文件 `papi.data_not_found` 定义），默认为空字符串。可在语言文件中自定义此提示。
