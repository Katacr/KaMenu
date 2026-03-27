# KaMenu v1.2.3 更新报告

## 📋 版本信息
- **版本号**: 1.2.3
- **发布日期**: 2026年3月28日

---

## ✨ 新增功能

### 1. 物品管理功能扩展

#### 新增条件方法
- **`hasItem`** - 判断玩家背包中是否有足够数量的普通物品
  - 支持指定材质（Material）
  - 支持按 lore（描述）判断
  - 支持按 item_model（物品模型）判断
  - 用法：`hasItem.[mats=DIAMOND;amount=10]`
  - 高级用法：`hasItem.[mats=DIAMOND;amount=10;lore=测试物品;model=oraxen:mana_crystal]`

- **`hasStockItem`** - 判断玩家背包中是否有足够数量的保存物品
  - 适用于通过 `/km item save` 保存的物品
  - 用法：`hasStockItem.物品名称;数量`

#### 新增动作
- **`item`** - 普通物品给予/扣除操作
  - `type=give` - 给予指定材质的物品
    - 用法：`item: type=give;mats=DIAMOND;amount=10`
    - 支持任意数量（无64上限）
    - 背包满时自动掉落在地上
  - `type=take` - 从玩家背包扣除物品
    - 用法：`item: type=take;mats=DIAMOND;amount=10`
    - 支持按 lore 判断：`lore=测试物品`
    - 支持按 item_model 判断：`model=oraxen:mana_crystal`
    - 可同时指定 lore 和 model 进行精确判断

- **`stock-item`** - 保存物品的给予/扣除操作
  - `type=give` - 给予保存的物品
    - 用法：`stock-item: type=give;name=物品名称;amount=16`
    - 支持任意数量（无64上限）
    - 背包满时自动掉落在地上
  - `type=take` - 扣除保存的物品
    - 用法：`stock-item: type=take;name=物品名称;amount=16`

### 2. 按钮工具提示（Tooltip）功能
- 为按钮添加了 `tooltip` 属性支持
- 鼠标悬停在按钮上时显示提示文本
- 支持多行显示（YAML 列表格式）
- 支持颜色代码
- 支持变量替换

**配置示例：**
```yaml
Bottom:
  type: 'multi'
  columns: 3
  buttons:
    my_button:
      text: '&9[ 按钮 ]'
      tooltip:
        - '&7这是提示文本第一行'
        - '&7这是提示文本第二行'
        - '&f点击执行操作'
      actions:
        - 'tell: 点击了按钮'
```

### 3. Body 组件增强
- **新增 `item_model` 属性支持**
  - 为 Body 的 item 组件添加了 `item_model` 配置项
  - 支持使用命名空间格式的物品模型（`namespace:key`）
  - 适用于 Minecraft 1.21.7 及以上版本
  - 用于显示具有特殊外观的物品（如特殊材质的武器、道具等）

**配置示例：**
```yaml
Body:
  custom_model_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&b&l光之圣剑'
    lore:
      - '&7拥有独特外观的神器'
      - '&a攻击力: &f+50'
    item_model: 'minecraft:custom_sword'  # 使用命名空间物品模型
    description: '&f这是使用自定义模型的特殊物品'
```

**说明：**
- 格式：`命名空间:键名`，例如 `minecraft:custom_sword` 或 `your_plugin:special_item`
- 工作原理：将模型数据存储在物品的 PersistentDataContainer 中，由 Minecraft 客户端根据模型定义渲染特殊外观
- 所有字段均支持条件判断

### 4. 用户体验优化
- **背包满时的提示优化**
  - 当物品无法放入背包时，自动掉落在玩家位置
  - 显示 ActionBar 提示："背包已满，X个物品掉在了地上"
  - 播放拾取物品音效（`ENTITY_ITEM_PICKUP`）
  - 适用于 `/km item give`、`item: type=give` 和 `stock-item: type=give`

---

## 🔧 优化改进

### 物品数量限制
- 移除了64个物品的上限限制
- 现在支持给予任意数量的物品（如100、200、500等）
- 最小数量仍限制为1，避免出现0或负数

### 指令优化
- 修复了 `/km item give` 指令数量参数无效的 bug
- 确保物品数量参数正确生效

---

## 📚 文档更新

### 新增文档
- `docs/menu/conditions_item.md` - 物品条件方法详细文档
- `src/main/resources/menus/example/item_demo.yml` - 中文版物品功能演示菜单
- `src/main/resources/menus/exampleEN/item_demo.yml` - 英文版物品功能演示菜单

### 更新文档
- `docs/menu/conditions.md` - 添加 hasItem 和 hasStockItem 条件说明
- `docs/menu/actions.md` - 添加 item 和 stock-item 动作详细文档
- `docs/menu/body.md` - 添加 item_model 配置项说明（1.21.7+ 物品模型支持）

---

## 🐛 Bug 修复
- 修复了 `stock-item: type=give` 和 `item: type=give` 动作数量设置无效的问题
- 修复了 `/km item give` 指令数量参数不起作用的问题
- 修复了 `/km item give` 指令背包满时物品不落地的问题

---

## 📌 使用示例

### 判断并扣除普通物品
```yaml
buttons:
  buy_diamond:
    text: '&e[ 购买物品 ]'
    actions:
      - condition: "hasItem.[mats=DIAMOND;amount=10]"
        allow:
          - 'item: type=take;mats=DIAMOND;amount=10'
          - 'item: type=give;mats=IRON_INGOT;amount=64'
          - 'tell: &a购买成功！'
        deny:
          - 'tell: &c购买失败！需要10个钻石'
```

### 使用保存物品
```yaml
buttons:
  use_stock_item:
    text: '&e[ 使用保存物品 ]'
    actions:
      - condition: "hasStockItem.神秘果实;5"
        allow:
          - 'stock-item: type=take;name=神秘果实;amount=5'
          - 'tell: &a使用了5个神秘果实！'
        deny:
          - 'tell: &c物品不足！需要5个神秘果实'
```

### 按钮工具提示
```yaml
buttons:
  advanced_button:
    text: '&6[ 高级功能 ]'
    tooltip:
      - '&7点击打开高级菜单'
      - '&e需要权限: admin'
      - '&7包含物品管理、数据设置等功能'
    actions:
      - 'open: advanced_menu'
```

---

## 🎯 兼容性说明
- 本版本完全兼容旧版本配置
- 新增功能不影响现有菜单的正常运行
- 建议更新后测试现有菜单，确保功能正常

---

## 📝 下一步计划
- 继续优化用户体验
- 添加更多条件判断方法
- 扩展物品操作功能

---

**感谢您使用 KaMenu！如有问题或建议，欢迎反馈。**

---

---

# KaMenu v1.2.3 Update Notes

## 📋 Version Information
- **Version**: 1.2.3
- **Release Date**: March 28, 2026

---

## ✨ New Features

### 1. Item Management Extensions

#### New Condition Methods
- **`hasItem`** - Check if player has enough regular items in inventory
  - Supports material specification (Material)
  - Supports lore (description) matching
  - Supports item_model matching
  - Usage: `hasItem.[mats=DIAMOND;amount=10]`
  - Advanced usage: `hasItem.[mats=DIAMOND;amount=10;lore=Test Item;model=oraxen:mana_crystal]`

- **`hasStockItem`** - Check if player has enough saved items in inventory
  - For items saved via `/km item save`
  - Usage: `hasStockItem.ItemName;Amount`

#### New Actions
- **`item`** - Regular item give/take operations
  - `type=give` - Give items with specified material
    - Usage: `item: type=give;mats=DIAMOND;amount=10`
    - Supports unlimited quantity (no 64 stack limit)
    - Automatically drops items on ground when inventory is full
  - `type=take` - Take items from player inventory
    - Usage: `item: type=take;mats=DIAMOND;amount=10`
    - Supports lore matching: `lore=Test Item`
    - Supports item_model matching: `model=oraxen:mana_crystal`
    - Can specify both lore and model for precise matching

- **`stock-item`** - Saved item give/take operations
  - `type=give` - Give saved items
    - Usage: `stock-item: type=give;name=ItemName;amount=16`
    - Supports unlimited quantity (no 64 stack limit)
    - Automatically drops items on ground when inventory is full
  - `type=take` - Take saved items
    - Usage: `stock-item: type=take;name=ItemName;amount=16`

### 2. Button Tooltip Feature
- Added `tooltip` property support for buttons
- Display hint text when hovering over buttons
- Supports multi-line display (YAML list format)
- Supports color codes
- Supports variable replacement

**Configuration Example:**
```yaml
Bottom:
  type: 'multi'
  columns: 3
  buttons:
    my_button:
      text: '&9[ Button ]'
      tooltip:
        - '&7This is tooltip line 1'
        - '&7This is tooltip line 2'
        - '&fClick to execute action'
      actions:
        - 'tell: Button clicked'
```

### 3. Body Component Enhancements
- **Added `item_model` Property Support**
  - Added `item_model` configuration option for Body's item component
  - Supports namespace-based item models (`namespace:key` format)
  - Compatible with Minecraft 1.21.7 and later versions
  - Used to display items with special appearance (e.g., custom weapons, items with unique textures)

**Configuration Example:**
```yaml
Body:
  custom_model_item:
    type: 'item'
    material: 'DIAMOND_SWORD'
    name: '&b&lHoly Sword of Light'
    lore:
      - '&7A legendary artifact with unique appearance'
      - '&aAttack Power: &f+50'
    item_model: 'minecraft:custom_sword'  # Use namespace-based item model
    description: '&fThis is a special item using a custom model'
```

**Notes:**
- Format: `namespace:key`, e.g., `minecraft:custom_sword` or `your_plugin:special_item`
- How it works: Stores model data in item's PersistentDataContainer, Minecraft client renders special appearance based on model definition
- All fields support conditional logic

### 4. User Experience Improvements
- **Inventory Full Notification Enhancement**
  - Automatically drops items at player location when inventory is full
  - Shows ActionBar notification: "Inventory full, X items dropped on ground"
  - Plays item pickup sound (`ENTITY_ITEM_PICKUP`)
  - Applies to `/km item give`, `item: type=give`, and `stock-item: type=give`

---

## 🔧 Optimizations

### Item Quantity Limit
- Removed 64-item stack limit restriction
- Now supports unlimited item quantities (e.g., 100, 200, 500)
- Minimum quantity still limited to 1 to prevent zero or negative amounts

### Command Optimization
- Fixed bug where `/km item give` command amount parameter was not working
- Ensured item quantity parameter takes effect correctly

---

## 📚 Documentation Updates

### New Documentation
- `docs/menu/conditions_item.md` - Detailed documentation for item condition methods
- `src/main/resources/menus/example/item_demo.yml` - Chinese version item feature demo menu
- `src/main/resources/menus/exampleEN/item_demo.yml` - English version item feature demo menu

### Updated Documentation
- `docs/menu/conditions.md` - Added hasItem and hasStockItem condition descriptions
- `docs/menu/actions.md` - Added detailed documentation for item and stock-item actions
- `docs/menu/body.md` - Added item_model configuration option documentation (1.21.7+ item model support)

---

## 🐛 Bug Fixes
- Fixed issue where `stock-item: type=give` and `item: type=give` action quantity parameter was not working
- Fixed issue where `/km item give` command amount parameter was not taking effect
- Fixed issue where items were not dropping on ground when inventory was full in `/km item give` command

---

## 📌 Usage Examples

### Check and Take Regular Items
```yaml
buttons:
  buy_diamond:
    text: '&e[ Buy Item ]'
    actions:
      - condition: "hasItem.[mats=DIAMOND;amount=10]"
        allow:
          - 'item: type=take;mats=DIAMOND;amount=10'
          - 'item: type=give;mats=IRON_INGOT;amount=64'
          - 'tell: &aPurchase successful!'
        deny:
          - 'tell: &cPurchase failed! Need 10 diamonds'
```

### Use Saved Items
```yaml
buttons:
  use_stock_item:
    text: '&e[ Use Saved Item ]'
    actions:
      - condition: "hasStockItem.Mystic Fruit;5"
        allow:
          - 'stock-item: type=take;name=Mystic Fruit;amount=5'
          - 'tell: &aUsed 5 Mystic Fruits!'
        deny:
          - 'tell: &cNot enough items! Need 5 Mystic Fruits'
```

### Button Tooltip
```yaml
buttons:
  advanced_button:
    text: '&6[ Advanced Features ]'
    tooltip:
      - '&7Click to open advanced menu'
      - '&eRequires permission: admin'
      - '&7Includes item management, data settings, etc.'
    actions:
      - 'open: advanced_menu'
```

---

## 🎯 Compatibility Notes
- This version is fully backward compatible with older configurations
- New features do not affect existing menu functionality
- Recommended to test existing menus after update to ensure proper operation

---

## 📝 Next Steps
- Continue optimizing user experience
- Add more condition checking methods
- Expand item operation features

---

**Thank you for using KaMenu! If you have any questions or suggestions, please feel free to provide feedback.**
