# 菜单引用 (References)

菜单引用用于复用同一份菜单文件中的文本、数值和简单列表。它适用于 Dialog 和容器类菜单，可以减少重复的按钮文字、Lore、动作参数和条件值。

## 引用类型一览

| 写法 | 读取范围 | 典型用途 |
|---|---|---|
| `{ref:path}` | 当前菜单的 `References.path` | 复用公共文本或模板 |
| `{config:path}` | 当前菜单根配置 | 读取 `Title`、`Settings` 或其他已存在字段 |
| `{self:<字段路径>}` | 当前组件或按钮自身节点 | 复用当前按钮的自定义数据 |
| `{self:id}` | 当前组件或按钮 ID | 在公共模板中显示当前 ID |
| `{self:path}` | 当前组件或按钮的完整配置路径 | 调试或组合动态路径 |
| `{refarg:n}` | 当前引用调用传入的第 `n` 个参数 | 在 `References` 模板内部插入参数 |

引用路径不区分大小写，但建议保持与 YAML 中的实际键名一致。引用只在当前菜单文件内生效，不支持跨菜单读取。

## 公共引用

在菜单顶层定义 `References`，然后通过 `{ref:path}` 读取：

```yaml
Title: '{ref:text.title}'

References:
  text:
    title: '&8服务器商店'
    currency: '&6金币'
    product_lore:
      - '&7价格：{refarg:0} {ref:text.currency}'
      - '&7左键购买'

Body:
  info:
    type: message
    text: '&7当前货币：{ref:text.currency}'

Bottom:
  type: notice
  confirm:
    text: '{ref:[text.product_lore;100]}'
    actions:
      - 'close'
```

标量会直接转为文本。简单列表会用换行连接，因此可以引用为多行消息、Lore 或悬浮文字。

## 带参数的模板

模板参数从 `0` 开始编号：

```yaml
References:
  buy: '&a购买 {refarg:0} 个 {refarg:1}，价格 {refarg:2}'

Body:
  info:
    type: message
    text: '{ref:[buy;5;钻石;100 金币]}'
```

结果为：

```text
&a购买 5 个 钻石，价格 100 金币
```

参数使用分号分隔。参数本身包含分号时，使用反引号、单引号或双引号包裹：

```yaml
text: '{ref:[buy;5;`钻石;限时`;100 金币]}'
```

模板可以继续引用其他 `References`，参数中也可以使用 PAPI、KaMenu 变量或其他引用。缺少必需的 `{refarg:n}` 参数会抛出配置异常，不会静默生成残缺内容。

## 读取当前菜单配置

`{config:path}` 从当前菜单根节点读取值：

```yaml
Title: '&8主菜单'

Body:
  debug:
    type: message
    text: '&7当前标题配置：{config:Title}'
```

该写法适合读取已经存在的简单字段。不要用它代替 `References` 存放公共模板，也不要引用包含 Map 的完整配置段。

## 当前组件引用

`{self:*}` 会根据正在解析的组件自动定位，其中 `*` 是相对于当前组件的字段路径：

- Dialog：`Body.<id>`、`Inputs.<id>`、`Bottom.confirm`、`Bottom.deny`、`Bottom.buttons.<id>`、`Bottom.exit`，以及 repeat 的 `Bottom.buttons.<id>.item`。
- 容器类菜单：`Buttons.<id>`。使用 `variants` 时仍以父按钮 ID 和父按钮路径为准。

```yaml
Type: CHEST
Title: '&8商店'

Layout:
  - '    S    '

Buttons:
  S:
    data:
      product: DIAMOND
      price: 100
    display:
      material: '{self:data.product}'
      name: '&b商品 {self:id}'
      lore:
        - '&7价格：{self:data.price}'
    actions:
      left:
        - 'actionbar: &a购买 {self:data.product}，价格 {self:data.price}'
```

`self` 适合让多个按钮使用相同结构但保留各自数据。它只能在组件上下文中使用；在 `Events.Open` 等没有当前组件的上下文中使用 `{self:*}` 会抛出异常。

## 在条件中使用引用

`{ref:*}`、`{config:*}` 和 `{self:*}` 会在条件表达式解析前展开，并按完整值安全参与比较：

```yaml
References:
  minimum_level: 10

Settings:
  max_price: 500

Buttons:
  S:
    data:
      price: 100
    view_condition: '%player_level% >= {ref:minimum_level} && {self:data.price} <= {config:Settings.max_price}'
```

该能力适用于条件 Map、按钮显示条件、`variants.condition` 和行尾 `{condition: ...}`。`ref` 与 `config` 只要求调用方处于当前菜单配置中；`self` 还要求当前条件属于一个 Dialog 组件或容器按钮。`{self:id}` 返回组件 ID，`{self:path}` 返回例如 `Buttons.S` 的完整路径，`{self:data.price}` 则读取该路径下的字段。

## 限制与错误

- 仅支持字符串、数字、布尔值和只包含标量的列表。
- 列表使用换行连接；Map、ConfigurationSection、嵌套列表和其他结构不能嵌入文本。
- 不支持通过引用继承完整按钮、Body 组件或其他 YAML 结构。
- 不支持跨菜单引用。
- 最大递归深度为 16；循环引用会抛出明确异常。
- `Type`、`Layout`、YAML 键名等结构字段不会因为文本引用而动态改变。

引用路径错误、结构类型错误、缺少模板参数或形成循环时，菜单会保留原有的失败行为并输出异常原因，便于定位配置问题。
