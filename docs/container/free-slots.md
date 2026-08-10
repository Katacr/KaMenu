# 自由槽位 (Free-Slots)

`Free-Slots` 允许玩家在 Container 菜单的指定槽位放入或取出真实物品，适合材料提交、自定义合成、物品鉴定和物品预览。

自由槽位不是按钮：不要为同一槽位配置 `Buttons` 点击动作。KaMenu 会校验并手动提交放入、取出、合并或交换事务。

## 字段一览

| 键 | 默认值 | 作用 |
|---|---|---|
| `Free-Slots.<id>.slots` | 必填 | 绑定一个或多个 0-based 顶部库存槽位 |
| `place.enabled` | `true` | 是否允许玩家放入物品 |
| `place.condition` | 无限制 | 使用候选物品变量检查放入条件 |
| `take.enabled` | `true` | 是否允许玩家手动取出物品 |
| `take.condition` | 无限制 | 使用已存物品变量检查取出条件 |
| `events.place` | 空 | 成功放入后执行一次 |
| `events.take` | 空 | 成功取出后执行一次 |
| `events.deny_place` | 空 | 放入被拒绝时执行一次 |
| `events.deny_take` | 空 | 取出被拒绝时执行一次 |
| `return.on_close` | `true` | 正常关闭时是否立即尝试返还背包 |
| `return.overflow` | `pending` | 背包不足时保存为待返还记录；第一版仅支持该值 |

第一版支持 `CHEST`、`HOPPER`、`DISPENSER` 和 `DROPPER`。熔炉类容器和铁砧暂不允许配置自由槽位。

## 基础配置

```yaml
Type: CHEST
Title: '&8材料提交'

Layout:
  - '#########'
  - '####  C##'
  - '#########'

Free-Slots:
  diamond:
    slots: [13]
    place:
      enabled: true
      condition: '{free:incoming.material} == DIAMOND'
    take:
      enabled: true
    events:
      place:
        - 'actionbar: &a钻石已放入'
      take:
        - 'actionbar: &e钻石已取出'
      deny_place:
        - 'actionbar: &c这里只能放入钻石'
    return:
      on_close: true
      overflow: pending

Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '
  C:
    display:
      material: LIME_CONCRETE
      name: '&a确认'
```

槽位编号从 `0` 开始。三行箱子第二行中间的编号是 `13`。`Layout` 中的自由槽必须写为空格，不能同时放置按钮；重复、越界或重叠会使菜单拒绝加载。

## 交互规则

| 操作 | 第一版行为 |
|---|---|
| 左键 | 放入整组、取出整组或交换不同物品 |
| 右键 | 放入一个或取出一半 |
| 相同物品 | 在物品最大堆叠数内合并 |
| Shift 点击玩家背包 | 按声明顺序放入可接收的自由槽组 |
| Shift 点击自由槽 | 背包容量允许时取出 |
| 拖拽 | 只有全部受影响的顶部槽位都是允许的自由槽时才提交 |
| 数字键、副手、双击、丢弃、创造克隆 | 拒绝 |

拒绝操作时物品保持原位。异种交换同时检查 `take` 和 `place`；成功后先执行 `events.take`，再执行 `events.place`，两个事件都能读取最终状态。

## 条件与属性变量

```text
{free:incoming.*}  本次尝试放入的物品
{free:stored.*}    操作前自由槽中的物品
{free:result.*}    操作完成后的物品
{free:id}          当前逻辑自由槽 ID
{free:slot}        当前物理槽位编号
```

`*` 支持 `empty`、`material`、`amount`、`name`、`plain_name`、`lore`、`enchantments`、`enchantment_count`、`enchantment.<key>`、`custom_model_data`、`item_model`、`max_stack_size`、`provider` 和 `id`。

```yaml
place:
  condition: >
    {free:incoming.material} == DIAMOND &&
    {free:incoming.custom_model_data} == 1001
```

空物品的 `material` 为 `AIR`、`amount` 为 `0`、`empty` 为 `true`，文本属性为空字符串。低版本不支持 `item_model` 时返回空字符串。

其他按钮通过 `{free:<id>.<属性>}` 读取当前会话。自由槽变化后会刷新全部按钮，因此可以直接驱动 `variants`：

```yaml
Buttons:
  C:
    variants:
      - priority: 0
        condition: '{free:diamond.amount} >= 2'
        display:
          material: LIME_CONCRETE
          name: '&a可以合成'
        actions:
          left:
            - 'free-slot: type=consume;id=diamond;amount=2'
            - 'item: type=give;mats=EMERALD;amount=1'
      - priority: 1
        display:
          material: RED_CONCRETE
          name: '&c还需要钻石'
```

## 完整物品预览

标量变量无法完整重建药水、书本、旗帜、PDC 或新版本组件。`[FREE:<id>]` 会克隆实际 `ItemStack`：

```yaml
Buttons:
  preview:
    display:
      material: '[FREE:diamond]'
      name: '&e提交物预览'
```

未显式配置的名称、Lore、附魔和其他元数据会保留；显式填写的显示字段会覆盖克隆结果。自由槽为空时不显示预览物品。

## 多物理槽位组

```yaml
Free-Slots:
  ingredients:
    slots: [10, 11, 12]
```

- Shift 放入按 `slots` 顺序先合并，再使用空槽。
- `{free:ingredients.amount}` 返回全部物理槽的数量总和。
- 非空材质一致时返回该材质，不一致时 `material` 返回 `MIXED`。
- 消费按声明顺序扣除；返还保持每个实际 `ItemStack` 及其元数据。

## free-slot 动作

`free-slot` 只对玩家当前有效的 Container 会话生效，不支持目标选择器。

```yaml
- 'free-slot: type=consume;id=input;amount=1'
- 'free-slot: type=consume;items=diamond:1,emerald:2'
- 'free-slot: type=return;id=input'
- 'free-slot: type=return;id=*'
- 'free-slot: type=refresh;id=input'
```

多材料消费是原子的：任意 ID 或数量不满足时完全不扣除，并中断当前动作链。奖励动作必须放在消费动作之后。

主动返还也要求玩家背包能完整接收所选物品；容量不足时不移动任何物品并中断后续动作。

## 返还与异常恢复

自由槽中的物品是玩家资产，KaMenu 会为每个物理槽维护持久化托管记录：

- 正常关闭、菜单跳转、重载和玩家退出会先返还或保留待返还记录。
- 背包已满时保存为 `RETURN_PENDING`，玩家下次登录时重试，不把物品作为掉落物处理。
- `return.on_close: false` 只关闭本次关闭时的立即返还；物品不会被消费，而会进入待返还状态。
- 插件停用时优先保留托管记录，由下次登录恢复，避免 Folia 调度与数据库关闭时序造成丢失。

玩家库存文件与 SQLite/MySQL 无法构成跨系统 ACID 事务。实现优先保证不丢失；断电、`kill -9` 或数据库提交窗口中的崩溃仍可能造成极低概率的重复恢复，不能同时承诺绝对零丢失和零复制。

## 自定义合成案例

```yaml
Type: CHEST
Title: '&8自定义合成'
Layout:
  - '#########'
  - '##   C###'
  - '#########'
Free-Slots:
  diamond:
    slots: [11]
    place:
      condition: '{free:incoming.material} == DIAMOND'
  emerald:
    slots: [12]
    place:
      condition: '{free:incoming.material} == EMERALD'
Buttons:
  '#':
    display:
      material: GRAY_STAINED_GLASS_PANE
      name: ' '
  C:
    variants:
      - priority: 0
        condition: '{free:diamond.amount} >= 1 && {free:emerald.amount} >= 1'
        display:
          material: LIME_CONCRETE
          name: '&a合成'
        actions:
          left:
            - 'free-slot: type=consume;items=diamond:1,emerald:1'
            - 'item: type=give;mats=NETHER_STAR;amount=1'
      - priority: 1
        display:
          material: RED_CONCRETE
          name: '&c材料不足'
```
