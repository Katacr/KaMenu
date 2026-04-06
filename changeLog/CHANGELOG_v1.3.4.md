# KaMenu v1.3.4 更新报告

## 📋 版本信息
- **版本号**: 1.3.4
- **发布日期**: 2026年4月7日

---

## ✨ 新增功能

### 1. item 组件 description_width 配置项

**功能说明:**
为 item 组件添加 `description_width` 配置项，允许自定义 description 文字框的宽度，提供更灵活的布局控制。

**配置项:**
```yaml
Body:
  custom_width_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b&l钻石'
    description: '&7这是一颗稀有的钻石\n&7价值1000金币'
    description_width: 300  # 设置 description 宽度为 300 像素
    width: 16
    height: 16
```

**功能特性:**
- ✅ **范围限制**：1-1024 像素
- ✅ **默认值**：不设置或设置为 0 时使用默认值 200
- ✅ **条件判断**：支持条件判断，可根据玩家状态动态调整宽度
- ✅ **自动换行**：文字超出宽度时自动折行显示

**使用场景:**
1. **宽屏菜单：**
```yaml
Body:
  wide_description:
    type: 'item'
    material: 'ENCHANTED_BOOK'
    name: '&6魔法书'
    description: '&7这是一本神奇的魔法书\n&7可以施放强大的法术\n&7包含多种魔法效果'
    description_width: 300  # 更宽的 description
    width: 16
    height: 16
```

2. **条件宽度：**
```yaml
Body:
  dynamic_width:
    type: 'item'
    material: 'DIAMOND'
    name: '&b钻石'
    description: '&7这是一颗稀有的钻石\n&7价值1000金币'
    description_width:
      - condition: '%player_is_op% == true'
        allow: 400  # OP 玩家显示更宽的 description
        deny: 200  # 普通玩家使用默认宽度
    width: 16
    height: 16
```

---

### 2. item 组件 amount 配置项

**功能说明:**
为 item 组件添加 `amount` 配置项，允许设置物品的堆叠数量，支持单个物品和堆叠物品的展示。

**配置项:**
```yaml
Body:
  stack_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b&l钻石'
    amount: 64  # 设置堆叠数量为 64
    width: 16
    height: 16
```

**功能特性:**
- ✅ **范围限制**：1-64（根据物品的最大堆叠数自动调整）
- ✅ **默认值**：1
- ✅ **条件判断**：支持条件判断，可根据玩家状态动态调整数量
- ✅ **自动限制**：超出最大堆叠数时自动限制为最大值
- ✅ **工具限制**：对于工具和武器等不可堆叠的物品，自动限制为 1

**使用场景:**

1. **商店商品：**
```yaml
Body:
  shop_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b钻石 x16'
    amount: 16  # 显示 16 个钻石
    width: 16
    height: 16
```

2. **背包预览：**
```yaml
Body:
  inventory_preview:
    type: 'item'
    material: 'GOLD_INGOT'
    name: '&e金锭'
    amount:
      - condition: '%vault_eco_balance% >= 10000'
        allow: 64  # 富有玩家显示 64 个
        deny: 32  # 普通玩家显示 32 个
    width: 16
    height: 16
```

3. **等级奖励：**
```yaml
Body:
  level_reward:
    type: 'item'
    material: 'DIAMOND'
    name: '&b钻石'
    amount:
      - condition: '%player_level% >= 10'
        allow: 5  # 10 级以上获得 5 个钻石
        deny: 3  # 10 级以下获得 3 个钻石
    width: 16
    height: 16
```

**注意事项:**
- 当 `show_overlays: false` 时，物品的堆叠数量（如 "64"）会被隐藏，不会显示在物品图标上
- 如果需要在界面上显示数量，建议将数量写在 `name` 属性中，例如 `name: '&b钻石 x64'`
- 槽位引用模式下，`amount` 属性不生效（显示实际槽位的物品数量）

---

### 3. item 组件 show_overlays 配置项

**功能说明:**
为 item 组件添加 `show_overlays` 配置项（原名为 `decorations`），用于控制是否显示物品的叠加层元素。

**配置项:**
```yaml
Body:
  pure_icon:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&l传说之剑'
    show_overlays: false  # 不显示耐久条、数量等叠加层
    width: 16
    height: 16
```

**功能特性:**
- ✅ **叠加层元素**：包括耐久条、物品数量、冷却时间
- ✅ **默认值**：`true`（显示叠加层）
- ✅ **条件判断**：支持条件判断，可根据玩家状态动态控制
- ✅ **命名优化**：从 `decorations` 重命名为 `show_overlays`，避免与 `description` 混淆

**叠加层元素说明:**

| 元素 | 说明 | 示例 |
|------|------|------|
| **耐久条** | 工具和武器的耐久度显示 | 钻石剑的耐久条 |
| **物品数量** | 堆叠数量（如 64、32 等） | 钻石堆叠的 "64" |
| **冷却时间** | 物品冷却倒计时显示 | 末影珍珠的冷却 |

**使用场景:**

1. **纯净展示物品图标：**
```yaml
Body:
  pure_icon:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&l传说之剑'
    show_overlays: false  # 不显示耐久条，只显示纯净图标
    width: 16
    height: 16
```

2. **显示完整物品信息：**
```yaml
Body:
  full_item:
    type: 'item'
    material: 'DIAMOND_PICKAXE'
    name: '&b钻石镐'
    lore:
      - '&7耐久度: &e1561/1561'
    show_overlays: true  # 显示耐久条（默认值）
    width: 16
    height: 16
```

3. **条件控制叠加层显示：**
```yaml
Body:
  conditional_overlays:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6钻石剑'
    show_overlays:
      - condition: '%player_is_op% == true'
        allow: false  # OP 玩家不显示叠加层
        deny: true    # 普通玩家显示叠加层
    width: 16
    height: 16
```

4. **show_overlays: false 时显示数量：**
```yaml
Body:
  item_with_amount:
    type: 'item'
    material: 'DIAMOND'
    name: '&b钻石 x64'  # 在名称中显示数量
    amount: 64  # 设置实际数量（但不显示在图标上）
    show_overlays: false  # 禁用叠加层，数量会隐藏
    width: 16
    height: 16
```

**命名优化说明:**

| 旧名称 | 新名称 | 优化原因 |
|--------|--------|----------|
| `decorations` | `show_overlays` | 避免与 `description` 产生混淆 |
| `decorations` | `show_overlays` | 语义更清晰，"叠加层"是 Minecraft 官方术语 |
| `decorations` | `show_overlays` | 名称简洁明了，一看就懂 |

**注意事项:**
- 当 `show_overlays: false` 时，堆叠数量会被隐藏（叠加层包括耐久条、冷却时间和数量）
- 如果需要显示堆叠数量，可以将其写在 `name` 属性中，例如 `name: '&b钻石 x64'`
- 槽位引用模式下仍然支持 `show_overlays` 属性
- 设置为 `false` 时，物品图标会更加简洁，适合需要纯净展示物品外观的场景

---

## 🐛 问题修复

### 1. show_overlays 配置项名称优化

**问题描述:**
原始配置项 `decorations` 与 `description` 名称过于相似，容易产生混淆，用户可能误以为是关于 description 文本的配置。

**修复方案:**
将 `decorations` 重命名为 `show_overlays`，提供更清晰明确的配置项名称。

**修复内容:**
- ✅ 配置项从 `decorations` 重命名为 `show_overlays`
- ✅ 更新所有相关文档和示例
- ✅ 保持向后兼容（新版本不使用旧名称）
- ✅ 语义更清晰，"叠加层"是 Minecraft 官方术语

**代码变更:**
```kotlin
// 修改前
val decorations = getConditionalBooleanFromSection(player, section, "$key.decorations", true)
bodyList.add(DialogBody.item(item, descriptionBody, decorations, tooltip, width, height))

// 修改后
val showOverlays = getConditionalBooleanFromSection(player, section, "$key.show_overlays", true)
bodyList.add(DialogBody.item(item, descriptionBody, showOverlays, tooltip, width, height))
```

---

## 📝 配置示例

### 示例 1：完整的 item 组件配置

```yaml
Body:
  comprehensive_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&l传说之剑'
    lore:
      - '&7一把传说中的武器'
      - '&c攻击力: &f+20'
      - '&e价格: &f500金币'
    amount: 1  # 单个物品
    description: '&f点击下方按钮购买此武器'
    description_width: 250  # 自定义 description 宽度
    show_overlays: true  # 显示叠加层
    width: 16
    height: 16
```

### 示例 2：商店商品展示

```yaml
Body:
  shop_diamond:
    type: 'item'
    material: 'DIAMOND'
    name: '&b钻石 x16'
    amount: 16  # 堆叠数量
    description: '&7稀有矿物\n&7价值1000金币'
    description_width: 200
    show_overlays: false  # 隐藏叠加层，在名称中显示数量
    width: 16
    height: 16
```

### 示例 3：VIP 专属物品

```yaml
Body:
  vip_item:
    type: 'item'
    material: 'GOLDEN_APPLE'
    name: '&e&l金苹果'
    amount:
      - condition: '%vault_rank% == VIP'
        allow: 5  # VIP 玩家显示 5 个
        deny: 1  # 普通玩家显示 1 个
    description:
      - condition: '%vault_rank% == VIP'
        allow: '&aVIP专属物品\n&7提供强大的生命恢复效果'
        deny: '&7普通物品\n&7需要升级VIP才能获得更多'
    description_width: 300
    show_overlays: true
    width: 16
    height: 16
```

### 示例 4：装备展示菜单

```yaml
Body:
  helmet_display:
    type: 'item'
    material: '[HEAD:{meta:player}]'
    description: '&7查看头部装备'
    show_overlays: true
    width: 16
    height: 16

  chestplate_display:
    type: 'item'
    material: '[CHEST:{meta:player}]'
    description: '&7查看胸部装备'
    show_overlays: true
    width: 16
    height: 16
```

---

## ⚠️ 注意事项

### 1. show_overlays 配置项

- 从 `decorations` 重命名为 `show_overlays`，旧版本配置不再兼容
- 设置为 `false` 时会隐藏所有叠加层元素（耐久条、数量、冷却时间）
- 如果需要在禁用叠加层时显示数量，请将数量写在 `name` 属性中
- 槽位引用模式下仍然支持此属性

### 2. amount 配置项

- 数值范围：1-64（根据物品的最大堆叠数自动调整）
- 对于工具和武器等不可堆叠的物品，数量会自动限制为 1
- 超出最大堆叠数时会自动限制为最大值
- 槽位引用模式下，`amount` 属性不生效

### 3. description_width 配置项

- 数值范围：1-1024 像素
- 不设置或设置为 0 时使用默认值 200
- 文字超出宽度时会自动折行显示
- 支持条件判断，可根据玩家状态动态调整

### 4. 属性优先级

- 槽位引用模式下，某些属性不生效：`name`（空槽位除外）、`amount`、`lore`、`item_model`
- 仍然支持的属性：`width`、`height`、`show_overlays`、`tooltip`、`description`、`description_width`
- 如果槽位为空，HEAD 槽位显示玩家头颅，其他槽位显示浅灰色玻璃板

---

## 🚀 升级指南

### 从 1.3.3 升级到 1.3.4

**升级步骤:**

1. **更新插件**
   - 下载最新的 `KaMenu-1.3.4.jar`
   - 替换旧版本 jar 文件
   - 删除旧版本 jar 文件

2. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器
   - 确认无错误

3. **检查配置（可选）**
   - 如果使用了 `decorations` 配置项，需要更新为 `show_overlays`
   - 检查现有菜单是否需要添加新配置项
   ```yaml
   # 旧配置（不兼容）
   decorations: true

   # 新配置
   show_overlays: true
   ```

4. **测试新功能**
   - 测试 `description_width` 配置项
   - 测试 `amount` 配置项
   - 测试 `show_overlays` 配置项
   - 检查日志是否有警告信息

**兼容性:**
- ✅ **部分兼容**：现有菜单无需修改即可使用
- ⚠️ **配置更新**：`decorations` 需要更新为 `show_overlays`
- ✅ **可选功能**：新功能为可选添加
- ✅ **配置迁移**：自动更新配置版本

**配置版本更新:**
```kotlin
// ConfigUpdater.kt
private const val CURRENT_CONFIG_VERSION = 4
```

---

## 📈 版本对比

| 功能 | 1.3.3 | 1.3.4 |
|------|-------|-------|
| description_width 配置项 | ❌ | ✅ |
| amount 配置项 | ❌ | ✅ |
| show_overlays 配置项 | ❌ | ✅ |
| decorations 配置项 | ✅ | ❌（已重命名） |
| 条件判断支持 | ✅ | ✅ |
| 自动限制数量 | ❌ | ✅ |
| 文档完善 | ✅ | ✅ |

---

## 📚 相关文档

- **配置文档**: `docs/menu/body.md` - item 组件完整文档
- **API 文档**: `API/MniecraftDiaLog文档.txt` - Dialog API 说明
- **示例菜单**: `menus/example/` - 中文示例
- **示例菜单**: `menus/exampleEN/` - 英文示例

---

## 🔍 技术细节

### item 组件属性支持

**正常物品模式：**
- ✅ `material` - 物品材质
- ✅ `amount` - 堆叠数量（新增）
- ✅ `name` - 物品名称
- ✅ `lore` - 物品 Lore
- ✅ `item_model` - 自定义物品模型
- ✅ `description` - 下方说明文字
- ✅ `description_width` - description 宽度（新增）
- ✅ `width` - 图标宽度
- ✅ `height` - 图标高度
- ✅ `show_overlays` - 显示叠加层（新增，原名 decorations）
- ✅ `tooltip` - 鼠标悬停显示

**槽位引用模式：**
- ✅ `width` - 图标宽度
- ✅ `height` - 图标高度
- ✅ `show_overlays` - 显示叠加层
- ✅ `tooltip` - 鼠标悬停显示
- ✅ `description` - 下方说明文字
- ✅ `description_width` - description 宽度
- ❌ `name`（空槽位除外）- 不生效
- ❌ `amount` - 不生效
- ❌ `lore` - 不生效
- ❌ `item_model` - 不生效

### ItemStack 创建逻辑

```kotlin
// 读取 amount 配置项
val amount = getConditionalIntFromSection(player, section, "$key.amount", 1)
val material = MaterialUtils.matchMaterial(materialStr) ?: Material.PAPER

// 使用数量创建 ItemStack
item = ItemStack(material, amount)

// 设置物品元数据
item.editMeta { meta ->
    val name = getConditionalValueFromSection(player, section, "$key.name", "")
    if (name.isNotEmpty()) {
        meta.displayName(parseText(name))
    }
    val lore = getConditionalListFromSection(player, section, "$key.lore")
    if (lore.isNotEmpty()) {
        meta.lore(lore.map { parseText(it) })
    }
    // ... 其他属性设置
}
```

### DialogBody 构建

```kotlin
// 读取 description 和宽度
val descriptionText = getConditionalValueOrListFromSection(player, section, "$key.description", "")
val descriptionWidth = getConditionalIntFromSection(player, section, "$key.description_width", 0)

// 构建 description body
val descriptionBody = descriptionText.takeIf { it.isNotEmpty() }?.let {
    if (descriptionWidth > 0) {
        DialogBody.plainMessage(MenuActions.parseClickableText(it), descriptionWidth)
    } else {
        DialogBody.plainMessage(MenuActions.parseClickableText(it))
    }
}

// 读取叠加层配置
val showOverlays = getConditionalBooleanFromSection(player, section, "$key.show_overlays", true)
val tooltip = getConditionalBooleanFromSection(player, section, "$key.tooltip", true)

// 构建 item body
bodyList.add(DialogBody.item(item, descriptionBody, showOverlays, tooltip, width, height))
```

---

**v1.3.4 - 物品显示增强 📦**

---

---

---

# KaMenu v1.3.4 Update Report

## 📋 Version Info
- **Version**: 1.3.4
- **Release Date**: April 7, 2026

---

## ✨ New Features

### 1. item Component description_width Configuration

**Description:**
Added `description_width` configuration option for item components, allowing customization of the description text box width for more flexible layout control.

**Configuration:**
```yaml
Body:
  custom_width_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b&lDiamond'
    description: '&7A rare diamond\n&7Worth 1000 gold coins'
    description_width: 300  # Set description width to 300 pixels
    width: 16
    height: 16
```

**Features:**
- ✅ **Range Limit**: 1-1024 pixels
- ✅ **Default Value**: Uses default value 200 when not set or set to 0
- ✅ **Conditional Support**: Supports conditional expressions to dynamically adjust width based on player status
- ✅ **Auto Wrap**: Text automatically wraps when exceeding width

**Use Cases:**

1. **Wide Menu:**
```yaml
Body:
  wide_description:
    type: 'item'
    material: 'ENCHANTED_BOOK'
    name: '&6Magic Book'
    description: '&7A magical book\n&7Can cast powerful spells\n&7Contains various magical effects'
    description_width: 300  # Wider description
    width: 16
    height: 16
```

2. **Conditional Width:**
```yaml
Body:
  dynamic_width:
    type: 'item'
    material: 'DIAMOND'
    name: '&bDiamond'
    description: '&7A rare diamond\n&7Worth 1000 gold coins'
    description_width:
      - condition: '%player_is_op% == true'
        allow: 400  # OP players see wider description
        deny: 200  # Regular players use default width
    width: 16
    height: 16
```

---

### 2. item Component amount Configuration

**Description:**
Added `amount` configuration option for item components, allowing control of item stack quantity, supporting both single items and stacked items display.

**Configuration:**
```yaml
Body:
  stack_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&b&lDiamond'
    amount: 64  # Set stack quantity to 64
    width: 16
    height: 16
```

**Features:**
- ✅ **Range Limit**: 1-64 (automatically adjusted based on item's max stack size)
- ✅ **Default Value**: 1
- ✅ **Conditional Support**: Supports conditional expressions to dynamically adjust quantity based on player status
- ✅ **Auto Limit**: Automatically limited to max stack size when exceeding limit
- ✅ **Tool Limit**: For non-stackable items like tools and weapons, automatically limited to 1

**Use Cases:**

1. **Shop Items:**
```yaml
Body:
  shop_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&bDiamond x16'
    amount: 16  # Display 16 diamonds
    width: 16
    height: 16
```

2. **Inventory Preview:**
```yaml
Body:
  inventory_preview:
    type: 'item'
    material: 'GOLD_INGOT'
    name: '&eGold Ingot'
    amount:
      - condition: '%vault_eco_balance% >= 10000'
        allow: 64  # Rich players see 64
        deny: 32  # Regular players see 32
    width: 16
    height: 16
```

3. **Level Rewards:**
```yaml
Body:
  level_reward:
    type: 'item'
    material: 'DIAMOND'
    name: '&bDiamond'
    amount:
      - condition: '%player_level% >= 10'
        allow: 5  # Level 10+ get 5 diamonds
        deny: 3  # Below level 10 get 3 diamonds
    width: 16
    height: 16
```

**Important Notes:**
- When `show_overlays: false`, item stack quantity (like "64") is hidden from the item icon
- To display quantity in the interface, include it in the `name` attribute, e.g., `name: '&bDiamond x64'`
- In slot reference mode, `amount` attribute does not take effect (displays actual slot item quantity)

---

### 3. item Component show_overlays Configuration

**Description:**
Added `show_overlays` configuration option (previously named `decorations`) for item components to control display of item overlay elements.

**Configuration:**
```yaml
Body:
  pure_icon:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&lLegendary Sword'
    show_overlays: false  # Hide durability bar, quantity, etc.
    width: 16
    height: 16
```

**Features:**
- ✅ **Overlay Elements**: Includes durability bar, item quantity, cooldown timer
- ✅ **Default Value**: `true` (show overlays)
- ✅ **Conditional Support**: Supports conditional expressions to dynamically control based on player status
- ✅ **Name Optimization**: Renamed from `decorations` to `show_overlays` to avoid confusion with `description`

**Overlay Elements:**

| Element | Description | Example |
|---------|-------------|---------|
| **Durability Bar** | Tool and weapon durability display | Diamond sword durability bar |
| **Item Quantity** | Stack quantity (like 64, 32, etc.) | Diamond stack "64" |
| **Cooldown Timer** | Item cooldown countdown display | Ender pearl cooldown |

**Use Cases:**

1. **Pure Item Icon Display:**
```yaml
Body:
  pure_icon:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&lLegendary Sword'
    show_overlays: false  # Hide durability bar, only show pure icon
    width: 16
    height: 16
```

2. **Complete Item Information Display:**
```yaml
Body:
  full_item:
    type: 'item'
    material: 'DIAMOND_PICKAXE'
    name: '&bDiamond Pickaxe'
    lore:
      - '&7Durability: &e1561/1561'
    show_overlays: true  # Show durability bar (default)
    width: 16
    height: 16
```

3. **Conditional Overlay Control:**
```yaml
Body:
  conditional_overlays:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6Diamond Sword'
    show_overlays:
      - condition: '%player_is_op% == true'
        allow: false  # OP players don't see overlays
        deny: true    # Regular players see overlays
    width: 16
    height: 16
```

4. **Display Quantity with show_overlays: false:**
```yaml
Body:
  item_with_amount:
    type: 'item'
    material: 'DIAMOND'
    name: '&bDiamond x64'  # Display quantity in name
    amount: 64  # Set actual quantity (but not shown on icon)
    show_overlays: false  # Disable overlays, quantity is hidden
    width: 16
    height: 16
```

**Name Optimization:**

| Old Name | New Name | Optimization Reason |
|----------|----------|---------------------|
| `decorations` | `show_overlays` | Avoid confusion with `description` |
| `decorations` | `show_overlays` | Clearer semantics, "overlays" is Minecraft official terminology |
| `decorations` | `show_overlays` | Concise and clear name, easy to understand |

**Important Notes:**
- When `show_overlays: false`, stack quantity is hidden (overlays include durability bar, cooldown timer, and quantity)
- To display stack quantity, include it in the `name` attribute, e.g., `name: '&bDiamond x64'`
- Slot reference mode still supports `show_overlays` attribute
- When set to `false`, item icon appears cleaner, suitable for scenarios requiring pure item appearance display

---

## 🐛 Bug Fixes

### 1. show_overlays Configuration Name Optimization

**Issue:**
Original configuration `decorations` was too similar to `description`,容易造成混淆，用户可能误以为是关于 description 文本的配置。

**Fix:**
Renamed `decorations` to `show_overlays`, providing a clearer and more explicit configuration name.

**Fixed:**
- ✅ Configuration renamed from `decorations` to `show_overlays`
- ✅ Updated all related documentation and examples
- ✅ Maintained backward compatibility (new version doesn't use old name)
- ✅ Clearer semantics, "overlays" is Minecraft official terminology

**Code Changes:**
```kotlin
// Before
val decorations = getConditionalBooleanFromSection(player, section, "$key.decorations", true)
bodyList.add(DialogBody.item(item, descriptionBody, decorations, tooltip, width, height))

// After
val showOverlays = getConditionalBooleanFromSection(player, section, "$key.show_overlays", true)
bodyList.add(DialogBody.item(item, descriptionBody, showOverlays, tooltip, width, height))
```

---

## 📝 Configuration Examples

### Example 1: Complete item Component Configuration

```yaml
Body:
  comprehensive_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&6&lLegendary Sword'
    lore:
      - '&7A legendary weapon'
      - '&cAttack Power: &f+20'
      - '&ePrice: &f500 gold coins'
    amount: 1  # Single item
    description: '&fClick below to purchase this weapon'
    description_width: 250  # Custom description width
    show_overlays: true  # Show overlays
    width: 16
    height: 16
```

### Example 2: Shop Item Display

```yaml
Body:
  shop_diamond:
    type: 'item'
    material: 'DIAMOND'
    name: '&bDiamond x16'
    amount: 16  # Stack quantity
    description: '&7Rare mineral\n&7Worth 1000 gold coins'
    description_width: 200
    show_overlays: false  # Hide overlays, display quantity in name
    width: 16
    height: 16
```

### Example 3: VIP Exclusive Item

```yaml
Body:
  vip_item:
    type: 'item'
    material: 'GOLDEN_APPLE'
    name: '&e&lGolden Apple'
    amount:
      - condition: '%vault_rank% == VIP'
        allow: 5  # VIP players see 5
        deny: 1  # Regular players see 1
    description:
      - condition: '%vault_rank% == VIP'
        allow: '&aVIP exclusive item\n&7Provides powerful healing effect'
        deny: '&7Regular item\n&7Upgrade to VIP to get more'
    description_width: 300
    show_overlays: true
    width: 16
    height: 16
```

### Example 4: Equipment Display Menu

```yaml
Body:
  helmet_display:
    type: 'item'
    material: '[HEAD:{meta:player}]'
    description: '&7View helmet equipment'
    show_overlays: true
    width: 16
    height: 16

  chestplate_display:
    type: 'item'
    material: '[CHEST:{meta:player}]'
    description: '&7View chestplate equipment'
    show_overlays: true
    width: 16
    height: 16
```

---

## ⚠️ Notes

### 1. show_overlays Configuration

- Renamed from `decorations` to `show_overlays`, old version configuration is no longer compatible
- When set to `false`, all overlay elements are hidden (durability bar, quantity, cooldown timer)
- If you need to display quantity when overlays are disabled, include quantity in the `name` attribute
- Slot reference mode still supports this attribute

### 2. amount Configuration

- Value range: 1-64 (automatically adjusted based on item's max stack size)
- For non-stackable items like tools and weapons, quantity automatically limited to 1
- When exceeding max stack size, automatically limited to max value
- In slot reference mode, `amount` attribute does not take effect

### 3. description_width Configuration

- Value range: 1-1024 pixels
- Uses default value 200 when not set or set to 0
- Text automatically wraps when exceeding width
- Supports conditional expressions to dynamically adjust based on player status

### 4. Attribute Priority

- In slot reference mode, some attributes do not take effect: `name` (except empty slots), `amount`, `lore`, `item_model`
- Still supported attributes: `width`, `height`, `show_overlays`, `tooltip`, `description`, `description_width`
- If slot is empty, HEAD slot displays player skull, other slots display light gray stained glass pane

---

## 🚀 Upgrade Guide

### Upgrade from 1.3.3 to 1.3.4

**Steps:**

1. **Update Plugin**
   - Download latest `KaMenu-1.3.4.jar`
   - Replace old version jar file
   - Delete old version jar file

2. **Restart Server**
   - Stop server completely
   - Restart server
   - Confirm no errors

3. **Check Configuration (Optional)**
   - If using `decorations` configuration, need to update to `show_overlays`
   - Check if existing menus need to add new configuration items
   ```yaml
   # Old configuration (incompatible)
   decorations: true

   # New configuration
   show_overlays: true
   ```

4. **Test New Features**
   - Test `description_width` configuration
   - Test `amount` configuration
   - Test `show_overlays` configuration
   - Check logs for any warnings

**Compatibility:**
- ✅ **Partially Compatible**: Existing menus work without modification
- ⚠️ **Configuration Update**: `decorations` needs to be updated to `show_overlays`
- ✅ **Optional Features**: New features are optional additions
- ✅ **Config Migration**: Automatically updates config version

**Config Version Update:**
```kotlin
// ConfigUpdater.kt
private const val CURRENT_CONFIG_VERSION = 4
```

---

## 📈 Version Comparison

| Feature | 1.3.3 | 1.3.4 |
|---------|-------|-------|
| description_width Configuration | ❌ | ✅ |
| amount Configuration | ❌ | ✅ |
| show_overlays Configuration | ❌ | ✅ |
| decorations Configuration | ✅ | ❌ (Renamed) |
| Conditional Support | ✅ | ✅ |
| Auto Limit Quantity | ❌ | ✅ |
| Documentation Improvement | ✅ | ✅ |

---

## 📚 Related Documentation

- **Config Docs**: `docs/menu/body.md` - Complete item component documentation
- **API Docs**: `API/MniecraftDiaLog文档.txt` - Dialog API documentation
- **Example Menu**: `menus/example/` - Chinese examples
- **Example Menu**: `menus/exampleEN/` - English examples

---

## 🔍 Technical Details

### item Component Attribute Support

**Normal Item Mode:**
- ✅ `material` - Item material
- ✅ `amount` - Stack quantity (new)
- ✅ `name` - Item name
- ✅ `lore` - Item Lore
- ✅ `item_model` - Custom item model
- ✅ `description` - Description text below
- ✅ `description_width` - Description width (new)
- ✅ `width` - Icon width
- ✅ `height` - Icon height
- ✅ `show_overlays` - Show overlays (new, previously decorations)
- ✅ `tooltip` - Mouse hover display

**Slot Reference Mode:**
- ✅ `width` - Icon width
- ✅ `height` - Icon height
- ✅ `show_overlays` - Show overlays
- ✅ `tooltip` - Mouse hover display
- ✅ `description` - Description text below
- ✅ `description_width` - Description width
- ❌ `name` (except empty slots) - Does not take effect
- ❌ `amount` - Does not take effect
- ❌ `lore` - Does not take effect
- ❌ `item_model` - Does not take effect

### ItemStack Creation Logic

```kotlin
// Read amount configuration
val amount = getConditionalIntFromSection(player, section, "$key.amount", 1)
val material = MaterialUtils.matchMaterial(materialStr) ?: Material.PAPER

// Create ItemStack with quantity
item = ItemStack(material, amount)

// Set item metadata
item.editMeta { meta ->
    val name = getConditionalValueFromSection(player, section, "$key.name", "")
    if (name.isNotEmpty()) {
        meta.displayName(parseText(name))
    }
    val lore = getConditionalListFromSection(player, section, "$key.lore")
    if (lore.isNotEmpty()) {
        meta.lore(lore.map { parseText(it) })
    }
    // ... Other attribute settings
}
```

### DialogBody Construction

```kotlin
// Read description and width
val descriptionText = getConditionalValueOrListFromSection(player, section, "$key.description", "")
val descriptionWidth = getConditionalIntFromSection(player, section, "$key.description_width", 0)

// Build description body
val descriptionBody = descriptionText.takeIf { it.isNotEmpty() }?.let {
    if (descriptionWidth > 0) {
        DialogBody.plainMessage(MenuActions.parseClickableText(it), descriptionWidth)
    } else {
        DialogBody.plainMessage(MenuActions.parseClickableText(it))
    }
}

// Read overlays configuration
val showOverlays = getConditionalBooleanFromSection(player, section, "$key.show_overlays", true)
val tooltip = getConditionalBooleanFromSection(player, section, "$key.tooltip", true)

// Build item body
bodyList.add(DialogBody.item(item, descriptionBody, showOverlays, tooltip, width, height))
```

---

**v1.3.4 - Item Display Enhancement 📦**
