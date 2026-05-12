 # KaMenu v1.4.2 更新报告

## 📋 版本信息
- **版本号**: 1.4.2

---

## ✨ 新增功能

### 1. 新增 `hide_text` 属性

**功能说明:**
现在 `input` 与 `dropdown` 输入组件支持 `hide_text: true/false` 配置，用于控制输入项自身的标题文字是否显示。

**适用组件:**
- `type: 'input'`
- `type: 'dropdown'`

**使用场景:**
- 已经在 `Body.message` 中写了说明文字，不希望输入组件再重复显示一遍标题
- 想让 Dialog 界面更紧凑，减少输入区的视觉占用
- 只保留输入控件本体，让说明和交互分层更清晰

**示例:**
```yaml
Inputs:
  feedback:
    type: 'input'
    text: '&7反馈内容'
    hide_text: true
    default: '请在此输入...'

  color:
    type: 'dropdown'
    text: '&b选择颜色'
    hide_text: true
    options:
      - 'red'
      - 'blue'
```

**效果说明:**
- `hide_text: true`：隐藏输入项标题文字
- `hide_text: false` 或不填写：保持默认显示

---

## 🔧 优化改进

### 2. 优化 dropdown 的 `options` 设计

**问题背景:**
旧版 `dropdown` 的 `options` 只能直接写字符串列表，导致“玩家看到的文字”和“动作里真正提交的值”必须完全相同。

例如旧写法中：
```yaml
options:
  - '&c红色'
  - '&a绿色'
```

这样虽然显示没问题，但后续动作里拿到的 `$(color_select)` 也会是带颜色文本，不利于条件判断、数据存储和逻辑分支处理。

**本次优化:**
新增字符串分隔格式：

```yaml
options:
  - 'red => &c红色'
  - 'green => &a绿色'
  - 'blue => &b蓝色'
```

含义如下：
- 左侧 `red / green / blue`：作为实际提交值（id）
- 右侧 `&c红色 / &a绿色 / &b蓝色`：作为玩家看到的显示文本（display）

**效果说明:**
- 玩家界面看到的是更友好的显示文本
- 动作中的 `$(变量名)` 拿到的是稳定的 id 值

例如：
```yaml
- condition: '$(color_select) == "red"'
```
即使显示文本改成 `&4深红色`，判断逻辑也不会受影响。

---

## ✅ 兼容性说明

### 旧格式仍然完全兼容

原有写法仍可继续使用：
```yaml
options:
  - 'red'
  - 'green'
  - 'blue'
```

此时行为为：
- `id = red`
- `display = red`

也就是说，不更新旧配置也不会报错。

---

### 条件判断写法仍然兼容

由于 KaMenu 当前的 `options` 本身支持条件判断列表，因此本次没有采用 `Map{id,display}` 的新结构，而是使用字符串分隔格式，确保与现有条件系统兼容。

例如下面这种写法依然有效：
```yaml
Inputs:
  color_select:
    type: 'dropdown'
    text: '&b选择颜色'
    hide_text: true
    options:
      - condition: "%player_is_op% == true"
        allow:
          - 'red => &cOP-红色'
          - 'green => &aOP-绿色'
          - 'blue => &bOP-蓝色'
          - 'yellow => &eOP-黄色'
        deny:
          - 'red => &c玩家-红色'
          - 'green => &a玩家-绿色'
          - 'blue => &b玩家-蓝色'
          - 'yellow => &e玩家-黄色'
```

**最终效果:**
- 不同玩家可以看到不同的显示文本
- 但动作中得到的值始终是统一的 `red / green / blue / yellow`

---

## 📝 配置示例

### input + hide_text

```yaml
Inputs:
  report:
    type: 'input'
    text: '&7问题描述'
    hide_text: true
    default: '请在此输入你遇到的问题...'
    multiline:
      max_lines: 5
      height: 80
```

### dropdown + id/display 分离

```yaml
Inputs:
  profession:
    type: 'dropdown'
    text: '&e选择职业'
    default_id: 'mage'
    options:
      - 'warrior => &c战士'
      - 'mage => &b法师'
      - 'archer => &a弓箭手'
```

### dropdown + 条件判断

```yaml
Inputs:
  rank_color:
    type: 'dropdown'
    text: '&e选择称号颜色'
    options:
      - condition: 'hasPerm.kamenu.admin'
        allow:
          - 'gold => &6管理员金色'
          - 'red => &c管理员红色'
        deny:
          - 'green => &a玩家绿色'
          - 'blue => &b玩家蓝色'
```

---

## ⚠️ 注意事项

- `hide_text` 当前仅作用于 `input` 和 `dropdown`
- `dropdown` 新格式使用 `=>` 分隔 id 与 display，建议左右各保留空格，便于阅读
- 若未写 `=>`，则默认“显示什么，提交什么”
- 条件判断中的 `allow` / `deny` 依然应返回字符串列表，只是现在每个字符串可以写成 `id => display`

---

## 🚀 升级指南

### 从 1.4.1 升级到 1.4.2

**升级步骤:**

1. **更新插件**
   - 下载最新的 `KaMenu-1.4.2.jar`
   - 替换旧版本 jar 文件

2. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器

3. **（可选）更新配置**
   - 如需隐藏输入标题，可为 `input` / `dropdown` 添加 `hide_text: true`
   - 如需区分 dropdown 的显示文本与提交值，可将选项写成 `id => display`

**兼容性:**
- ✅ **完全兼容旧配置**：旧版 dropdown options 字符串列表可继续使用
- ✅ **完全兼容条件判断**：现有 `condition / allow / deny` 结构无需重写
- ✅ **新增属性按需使用**：不修改配置也不会影响原有菜单运行

---

## 📈 版本对比

| 功能 | 1.4.1 | 1.4.2 |
|------|-------|-------|
| input 的 `hide_text` | ❌ | ✅ |
| dropdown 的 `hide_text` | ❌ | ✅ |
| dropdown 显示文本与提交值分离 | ❌ | ✅ |
| dropdown 条件 options 兼容分离设计 | ❌ | ✅ |

---

## 📚 相关文档

- **菜单配置文档**: `docs/menu/`
- **示例菜单**: `menus/example/` - 中文示例
- **示例菜单**: `menus/exampleEN/` - 英文示例

---

# KaMenu v1.4.2 Update Report

## 📋 Version Info
- **Version**: 1.4.2

---

## ✨ New Features

### 1. Added `hide_text` Property

**Description:**
`input` and `dropdown` components now support `hide_text: true/false` to control whether the input's own label text should be shown.

**Supported Components:**
- `type: 'input'`
- `type: 'dropdown'`

**Use Cases:**
- You already explained the input in `Body.message` and don't want the label repeated
- You want a cleaner and more compact dialog layout
- You want to keep only the input control visible while moving descriptions into the body area

**Example:**
```yaml
Inputs:
  feedback:
    type: 'input'
    text: '&7Feedback'
    hide_text: true
    default: 'Type here...'

  color:
    type: 'dropdown'
    text: '&bChoose a color'
    hide_text: true
    options:
      - 'red'
      - 'blue'
```

**Behavior:**
- `hide_text: true`: hides the label text
- `hide_text: false` or omitted: keeps the label visible

---

## 🔧 Improvements

### 2. Improved dropdown `options` Design

**Background:**
Previously, dropdown `options` only supported plain string lists, meaning the text shown to players had to be exactly the same as the submitted value used in actions.

**New Format:**
You can now separate the internal id from the display text using `=>`:

```yaml
options:
  - 'red => &cRed'
  - 'green => &aGreen'
  - 'blue => &bBlue'
```

This means:
- Left side (`red`, `green`, `blue`) = submitted value / id
- Right side (`&cRed`, `&aGreen`, `&bBlue`) = text shown to players

**Practical Effect:**
- Players see user-friendly names
- Actions and conditions receive stable ids

Example:
```yaml
- condition: '$(color_select) == "red"'
```
Even if the displayed text changes later, your logic remains stable.

---

## ✅ Compatibility Notes

### Old Format Still Works

The previous format remains fully supported:
```yaml
options:
  - 'red'
  - 'green'
  - 'blue'
```

In this case:
- `id = red`
- `display = red`

So no existing dropdown config needs to be changed.

---

### Conditional Options Still Work

Because KaMenu already supports conditional list resolution inside `options`, this update intentionally uses a **string-based separator format** instead of a `Map{id, display}` structure.

That keeps existing conditional dropdown config fully compatible:
```yaml
Inputs:
  color_select:
    type: 'dropdown'
    text: '&bChoose a color'
    hide_text: true
    options:
      - condition: "%player_is_op% == true"
        allow:
          - 'red => &cOP-Red'
          - 'green => &aOP-Green'
        deny:
          - 'red => &cPlayer-Red'
          - 'green => &aPlayer-Green'
```

**Result:**
- Different players may see different display text
- But the submitted values remain stable ids like `red` and `green`

---

## 📝 Configuration Examples

### input + hide_text

```yaml
Inputs:
  report:
    type: 'input'
    text: '&7Issue Description'
    hide_text: true
    default: 'Describe your issue here...'
    multiline:
      max_lines: 5
      height: 80
```

### dropdown + separated id/display

```yaml
Inputs:
  profession:
    type: 'dropdown'
    text: '&eChoose a class'
    default_id: 'mage'
    options:
      - 'warrior => &cWarrior'
      - 'mage => &bMage'
      - 'archer => &aArcher'
```

### dropdown + conditional options

```yaml
Inputs:
  rank_color:
    type: 'dropdown'
    text: '&eChoose title color'
    options:
      - condition: 'hasPerm.kamenu.admin'
        allow:
          - 'gold => &6Admin Gold'
          - 'red => &cAdmin Red'
        deny:
          - 'green => &aPlayer Green'
          - 'blue => &bPlayer Blue'
```

---

## ⚠️ Notes

- `hide_text` currently applies only to `input` and `dropdown`
- The new dropdown format uses `=>` to separate id and display text
- If `=>` is not present, KaMenu treats the option as `id = display`
- Conditional `allow` / `deny` blocks should still return string lists, but each string can now be written as `id => display`

---

## 🚀 Upgrade Guide

### Upgrading from 1.4.1 to 1.4.2

**Steps:**

1. **Update the plugin**
   - Download the latest `KaMenu-1.4.2.jar`
   - Replace the old jar file

2. **Restart the server**
   - Fully stop the server
   - Start it again

3. **(Optional) Update config**
   - Add `hide_text: true` to `input` / `dropdown` if needed
   - Use `id => display` if you want dropdown display text and submitted value to differ

**Compatibility:**
- ✅ Fully compatible with old dropdown string lists
- ✅ Fully compatible with conditional `options`
- ✅ No mandatory config changes required

---

## 📈 Version Comparison

| Feature | 1.4.1 | 1.4.2 |
|---------|-------|-------|
| `hide_text` for input | ❌ | ✅ |
| `hide_text` for dropdown | ❌ | ✅ |
| Separate dropdown display text and submitted value | ❌ | ✅ |
| Conditional dropdown options compatible with separated format | ❌ | ✅ |

---

## 📚 Related Docs

- **Menu configuration docs**: `docs/menu/`
- **Example menus**: `menus/example/` - Chinese examples
- **Example menus**: `menus/exampleEN/` - English examples
