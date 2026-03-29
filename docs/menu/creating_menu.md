# 📝 创建菜单教程

本教程将引导您从零开始创建一个完整的菜单，帮助您快速掌握 KaMenu 的菜单制作方法。

---

## 🎯 学习目标

通过本教程，您将学会：
- ✅ 创建菜单文件的基本结构
- ✅ 配置菜单标题和设置
- ✅ 添加文字内容
- ✅ 添加物品展示
- ✅ 添加输入组件
- ✅ 配置按钮和动作
- ✅ 使用条件判断

---

## 📄 第一步：创建菜单文件

### 1.1 创建文件位置

在 `plugins/KaMenu/menus/` 目录下创建新文件，建议使用有意义的文件名：

```
plugins/KaMenu/menus/
└── my_first_menu.yml    # 创建这个文件
```

### 1.2 基本结构

要创建一个菜单，我们需要首先了解菜单的结构。一个完整菜单文件通常包括`Title`、`Body`、`Inputs`、`Bottom`这四个区域来显示内容。另外还有`Settings`和`Events`对菜单的功能进行扩展。
#### 参数说明

| 参数         | 说明             | 必需 | 跳转文档                              |
|------------|----------------|----|-----------------------------------|
| `Title`    | 菜单最顶部的标题文本     | ✅  | [🎬 菜单标题 (Title)](layout.md)      |
| `Body`     | 通常展示信息的区域      |    | [🧩 内容组件 (Body)](body.md)         |
| `Inputs`   | 创建输入组件的区域      |    | [⌨️ 输入组件 (Inputs)](inputs.md)     |
| `Bottom`   | 底部的按钮区域        | ✅  | [📋 底部按钮 (Bottom)](bottom.md)     |
| `Settings` | 用于配置菜单的一些选项    |    | [⚙️ 全局设置 (Settings)](setting.md)  |
| `Events`   | 用于配置菜单的开启/关闭动作 |    | [🎯 事件 (Events)](events.md)   |


{% hint style="info" %}
你知道吗？
- 实际上所有区域都是可选的，你可以创建一个空文件，KaMenu依旧可以打开，但这没有任何意义。
{% endhint %}
```yaml
# 菜单标题
Title: '&6我的第一个菜单'

# 可选：全局设置
Settings:
  can_escape: true
  after_action: CLOSE

# 可选：菜单开启时执行的动作
Events:
  Open:
    - 'tell: &a欢迎来到菜单！'

# 可选：内容展示区
Body:
  ...

# 可选：输入组件区
Inputs:
  ...

# 底部按钮区
Bottom:
  ...
```

**当前状态：** 现在您已经有了一个基础的空菜单框架！

---

## 📝 第二步：添加文字内容

### 2.1 添加欢迎消息

在 `Body` 节点中添加文字组件：

```yaml
Title: '&6我的第一个菜单'

Body:
  welcome:
    type: 'message'
    text:
      - '&7欢迎来到服务器商店'
      - '&7点击下方按钮浏览商品'
```

### 2.2 添加分隔线和说明

```yaml
Body:
  welcome:
    type: 'message'
    text:
      - '&7欢迎来到服务器商店'
      - '&7点击下方按钮浏览商品'

  separator:
    type: 'message'
    text: '&8————————————————'

  tips:
    type: 'message'
    text:
      - '&e提示：'
      - '&7- 购买前请确认余额充足'
      - '&7- 如有问题请联系管理员'
```

**当前状态：** 现在您的菜单会显示欢迎消息和一些提示文字。

---

## 🎨 第三步：添加物品展示

### 3.1 添加单个物品

```yaml
Body:
  # ... 之前的文字内容 ...

  diamond_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&6&l钻石'
    lore:
      - '&7珍贵的宝石'
    description: '&f点击下方按钮获取'
```

### 3.2 添加多个物品

```yaml
Body:
  # ... 之前的文字内容 ...

  diamond_item:
    type: 'item'
    material: 'DIAMOND'
    name: '&6&l钻石'
    lore:
      - '&7珍贵的宝石'
    description: '&f点击下方按钮获取'

  iron_ingot:
    type: 'item'
    material: 'IRON_INGOT'
    name: '&f&l铁锭'
    lore:
      - '&7常见的金属'
    description: '&f点击下方按钮获取'

  gold_ingot:
    type: 'item'
    material: 'GOLD_INGOT'
    name: '&e&l金锭'
    lore:
      - '&7稀有的金属'
    description: '&f点击下方按钮获取'
```

**提示：** 物品材质名支持多种格式，如 `diamond_sword`、`Diamond-Sword`、`diamond sword` 等。

**当前状态：** 现在您的菜单显示了三个商品，每个物品都有名称、描述。

---

## 🎯 第四步：添加输入组件

### 4.1 添加数量选择滑块

```yaml
Inputs:
  diamond_amount: 
    type: 'slider'
    text: '&a选择获取钻石的数量'
    min: 1
    max: 64
    default: 1
```

### 4.2 添加更多输入组件

```yaml
Inputs:
  diamond_amount:
    type: 'slider'
    text: '&a选择获取钻石的数量'
    min: 1
    max: 64
    default: 1

  note:
    type: 'input'
    text: '&7备注信息'
    default: ''
    placeholder: '可选填...'
```
**当前状态：** 现在玩家可以选择获得数量，并添加备注信息。


当我们创建了`Inputs`组件后，每个组件会对应一个内置键，以上述菜单为例：`diamond_amount`拖动条的值将会在动作中对应`$(diamond_amount)`，`note`输入框的值将会在动作中对应`$(note)`


---

## 🔘 第五步：添加底部按钮

### 5.1 简单确认按钮

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 点击获得 ]'
    actions:
      - 'item: type=give;mats=DIAMOND;amount=$(diamond_amount)' 
      - 'tell: &a执行成功！获得了 $(diamond_amount) 个物品'
      # 使用内置键 $(diamond_amount)，会自动解析为拖动条的值。
      - 'sound: entity.player.levelup'
```

### 5.2 完整按钮配置

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 点击获得 ]'
    actions:
      - 'item: type=give;mats=DIAMOND;amount=$(diamond_amount)'
      - 'tell: &a执行成功！获得了 $(diamond_amount) 个物品'
      # 使用内置键 $(diamond_amount)，会自动解析为拖动条的值。
      - 'tell: &a你填写的备注为：$(note)'
      # 使用内置键 $(note)，会自动解析为输入框的值。
      - 'sound: entity.player.levelup'
  cancel:
    text: '&c[ 取消 ]'
    actions:
      - 'tell: &7已取消购买'
      - 'close'
```

**当前状态：** 现在您的菜单有确认和取消按钮，点击会执行相应的动作。

---

## 🎨 第六步：使用条件判断

### 6.1 根据玩家状态显示不同内容
在KaMenu中，大部分区域均可使用条件判断，以显示定制内容。

- 普通模式：
```yaml
Body:
  player_status:
    type: 'message'
    text: '你好，欢迎来到服务器！'
```
- 使用条件判断
```yaml
Body:
  player_status:
    type: 'message'
    text:
      - condition: '%player_is_op% == true' 
        allow: '&6你好，管理员，欢迎来到服务器！'
        deny: '&7你好，玩家，欢迎来到服务器！'
```

### 6.2 在动作中使用判断条件

```yaml
Bottom:
  type: 'notice'
  confirm:
    text: '&a[ 确认 ]'
    actions:
      - condition: "%player_level% >= 10"
        allow:
          - 'tell: 你的等级大于或等于10级，允许执行该操作...'
            ...
        deny:
          - 'tell: 你的等级小于10级，无法执行该操作...'
            ...
```

**当前状态：** 现在您的菜单会根据玩家的状态做出智能响应。

---

## 🚀 测试您的菜单

### 7.1 重新加载插件

```bash
/km reload
```

### 7.2 打开菜单

```bash
/km open my_first_menu
```

或者使用 Tab 补全：

```bash
/km open <按 Tab 键>
```

### 7.3 测试功能

- ✅ 检查菜单是否正常打开
- ✅ 检查文字和物品是否正确显示
- ✅ 测试滑块是否可以拖动
- ✅ 测试按钮点击是否正常
- ✅ 测试条件判断是否生效

---

## 📚 进阶主题

掌握了基础后，您可以探索更多高级功能：

### 布局和样式
- [🧩 内容组件 (Body)](body.md) - 了解所有可用的组件类型
- [⌨️ 输入组件 (Inputs)](inputs.md) - 探索各种输入组件
- [📋 底部按钮 (Bottom)](bottom.md) - 自定义按钮布局

### 交互和动作
- [🤖 动作 (Actions)](actions.md) - 学习所有可用的动作
- [❓ 条件判断 (Conditions)](conditions.md) - 掌握条件判断的使用

### 其他功能
- [⚙️ 全局设置 (Settings)](setting.md) - 配置菜单行为
- [🎯 事件 (Events)](events.md) - 响应菜单事件
- [🗄️ 数据存储 (Storage)](../data/storage.md) - 使用数据库存储数据

---

## 🎉 恭喜！

您已经成功创建了第一个 KaMenu 菜单！

继续探索更多的功能和可能性，创建更丰富的菜单体验吧！

如有问题，请参考详细文档或联系社区支持。
