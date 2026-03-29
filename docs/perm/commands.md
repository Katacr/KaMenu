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

### /km reload

重新加载插件的所有配置文件和菜单文件，无需重启服务器。

**格式：** `/km reload`

**权限：** `kamenu.admin`

**重载内容：**
1. `config.yml` 全局配置
2. 语言文件（`lang/` 目录）
3. `menus/` 目录下的所有菜单文件（包括子文件夹）
4. 自定义指令注册

**示例：**

```bash
/km reload
# 输出: [KaMenu] 菜单已重载，共加载 12 个菜单和 3 个自定义指令。
```

{% hint style="info" %}
修改菜单文件后，只需执行 `/km reload` 即可立即生效，无需重启服务器。
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
- `tell:消息` - 发送聊天消息
- `actionbar:消息` - 发送 ActionBar 消息
- `title:参数` - 发送标题
- `sound:参数` - 播放声音
- `command:指令` - 玩家执行指令
- `console:指令` - 控制台执行指令
- `data:操作` - 玩家数据操作
- `gdata:操作` - 全局数据操作
- `meta:操作` - 元数据操作


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
- 输入玩家名后按 Tab 键，会显示常用动作前缀

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
