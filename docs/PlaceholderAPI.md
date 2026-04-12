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
| **玩家元数据** | `%kamenu_meta_<键名>%` | 内存 | ❌ 否 | 临时缓存的玩家数据 |
| **背包物品** | `%kamenu_hasitem_[物品属性]%` | 玩家背包 | - | 查询玩家背包中符合条件的物品数量 |
| **存储库物品** | `%kamenu_hasstockitem_<物品名>%` | 玩家背包 | - | 查询玩家背包中指定存储库物品的数量 |

**快速导航：**
- 📦 [玩家数据变量](#玩家数据变量)
- 🌐 [全局数据变量](#全局数据变量)
- 💾 [玩家元数据变量](#玩家元数据变量)
- 🎒 [背包物品变量](#背包物品变量)
- 📦 [存储库物品变量](#存储库物品变量)

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

### 玩家元数据变量

读取特定玩家的临时内存缓存数据（由 `set-meta` 动作写入）。

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

### 背包物品变量

查询玩家背包中符合条件的普通物品数量（材质、描述、模型等）。

**格式：** `%kamenu_hasitem_[mats=材质;lore=描述;model=模型]%`

**参数说明：**

| 参数 | 必需 | 说明 | 示例 |
|------|------|------|------|
| `mats` | ✅ 是 | 物品材质类型 | `DIAMOND`, `GOLD_INGOT`, `IRON_INGOT` |
| `lore` | ❌ 否 | 物品描述（支持模糊匹配） | `神器`, `锻造材料` |
| `model` | ❌ 否 | 物品模型（namespace:key格式） | `minecraft:custom_item` |


**示例：**

| 变量 | 说明 |
|------|------|
| `%kamenu_hasitem_[mats=DIAMOND]%` | 返回背包中钻石的数量 |
| `%kamenu_hasitem_[mats=GOLD_INGOT]%` | 返回背包中金锭的数量 |
| `%kamenu_hasitem_[mats=DIAMOND;lore=神器]%` | 返回背包中带有"神器"描述的钻石数量 |
| `%kamenu_hasitem_[mats=IRON_INGOT;lore=锻造材料;model=custom:iron]%` | 返回符合材质、描述和模型的铁锭数量 |

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

{% hint style="info" %}
**如何存储物品？**

存储库物品需要通过指令保存到数据库后才能使用。

1. 手持要保存的物品
2. 执行 `/km item save <物品名称>` 指令
3. 详细说明请查看：[📝 指令列表 - /km item](../perm/commands.md#km-item)
{% endhint %}

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
   - 使用 `set-data` 和 `set-gdata` 动作写入的数据会持久化保存
   - 使用 `set-meta` 动作写入的数据仅存储在内存中，不持久化
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
   - `hasitem` 变量支持多条件匹配（材质、lore、model）
   - `hasstockitem` 变量使用存储库物品进行精确匹配
   - 物品不存在时返回 `0`
   - 物品数量是实时计算的，包括所有匹配物品的总和
   - 支持在条件判断和文本显示中使用
9. **参数格式**：
   - 物品变量使用方括号 `[]` 包裹参数，但方括号是可选的
   - 参数之间使用分号 `;` 分隔
   - Lore 匹配支持模糊匹配（不区分大小写）
   - 模型格式为 `namespace:key`
