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

## 📄 菜单文件整体结构

一个完整的菜单 YAML 文件由以下几个顶层节点组成：

```yaml
# 菜单标题（支持颜色代码和条件判断）
Title: '&6菜单标题'
```
```yaml
# 可选设置
Settings:
  can_escape: true      # 是否允许按 ESC 关闭
  after_action: CLOSE   # 按钮动作执行后的行为

```
```yaml
# 内容展示区（纯文字、物品展示等）
Body:
  ...

```
```yaml
# 输入组件区（文本框、滑块、下拉框、复选框）
Inputs:
  ...

```
```yaml
# 底部按钮区（确认/取消/多按钮等）
Bottom:
  type: 'notice'   # notice | confirmation | multi
   ...

```

各节点的详细说明请参见后续章节。

---

## ⚙️ Settings 节点

`Settings` 节点用于配置菜单的全局行为参数。

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `can_escape` | `Boolean` | `true` | 是否允许玩家通过 ESC 键关闭菜单 |
| `after_action` | `String` | `CLOSE` | 点击按钮执行动作后的客户端行为 |

### can_escape 参数

控制菜单的关闭方式。

**值说明：**
- `true`（默认）：玩家可通过 ESC 键关闭菜单，关闭时将执行对应按钮的动作（`multi` 类型为 `exit` 按钮，`confirmation` 类型为 `deny` 按钮）
- `false`：玩家必须点击指定按钮才能关闭菜单，禁用 ESC 退出功能

**示例配置：**

```yaml
Settings:
  can_escape: false  # 强制玩家必须点击按钮关闭菜单
```

### after_action 参数

定义玩家点击按钮后，客户端在等待服务器响应期间的行为。

**背景说明：**
服务器与客户端之间的网络通信存在一定的延迟。虽然正常情况下该延迟仅为几十毫秒，但在网络环境较差或服务器负载较高时可能达到 1 秒或更长。在此期间，如果玩家对游戏世界进行操作（如移动物品、丢弃物品等），可能导致后续菜单逻辑与实际状态不一致，引发异常行为。

`after_action` 参数用于声明在客户端本地执行指定操作，在此期间即使服务器爆炸或网络中断，玩家点击后仍会执行相关的本地操作。防止玩家在服务器响应期间进行非法操作，确保菜单逻辑的完整性。

**可选值：**
- `CLOSE`（默认）：客户端直接关闭菜单界面
- `NONE`：点击按钮后客户端不执行任何本地行为
- `WAIT_FOR_RESPONSE`：客户端显示遮罩界面，等待服务器响应后继续操作

**适用场景：**
- **默认使用 `NONE`**：点击按钮后客户端不执行任何本地行为，适用大部分场景，点击后的动作均由服务器决定
- **推荐使用 `CLOSE`**：没有二级菜单或无需处理后续行为的场景
- **建议使用 `WAIT_FOR_RESPONSE`**：有二级菜单、涉及重要操作（如交易、权限变更）且需要防止网络延迟不稳定/TPS较低情况的场景

{% hint style="info" %}
使用 `WAIT_FOR_RESPONSE` 时，若按钮没有二级菜单功能，必须在动作列表中添加 `close` 动作。否则客户端将持续显示遮罩界面。
{% endhint %}

**示例配置：**

```yaml
Settings:
  after_action: WAIT_FOR_RESPONSE  # 防止玩家在服务器响应期间进行非法操作

```


