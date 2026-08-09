# 创建容器类菜单教程

本教程从零创建一个可直接打开的三行箱子菜单。完成后，你将了解容器类菜单的文件位置、顶层结构、布局槽位、按钮显示、点击动作、重载和常见错误。

## 学习目标

- ✅ 创建容器类菜单文件并确定菜单 ID
- ✅ 选择正确的容器 `Type`
- ✅ 使用 `Layout` 安排库存槽位
- ✅ 使用 `Buttons` 创建物品按钮和点击动作
- ✅ 重载并打开菜单
- ✅ 使用变量和 `variants` 扩展按钮状态

## 第一步：创建菜单文件

在 `plugins/KaMenu/menus/` 下创建文件。子文件夹会成为菜单 ID 的一部分：

```text
plugins/KaMenu/menus/
└── tutorial/
    └── first_container.yml
```

该文件的菜单 ID 是 `tutorial/first_container`，打开时不写 `.yml`。

## 第二步：选择类型和标题

先写入最基本的容器类菜单标识：

```yaml
Type: CHEST
Title: '&8我的第一个箱子菜单'
```

`CHEST` 每行固定 9 个逻辑槽位，可配置 1 至 6 行。本教程使用 3 行。其他类型的尺寸参见 [Type](type.md)。

## 第三步：设计 Layout

添加三行布局：

```yaml
Layout:
  - '#########'
  - '#   D   #'
  - '#H     X#'
```

每一行都有 9 个逻辑槽位：

| 字符 | 对应按钮 | 用途 |
|---|---|---|
| `#` | `Buttons.#` | 灰色边框 |
| `D` | `Buttons.D` | 钻石按钮 |
| `H` | `Buttons.H` | 帮助按钮 |
| `X` | `Buttons.X` | 关闭按钮 |
| 空格 | 无 | 空槽位 |

同一个 `#` 出现在多个槽位时，只需定义一次 `Buttons.#`。多字符按钮 ID 必须写成反引号形式，例如 `` `shop` ``。详见 [Layout](layout.md)。

## 第四步：定义按钮

在 `Buttons` 下定义 Layout 引用的四个按钮：

```yaml
Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '

  D:
    display:
      material: DIAMOND
      name: '&b领取钻石'
      lore:
        - '&7点击获得 1 个钻石'
    actions:
      left:
        - 'item: type=give;mats=DIAMOND;amount=1'
        - 'actionbar: &a你获得了 1 个钻石'
        - 'sound: ENTITY_PLAYER_LEVELUP;volume=1.0;pitch=1.0'

  H:
    display:
      material: BOOK
      name: '&e帮助'
      lore:
        - '&7查看菜单说明'
    actions:
      left:
        - 'tell: &e点击钻石即可领取物品。'

  X:
    display:
      material: BARRIER
      name: '&c关闭'
    actions:
      left:
        - 'close'
```

每个普通按钮至少需要 `display.material`。`display` 决定物品外观，`actions.left` 决定左键动作；其他点击类型参见 [Buttons](buttons.md)。

## 第五步：加入 Settings 和 Events

为有效按钮设置 250 毫秒点击间隔，并在菜单打开时发送动作栏提示：

```yaml
Settings:
  min_click_delay: 250

Events:
  Open:
    - 'actionbar: &a欢迎，%player_name%'
  Close:
    - 'actionbar: &7菜单已关闭'
```

`min_click_delay` 不限制空槽位或没有动作的边框按钮。`Events.Open` 在库存显示前执行，`Events.Close` 在 KaMenu 观察到菜单关闭后执行。

## 第六步：完整菜单

最终的 `plugins/KaMenu/menus/tutorial/first_container.yml`：

```yaml
Type: CHEST
Title: '&8我的第一个箱子菜单'

Settings:
  min_click_delay: 250

Layout:
  - '#########'
  - '#   D   #'
  - '#H     X#'

Events:
  Open:
    - 'actionbar: &a欢迎，%player_name%'
  Close:
    - 'actionbar: &7菜单已关闭'

Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '

  D:
    display:
      material: DIAMOND
      name: '&b领取钻石'
      lore:
        - '&7点击获得 1 个钻石'
    actions:
      left:
        - 'item: type=give;mats=DIAMOND;amount=1'
        - 'actionbar: &a你获得了 1 个钻石'
        - 'sound: ENTITY_PLAYER_LEVELUP;volume=1.0;pitch=1.0'

  H:
    display:
      material: BOOK
      name: '&e帮助'
      lore:
        - '&7查看菜单说明'
    actions:
      left:
        - 'tell: &e点击钻石即可领取物品。'

  X:
    display:
      material: BARRIER
      name: '&c关闭'
    actions:
      left:
        - 'close'
```

## 第七步：重载并打开

保存文件后执行：

```text
/km reload menu
/km open tutorial/first_container
```

如果菜单未出现在 `/km open ` 的 Tab 补全中，先查看控制台中的 YAML 或容器类菜单解析错误。KaMenu 会拒绝打开槽位数量错误、引用未知按钮或混用 Dialog 字段的残缺菜单。

## 第八步：添加动态状态

按钮需要根据权限或状态改变外观和动作时，将普通 `display/actions` 改为 `variants`：

```yaml
Buttons:
  D:
    variants:
      - priority: 0
        condition: 'hasPerm.tutorial.claim'
        display:
          material: DIAMOND
          name: '&a可以领取'
        actions:
          left:
            - 'item: type=give;mats=DIAMOND;amount=1'
            - 'refresh: D'
      - priority: 1
        display:
          material: COAL
          name: '&c没有权限'
        actions:
          left:
            - 'actionbar: &c你没有领取权限'
```

不要在同一个按钮下同时保留顶层 `display/actions` 和 `variants`。

## 常见错误

| 现象 | 常见原因 | 修复方法 |
|---|---|---|
| 菜单解析失败 | `CHEST` 某行不是 9 个逻辑槽位 | 重新计算字符、空格和反引号 ID |
| 提示未知按钮 | Layout 使用了字符，但 `Buttons` 未定义 | 添加对应 `Buttons.<id>` 或改为空格 |
| 多字符按钮被拆开 | `shop` 未使用反引号 | 在 Layout 中写 `` `shop` `` |
| 菜单被识别为错误类型 | 混用了 `Body`、`Inputs` 或 `Bottom` | 容器类菜单只使用 `Layout` 和 `Buttons` |
| PAPI 显示未解析 | 扩展未安装或未列入 `need_placeholder` | 安装扩展并检查 [Settings](settings.md) |
| 点击后状态不更新 | 修改数据后没有刷新 | 使用 `refresh: <按钮ID>` 或 `refresh` |

接下来可继续阅读 [Settings](settings.md)、[Buttons 状态变体](buttons.md)、[刷新机制](refresh.md)、[熔炉和铁砧属性](properties.md)以及 [Events](events.md)。
