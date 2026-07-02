# 📊 数据存储

KaMenu 内置了数据存储系统，无需额外插件即可在菜单中持久化读写玩家数据、全局数据、玩家列表和全局列表。

---

## 数据类型

### 玩家数据 (Player Data)

以玩家 UUID 为作用域的键值对，每位玩家的数据相互独立。

**写入（动作中）：**

```yaml
actions:
  - 'data: type=set;key=<键名>;var=<值>'
  - 'data: type=add;key=<键名>;var=<数字>'
  - 'data: type=take;key=<键名>;var=<数字>'
  - 'data: type=delete;key=<键名>'
```

`type` 支持：

- `set`：设置文本或数字值
- `add`：增加数字值
- `take`：减少数字值
- `delete`：删除该键

简写格式 `set-data: <键名> <值>` 也可使用，适合只需要设置一个值的场景；需要 `add` / `take` / `delete` 时使用 `data:` 参数写法。

**读取（任意文本位置）：**

| 方式 | 格式 | 说明 |
|------|------|------|
| 内置变量 | `{data:键名}` | 直接在菜单文本中使用 |
| PAPI 变量 | `%kamenu_data_键名%` | 通过 PlaceholderAPI 使用（可跨插件）|

**示例：**

```yaml
# 写入
actions:
  - 'data: type=set;key=vip_level;var=`3`'
  - 'data: type=set;key=nickname;var=`$(input_nickname)`'
  - 'data: type=add;key=points;var=`10`'
  - 'data: type=take;key=points;var=`5`'

# 读取 - 在菜单文字中
text: '&7你的 VIP 等级: &6{data:vip_level}'
text: '&7你的昵称: &f{data:nickname}'
text: '&7你的积分: &e{data:points}'

# 读取 - 在条件判断中
condition: '{data:vip_level} >= 2'
```

**应用示例：记录玩家是否领取过礼包**

```yaml
Bottom:
  type: notice
  confirm:
    text: '&a[ 领取礼包 ]'
    actions:
      - condition: "{data:first_gift} == true"
        allow:
          - 'toast: type=error;msg=已经领过;icon=barrier'
          - 'return'
        deny:
          - 'data: type=set;key=first_gift;var=`true`'
          - 'item: type=give;mats=APPLE;amount=5'
          - 'toast: type=task;msg=领取成功;icon=apple'
```

---

### 全局数据 (Global Data)

所有玩家共享的键值对，常用于存储服务器级别的状态信息。

**写入（动作中）：**

```yaml
actions:
  - 'gdata: type=set;key=<键名>;var=<值>'
  - 'gdata: type=add;key=<键名>;var=<数字>'
  - 'gdata: type=take;key=<键名>;var=<数字>'
  - 'gdata: type=delete;key=<键名>'
```

`type` 与 `data:` 相同。简写格式 `set-gdata: <键名> <值>` 也可使用，适合只需要设置一个全局值的场景；需要 `add` / `take` / `delete` 时使用 `gdata:` 参数写法。

**读取（任意文本位置）：**

| 方式 | 格式 | 说明 |
|------|------|------|
| 内置变量 | `{gdata:键名}` | 直接在菜单文本中使用 |
| PAPI 变量 | `%kamenu_gdata_键名%` | 通过 PlaceholderAPI 使用（可跨插件）|

**示例：**

```yaml
# 写入
actions:
  - 'gdata: type=set;key=server_event;var=`active`'
  - 'gdata: type=set;key=event_winner;var=`%player_name%`'
  - 'gdata: type=add;key=event_join_count;var=`1`'

# 读取 - 在菜单文字中
text: '&7服务器活动状态: &a{gdata:server_event}'
text: '&7活动获胜者: &e{gdata:event_winner}'
text: '&7活动参与次数: &f{gdata:event_join_count}'

# 读取 - 在条件判断中
condition: '{gdata:server_event} == active'
```

**应用示例：全服活动报名计数**

```yaml
Bottom:
  type: notice
  confirm:
    text: '&b[ 报名活动 ]'
    actions:
      - 'gdata: type=add;key=event_join_count;var=`1`'
      - 'toast: type=task;msg=报名成功;icon=emerald'
      - 'reset'

Body:
  event_info:
    type: message
    text:
      - '&a当前活动状态: &f{gdata:server_event}'
      - '&e报名人数: &f{gdata:event_join_count}'
```

---

### 玩家私有列表 (Per-Player List)

以单个玩家 UUID 为作用域的私有字符串列表，不是当前服务器在线玩家列表。列表以 JSON 数组字符串保存到玩家数据键中，适合好友列表、传送点 ID 列表、收藏列表、任务记录等简单字符串集合。

**写入（动作中）：**

```yaml
actions:
  - 'list: type=set;key=friends;var=`Steve,Alex`;split=,'
  - 'list: type=add;key=friends;var=`Notch`'
  - 'list: type=remove;key=friends;var=`Alex`'
  - 'list: type=clear;key=friends'
```

**读取与判断：**

| 方式 | 格式 | 说明 |
|------|------|------|
| 内置变量 | `{list:friends}` | 返回当前玩家自己的 `friends` 列表 JSON，例如 `["Steve","Notch"]` |
| PAPI 变量 | `%kamenu_list_friends%` | 通过 PlaceholderAPI 读取列表 JSON |
| PAPI 数量 | `%kamenu_list_size_friends%` | 读取列表项目数量 |
| 条件方法 | `inList.Steve;{list:friends}` | 判断值是否在列表中 |
| JavaScript | `JSON.parse(list("friends"))` | 在 JS 内读取并转成数组 |

**用于动态按钮：**

```yaml
Bottom:
  type: multi
  buttons:
    friends:
      type: repeat
      source: "{list:friends}"
      item:
        text: "&a{item.value}"
        actions:
          - "tell: 你点击了 {item.value}"
```

**应用示例：玩家私有收藏服务器列表**

```yaml
Bottom:
  type: multi
  buttons:
    add_survival:
      text: '&a[ 收藏生存服 ]'
      actions:
        - 'list: type=add;key=favorite_servers;var=`survival`'
        - 'toast: type=task;msg=已收藏;icon=emerald'
        - 'reset'

    favorites:
      type: repeat
      source: "{list:favorite_servers}"
      item:
        text: "&b{item.value}"
        actions:
          - "server: {item.value}"
```

---

### 全局列表 (Global List)

所有玩家共享的字符串列表。用法与 `list` 相同，但通过 `glist:` 动作写入，使用 `{glist:key}` / `%kamenu_glist_key%` / `glist("key")` 读取。

**示例：**

```yaml
actions:
  - 'glist: type=set;key=servers;var=`survival,skyblock,resource`;split=,'
  - 'glist: type=add;key=vip_players;var=`%player_name%`'
```

```yaml
condition: "inGlist.%player_name%;{glist:vip_players}"
text: "&7服务器数量: %kamenu_glist_size_servers%"
```

**应用示例：全局 VIP 名单判断**

```yaml
Events:
  Open:
    - condition: "inGlist.%player_name%;{glist:vip_players}"
      allow:
        - 'toast: type=task;msg=欢迎VIP;icon=diamond'
      deny:
        - 'toast: type=error;msg=非VIP;icon=barrier'
```

**注意：**

- `add` 默认 `unique=true`，已存在的项目不会重复添加；需要重复记录时设置 `unique=false`
- `set` / `add` / `remove` 的 `var` 支持单个字符串、JSON 数组字符串，或配合 `split` / `separator` 拆分简单列表
- `remove` / `take` 会移除所有完全匹配的项目
- `list/glist` 仍是持久化数据库数据，高频刷新菜单时应避免每次渲染都写入

---

## 完整使用示例

### 下面是一个使用数据存储系统制作"每日签到"菜单的示例：

原理： 使用`last_sign`数据存储玩家点击签到的日期，对该值与当前日期进行判断。若相同就说明是今天签到了，若不同则说明今日未签到。

```yaml
Title: '&6每日签到'

Settings:
  need_placeholder:
    - 'server'

Body:
  reward_item:
    type: 'item'
    material: 'CHEST'
    name: '&6今日签到奖励'
  reward_text:
    type: 'message'
    text: |
      &6每日签到可获得奖励：
      &e100 金币
      &e1 颗钻石
  info:
    type: 'message'
    text:
      - condition: "{data:last_sign} == %server_time_YYYYMMdd%"
        allow: '&c今日已签到，明天再来吧！'
        deny: '&a今日尚未签到，点击下方按钮领取奖励。'

Bottom:
  type: 'notice'
  confirm:
    text:
      - condition: "{data:last_sign} == %server_time_YYYYMMdd%"
        allow: '&8[ 已签到 ]'
        deny: '&a[ 立即签到 ]'
    actions:
      - condition: "{data:last_sign} == %server_time_YYYYMMdd%"
        allow:
          - 'actionbar: &c今日已签到！请明天再来。'
          - 'sound: block.note_block.bass'
        deny:
          - 'data: type=set;key=last_sign;var=`%server_time_YYYYMMdd%`'
          - 'console: eco give %player_name% 100'
          - 'console: give %player_name% diamond 1'
          - 'tell: &a签到成功！获得 100 金币和 1 颗钻石。'
          - 'title: title=&6签到成功;subtitle=&f奖励已发放'
          - 'sound: entity.player.levelup'
```

### 下面是一个使用数据存储系统制作"每日限制购买100个钻石"菜单的示例：

原理： 
- 使用`last_day`数据存储玩家上次进入菜单的日期，对该值与当前日期进行判断。若不同则说明日期已变更，需要重置购买数量。
- 使用`diamond_amount`数据存储玩家剩余可购买数量。



```yaml
Title: '&6钻石商店'

Settings:
  need_placeholder:
    - 'server'
    - 'math'

Events:
  Open:
    - condition: '{data:last_day} != %server_time_YYYYMMdd%'
      allow:
        - 'data: type=set;key=last_day;var=`%server_time_YYYYMMdd%`'
        - 'data: type=set;key=diamond_amount;var=`100`'
        - 'tell: &a欢迎进入钻石商店，今日钻石数量已补货。'
        - 'wait: 1'

Body:
  item:
    type: 'item'
    material: 'DIAMOND'
    name: '&6钻石'
  text-info:
    type: 'message'
    text: |
      &6请选择购买钻石的数量：
      &e50 金币 / 个
      &e每天最多只能购买 100 个
  remaining_amount:
    type: 'message'
    text: '&9剩余可购买数量： {data:diamond_amount}'

Inputs:
  amount:
    type:
      - condition: '{data:diamond_amount} > 0'
        allow: 'slider'
        deny: 'none'
    text: '&b购买数量：'
    min: 0
    max: '{data:diamond_amount}'
    default: 1
    format: '%s %s '

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 立即购买 ]'
    actions:
      - condition: '{data:diamond_amount} > 0'
        allow:
          - condition: '!isPosInt.$(amount)'
            allow:
              - 'tell: &c请输入有效的数字。'
              - 'return'
          - condition: 'hasMoney.%math_2_50*$(amount)%'
            allow:
              - 'money: type=take;num=%math_2_50*$(amount)%' # 扣除金币
              - 'item: type=give;mats=DIAMOND;amount=$(amount)' # 给予钻石
              - 'data: type=take;key=diamond_amount;var=$(amount)' # 扣除数据库中的剩余数量
              - 'tell: &a购买成功，消耗了 %math_2_50*$(amount)% 金币 购买了 钻石 x$(amount)'
            deny:
              - 'tell: &c金币不足！需要 金币 x%math_2_50*$(amount)%'
        deny:
          - 'actionbar: &c今日钻石已售罄！请明天再来。'
  deny:
    text: '&c[ 取消 ]'
    actions:
      - 'tell: &7已取消购买。'
      - 'sound: block.note_block.bass'
```

---

## 数据库配置

数据存储的后端数据库可在 `config.yml` 中配置，支持 SQLite 和 MySQL 两种方式。

详细配置请参阅 [⛳ 配置文件: config.yml](../config/config.md)。

---

## 数据表结构（参考）

KaMenu 在数据库中创建以下数据表：

**player_data 表（玩家数据）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INTEGER | 自增主键 |
| `player_uuid` | VARCHAR(36) | 玩家 UUID |
| `data_key` | VARCHAR(64) | 数据键名 |
| `data_value` | TEXT | 数据值 |
| `update_time` | BIGINT | 最后更新时间戳 |

{% hint style="info" %}
`list` 使用 `player_data` 表保存 JSON 数组字符串，`glist` 使用 `global_data` 表保存 JSON 数组字符串，不会额外创建独立列表表。
{% endhint %}

**global_data 表（全局数据）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INTEGER | 自增主键 |
| `data_key` | VARCHAR(64) | 数据键名（唯一）|
| `data_value` | TEXT | 数据值 |
| `update_time` | BIGINT | 最后更新时间戳 |
