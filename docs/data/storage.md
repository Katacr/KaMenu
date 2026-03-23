# 📊 玩家数据与全局数据

KaMenu 内置了键值对存储系统，无需额外插件即可在菜单中持久化读写数据。

---

## 数据类型

### 玩家数据 (Player Data)

以玩家 UUID 为作用域的键值对，每位玩家的数据相互独立。

**写入（动作中）：**

```yaml
actions:
  - 'set-data: <键名> <值>'
```

**读取（任意文本位置）：**

| 方式 | 格式 | 说明 |
|------|------|------|
| 内置变量 | `{data:键名}` | 直接在菜单文本中使用 |
| PAPI 变量 | `%kamenu_data_键名%` | 通过 PlaceholderAPI 使用（可跨插件）|

**示例：**

```yaml
# 写入
actions:
  - 'set-data: vip_level 3'
  - 'set-data: nickname $(input_nickname)'

# 读取 - 在菜单文字中
text: '&7你的 VIP 等级: &6{data:vip_level}'
text: '&7你的昵称: &f{data:nickname}'

# 读取 - 在条件判断中
condition: '{data:vip_level} >= 2'
```

---

### 全局数据 (Global Data)

所有玩家共享的键值对，常用于存储服务器级别的状态信息。

**写入（动作中）：**

```yaml
actions:
  - 'set-gdata: <键名> <值>'
```

**读取（任意文本位置）：**

| 方式 | 格式 | 说明 |
|------|------|------|
| 内置变量 | `{gdata:键名}` | 直接在菜单文本中使用 |
| PAPI 变量 | `%kamenu_gdata_键名%` | 通过 PlaceholderAPI 使用（可跨插件）|

**示例：**

```yaml
# 写入
actions:
  - 'set-gdata: server_event active'
  - 'set-gdata: event_winner %player_name%'

# 读取 - 在菜单文字中
text: '&7服务器活动状态: &a{gdata:server_event}'
text: '&7活动获胜者: &e{gdata:event_winner}'

# 读取 - 在条件判断中
condition: '{gdata:server_event} == active'
```

---

## 完整使用示例

下面是一个使用数据存储系统制作"每日签到"菜单的示例：

```yaml
Title: '&6每日签到'

Body:
  info:
    type: 'message'
    text:
      - condition: "{data:last_sign} == today"
        allow: '&c今日已签到，明天再来吧！'
        deny: '&a今日尚未签到，点击下方按钮领取奖励。'

  reward_item:
    type: 'item'
    material: 'CHEST'
    name: '&6今日签到奖励'
    lore:
      - '&7- 100 金币'
      - '&7- 1 颗钻石'

Bottom:
  type: 'notice'
  confirm:
    text:
      - condition: "{data:last_sign} == today"
        allow: '&8[ 已签到 ]'
        deny: '&a[ 立即签到 ]'
    actions:
      - condition: "{data:last_sign} == today"
        allow:
          - 'tell: &c今日已签到！请明天再来。'
          - 'sound: block.note_block.bass'
        deny:
          - 'set-data: last_sign today'
          - 'console: eco give %player_name% 100'
          - 'console: give %player_name% diamond 1'
          - 'tell: &a签到成功！获得 100 金币和 1 颗钻石。'
          - 'title: title=&6签到成功;subtitle=&f已累计签到 {data:sign_count} 天'
          - 'sound: entity.player.levelup'
```

---

## 数据库配置

数据存储的后端数据库可在 `config.yml` 中配置，支持 SQLite 和 MySQL 两种方式。

详细配置请参阅 [⛳ 配置文件: config.yml](../config/config.md)。

---

## 数据表结构（参考）

KaMenu 在数据库中创建以下两张表：

**player_data 表（玩家数据）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INTEGER | 自增主键 |
| `player_uuid` | VARCHAR(36) | 玩家 UUID |
| `data_key` | VARCHAR(64) | 数据键名 |
| `data_value` | TEXT | 数据值 |
| `update_time` | BIGINT | 最后更新时间戳 |

**global_data 表（全局数据）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INTEGER | 自增主键 |
| `data_key` | VARCHAR(64) | 数据键名（唯一）|
| `data_value` | TEXT | 数据值 |
| `update_time` | BIGINT | 最后更新时间戳 |
