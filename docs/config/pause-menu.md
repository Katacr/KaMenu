# ESC 暂停菜单: pause_menu.yml

`plugins/KaMenu/pause_menu.yml` 用于设计原版 ESC 暂停菜单中的 KaMenu 入口 Dialog。插件启动时会检查该文件，不存在时自动释放默认模板，已经存在的文件不会被覆盖。

编辑完成后执行：

```bash
/km pause register
```

KaMenu 会把该文件编译到 `world/datapacks/KaMenuPauseEntry`。必须完整重启服务器，客户端 ESC 暂停菜单才会更新。

{% hint style="info" %}
KaMenu 固定只注册一个 ESC 入口，避免原版把多个入口自动合并为二级菜单。需要多个操作时，请在 `Bottom.type: multi` 内配置按钮矩阵。
{% endhint %}

## 完整结构

```yaml
Title: '&bKaMenu'
External-Title: '服务器菜单'

Settings:
  can_escape: false
  pause: false
  after_action: NONE

Body:
  website_link:
    type: message
    width: 340
    text: '<text="&b[ 访问官网 ]";hover="&a点击打开官网";url=https://example.com>'

  welcome:
    type: message
    width: 300
    text:
      - '&f欢迎使用 KaMenu！'
      - '&7请在下方选择功能。'

  guide_item:
    type: item
    material: PAPER
    amount: 1
    description: '&7KaMenu 菜单指南'
    description_width: 200
    show_overlays: true
    show_tooltip: true
    width: 32
    height: 32

Inputs:
  player_name:
    type: input
    text: '&e玩家名'
    default: 'Steve'
    max_length: 16
    remove_chars: global

Bottom:
  type: multi
  columns: 3
  buttons:
    main:
      text: '&a打开菜单'
      tooltip: '&7打开 KaMenu 主菜单'
      width: 160
      menu: 'example/main_menu'
    website:
      text: '&b官方网站'
      tooltip: '&7在浏览器中打开'
      width: 160
      url: 'https://example.com'
    address:
      text: '&e复制地址'
      width: 160
      copy: 'play.example.com'
    greet:
      text: '&d发送问候'
      width: 160
      actions:
        - 'tell: &a你好，$(player_name)！'
        - 'close'
  exit:
    text: '&c关闭'
    tooltip: '&7返回游戏'
    width: 160
```

## 顶层字段

| 字段 | 说明 |
|------|------|
| `Title` | Dialog 内部标题，支持 Legacy 颜色和 MiniMessage |
| `External-Title` | 客户端 ESC 暂停菜单中的入口文本；未填写时使用 `Title` |
| `Settings` | 原版 Dialog 行为设置 |
| `Body` | 按 YAML 顺序显示的静态正文组件 |
| `Inputs` | 静态输入组件；提交后可在 `actions` 中通过 `$(key)` 读取 |
| `Bottom` | `notice`、`confirmation` 或 `multi` 底部按钮 |

## Settings

```yaml
Settings:
  can_escape: false
  pause: false
  after_action: NONE
```

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `can_escape` | `true` | 是否允许按 ESC 关闭该 Dialog |
| `pause` | `false` | 是否暂停单人游戏；多人服务器通常保持 `false` |
| `after_action` | `CLOSE` | 点击后的客户端行为：`CLOSE`、`NONE`、`WAIT_FOR_RESPONSE` |

`pause: true` 不能与 `after_action: NONE` 同时使用，原版 Dialog 编解码器会拒绝该组合。

## Body

暂停菜单支持静态 `message` 和 `item` 两种 Body。Body 不会解析 PAPI、KaMenu 变量、条件、JS 或动态列表。

### message

```yaml
Body:
  info:
    type: message
    width: 340
    text:
      - '&f普通文本'
      - '<text="&b[ 官网 ]";hover="&7点击打开";url=https://example.com>'
```

`text` 支持字符串或字符串列表，并复用 KaMenu 的 Legacy、MiniMessage 和 `<text=...>` 文本解析。静态暂停菜单中的 `<text>` 支持 `hover`、`copy`、`url`、`command` 和 `newline`；不支持需要运行期玩家上下文的 `actions` 与 `hover_item`。

### item

```yaml
Body:
  icon:
    type: item
    material: DIAMOND
    amount: 3
    description: '&b奖励物品'
    description_width: 200
    show_overlays: true
    show_tooltip: true
    width: 32
    height: 32
```

| 字段 | 默认值 | 说明 |
|------|--------|------|
| `material` | `PAPER` | 原版 Bukkit 材质；不支持玩家槽位引用 |
| `amount` | `1` | 显示数量，范围 1-99 |
| `description` | 无 | 物品下方的静态描述文本 |
| `description_width` | `200` | 描述宽度，范围 1-1024 |
| `show_overlays` | `true` | 是否显示数量、耐久等装饰层 |
| `show_tooltip` | `true` | 是否显示原版物品悬浮提示 |
| `width` / `height` | `16` | 物品显示尺寸，范围 1-256 |

数据包物品 Body 当前不支持运行期物品、`name`、`lore`、玩家头颅、`item_model` 或自定义模型数据。

## Inputs

暂停菜单支持与普通菜单相同的四种标准输入：`input`、`slider`、`dropdown` 和 `checkbox`。输入定义、标签、默认值和选项在执行 `register` 时静态编译，不支持条件、PAPI 或 JS 动态生成。

```yaml
Inputs:
  player_name:
    type: input
    text: '&e玩家名'
    default: 'Steve'
    max_length: 16
    width: 250
    remove_chars: global

  amount:
    type: slider
    text: '&e数量'
    min: 1
    max: 10
    step: 1
    default: 2
    format: '%s: %s'

  server:
    type: dropdown
    text: '&b服务器'
    options:
      - 'survival => &a生存服'
      - 'lobby => &e大厅'
    default_id: survival
    width: 200

  notify:
    type: checkbox
    text: '&a开启通知'
    default: true
    on_true: enabled
    on_false: disabled
```

点击配置了 `actions` 的按钮时，KaMenu 会从客户端响应读取输入。动作中使用 `$(player_name)`、`$(amount)`、`$(server)` 和 `$(notify)`。文本输入继续遵循 `config.yml` 的 `input-capture.trim-edge-spaces` 和 `remove_chars` 清理规则。

字段与普通菜单一致：文本输入支持 `hide_text`、`default`、`max_length`、`width`、`multiline` 和 `remove_chars`；滑块支持 `min`、`max`、`step`、`default`、`format` 和 `width`；单项选择按钮支持 `options`、`default_id`、`hide_text` 和 `width`；复选框支持 `default`、`on_true` 和 `on_false`。

`menu`、`url`、`copy`、`command` 不会把输入传入动作系统；需要使用输入时必须配置 `actions`，再在队列中使用 `open:`、`command:` 等动作。

## Bottom

按钮公共字段：

| 字段 | 说明 |
|------|------|
| `text` | 按钮文本 |
| `tooltip` | 悬浮提示 |
| `width` | 按钮宽度，范围 1-1024 |
| `menu` | 回调 KaMenu 并打开指定菜单 ID |
| `actions` | 回调 KaMenu 并执行标准动作列表，支持条件、JS、动作包和 `$(key)` |
| `url` | 客户端打开 URL |
| `copy` | 客户端复制文本 |
| `command` | 客户端以玩家身份执行命令 |

每个按钮最多配置 `menu`、`actions`、`url`、`copy`、`command` 中的一项。客户端只提交固定按钮 ID，实际菜单 ID 和动作列表以服务器注册时缓存的 `pause_menu.yml` 为准。

### notice

```yaml
Bottom:
  type: notice
  confirm:
    text: '&a提交'
    actions:
      - condition: 'isNull.$(player_name)'
        allow:
          - 'toast: type=task;msg=请输入名称;icon=barrier'
          - 'return'
      - 'tell: &a你好，$(player_name)！'
      - 'close'
```

暂停菜单按钮的 `actions` 使用完整 KaMenu 动作执行器。它支持嵌套条件、`wait`、`return`、JavaScript、全局动作包，以及同一 `pause_menu.yml` 内的 `Events.Click` 动作包。`reset` 因暂停菜单不属于 `MenuManager`，会被安全忽略；需要刷新时可用 `open:` 打开普通 KaMenu 菜单，或修改文件后重新注册并重启服务器。

### confirmation

```yaml
Bottom:
  type: confirmation
  confirm:
    text: '&a确认'
    menu: 'example/main_menu'
  deny:
    text: '&c取消'
```

### multi

```yaml
Bottom:
  type: multi
  columns: 2
  buttons:
    menu:
      text: '&a菜单'
      menu: 'example/main_menu'
    website:
      text: '&b官网'
      url: 'https://example.com'
  exit:
    text: '&c关闭'
```

## 管理指令

```bash
/km pause register
/km pause unregister
/km pause info
```

- `register`：重新读取 `pause_menu.yml`，校验并生成数据包。
- `unregister`：移除 KaMenu 生成的数据包。
- `info`：显示源文件和数据包路径及存在状态。

{% hint style="warning" %}
`/km reload` 不会重新注册原版数据包 Dialog。每次执行 `register` 或 `unregister` 后都必须完整重启服务器。
{% endhint %}
