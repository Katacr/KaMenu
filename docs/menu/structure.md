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
# 可选设置
Events:
  Open:      # 开启菜单时执行的动作
    ...

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

**配置项：**

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `can_escape` | `Boolean` | `true` | 是否允许玩家通过 ESC 键关闭菜单 |
| `after_action` | `String` | `CLOSE` | 点击按钮执行动作后的客户端行为 |

**详细说明和示例：** 详见 [⚙️ 全局设置 (Settings)](setting.md)

---



