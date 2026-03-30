# hasItem 和 hasStockItem 条件方法

## hasItem - 普通物品判断

判断玩家背包中是否有指定材质、数量（可选包含描述和模型）的物品。

### 格式

```
hasItem.[mats=材质;amount=数量;lore=描述;model=模型]
```

### 参数说明

| 参数 | 说明 | 必需 |
|------|------|------|
| `mats` | Minecraft 材质名称（支持多种格式）| ✅ |
| `amount` | 需要的数量 | ✅ |
| `lore` | 物品描述中需要包含的文本（忽略大小写）| ❌ |
| `model` | 物品的 item_model（格式：namespace:key）| ❌ |

### 判断规则

- 物品材质必须匹配
- 所有匹配物品的数量总和必须大于等于指定数量
- 如果指定了 `lore`，物品描述中必须包含该字符串（忽略大小写）
- 如果指定了 `model`，物品的 item_model 必须匹配（格式为 `namespace:key`）

### 物品材质名格式支持

KaMenu 支持多种材质名称格式，系统会自动规范化并匹配对应的 Material 枚举：

- 标准格式：`DIAMOND_SWORD`
- 小写：`diamond_sword`
- 混合大小写：`DiAMond swORd`
- 短杠：`Diamond-Sword`
- 空格：`diamond sword`

**示例：**

```yaml
# 以下格式均可匹配到 DIAMOND_SWORD
- condition: "hasItem.[mats=DIAMOND_SWORD;amount=1]"
- condition: "hasItem.[mats=diamond_sword;amount=1]"
- condition: "hasItem.[mats=Diamond-Sword;amount=1]"
- condition: "hasItem.[mats=diamond sword;amount=1]"
```

{% hint style="info" %}
系统会自动忽略大小写、将短杠和空格替换为下划线，并合并多余的下划线，因此所有上述格式都会正确匹配。
{% endhint %}

### 示例

**判断玩家是否有 10 个钻石：**

```yaml
- condition: "hasItem.[mats=DIAMOND;amount=10]"
  allow:
    - 'tell: &a你有足够的钻石！'
  deny:
    - 'tell: &c你需要 10 个钻石！'
```

**判断玩家是否有 1 个描述中包含"锻造材料"的钻石：**

```yaml
- condition: 'hasItem.[mats=DIAMOND;amount=1;lore=锻造材料]'
  allow:
    - 'tell: &a你有锻造材料钻石！'
  deny:
    - 'tell: &c你缺少锻造材料钻石！'
```

**判断玩家是否有 16 个自定义模型的物品（如 Oraxen 的物品）：**

```yaml
- condition: 'hasItem.[mats=PAPER;amount=16;model=oraxen:mana_crystal]'
  allow:
    - 'tell: &a你有足够的魔法水晶！'
  deny:
    - 'tell: &c你需要 16 个魔法水晶！'
```

**结合 item 动作使用：**

```yaml
actions:
  - condition: "hasItem.[mats=DIAMOND;amount=16]"
    allow:
      - 'item: type=take;mats=DIAMOND;amount=16'
      - 'tell: &a购买成功！已扣除 16 个钻石'
    deny:
      - 'tell: &c物品不足！需要 16 个钻石'
```

**反向判断（没有物品时满足条件）：**

```yaml
actions:
  - condition: "!hasItem.[mats=DIAMOND;amount=64]"
    allow:
      - 'tell: &a你可以继续采集钻石！'
    deny:
      - 'tell: &c你的背包已经满了！'
```

### 注意事项

- 材质名称**不区分大小写**，支持多种格式（见上方说明）
- `lore` 判断是包含关系，只要描述中包含指定字符串即可
- `model` 格式为 `namespace:key`，如：
  - `minecraft:book`（原版物品模型）
  - `oraxen:mana_crystal`（Oraxen 自定义物品）
  - `itemsadder:test_item`（ItemsAdder 自定义物品）
- 支持反向判断，如 `!hasItem.[...]` 表示没有该物品时条件满足
- 遍历所有背包槽位（主背包、盔甲槽、副手槽、主手槽）

---

## hasStockItem - 保存物品判断

判断玩家背包中是否有指定名称和数量的保存物品。

### 格式

```
hasStockItem.物品名称;数量
```

### 参数说明

| 参数 | 说明 | 必需 |
|------|------|------|
| 物品名称 | 已保存物品的名称 | ✅ |
| 数量 | 需要的数量 | ✅ |

### 判断规则

- 物品必须通过 `/km item save` 指令保存
- 物品比较使用 `ItemStack.isSimilar()` 方法，忽略物品数量差异
- 所有匹配物品的数量总和必须大于等于指定数量

### 示例

**判断玩家是否有 16 个神秘果：**

```yaml
- condition: "hasStockItem.神秘果;16"
  allow:
    - 'tell: &a你有足够的神秘果！'
  deny:
    - 'tell: &c你需要 16 个神秘果！'
```

**判断玩家是否有 1 个神圣之剑：**

```yaml
- condition: "hasStockItem.神圣之剑;1"
  allow:
    - 'tell: &a你可以购买这个物品！'
  deny:
    - 'tell: &c你需要神圣之剑才能购买！'
```

**结合 stock-item 动作使用：**

```yaml
actions:
  - condition: "hasStockItem.神秘果;16"
    allow:
      - 'stock-item: type=take;name=神秘果;amount=16'
      - 'tell: &a购买成功！'
    deny:
      - 'tell: &c物品不足！需要 16 个神秘果'
```

**反向判断（没有物品时满足条件）：**

```yaml
actions:
  - condition: "!hasStockItem.神圣之剑;1"
    allow:
      - 'tell: &a你还没有这个武器，可以购买！'
    deny:
      - 'tell: &c你已经拥有神圣之剑，不能再购买了！'
```

**使用变量：**

```yaml
actions:
  # 从数据中读取物品名称和数量
  - condition: "hasStockItem.{data:purchase_item};{data:required_amount}"
    allow:
      - 'stock-item: type=take;name={data:purchase_item};amount={data:required_amount}'
      - 'tell: &a购买成功！'
    deny:
      - 'tell: &c物品不足！'
```

### 注意事项

- 物品必须先通过 `/km item save` 保存才能使用
- 支持反向判断，如 `!hasStockItem.物品名称;数量`
- 物品比较会忽略 NBT 标签以外的差异
- 遍历所有背包槽位（主背包、盔甲槽、副手槽、主手槽）

---

## 完整示例：购物菜单
### 购买普通物品

将演示如何编写一个使用100金币/个的价格购买钻石的菜单。

```yaml
Title: '§6§l钻石商店'

Settings:
  need_placeholder:
    - 'math'
Body:
  diamond:
    type: 'item'
    text: '&a&l钻石'
    material: DIAMOND
    lore:
      - '&7价格: 100 金币 / 个'
    description: '请在下方拖动条选择购买数量。'

Inputs:
  amount:
    type: 'slider'
    text: '&a购买数量'
    min: 1
    max: 64
    default: 1
    format: '%s: %s个'

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 确认购买 ]'
    actions:
      # 购买钻石（使用 hasMoney 判断是否有足够的货币）
      - condition: "isIntNum.$(amount)"
        deny:
          - 'tell: &c请输入一个有效的数值。'
          - 'return'
      - condition: "hasMoney.%math_0_100*$(amount)%"
        allow:
          - 'money: type=take;num=%math_0_100*$(amount)%'
          - 'item: type=give;mats=DIAMOND;amount=$(amount)'
          - 'tell: &a购买成功！消耗了金币 x%math_0_100*$(amount)%，获得 钻石 x$(amount)'
        deny:
          - 'tell: &c货币不足！需要 金币 x%math_0_100*$(amount)% '
          - 'sound: block.note_block.bass'
  deny:
    text: '&c[ 取消购买 ]'
    actions:
      - 'actionbar: &c取消购买'
      - 'sound: block.note_block.bass'
      - 'close'

```


### 购买存储库的物品

**前提:** 若要使用该示例菜单，应该先使用指令`/km item save 神奇钻石剑`存储一个名为`神奇钻石剑`的物品。

```yaml
Title: '§6§l神奇钻石剑商店'

Settings:
  need_placeholder:
    - 'math'
Body:
  diamond_sword:
    type: 'item'
    text: '&a&l神奇钻石剑'
    material: DIAMOND_SWORD
    lore:
      - '&7价格: 10 个钻石'
    description: '请在下方拖动条选择兑换数量。'

Inputs:
  amount:
    type: 'slider'
    text: '&a购买数量'
    min: 1
    max: 64
    default: 1
    format: '%s: %s个'

Bottom:
  type: 'confirmation'
  confirm:
    text: '&a[ 确认购买 ]'
    actions:
      # 购买钻石剑（使用 hasItem 判断普通物品）
      - condition: "hasItem.[mats=DIAMOND;amount=%math_0_10*$(amount)%]"
        allow:
          - 'item: type=take;mats=DIAMOND;amount=%math_0_10*$(amount)%'
          - 'stock-item: type=give;name=神奇钻石剑;amount={data:purchase_amount}'
          - 'tell: &a购买成功！消耗了钻石 x%math_0_10*$(amount)%，获得 神奇钻石剑 x$(amount)'
        deny:
          - 'tell: &c物品不足！需要 钻石 x%math_0_10*$(amount)% '
          - 'sound: block.note_block.bass'
  deny:
    text: '&c[ 取消购买 ]'
    actions:
      - 'actionbar: &c取消购买'
      - 'sound: block.note_block.bass'
      - 'close'

```

---

## 两者的区别

| 特性 | hasItem | hasStockItem |
|------|---------|-------------|
| 适用范围 | 普通物品 | 已保存的物品 |
| 参数格式 | `hasItem.[mats=...;amount=...]` | `hasStockItem.物品名称;数量` |
| 材质判断 | 使用 Minecraft 原生材质 ID | 使用保存物品的完整 ItemStack |
| 物品匹配 | 仅匹配材质、可选 lore 和 model | 使用 `isSimilar()` 完整匹配 |
| 预先保存 | 不需要保存 | 需要通过 `/km item save` 保存 |
| 适用场景 | 判断基础物品（如钻石、铁矿）| 判断自定义物品（如附魔装备、特殊道具）|
