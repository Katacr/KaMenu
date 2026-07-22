# 🔖 PlaceholderAPI

KaMenu 内置了 PlaceholderAPI (PAPI) 扩展，将插件内的数据暴露为可在其他插件（如记分板、聊天插件）中使用的占位符变量。

---

## 前提条件

需要安装 [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) 插件，KaMenu 启动时会自动注册扩展，无需额外操作。

---

## 📊 变量类型一览表

| 变量类型 | 前缀 | 数据来源 | 持久化 | 说明 |
|---------|-------|---------|---------|------|
| **玩家数据** | `%kamenu_data_<键名>%` | 数据库 | ✅ 是 | 持久化存储的玩家个人数据 |
| **全局数据** | `%kamenu_gdata_<键名>%` | 数据库 | ✅ 是 | 服务器级别的全局共享数据 |
| **玩家列表** | `%kamenu_list_<键名>%` | 数据库 | ✅ 是 | 当前玩家的持久化字符串列表，返回 JSON 数组 |
| **全局列表** | `%kamenu_glist_<键名>%` | 数据库 | ✅ 是 | 全局共享字符串列表，返回 JSON 数组 |
| **玩家列表长度** | `%kamenu_list_size_<键名>%` | 数据库 | ✅ 是 | 当前玩家列表项目数量 |
| **全局列表长度** | `%kamenu_glist_size_<键名>%` | 数据库 | ✅ 是 | 全局列表项目数量 |
| **在线玩家列表** | `%kamenu_online_players%` | 在线玩家 | ❌ 否 | 当前在线玩家名称列表，返回 JSON 数组 |
| **玩家元数据** | `%kamenu_meta_<键名>%` | 内存 | ❌ 否 | 临时缓存的玩家数据 |
| **物品属性** | `%kamenu_checkitem_[来源;属性]%` | 玩家背包 / 内存 | ❌ 否 | 读取主手、副手、槽位或保存物品的常用属性 |
| **背包物品** | `%kamenu_hasitem_[物品属性]%` | 玩家背包 | - | 查询玩家背包中符合条件的物品数量 |
| **存储库物品** | `%kamenu_hasstockitem_<物品名>%` | 玩家背包 | - | 查询玩家背包中指定存储库物品的数量 |

**快速导航：**
- 📦 [玩家数据变量](#玩家数据变量)
- 🌐 [全局数据变量](#全局数据变量)
- 📋 [列表变量](#列表变量)
- 👥 [在线玩家列表变量](#在线玩家列表变量)
- 💾 [玩家元数据变量](#玩家元数据变量)
- 🧰 [物品属性变量](#物品属性变量)
- 🎒 [背包物品变量](#背包物品变量)
- 📦 [存储库物品变量](#存储库物品变量)

---

## 提供的变量

### 玩家数据变量

读取特定玩家的个人持久化数据（可由 `set-data` 简写动作或 `data:` 参数动作写入）。

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

读取服务器级别的全局共享数据（可由 `set-gdata` 简写动作或 `gdata:` 参数动作写入）。

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

### 列表变量

读取 KaMenu 内置列表数据，返回 JSON 字符串数组。适合作为动态按钮 `repeat.source` 或 `inList` / `inGlist` 条件的列表参数。

**格式：**

- `%kamenu_list_<键名>%`：读取当前玩家列表
- `%kamenu_glist_<键名>%`：读取全局列表

**示例：**

| 变量 | 说明 |
|------|------|
| `%kamenu_list_friends%` | 当前玩家的 `friends` 列表 |
| `%kamenu_glist_servers%` | 全局 `servers` 列表 |
| `%kamenu_list_size_friends%` | 当前玩家 `friends` 列表的项目数量 |
| `%kamenu_glist_size_servers%` | 全局 `servers` 列表的项目数量 |

```yaml
Bottom:
  type: multi
  buttons:
    friends:
      type: repeat
      source: "%kamenu_list_friends%"
      item:
        text: "&a{item.value}"
```

**用于条件判断：**

```yaml
condition: "%kamenu_list_size_friends% > 0"
condition: "%kamenu_glist_size_servers% >= 3"
```

---

### 在线玩家列表变量

读取当前在线玩家名称列表，返回 JSON 字符串数组。

**格式：** `%kamenu_online_players%`

**示例返回值：**

```json
["Steve","Alex","Notch"]
```

**用于动态按钮：**

```yaml
Bottom:
  type: multi
  buttons:
    online_players:
      type: repeat
      source: "%kamenu_online_players%"
      item:
        text: "&a{item.value}"
```

**用于条件判断：**

```yaml
condition: "inGlist.$(target);%kamenu_online_players%"
```

---

### 玩家元数据变量

读取特定玩家的临时内存缓存数据（可由 `set-meta` 简写动作或 `meta:` 参数动作写入）。

**格式：** `%kamenu_meta_<键名>%`

**示例：**

| 变量 | 说明 |
|------|------|
| `%kamenu_meta_time%` | 读取玩家的 `time` 元数据 |
| `%kamenu_meta_last_menu%` | 读取玩家的 `last_menu` 元数据 |
| `%kamenu_meta_temp_data%` | 读取玩家的 `temp_data` 元数据 |

**在其他插件中使用：**

```yaml
# 记分板配置示例（显示临时数据）
scoreboard:
  - '&6上次访问: &f%kamenu_meta_time%'
  - '&7临时状态: &f%kamenu_meta_temp_status%'
```

**注意：**
- 元数据仅存储在内存中，不持久化到数据库
- 玩家退出时自动清理该玩家的元数据
- 插件重载或关服时清理全部元数据
- 数据不存在时返回 `"null"`

---

### 物品属性变量

KaMenu 可以读取物品的常用属性。菜单内部优先使用不依赖 PlaceholderAPI 的大括号变量；需要在其他插件中读取时使用 PAPI：

```text
{checkitem:[hand;name]}
%kamenu_checkitem_[hand;name]%
```

两种写法的来源、属性和返回值完全相同。

**参数结构：**

```text
{checkitem:[<物品位置>;<输出属性>;<可选格式>]}
%kamenu_checkitem_[<物品位置>;<输出属性>;<可选格式>]%
```

`checkitem` 最多支持三个参数，参数之间使用英文分号 `;` 分隔：

| 顺序 | 参数 | 必需 | 作用 |
|------|------|------|------|
| 第一个 | 物品位置/来源 | ✅ | 定位需要读取的物品，例如 `hand`、`offhand`、`slot:0`、`stock:神奇之剑` |
| 第二个 | 输出属性 | ✅ | 指定需要返回的具体属性，例如 `name`、`lore:1`、`ench:sharpness`、`dura_pct` |
| 第三个 | 文本格式 | ❌ | 使用 `fmt:plain`、`fmt:legacy` 或 `fmt:mini`；未填写时默认 `fmt:plain` |

例如 `{checkitem:[slot:0;lore:1;fmt:mini]}` 可以拆解为：

1. `slot:0`：定位玩家背包的 `0` 号槽位。
2. `lore:1`：输出该物品第 `1` 行 Lore；Lore 行号从 `1` 开始。
3. `fmt:mini`：将该行文本转换为 MiniMessage 格式。

分号 `;` 用于分隔三个顶层参数，冒号 `:` 用于参数内部取值。因此 `slot:0`、`lore:1` 和 `fmt:mini` 都是一个完整参数。背包槽位从 `0` 开始，Lore 行号从 `1` 开始。

**物品来源：**

| 来源 | 说明 |
|------|------|
| `hand` | 当前玩家主手物品 |
| `offhand` | 当前玩家副手物品 |
| `slot:<索引>` | 玩家背包指定 Bukkit 槽位 |
| `stock:<名称>` | KaMenu 保存物品库；直接读取内存缓存，不查询 SQL |
| `itemsadder:<namespace:id>` / `ia:<namespace:id>` | ItemsAdder 物品模板 |
| `oraxen:<id>` | Oraxen 物品模板 |
| `craftengine:<namespace:id>` / `ce:<namespace:id>` | CraftEngine 物品模板 |

**可读属性：**

| 属性 | 返回值 |
|------|--------|
| `type` | 完整材质 ID，例如 `minecraft:diamond_sword` |
| `custom_id` / `external_id` / `item_id` | 带 KaMenu 提供方前缀的规范外部物品 ID；原版物品返回空字符串 |
| `plugin_id` / `native_id` | 插件自身的物品 ID，不含 KaMenu 提供方前缀 |
| `plugin` / `provider` | `ItemsAdder`、`Oraxen` 或 `CraftEngine`；原版物品返回空字符串 |
| `amt` | 堆叠数量 |
| `name` | 物品有效显示名称 |
| `lore` | Lore JSON 字符串数组 |
| `lore:<行号>` | 指定 Lore 行，从 `1` 开始；`lore:1` 表示第一行 |
| `enchants` | `[ {"key":"minecraft:sharpness","level":5} ]` 形式的 JSON 数组 |
| `ench:<附魔ID>` | 指定附魔等级；不存在返回 `0` |
| `model` / `item_model` | 物品模型 NamespacedKey |
| `cmd` / `custom_model_data` / `custom_model_id` | 自定义模型 ID，默认按整数输出；不存在返回空字符串 |
| `dmg` | 已损耗耐久 |
| `dura` | 剩余耐久 |
| `dura_pct` | 剩余耐久百分比 `0` 至 `100`，不带 `%` |

名称和 Lore 默认返回纯文本。格式选项使用 `fmt:<格式>`，并作为第三个分号参数填写：

| 格式选项 | 返回内容 |
|----------|----------|
| `fmt:plain` | 纯文本，默认值；移除颜色和事件 |
| `fmt:legacy` | 保留为 `&` Legacy 颜色格式 |
| `fmt:mini` | 转换为 MiniMessage 格式 |

`fmt` 仅影响 `name`、`lore` 和 `lore:<行号>`；材质、模型、附魔和数字属性不受影响。

以下每一行都是独立用法示例，请放入对应的菜单字段中；不要在同一个 YAML 节点内重复配置多个 `text` 键：

```yaml
# 输出玩家主手物品的有效显示名称；未指定 fmt，因此返回纯文本
text: '&f主手：{checkitem:[hand;name]}'

# 输出玩家主手物品的剩余耐久百分比；变量本身不包含百分号
text: '&7耐久：{checkitem:[hand;dura_pct]}%'

# 判断玩家主手物品的锋利附魔是否达到 5 级
condition: '{checkitem:[hand;ench:sharpness]} >= 5'

# 读取保存物品“神奇之剑”的全部附魔 JSON，可直接作为 repeat 数据源
source: '{checkitem:[stock:神奇之剑;enchants]}'

# 读取保存物品“神奇之剑”的第 1 行 Lore，并转换为 MiniMessage 格式
text: '{checkitem:[stock:神奇之剑;lore:1;fmt:mini]}'

# 同时输出主手物品的 ItemModel NamespacedKey 和整数 CustomModelData
text: '模型：{checkitem:[hand;item_model]} / ID：{checkitem:[hand;custom_model_id]}'

# 读取 ItemsAdder 模板名称，并检查玩家主手中的外部物品身份
text: '{checkitem:[itemsadder:my_pack:magic_sword;name;fmt:mini]}'
condition: '{checkitem:[hand;custom_id]} == itemsadder:my_pack:magic_sword'
```

原版附魔可省略 `minecraft:`，例如 `ench:sharpness`；自定义附魔必须填写完整命名空间，例如 `ench:myplugin:lifesteal`。

保存物品名包含分号时，必须使用反引号包裹物品名，避免单引号或双引号与 YAML 字符串边界冲突：

```yaml
text: '{checkitem:[stock:`活动;长剑`;name]}'
```

外层 YAML 仍可使用单引号或双引号；内部物品名只使用反引号。单引号和双引号不会被 `checkitem` 当作参数包裹符。

物品不存在时，字符串返回空字符串、数字返回 `0`、列表返回 `[]`。玩家背包和外部插件物品模板只能在 Paper 主线程或 Folia 当前玩家区域线程读取；异步第三方 PAPI 请求不会跨线程阻塞，此时返回空值。`stock:` 来源不受此限制。

---

### 背包物品变量

查询玩家背包中符合条件的原版或外部插件物品数量（物品 ID、描述、模型等）。

**格式：** `%kamenu_hasitem_[mats=材质;lore=描述;model=物品模型;custom_model_id=整数ID]%`

`mats` 支持原版材质，以及 `itemsadder:`/`ia:`、`oraxen:`、`craftengine:`/`ce:` 外部物品前缀。外部物品按插件物品 ID 精确匹配。

**参数说明：**

| 参数 | 必需 | 说明 | 示例 |
|------|------|------|------|
| `mats` | ✅ 是 | 物品材质类型 | `DIAMOND`, `GOLD_INGOT`, `IRON_INGOT` |
| `lore` | ❌ 否 | 物品描述（支持模糊匹配） | `神器`, `锻造材料` |
| `model` / `item_model` | ❌ 否 | ItemModel（namespace:key 格式） | `minecraft:custom_item` |
| `cmd` / `custom_model_data` / `custom_model_id` | ❌ 否 | 整数 CustomModelData | `10001` |


**示例：**

| 变量 | 说明 |
|------|------|
| `%kamenu_hasitem_[mats=DIAMOND]%` | 返回背包中钻石的数量 |
| `%kamenu_hasitem_[mats=GOLD_INGOT]%` | 返回背包中金锭的数量 |
| `%kamenu_hasitem_[mats=DIAMOND;lore=神器]%` | 返回背包中带有"神器"描述的钻石数量 |
| `%kamenu_hasitem_[mats=IRON_INGOT;lore=锻造材料;model=custom:iron]%` | 返回符合材质、描述和模型的铁锭数量 |
| `%kamenu_hasitem_[mats=PAPER;custom_model_id=10001]%` | 返回自定义模型 ID 为 `10001` 的纸张数量 |

**在菜单中使用：**

```yaml
# 显示物品数量
Body:
  diamond_info:
    type: 'message'
    text: '&a背包中钻石数量: &f%kamenu_hasitem_[mats=DIAMOND]%'

# 条件判断
Bottom:
  type: 'multi'
  buttons:
    check_diamond:
      text: '&a使用钻石'
      actions:
        - condition: '%kamenu_hasitem_[mats=DIAMOND]% >= 10'
          allow:
            - 'tell: &a你有足够的钻石！'
          deny:
            - 'tell: &c钻石不足，当前只有 %kamenu_hasitem_[mats=DIAMOND]% 个'
```

---

### 存储库物品变量

查询玩家背包中指定存储库物品的数量。

**格式：** `%kamenu_hasstockitem_<物品名>%`

**参数说明：**

| 参数 | 必需 | 说明 |
|------|------|------|
| `物品名` | ✅ 是 | 存储库中保存的物品名称 |

 

{% hint style="info" %}
**如何存储物品？**

存储库物品需要通过指令保存到数据库后才能使用。

1. 手持要保存的物品
2. 执行 `/km item save <物品名称>` 指令
3. 详细说明请查看：[📝 指令列表 - /km item](perm/commands.md#km-item)
{% endhint %}

**示例：**

| 变量 | 说明 |
|------|------|
| `%kamenu_hasstockitem_神秘果%` | 返回背包中"神秘果"的数量 |
| `%kamenu_hasstockitem_传送卷轴%` | 返回背包中"传送卷轴"的数量 |
| `%kamenu_hasstockitem_高级经验书%` | 返回背包中"高级经验书"的数量 |

**在菜单中使用：**

```yaml
# 显示存储库物品数量
Body:
  fruit_info:
    type: 'message'
    text: '&a神秘果数量: &f%kamenu_hasstockitem_神秘果%'

# 条件判断
Bottom:
  type: 'multi'
  buttons:
    use_fruit:
      text: '&a使用神秘果'
      actions:
        - condition: '%kamenu_hasstockitem_神秘果% >= 5'
          allow:
            - 'tell: &a你有足够的神秘果！'
          deny:
            - 'tell: &c神秘果不足，当前只有 %kamenu_hasstockitem_神秘果% 个'
```

**注意事项：**
- 物品名称必须与存储库中的文件名匹配
- 如果物品不存在，返回 `0`
- 物品匹配使用 `isSimilar` 方法，包括材质、NBT等
- 不区分物品堆叠数量，返回所有匹配物品的总和

---

## 在 KaMenu 菜单内使用 PAPI 变量

KaMenu 也完整支持**解析来自其他 PAPI 扩展的变量**，可以在菜单的任意文本字段和条件判断中使用：

### 菜单标题

```yaml
Title: '&a欢迎，&f%player_name%！'
```

### 组件文字

```yaml
Body:
  stats:
    type: 'message'
    text: '&7等级: &f%player_level% &7| 血量: &c%player_health%'
```

### 条件判断

```yaml
condition: "%player_level% >= 10 && %player_balance% >= 500"
```

### 动作中

```yaml
actions:
  - 'tell: &a你好，%player_name%！你现在在 %player_world%。'
  - 'console: give %player_name% diamond 1'
```

---

## 数据未找到的处理

当指定键名对应的数据不存在时：

1. **PAPI 变量（%kamenu_data_key%）**：
   - 返回语言文件中定义的提示（键名：`papi.data_not_found`）
   - 默认返回空字符串
   - 可在语言文件中自定义此提示

2. **内置变量（{data:key}）**：
   - 返回字面量字符串 `"null"`
   - 可以在条件判断中使用 `{data:key} == null` 来判断数据是否存在
   - 例如：`{data:counter} != null` 表示数据存在

3. **PAPI 元数据变量（%kamenu_meta_key%）**：
   - 返回字面量字符串 `"null"`
   - 元数据仅存储在内存中，玩家退出或插件重载后自动清空
   - 可以在条件判断中使用 `{meta:key} == null` 来判断数据是否存在

**示例：**

```yaml
# 在条件判断中使用内置变量判断数据是否存在
actions:
  - condition: "{data:counter} != null"
    allow:
      - 'tell: 数据存在，值为: {data:counter}'
    deny:
      - 'tell: 数据不存在'

# 在记分板中使用 PAPI 变量（数据不存在时显示自定义提示）
scoreboard:
  - '&6访问次数: &f%kamenu_data_visit_count%'
  # 如果 visit_count 不存在，显示默认提示或空字符串
```

---

## 变量使用场景示例

### 在记分板中显示玩家数据

```yaml
scoreboard:
  title: '&6玩家信息'
  lines:
    - '&a玩家: &f%player_name%'
    - '&b等级: &f%player_level%'
    - '&cVIP: &f%kamenu_data_vip_level%'
    - '&d昵称: &f%kamenu_data_nickname%'
    - '&e访问次数: &f%kamenu_data_visit_count%'
```

### 在公告栏中显示全局数据

```yaml
announcements:
  - '&6当前活动: &f%kamenu_gdata_server_event%'
  - '&a获胜者: &f%kamenu_gdata_event_winner%'
  - '&b服务器状态: &f%kamenu_gdata_server_status%'
```

### 在聊天格式中使用

```yaml
chat_format:
  format: '&7[%kamenu_data_vip_level%&7] &f%player_name%: &7%message%'
  # 如果玩家没有 VIP 等级数据，显示 [空] 或自定义提示
```

### 使用元数据存储临时状态

```yaml
# 在菜单中设置临时数据
actions:
  - 'set-meta: last_visit %player_time%'
  - 'set-meta: temp_status shopping'

# 在其他菜单中读取临时数据
Title: '欢迎回来，%player_name%'
Body:
  status:
    type: 'message'
    text: '&7上次访问: %kamenu_meta_last_visit% | 状态: %kamenu_meta_temp_status%'
```

### 在记分板中显示物品数量

```yaml
scoreboard:
  title: '&6背包信息'
  lines:
    - '&a玩家: &f%player_name%'
    - '&6钻石: &f%kamenu_hasitem_[mats=DIAMOND]%'
    - '&e金锭: &f%kamenu_hasitem_[mats=GOLD_INGOT]%'
    - '&7铁锭: &f%kamenu_hasitem_[mats=IRON_INGOT]%'
    - '&d神秘果: &f%kamenu_hasstockitem_神秘果%'
    - '&b传送卷轴: &f%kamenu_hasstockitem_传送卷轴%'
```

### 在菜单中使用物品条件判断

```yaml
# 检查背包中是否有足够的钻石
Bottom:
  type: 'multi'
  buttons:
    buy_item:
      text: '&a购买物品'
      actions:
        - condition: '%kamenu_hasitem_[mats=DIAMOND]% >= 10'
          allow:
            - 'tell: &a支付成功！'
            - 'command: give %player_name% diamond_sword 1'
            - 'console: clear %player_name% diamond 10'
          deny:
            - 'tell: &c钻石不足，需要10个钻石！'

    use_fruit:
      text: '&b使用神秘果'
      actions:
        - condition: '%kamenu_hasstockitem_神秘果% >= 1'
          allow:
            - 'tell: &a使用了神秘果！'
            - 'command: effect %player_name% regeneration 600 2'
            - 'data: take 神秘果 1'
          deny:
            - 'tell: &c你没有神秘果！'
```

---

## 注意事项

1. **PAPI 插件依赖**：需要安装 PlaceholderAPI 才能使用这些变量
2. **数据持久化**：
   - 使用 `set-data` / `data:` 和 `set-gdata` / `gdata:` 动作写入的数据会持久化保存
   - 使用 `set-meta` / `meta:` 动作写入的数据仅存储在内存中，不持久化
3. **键名区分**：玩家数据键、全局数据键和元数据键使用不同的前缀（`data_`、`gdata_` 和 `meta_`），存储位置和生命周期也不同
4. **类型限制**：所有数据都以字符串形式存储，使用时需要根据需要进行类型转换
5. **性能考虑**：频繁读取大量数据可能影响性能，建议合理使用
6. **数据存在性判断**：
   - PAPI 变量：数据不存在时返回提示或空字符串
   - 内置变量：数据不存在时返回 `"null"`，便于条件判断
   - 元数据：数据不存在时返回 `"null"`，玩家退出或插件重载后自动清空
7. **元数据生命周期**：
   - 元数据仅存储在内存中
   - 玩家退出时自动清理该玩家的元数据
   - 插件重载或关服时清理全部元数据
   - 适用于需要短时间存储临时数据的场景
8. **物品变量特性**：
   - `hasitem` 变量支持多条件匹配（材质、lore、ItemModel、整数 CustomModelData）
   - `hasstockitem` 变量使用存储库物品进行精确匹配
   - 物品不存在时返回 `0`
   - 物品数量是实时计算的，包括所有匹配物品的总和
   - 支持在条件判断和文本显示中使用
9. **参数格式**：
   - 物品变量使用方括号 `[]` 包裹参数，但方括号是可选的
   - 参数之间使用分号 `;` 分隔
   - Lore 匹配支持模糊匹配（不区分大小写）
   - 模型格式为 `namespace:key`
