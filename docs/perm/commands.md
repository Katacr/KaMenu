# ⌨️ 指令列表

KaMenu 提供了简洁的指令体系，主指令为 `/km`（或 `/kamenu`、`/menu`）。

---

## 主指令

```
/km <子指令> [参数]
```

**别名：** `/kamenu`、`/menu`

---

## 子指令详情

### /km help

显示指令帮助信息。

**格式：** `/km help`

**权限：** 无（所有玩家和都可使用）

**示例：**

```bash
/km help
```

**显示内容：**
- 插件版本信息
- 所有可用子指令列表
- 每个指令的简要说明

{% hint style="info" %}
输入 `/km`（不带参数）也会自动显示帮助信息。
{% endhint %}

---

### /km open

打开一个指定的菜单。

**格式：**
- `/km open <菜单ID>` - 为自己打开菜单（仅限玩家）
- `/km open <菜单ID> <玩家>` - 为指定玩家打开菜单（支持控制台）

**权限：** `kamenu.admin`

**Tab 补全：** 输入 `/km open ` 后按 Tab 键，会自动补全所有已加载的菜单 ID（包括子文件夹路径）

**示例：**

```bash
# 玩家为自己打开菜单
/km open main_menu

# 玩家为指定玩家打开菜单
/km open shop/weapons Player1

# 控制台为指定玩家打开菜单（必须指定玩家）
/km open example/actions_demo Player1

# 打开子文件夹中的菜单
/km open shop/weapons
```

**使用说明：**
- 若菜单 ID 不存在，玩家将收到错误消息
- 玩家可以省略第二个参数，默认为自己打开菜单
- 控制台必须指定第二个参数（玩家名）
- 若指定的玩家不存在，将收到错误消息

{% hint style="warning" %}
由于该指令可以打开任意菜单，需要 `kamenu.admin` 权限。建议使用 [自定义指令](../config/customCommands.md) 来为玩家提供更安全的菜单访问方式。
{% endhint %}

---

### /km list

查看服务器中所有已加载的菜单列表。

**格式：** `/km list [页码]`

**权限：** `kamenu.admin`

**功能：**
- 显示所有已加载的菜单（每页 10 个）
- 点击菜单名称可直接打开
- 支持分页浏览
- 使用可点击文本，交互便捷

**示例：**

```bash
# 查看第一页菜单列表
/km list

# 查看指定页
/km list 2
```

**显示效果：**

```

§6§l菜单列表
§f1. example/main_menu §e[点击打开]
§f2. example/shop_menu §e[点击打开]
§f3. example/vip_menu §e[点击打开]
...
§7[上一页]  §7第 1/3 页  §e[下一页]

```

---

### /km guide

打开内置入门向导菜单。

**格式：** `/km guide`

**权限：** `kamenu.admin`

**使用说明：**
- 向导菜单从插件 jar 内部加载到内存，不会写入 `menus` 目录
- 可用于首次配置语言、释放示例菜单、查看示例菜单说明
- 当服务器没有加载任何菜单且 OP 玩家进入服务器时，KaMenu 会发送可点击文本引导打开该菜单

**示例：**

```bash
/km guide
/kamenu guide
```

---

### /km language

设置插件语言并立即重载配置和菜单。语言 ID 对应 `plugins/KaMenu/lang/` 下的 `.yml` 文件名，不含扩展名。

**格式：** `/km language <语言ID>`

**权限：** `kamenu.admin`

**别名：** `/km lang <语言ID>`

**示例：**

```bash
# 切换为简体中文
/km language zh_CN

# 切换为英文
/km language en_US
```

---

### /km examples

按指定语言释放内置示例菜单到 `plugins/KaMenu/menus/example/`。

**格式：** `/km examples [zh_CN|en_US] [overwrite]`

**权限：** `kamenu.admin`

**别名：** `/km example`、`/km release-examples`

**使用说明：**
- 不填写语言时，使用当前 `config.yml` 中的 `language`
- 中文示例和英文示例都会释放到 `menus/example/`，不会生成 `exampleEN` 目录
- 默认不会覆盖已有文件
- 添加 `overwrite` 参数会覆盖已存在的同名示例菜单
- 释放完成后会自动重载菜单

**示例：**

```bash
# 按当前语言释放示例
/km examples

# 释放中文示例
/km examples zh_CN

# 释放英文示例
/km examples en_US

# 覆盖释放中文示例
/km examples zh_CN overwrite
```

---

### /km pause

生成或移除 ESC 暂停菜单入口数据包。`register` 会读取插件根目录的 `pause_menu.yml`，并将其中的静态 KaMenu 风格布局编译为原版 Dialog。

**格式：**
- `/km pause register` - 按当前 `plugins/KaMenu/pause_menu.yml` 生成入口数据包
- `/km pause unregister` - 移除 KaMenu 生成的入口数据包
- `/km pause info` - 查看当前入口状态和数据包路径

**权限：** `kamenu.admin`

**示例：**

```bash
/km pause register
/km pause info
/km pause unregister
```

**使用说明：**
- 数据包写入 `world/datapacks/KaMenuPauseEntry`
- 插件启动时若缺少 `pause_menu.yml`，会自动释放默认模板且不会覆盖已有文件
- 新增、修改或移除后必须完整重启服务器才会影响 ESC 暂停菜单
- KaMenu 只注册一个 ESC 入口；可在 `Bottom.buttons` 中配置该入口内部的按钮矩阵
- `Body.message` 支持 Legacy、MiniMessage 和静态 `<text=...>` 可点击文本
- 支持静态 `Inputs`；配置 `actions` 的按钮可通过 `$(key)` 接收客户端输入
- 标题、Body、输入标签和按钮文本不解析运行期变量；按钮 `actions` 回调可使用 PAPI、KaMenu 变量、条件、JS 和动作包
- `menu` 按钮打开的目标菜单仍按普通 KaMenu 菜单完整解析
- 完整语法见 [ESC 暂停菜单](../config/pause-menu.md)

---

### /km reload

重新加载插件配置、菜单或资源包，无需重启服务器。不填写目标时默认重载全部。

**格式：** `/km reload [all|menu|actions|js|lang|config]`

**权限：** `kamenu.admin`

**目标：**

| 目标 | 说明 |
|------|------|
| `all` | 重载全部模块，等同于不填写目标 |
| `menu` | 仅重载 `menus/` 目录下的菜单文件 |
| `actions` | 仅重载 `plugins/KaMenu/actions/` 全局动作包 |
| `js` | 仅重载 `plugins/KaMenu/js/` 全局 JavaScript 包 |
| `lang` | 仅重载当前语言文件 |
| `config` | 重载 `config.yml`、语言文件和自定义指令 |

每个目标都会返回独立统计：总数、成功、失败、耗时 ms。`config` 的统计对象是 `custom-commands` 中的自定义指令；不填写目标或使用 `all` 时，会依次输出各模块的重载结果。

**示例：**

```bash
/km reload
/km reload menu
/km reload actions
/km reload js
/km reload lang
/km reload config
```

{% hint style="info" %}
仅修改菜单文件时，推荐执行 `/km reload menu`；需要同时重载全部模块时再使用 `/km reload`。
{% endhint %}

---

### /km item

管理保存的物品，包括保存手持物品、发送保存的物品和删除保存的物品。

**格式：**
- `/km item save <物品名称>` - 保存手持物品到数据库（仅限玩家）
- `/km item give <物品名称>` - 为自己发送1个物品（仅限玩家）
- `/km item give <物品名称> <数量>` - 为自己发送指定数量的物品（仅限玩家）
- `/km item give <物品名称> <玩家>` - 为指定玩家发送1个物品（支持控制台）
- `/km item give <物品名称> <玩家> <数量>` - 为指定玩家发送指定数量的物品（支持控制台）
- `/km item delete <物品名称>` - 删除保存的物品

**权限：** `kamenu.admin`

**功能说明：**
- 物品以Base64格式序列化存储在数据库中
- 支持所有类型的物品（包括带有NBT标签的物品）
- 可以保存复杂物品，如附魔物品、特殊材质物品等
- 保存物品时会自动将数量设置为1，避免保存数量信息
- 发送物品时可以指定数量，范围1-64
- 删除物品将永久从数据库中移除

**示例：**

```bash
# 保存手持物品
/km item save diamond_sword
/km item save vip_reward

# 为自己发送1个物品
/km item give diamond_sword

# 为自己发送10个物品
/km item give diamond_sword 10

# 为指定玩家发送1个物品
/km item give vip_reward Player1

# 为指定玩家发送10个物品
/km item give vip_reward Player1 10

# 控制台为指定玩家发送物品（必须指定玩家）
/km item give diamond_sword Player1

# 控制台为指定玩家发送多个物品
/km item give diamond_sword Player1 5

# 删除保存的物品
/km item delete diamond_sword
/km item delete vip_reward
```

**Tab 补全：**
- 输入 `/km item ` 后按 Tab 键，会显示子指令（save、give、delete）
- 输入 `/km item give ` 后按 Tab 键，会显示所有保存的物品名称
- 输入 `/km item delete ` 后按 Tab 键，会显示所有保存的物品名称
- 输入物品名称后按 Tab 键，会显示所有在线玩家（仅 give 指令）

{% hint style="warning" %}
- save 指令只能由玩家使用，控制台无法使用
- give 指令中，玩家参数可选，未指定则为自己，控制台必须指定玩家
- 数量参数可选，默认为1，范围1-64
- 手持物品为空时无法保存
- 若玩家物品栏已满，剩余物品将无法发放
{% endhint %}

---

### /km action

测试执行指定的动作，方便调试和验证动作配置。

**格式：** `/km action <玩家> <动作>`

**权限：** `kamenu.admin`

**使用说明：** 该指令支持玩家和控制台使用，必须指定目标玩家。

**支持的动作类型：**
- 支持所有服务端执行的动作前缀，例如 `tell:`、`actionbar:`、`title:`、`hovertext:`、`command:`、`chat:`、`console:`、`sound:`、`open:`、`force-open:`、`close`、`force-close`、`reset`、`server:`、`tppos:`、`data:`、`gdata:`、`list:`、`glist:`、`meta:`、`toast:`、`money:`、`stock-item:`、`item:`、`js:` 等。
- `wait`、`return`、`run-task:`、`stop-task:`、`stop-current-task`、`page:`、`actions:` 等动作链/菜单上下文动作可以输入，但部分效果依赖当前菜单配置或任务生命周期。
- `url:` 和 `copy:` 是 Paper Dialog 按钮的静态点击事件，只在菜单按钮中作为单动作使用，不适合作为 `/km action` 测试目标。

详细动作类型请参阅 [🤖 动作 (Actions)](../menu/actions.md)。

---

**示例：**

```bash
# 发送消息
/km action Player1 tell:Hello World

# 播放声音
/km action Player2 sound:block_note_block_harp;volume=1.0;pitch=1.0

# 发送标题
/km action Player3 title:title=测试;subtitle=副标题

# 操作数据
/km action Player4 data:type=set;key=test;var=100

# 支持变量和 PAPI
/km action Player5 tell: 你的等级是%player_level%，积分是{data:score}
```

**Tab 补全：**
- 输入 `/km action ` 后按 Tab 键，会显示所有在线玩家
- 输入玩家名后按 Tab 键，会显示已支持的服务端动作前缀

{% hint style="info" %}
该指令支持所有内置变量（`{data:var}`、`{gdata:var}`、`{meta:var}`）和 PlaceholderAPI 变量（`%player_name%` 等）。
{% endhint %}

---

## 自定义快捷指令

除了 `/km open` 之外，你还可以在 `config.yml` 中注册自定义快捷指令，直接将一个简短的指令映射到打开某个菜单：

```yaml
custom-commands:
  shop: 'server_shop'   # 玩家执行 /shop 即打开 server_shop 菜单
  menu: 'main_menu'     # 玩家执行 /menu 即打开 main_menu 菜单
```

详细配置请参阅 [⌨️ 自定义指令](../home/commands.md)。
