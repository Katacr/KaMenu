# 📦 actions 文件夹

`plugins/KaMenu/actions/` 用于存放全局 actions 动作包。动作包适合放置多个菜单都会复用的动作队列，例如通用提示、奖励发放、权限校验、跨菜单跳转等。

全局 actions 包可以在任意菜单动作列表、`Events.Click`、可点击文本、周期任务和自定义注册指令中调用。

---

## 文件位置

运行目录：

```text
plugins/KaMenu/actions/
```

一个 `.yml` 文件就是一个动作包。包 ID 为相对路径去掉 `.yml` 后缀，并使用 `/` 作为路径分隔符：

```text
plugins/KaMenu/actions/reward/daily.yml -> reward/daily
plugins/KaMenu/actions/common/close.yml -> common/close
```

首次启动且 `plugins/KaMenu/actions/` 不存在时，KaMenu 会释放内置示例：

```text
plugins/KaMenu/actions/example/welcome.yml -> example/welcome
```

如果该文件夹已存在，KaMenu 不会自动写入示例文件，避免影响已有服务器文件。

---

## 命名与大小限制

包 ID 只能使用英文、数字、`_`、`-`、`.`、`/`，不能包含 `..`，不能以 `/` 开头或结尾，也不能包含连续的 `//`。

每个动作包文件最大为 `1 MiB`。超过上限的文件会被跳过，并在控制台输出本地化警告。

---

## 文件格式

动作包根节点必须是 `actions` 列表：

```yaml
actions:
  - 'toast: type=task;msg=领取成功;icon=emerald'
  - 'sound: entity.experience_orb.pickup;volume=1.0;pitch=1.2'
  - 'money: type=add;num={arg:0}'
```

动作包内支持普通动作、条件分支、`wait`、`return`、`actions:` 嵌套调用等完整动作队列能力。

---

## 调用方式

```yaml
actions:
  - 'actions: reward/daily,100'
  - 'actions: common/close'
```

参数可以用英文逗号或空格分隔：

```yaml
- 'actions: reward/daily,100,vip'
- 'actions: reward/daily 100 vip'
```

参数中包含空格或逗号时，使用单引号、双引号或反引号包裹：

```yaml
- 'actions: reward/message,"100 coins",`vip,plus`'
```

动作包内部通过 `{arg:0}`、`{arg:1}` 读取参数。

---

## 查找优先级

当调用：

```yaml
- 'actions: reward/daily'
```

KaMenu 按以下顺序查找：

1. 当前菜单 `Events.Click.reward/daily`
2. 全局动作包 `plugins/KaMenu/actions/reward/daily.yml`

如果菜单内 `Events.Click` 和全局包同名，优先执行菜单内动作列表。

---

## 递归规则

为了避免直接递归，动作列表不能直接调用自身：

```yaml
# reward/daily.yml 内不要直接写：
- 'actions: reward/daily'
```

检测到直接自调用时，KaMenu 会跳过这条 `actions:` 调用，并继续执行后续动作。

---

## 重载

全局 actions 包会在服务器启动、`/km reload` 或 `/km reload actions` 时加载。

仅加载 `.yml` 文件，不加载 `.yaml` 文件。

---

## 示例

文件：

```text
plugins/KaMenu/actions/reward/daily.yml
```

内容：

```yaml
actions:
  - condition: 'hasPerm.reward.daily'
    allow:
      - 'toast: type=task;msg=领取成功;icon=emerald'
      - 'money: type=add;num={arg:0}'
      - 'sound: entity.experience_orb.pickup;volume=1.0;pitch=1.2'
    deny:
      - 'toast: type=challenge;msg=无权限;icon=barrier'
```

菜单中调用：

```yaml
Bottom:
  type: multi
  buttons:
    daily:
      text: '&a[ 每日奖励 ]'
      actions:
        - 'actions: reward/daily,100'
        - 'reset'
```

---

## 常见问题

**动作包没有执行**

- 检查文件是否位于 `plugins/KaMenu/actions/`
- 检查后缀是否为 `.yml`
- 检查包 ID 是否只使用英文、数字、`_`、`-`、`.`、`/`
- 检查文件大小是否不超过 `1 MiB`
- 检查根节点是否为 `actions`
- 执行 `/km reload actions`

**提示找不到动作列表**

在菜单上下文中，KaMenu 会检查当前菜单 `Events.Click.xxx` 和全局 `actions/xxx.yml`。在自定义注册指令等无菜单上下文中，只检查全局 actions 包。
