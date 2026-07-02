# 📊 Data Storage

KaMenu has a built-in data storage system, allowing persistent read/write of player data, global data, player lists, and global lists without additional plugins.

---

## Data Types

### Player Data (Player Data)

Key-value pairs scoped by player UUID, with each player's data independent.

**Writing (in actions):**

```yaml
actions:
  - 'data: type=set;key=<key>;var=<value>'
  - 'data: type=add;key=<key>;var=<number>'
  - 'data: type=take;key=<key>;var=<number>'
  - 'data: type=delete;key=<key>'
```

Supported `type` values:

- `set`: set a text or numeric value
- `add`: add a numeric value
- `take`: subtract a numeric value
- `delete`: delete the key

The short form `set-data: <key> <value>` is also supported and is convenient when you only need to set one value. Use the parameter form `data:` when you need `add` / `take` / `delete`.

**Reading (anywhere in text):**

| Method | Format | Description |
|--------|--------|-------------|
| Internal variable | `{data:key}` | Use directly in menu text |
| PAPI variable | `%kamenu_data_key%` | Use via PlaceholderAPI (cross-plugin) |

**Example:**

```yaml
# Write
actions:
  - 'data: type=set;key=vip_level;var=`3`'
  - 'data: type=set;key=nickname;var=`$(input_nickname)`'
  - 'data: type=add;key=points;var=`10`'
  - 'data: type=take;key=points;var=`5`'

# Read - In menu text
text: '&7Your VIP level: &6{data:vip_level}'
text: '&7Your nickname: &f{data:nickname}'
text: '&7Your points: &e{data:points}'

# Read - In conditions
condition: '{data:vip_level} >= 2'
```

**Application example: track whether a player has claimed a gift**

```yaml
Bottom:
  type: notice
  confirm:
    text: '&a[ Claim Gift ]'
    actions:
      - condition: "{data:first_gift} == true"
        allow:
          - 'toast: type=error;msg=Claimed;icon=barrier'
          - 'return'
        deny:
          - 'data: type=set;key=first_gift;var=`true`'
          - 'item: type=give;mats=APPLE;amount=5'
          - 'toast: type=task;msg=Claimed;icon=apple'
```

---

### Global Data (Global Data)

Key-value pairs shared by all players, commonly used for storing server-level status information.

**Writing (in actions):**

```yaml
actions:
  - 'gdata: type=set;key=<key>;var=<value>'
  - 'gdata: type=add;key=<key>;var=<number>'
  - 'gdata: type=take;key=<key>;var=<number>'
  - 'gdata: type=delete;key=<key>'
```

The supported `type` values are the same as `data:`. The short form `set-gdata: <key> <value>` is also supported and is convenient when you only need to set one global value. Use the parameter form `gdata:` when you need `add` / `take` / `delete`.

**Reading (anywhere in text):**

| Method | Format | Description |
|--------|--------|-------------|
| Internal variable | `{gdata:key}` | Use directly in menu text |
| PAPI variable | `%kamenu_gdata_key%` | Use via PlaceholderAPI (cross-plugin) |

**Example:**

```yaml
# Write
actions:
  - 'gdata: type=set;key=server_event;var=`active`'
  - 'gdata: type=set;key=event_winner;var=`%player_name%`'
  - 'gdata: type=add;key=event_join_count;var=`1`'

# Read - In menu text
text: '&7Server event status: &a{gdata:server_event}'
text: '&7Event winner: &e{gdata:event_winner}'
text: '&7Event joins: &f{gdata:event_join_count}'

# Read - In conditions
condition: '{gdata:server_event} == active'
```

**Application example: global event registration counter**

```yaml
Bottom:
  type: notice
  confirm:
    text: '&b[ Join Event ]'
    actions:
      - 'gdata: type=add;key=event_join_count;var=`1`'
      - 'toast: type=task;msg=Joined;icon=emerald'
      - 'reset'

Body:
  event_info:
    type: message
    text:
      - '&aEvent status: &f{gdata:server_event}'
      - '&eJoined players: &f{gdata:event_join_count}'
```

---

### Per-Player List

A private string list scoped by one player's UUID, not the current server online-player list. Lists are stored as JSON array strings under player data keys. They are useful for friend lists, warp ID lists, favorites, task records, and other simple string collections.

**Writing (in actions):**

```yaml
actions:
  - 'list: type=set;key=friends;var=`Steve,Alex`;split=,'
  - 'list: type=add;key=friends;var=`Notch`'
  - 'list: type=remove;key=friends;var=`Alex`'
  - 'list: type=clear;key=friends'
```

**Reading and checks:**

| Method | Format | Description |
|--------|--------|-------------|
| Internal variable | `{list:friends}` | Returns the current player's own `friends` list JSON, such as `["Steve","Notch"]` |
| PAPI variable | `%kamenu_list_friends%` | Read list JSON through PlaceholderAPI |
| PAPI size | `%kamenu_list_size_friends%` | Read the number of list items |
| Condition method | `inList.Steve;{list:friends}` | Check whether a value is in the list |
| JavaScript | `JSON.parse(list("friends"))` | Read and parse the list in JS |

**Using with dynamic buttons:**

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
          - "tell: You clicked {item.value}"
```

**Application example: per-player favorite server list**

```yaml
Bottom:
  type: multi
  buttons:
    add_survival:
      text: '&a[ Favorite Survival ]'
      actions:
        - 'list: type=add;key=favorite_servers;var=`survival`'
        - 'toast: type=task;msg=Favorited;icon=emerald'
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

### Global List

A server-wide shared string list. It works like `list`, but writes through `glist:` actions and reads through `{glist:key}` / `%kamenu_glist_key%` / `glist("key")`.

**Examples:**

```yaml
actions:
  - 'glist: type=set;key=servers;var=`survival,skyblock,resource`;split=,'
  - 'glist: type=add;key=vip_players;var=`%player_name%`'
```

```yaml
condition: "inGlist.%player_name%;{glist:vip_players}"
text: "&7Server count: %kamenu_glist_size_servers%"
```

**Application example: global VIP list check**

```yaml
Events:
  Open:
    - condition: "inGlist.%player_name%;{glist:vip_players}"
      allow:
        - 'toast: type=task;msg=VIP;icon=diamond'
      deny:
        - 'toast: type=error;msg=Not VIP;icon=barrier'
```

**Notes:**

- `add` defaults to `unique=true`, so existing items are skipped. Set `unique=false` when duplicate records are required.
- `var` in `set` / `add` / `remove` supports a single string, a JSON array string, or a simple list split with `split` / `separator`.
- `remove` / `take` removes all exact matches.
- `list/glist` is still persistent database data, so avoid writing it on every render in high-frequency refresh menus.

---

## Complete Usage Examples

### Below is an example of creating a "Daily Sign-in" menu using the data storage system:

Principle: Use `last_sign` data to store the date when the player clicked to sign in. Compare this value with the current date. If they are the same, it means the player has already signed in today; if different, it means they haven't signed in today.

```yaml
Title: '&6Daily Sign-in'

Settings:
  need_placeholder:
    - 'server'

Body:
  reward_item:
    type: 'item'
    material: 'CHEST'
    name: '&6Today''s Sign-in Reward'
  reward_text:
    type: 'message'
    text: |
      &6Daily sign-in rewards:
      &e100 coins
      &e1 diamond
  info:
    type: 'message'
    text:
      - condition: "{data:last_sign} == %server_time_YYYYMMdd%"
        allow: '&cAlready signed in today, come back tomorrow!'
        deny: '&aNot signed in yet today, click the button below to claim rewards.'

Bottom:
  type: 'notice'
  confirm:
    text:
      - condition: "{data:last_sign} == %server_time_YYYYMMdd%"
        allow: '&8[ Already Signed In ]'
        deny: '&a[ Sign In Now ]'
    actions:
      - condition: "{data:last_sign} == %server_time_YYYYMMdd%"
        allow:
          - 'actionbar: &cAlready signed in today! Please come back tomorrow.'
          - 'sound: block.note_block.bass'
        deny:
          - 'data: type=set;key=last_sign;var=`%server_time_YYYYMMdd%`'
          - 'console: eco give %player_name% 100'
          - 'console: give %player_name% diamond 1'
          - 'tell: &aSign-in successful! Received 100 coins and 1 diamond.'
          - 'title: title=&6Sign-in successful;subtitle=&fRewards have been sent'
          - 'sound: entity.player.levelup'
```

### Below is an example of creating a "Daily Limit Purchase 100 Diamonds" menu using the data storage system:

Principle:
- Use `last_day` data to store the date when the player last entered the menu. Compare this value with the current date. If different, it means the date has changed, and the purchase quantity needs to be reset.
- Use `diamond_amount` data to store the player's remaining purchasable quantity.

```yaml
Title: '&6Diamond Shop'

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
        - 'tell: &aWelcome to Diamond Shop, today''s diamonds have been restocked.'
        - 'wait: 1'

Body:
  item:
    type: 'item'
    material: 'DIAMOND'
    name: '&6Diamond'
  text-info:
    type: 'message'
    text: |
      &6Please select diamond purchase quantity:
      &e50 coins / each
      &eMaximum 100 per day
  remaining_amount:
    type: 'message'
    text: '&9Remaining purchasable quantity: {data:diamond_amount}'

Inputs:
  amount:
    type:
      - condition: '{data:diamond_amount} > 0'
        allow: 'slider'
        deny: 'none'
    text: '&bPurchase quantity:'
    min: 0
    max: '{data:diamond_amount}'
    default: 1
    format: '%s %s '

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ Purchase Now ]'
    actions:
      - condition: '{data:diamond_amount} > 0'
        allow:
          - condition: '!isPosInt.$(amount)'
            allow:
              - 'tell: &cPlease enter a valid number.'
              - 'return'
          - condition: 'hasMoney.%math_2_50*$(amount)%'
            allow:
              - 'money: type=take;num=%math_2_50*$(amount)%' # Deduct coins
              - 'item: type=give;mats=DIAMOND;amount=$(amount)' # Give diamonds
              - 'data: type=take;key=diamond_amount;var=$(amount)' # Deduct remaining quantity in database
              - 'tell: &aPurchase successful, spent %math_2_50*$(amount)% coins to purchase diamonds x$(amount)'
            deny:
              - 'tell: &cInsufficient coins! Need %math_2_50*$(amount)% coins'
        deny:
          - 'actionbar: &cToday''s diamonds are sold out! Please come back tomorrow.'
  deny:
    text: '&c[ Cancel ]'
    actions:
      - 'tell: &7Purchase cancelled.'
      - 'sound: block.note_block.bass'
```

---

## Database Configuration

The backend database for data storage can be configured in `config.yml`, supporting both SQLite and MySQL.

For detailed configuration, see [⛳ Configuration File: config.yml](../config/config.md).

---

## Database Table Structure (Reference)

KaMenu creates the following database tables:

**player_data table (Player Data):**

| Field | Type | Description |
|-------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `player_uuid` | VARCHAR(36) | Player UUID |
| `data_key` | VARCHAR(64) | Data key |
| `data_value` | TEXT | Data value |
| `update_time` | BIGINT | Last update timestamp |

{% hint style="info" %}
`list` stores JSON array strings in the `player_data` table, and `glist` stores JSON array strings in the `global_data` table. No separate list table is created.
{% endhint %}

**global_data table (Global Data):**

| Field | Type | Description |
|-------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `data_key` | VARCHAR(64) | Data key (unique) |
| `data_value` | TEXT | Data value |
| `update_time` | BIGINT | Last update timestamp |
