# 📊 Player Data & Global Data

KaMenu has a built-in key-value storage system, allowing persistent read/write of data in menus without additional plugins.

---

## Data Types

### Player Data (Player Data)

Key-value pairs scoped by player UUID, with each player's data independent.

**Writing (in actions):**

```yaml
actions:
  - 'set-data: <key> <value>'
```

**Reading (anywhere in text):**

| Method | Format | Description |
|--------|--------|-------------|
| Internal variable | `{data:key}` | Use directly in menu text |
| PAPI variable | `%kamenu_data_key%` | Use via PlaceholderAPI (cross-plugin) |

**Example:**

```yaml
# Write
actions:
  - 'set-data: vip_level 3'
  - 'set-data: nickname $(input_nickname)'

# Read - In menu text
text: '&7Your VIP level: &6{data:vip_level}'
text: '&7Your nickname: &f{data:nickname}'

# Read - In conditions
condition: '{data:vip_level} >= 2'
```

---

### Global Data (Global Data)

Key-value pairs shared by all players, commonly used for storing server-level status information.

**Writing (in actions):**

```yaml
actions:
  - 'set-gdata: <key> <value>'
```

**Reading (anywhere in text):**

| Method | Format | Description |
|--------|--------|-------------|
| Internal variable | `{gdata:key}` | Use directly in menu text |
| PAPI variable | `%kamenu_gdata_key%` | Use via PlaceholderAPI (cross-plugin) |

**Example:**

```yaml
# Write
actions:
  - 'set-gdata: server_event active'
  - 'set-gdata: event_winner %player_name%'

# Read - In menu text
text: '&7Server event status: &a{gdata:server_event}'
text: '&7Event winner: &e{gdata:event_winner}'

# Read - In conditions
condition: '{gdata:server_event} == active'
```

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
          - 'set-data: last_sign %server_time_YYYYMMdd%'
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
        - 'set-data: last_day %server_time_YYYYMMdd%'
        - 'set-data: diamond_amount 100'
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

KaMenu creates the following two tables in the database:

**player_data table (Player Data):**

| Field | Type | Description |
|-------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `player_uuid` | VARCHAR(36) | Player UUID |
| `data_key` | VARCHAR(64) | Data key |
| `data_value` | TEXT | Data value |
| `update_time` | BIGINT | Last update timestamp |

**global_data table (Global Data):**

| Field | Type | Description |
|-------|------|-------------|
| `id` | INTEGER | Auto-increment primary key |
| `data_key` | VARCHAR(64) | Data key (unique) |
| `data_value` | TEXT | Data value |
| `update_time` | BIGINT | Last update timestamp |
