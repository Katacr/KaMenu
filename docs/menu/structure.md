# 📁 菜单文件结构

## 📂 文件夹布局

所有菜单文件统一存放在 `plugins/KaMenu/menus/` 目录下，支持任意层级的子文件夹结构：

```
plugins/KaMenu/menus/
├── main_menu.yml           # 根目录菜单
├── server_shop.yml         # 根目录菜单
├── example/                # 示例文件夹
│   └── actions_demo.yml    # 演示菜单
├── shop/                   # 商店文件夹
│   ├── main.yml            # 商店主菜单
│   ├── weapons.yml         # 武器商店
│   └── armor.yml           # 护甲商店
└── admin/                  # 管理员工具文件夹
    └── tools.yml           # 管理工具
```

---

## 🎯 菜单 ID 规则

菜单 ID 由文件路径决定：

- **根目录菜单**：直接使用文件名（不含 `.yml`）
  ```
  /km open main_menu
  /km open server_shop
  ```

- **子文件夹菜单**：使用 `/` 分隔的相对路径
  ```
  /km open example/actions_demo
  /km open shop/weapons
  /km open admin/tools
  ```

---

## ✏️ 添加自定义菜单

1. 在 `plugins/KaMenu/menus/` 下创建 `.yml` 文件（可以按需创建子文件夹）
2. 按照菜单配置格式编写内容（参见后续章节）
3. 执行 `/km reload` 重新加载

**文件命名说明：**
- ✅ 支持中文文件名和文件夹名
- ⚠️ 文件扩展名必须是 `.yml`（不是 `.yaml`）
- ⚠️ 路径分隔符使用 `/`，不使用 `\`

---

## 📝 Tab 补全

输入 `/km open ` 后按 Tab 键，会自动列出所有已加载的菜单 ID，包括子文件夹路径：

```
demo
server_shop
example/actions_demo
shop/weapons
admin/tools
```

---

## 📄 菜单文件基础结构

一个完整的菜单 YAML 文件的基本结构如下：

```yaml
# 菜单标题（支持颜色代码和条件判断）
Title: '&6菜单标题'

# 可选：全局设置
Settings:
  can_escape: true      # 是否允许按 ESC 关闭
  after_action: CLOSE   # 按钮动作执行后的行为

# 可选：菜单事件
Events:
  Open:                # 开启菜单时执行的动作
    - 'tell: &a欢迎！'

# 可选：内容展示区（纯文字、物品展示等）
Body:
  ...

# 可选：输入组件区（文本框、滑块、下拉框、复选框）
Inputs:
  ...

# 可选：底部按钮区（确认/取消/多按钮等）
Bottom:
  type: 'notice'       # notice | confirmation | multi
  ...
```

{% hint style="info" %}
只有 `Title` 节点是必需的，其他所有节点都是可选的。您可以根据需要添加相应的功能。
{% endhint %}

---

## 🎨 菜单文件节点说明

### Title - 菜单标题

必需的顶层节点，定义菜单显示的标题。

**格式：**
- 单行文本：`Title: '&6菜单标题'`
- 条件判断：支持根据不同条件显示不同标题

**示例：**
```yaml
Title: '&6商店'

# 使用条件判断
Title:
  - condition: "%player_is_op% == true"
    allow: '&4管理员商店'
    deny: '&6普通商店'
```

### Settings - 全局设置

配置菜单的全局行为参数。

**配置项：**

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `can_escape` | `Boolean` | `true` | 是否允许玩家通过 ESC 键关闭菜单 |
| `after_action` | `String` | `CLOSE` | 点击按钮执行动作后的客户端行为 |

**详细说明和示例：** 详见 [⚙️ 全局设置 (Settings)](setting.md)

### Events - 菜单事件

定义菜单在特定时刻执行的动作。

**支持的事件：**

| 事件名 | 触发时机 |
|--------|---------|
| `Open` | 玩家打开菜单时 |
| `Close` | 玩家关闭菜单时 |

**示例：**
```yaml
Events:
  Open:
    - 'tell: &a欢迎来到菜单！'
    - 'sound: entity.experience_orb.pickup'
  Close:
    - 'tell: &7再见！'
```

**详细说明和示例：** 详见 [🎯 菜单事件 (Events)](events.md)

### Body - 内容展示区

在菜单主体区域显示各种内容，如纯文字消息和物品展示。

**组件类型：**
- `message` - 纯文字消息
- `item` - 物品展示

**详细说明和示例：** 详见 [🧩 内容组件 (Body)](body.md)

### Inputs - 输入组件区

提供用户输入组件，如文本框、滑块、下拉框等。

**组件类型：**
- `input` - 文本输入框
- `slider` - 滑块
- `dropdown` - 下拉选择框
- `checkbox` - 复选框

**详细说明和示例：** 详见 [⌨️ 输入组件 (Inputs)](inputs.md)

### Bottom - 底部按钮区

配置菜单底部的按钮，支持多种布局类型。

**布局类型：**
- `notice` - 通知类型（单个确认按钮）
- `confirmation` - 确认类型（确认和取消按钮）
- `multi` - 多按钮类型（自定义多个按钮）

**详细说明和示例：** 详见 [📋 底部按钮 (Bottom)](bottom.md)

---

## 🚀 下一步

了解菜单文件结构后，您可以：

1. **创建您的第一个菜单**：查看 [📝 创建菜单教程](creating_menu.md)
2. **深入了解各个组件**：阅读对应的详细文档
3. **探索高级功能**：条件判断、数据存储、动作系统等

{% hint style="success" %}
建议从 [📝 创建菜单教程](creating_menu.md) 开始，跟随教程一步步创建您的第一个菜单！
{% endhint %}
