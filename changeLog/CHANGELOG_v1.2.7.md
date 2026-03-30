# KaMenu v1.2.7 更新报告

## 📋 版本信息
|- **版本号**: 1.2.7
|- **发布日期**: 2026年3月30日

---

## ✨ 新增功能

### 1. MiniMessage 完整支持优化

#### 功能说明

优化了 `tell` 和 `actionbar` 动作，现在完整支持 Adventure MiniMessage 的所有功能，包括：
- 颜色和格式化标签（`<red>`, `<bold>`, `<italic>` 等）
- 渐变色标签（`<gradient:red:blue>`）
- 点击事件标签（`<click:run_command:xxx>`, `<click:open_url:xxx>` 等）
- 悬停事件标签（`<hover:show_text:xxx>`, `<hover:show_item:xxx>` 等）
- 其他高级标签（`<key>`, `<score>`, `<newline>`, `<nbt>` 等）

#### 技术改进

**之前的问题：**
- `parseText()` 方法会将 MiniMessage 先序列化为 Legacy 格式，再解析回来
- 这个过程会丢失 MiniMessage 的高级特性（点击、悬停等）
- 导致 `<click>` 和 `<hover>` 标签在 `tell` 动作中无法正常工作

**优化方案：**
- 检测文本是否包含 MiniMessage 标签（使用正则表达式）
- 如果包含，直接使用 `MiniMessage.miniMessage().deserialize()` 解析
- 否则，使用 Legacy 序列化器解析颜色代码
- 保留所有 MiniMessage 的高级功能

#### 颜色代码兼容性优化

**新增功能：** 支持 Legacy 颜色代码和 MiniMessage 标签的混合使用

**问题背景：**
- MiniMessage 不支持直接包含 Legacy 颜色代码（`&a`、`§a` 等）
- 当文本同时包含两种格式时，MiniMessage 会抛出异常

**解决方案：**
- 创建 Legacy 到 MiniMessage 的颜色代码映射表
- 当检测到 MiniMessage 标签时，先将 Legacy 颜色代码转换为对应的 MiniMessage 标签
- 然后再用 MiniMessage 解析器解析

**转换示例：**
```
&a    → <green>
&c    → <red>
&l    → <bold>
&o    → <italic>
&r    → <reset>
```

**使用示例：**
```yaml
# 物品名称混合使用
Body:
  items:
    test:
      material: diamond
      name: '&c<bold>稀有物品</bold>'
      lore:
        - '&7这是 &a传说 &7物品'

# 按钮文本混合使用
Bottom:
  type: 'multi'
  buttons:
    test:
      text: '<bold><sprite:block/emerald_block> &a绿宝石方块</bold>'
```

**兼容性：**
- ✅ **完全兼容**：所有旧的 Legacy 颜色代码配置继续正常工作
- ✅ **智能转换**：自动检测并转换 Legacy 颜色代码
- ✅ **灵活使用**：可以自由混合使用两种格式

#### 使用示例

```yaml
# 点击事件
- 'tell: <click:run_command:/say 你点击了这里！><gold>点我！</gold></click>'
- 'tell: <click:copy_to_clipboard:Hello KaMenu><gold>复制这段文字</gold></click>'
- 'tell: <click:open_url:https://minecraft.wiki><gold>打开Minecraft Wiki</gold></click>'

# 悬停事件
- 'tell: <hover:show_text:"<red>这是悬停文字<reset>\n<blue>支持多行显示"><gold>把鼠标放上来！</gold></hover>'
- 'tell: <hover:show_item:diamond_sword>显示钻石剑</hover>'

# 组合使用
- 'tell: <click:run_command:/say 联合事件><hover:show_text:"<green>点击执行指令"><gold>点击并悬停</gold></hover></click>'
```

#### 兼容性

- ✅ **向后兼容**：现有的 Legacy 颜色代码（`&a` 等）继续正常工作
- ✅ **智能检测**：自动识别 MiniMessage 标签并使用相应的解析器
- ✅ **全场景支持**：`tell`、`actionbar`、`title`、`hovertext` 等所有文本动作都受益

---

### 2. 点击执行动作列表功能

#### 功能说明

现在可以通过点击文本或按钮来执行 `Events.Click` 下定义的动作列表。这允许你在配置中复用已定义的动作序列，避免重复代码。

#### 使用方式

**方式一：通过 <text> 标签的 actions 参数**

```yaml
Events:
  Click:
    greet:
      - 'tell: &a你好！'
      - 'sound: ENTITY_PLAYER_LEVELUP'

Body:
  text:
    type: 'message'
    text: '<text="点击问候";actions=greet;hover=点击执行问候>'
```

**方式二：通过 actions 动作**

```yaml
Events:
  Click:
    vip_check:
      - condition: 'hasPerm.kamenu.vip'
        allow:
          - 'tell: &aVIP 欢迎你'
        deny:
          - 'tell: &c你需要 VIP 权限'

Bottom:
  type: 'multi'
  buttons:
    btn_vip:
      text: 'VIP 检查'
      actions:
        - 'actions: vip_check'
```

#### 特性

- **异步执行**：动作列表在异步线程中执行，不会阻塞主线程
- **支持条件判断**：引用的动作列表中可以使用 `condition` 进行条件分支
- **变量支持**：动作列表中支持所有 KaMenu 变量（`{data:xxx}`, `{permission:xxx}` 等）
- **复用代码**：避免在多个按钮中重复定义相同的动作序列
- **错误处理**：动作列表不存在时会提示玩家错误信息

#### 使用场景

1. **按钮复用动作列表**：多个按钮执行相同的动作序列
2. **条件分支**：根据玩家状态执行不同动作
3. **命令快捷方式**：通过命令触发预定义的动作列表
4. **动作链复用**：避免重复定义复杂的动作序列

#### 配置语法

**<text> 标签支持的参数：**

| 参数 | 说明 | 必需 |
|------|------|------|
| `text` | 可点击的显示文字 | ✅ |
| `hover` | 鼠标悬停时显示的提示文字 | ❌ |
| `command` | 点击时玩家执行的指令 | ❌ |
| `url` | 点击时打开的链接 | ❌ |
| `actions` | 点击时执行的动作列表（Events.Click 下的键名）| ❌ |
| `newline` | 是否在文字后换行（`true`/`false`）| ❌ |

**点击事件优先级：**

当同时存在多个点击参数时，优先级如下（从高到低）：
1. `actions` - 执行动作列表
2. `url` - 打开链接
3. `command` - 执行指令

---

## 🐛 Bug 修复

### 1. 按钮无 Actions 时点击出错

**问题描述：**

当按钮配置中没有 `actions` 列表时，点击按钮会抛出错误。之前的代码返回执行空命令 `/empty`，这不是最佳解决方案。

**修复方案：**

修改 `buildActionFromConfig()` 方法，当按钮没有 actions 列表时，返回一个真正的"无操作"动作：

```kotlin
// 修复前
return DialogAction.staticAction(ClickEvent.runCommand("/empty"))

// 修复后
return DialogAction.customClick({ _, _ -> }, ClickCallback.Options.create())
```

**修复内容：**

1. ✅ **空 actions 列表**：返回无操作动作
2. ✅ **未知 Map 类型**：返回无操作动作
3. ✅ **按钮正常显示**：没有 actions 的按钮仍可正常显示和交互
4. ✅ **点击无反应**：点击时不会执行任何操作，不会出错

**使用示例：**

```yaml
Bottom:
  type: multiAction
  columns: 2
  buttons:
    no_actions:
      text: '&c无Actions按钮'
      tooltip:
        - '&7这个按钮没有 actions 列表'
        - '&a点击不会有任何反应'
      # 注意：没有 actions 字段
    
    pure_display:
      text: '<sprite:block/emerald> &a<bold>纯显示按钮</bold>'
      tooltip:
        - '<gray>使用 MiniMessage 格式'
        - '<green>没有 actions 也能正常显示'
      # 注意：只有 tooltip，没有 actions
```

**兼容性：**

- ✅ **向后兼容**：所有现有配置继续正常工作
- ✅ **优雅降级**：没有 actions 的按钮不会导致错误
- ✅ **文档更新**：更新文档说明 actions 字段是可选的

---

## 📚 文档更新

### 1. actions.md 文档更新

- 新增 `tell` 动作的 MiniMessage 完整支持说明
- 新增 `actionbar` 动作的 MiniMessage 完整支持说明
- 添加 MiniMessage 常用标签表格（颜色、渐变、点击、悬停等）
- 添加丰富的 MiniMessage 使用示例（点击事件、悬停事件、组合使用等）
- 新增 `actions` 动作类型的详细说明
- 更新 `hovertext` 动作说明，添加 `actions` 参数支持
- 添加使用场景对比表格
- 添加完整配置示例

### 2. body.md 文档更新

- 更新 `<text>` 标签语法说明，添加 `actions` 参数
- 添加点击事件优先级说明
- 新增使用 `actions` 参数的示例

### 3. README.md 文档更新

- 在核心特性中添加 `actions` 动作说明

---

## 🐛 兼容性说明

- ✅ **向后兼容**：所有现有配置文件无需修改即可正常使用
- ✅ **Paper 1.21.6+**：使用 Paper Dialog API 和 ClickCallback 功能
- ✅ **Java 17+**：要求 Java 17 或更高版本

---

## 📝 配置示例

### 完整示例：文本点击 + 按钮点击

```yaml
Title: 点击动作演示

Events:
  Click:
    # 文本点击使用的动作列表
    simple_greet:
      - 'tell: &a你好！这是通过点击文本触发的问候。'
      - 'sound: ENTITY_PLAYER_LEVELUP'

    # 按钮点击使用的动作列表
    vip_reward:
      - condition: 'hasPerm.kamenu.vip'
        allow:
          - 'tell: &a欢迎 VIP 用户！'
          - 'tell: &e获得 100 金币奖励！'
          - 'console: eco give %player_name% 100'
        deny:
          - 'tell: &c你需要 VIP 权限才能领取奖励'

Body:
  text:
    type: 'message'
    text: |
      点击文本测试：
      <text='简单问候';actions=simple_greet;hover='点击发送问候'>
      <text='VIP 奖励';actions=vip_reward;hover='检查 VIP 并领取奖励'>

Bottom:
  type: 'multi'
  buttons:
    btn_greet:
      text: '按钮问候'
      actions:
        - 'actions: simple_greet'  # 使用 actions 动作

    btn_vip:
      text: 'VIP 奖励'
      actions:
        - 'actions: vip_reward'

    exit:
      text: '关闭'
      actions:
        - 'close'
```

---

## 🔨 技术细节

### 1. ClickCallback 实现

使用 Paper 的 `ClickCallback` API 实现点击回调功能：
- 回调函数在服务器端执行
- 支持设置使用次数和生命周期
- 默认有效期为 5 分钟

### 2. 异步执行

- `actions` 动作使用 `Bukkit.getScheduler().runTaskAsynchronously()` 异步执行
- `ClickCallback` 回调本身也支持异步执行
- 不阻塞主线程，确保服务器性能

### 3. 动作列表加载

- 从 `Events.Click.<动作列表名>` 路径加载配置
- 支持所有现有动作类型
- 支持嵌套的条件判断
- 支持变量替换

---

## ⚠️ 注意事项

1. 动作列表必须在 `Events.Click` 下定义
2. 避免循环引用（如动作列表 A 引用自己）
3. `actions` 参数仅在 Body.message 文本组件和按钮 actions 列表中有效
4. ClickCallback 有效期为 5 分钟，超时后会自动失效
5. 如果引用的动作列表不存在，玩家会收到错误提示

---

## 💡 提示

- 使用 `actions` 功能可以大大简化配置文件
- 建议将常用的动作序列定义在 `Events.Click` 下，然后通过 `actions` 动作或参数引用
- 对于简单的操作，可以直接使用 `command` 或 `url` 参数
- 对于复杂的操作（多条动作、条件判断），使用 `actions` 参数更合适

---

## 🙏 致谢

感谢所有使用和测试 KaMenu 的用户！您的反馈是我们不断改进的动力。
