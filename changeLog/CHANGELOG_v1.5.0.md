# KaMenu v1.5.0 更新报告

## 📋 版本信息
- **版本号**: 1.5.0
- **发布日期**: 2026年6月25日

---

## ✨ 新增功能

### 1. 新增外部插件菜单 API

KaMenu 现在支持外部插件直接渲染内存 YAML 菜单，不需要把菜单文件写入 `menus` 目录，也不需要触发菜单重载。

新增公开 API：

```kotlin
KaMenuAPI.openYaml(player, yaml, "kagamecenter:main")
KaMenuAPI.openConfig(player, config, "external")
KaMenuAPI.registerActionHandler("kgc") { player, action, variables, config ->
    true
}
KaMenuAPI.unregisterActionHandler("kgc")
```

新增接口：

```kotlin
fun interface KaMenuActionHandler {
    fun execute(
        player: Player,
        action: String,
        variables: Map<String, String>,
        rawConfig: YamlConfiguration?
    ): Boolean
}
```

外部动作 handler 可拦截形如 `namespace:payload` 的动作。handler 返回 `true` 表示已处理，返回 `false` 时会继续走 KaMenu 原有动作逻辑。

### 2. 新增菜单周期任务 `Events.Tasks`

菜单现在可以在打开期间周期执行动作列表，适合用于状态刷新、超时检测、提示音、动态检查等场景。

```yaml
Events:
  Tasks:
    refresh:
      interval: 20
      run_immediately: true
      skip_if_running: true
      actions:
        - 'tell: &7菜单周期刷新'
        - condition: '{data:warning} == true'
          allow:
            - 'sound: block.note_block.pling 1 1'
```

### 3. 周期任务复用完整动作系统

- 支持普通动作、条件动作、`allow/deny` 分支
- 支持 `wait` 延迟动作
- 支持嵌套 `actions:` 动作组
- 支持外部插件注册的自定义 action handler
- 支持内置变量、PlaceholderAPI、`{js:...}` 等文本解析能力

### 4. 菜单实例级任务生命周期

- 菜单成功显示后才会启动周期任务
- `Events.Open` 中执行 `return` 阻止菜单打开时，不会启动周期任务
- 玩家打开新菜单、执行 `open` / `force-open` / `reset`、执行 `close` / `force-close`、退出服务器或插件重载时，会取消当前菜单任务
- 使用 `skip_if_running: true` 时，上一轮动作链未结束会跳过本轮，避免 `wait` 导致任务堆叠

### 5. 周期任务手动控制动作

新增周期任务控制动作，用于在任意动作包中启动、停止菜单内的周期任务：

```yaml
- 'run-task: countdown 10'
- 'stop-task: countdown'
- 'stop-task: *'
- 'stop-current-task'
```

- `run-task: <任务ID>` 可手动启动 `mode: manual` 的任务
- `run-task: <任务ID> <次数>` 可为本次运行指定循环次数
- `run-task: *` / `stop-task: *` 支持通配符，批量控制当前菜单内任务
- `stop-current-task` 可在周期任务自身动作链中停止当前循环，并中断本轮后续动作
- 同一个任务已在当前菜单生命周期内运行时，不会重复创建任务

### 6. 动作包参数传递

`actions:` 动作包调用和可点击文本的 `actions=` 参数现在支持传参。动作包内部可通过 `{arg:0}`、`{arg:1}` 读取参数。

```yaml
Body:
  info:
    type: 'message'
    text:
      - '<text="&a[点击问候]";actions=hello,玩家,生存服务器>'

Events:
  Click:
    hello:
      - 'tell: &a你好，{arg:0}！欢迎来到 &e{arg:1}&a。'
```

---

## 📝 兼容说明

- 不需要修改旧菜单语法
- 未配置 `Events.Tasks` 的菜单行为不变
- 现有文件菜单、`/km open`、`MenuManager` 行为保持兼容
- 外部内存菜单不要求配置来自 `MenuManager`
- Paper 当前没有可靠的 Dialog 关闭事件。如果允许玩家按 ESC 关闭菜单，服务端无法立即感知该关闭动作；需要严格生命周期时，建议设置 `Settings.can_escape: false` 并通过菜单按钮执行 `close` 或 `force-close`
- 周期任务没有实时输入响应，不支持实时读取 `$(input_key)`
- 周期任务内的 `return` 只会结束当前这一轮动作链，不会停止整个周期任务；需要停止循环时请使用 `stop-current-task` 或 `stop-task`

---

## 🚀 升级指南

### 从 1.4.5 / 1.4.6 测试版升级到 1.5.0

1. 替换插件 jar 文件为 `KaMenu-1.5.0.jar`
2. 完整重启服务器
3. 可选：外部插件可改用 `KaMenuAPI.openYaml` / `openConfig` 渲染动态菜单
4. 可选：在需要周期逻辑的菜单中添加 `Events.Tasks`
5. 可选：使用 `run-task` / `stop-task` / `stop-current-task` 控制手动任务或倒计时任务

---

# KaMenu v1.5.0 Update Report

## 📋 Version Info
- **Version**: 1.5.0
- **Release Date**: June 25, 2026

---

## ✨ New Features

### 1. Added external plugin menu APIs

KaMenu now allows external plugins to render in-memory YAML menus directly, without writing menu files to the `menus` directory and without reloading menu files.

New public APIs:

```kotlin
KaMenuAPI.openYaml(player, yaml, "kagamecenter:main")
KaMenuAPI.openConfig(player, config, "external")
KaMenuAPI.registerActionHandler("kgc") { player, action, variables, config ->
    true
}
KaMenuAPI.unregisterActionHandler("kgc")
```

New interface:

```kotlin
fun interface KaMenuActionHandler {
    fun execute(
        player: Player,
        action: String,
        variables: Map<String, String>,
        rawConfig: YamlConfiguration?
    ): Boolean
}
```

External action handlers can intercept actions in the `namespace:payload` format. Returning `true` means the action was handled. Returning `false` lets KaMenu continue with its normal action handling.

### 2. Added periodic menu tasks: `Events.Tasks`

Menus can now run action lists repeatedly while they are open. This is useful for status refreshes, timeout checks, sounds, dynamic checks, and similar workflows.

```yaml
Events:
  Tasks:
    refresh:
      interval: 20
      run_immediately: true
      skip_if_running: true
      actions:
        - 'tell: &7Periodic menu refresh'
        - condition: '{data:warning} == true'
          allow:
            - 'sound: block.note_block.pling 1 1'
```

### 3. Periodic tasks reuse the full action system

- Supports normal actions, conditional actions, and `allow/deny` branches
- Supports delayed actions with `wait`
- Supports nested `actions:` groups
- Supports external custom action handlers registered by other plugins
- Supports built-in variables, PlaceholderAPI, `{js:...}`, and the existing text resolution pipeline

### 4. Menu-instance task lifecycle

- Periodic tasks start only after the menu is successfully displayed
- If `Events.Open` uses `return` to prevent the menu from opening, tasks do not start
- Current menu tasks are cancelled when the player opens another menu, uses `open` / `force-open` / `reset`, uses `close` / `force-close`, leaves the server, or the plugin reloads
- With `skip_if_running: true`, a new round is skipped when the previous action chain is still running, preventing `wait` from stacking overlapping executions

### 5. Manual periodic task control actions

New task control actions can start and stop periodic tasks from any action list:

```yaml
- 'run-task: countdown 10'
- 'stop-task: countdown'
- 'stop-task: *'
- 'stop-current-task'
```

- `run-task: <taskId>` starts a `mode: manual` task
- `run-task: <taskId> <count>` overrides the run count for this run
- `run-task: *` / `stop-task: *` support wildcard control for tasks in the current menu
- `stop-current-task` stops the current periodic task from inside its own action chain and interrupts the rest of the current round
- If the same task is already running in the current menu lifecycle, it will not be duplicated

### 6. Action list arguments

`actions:` action-list calls and clickable text `actions=` values now support arguments. Inside the action list, use `{arg:0}` and `{arg:1}` to read them.

```yaml
Body:
  info:
    type: 'message'
    text:
      - '<text="&a[click greet]";actions=hello,player,survival server>'

Events:
  Click:
    hello:
      - 'tell: &aHello, {arg:0}! Welcome to &e{arg:1}&a.'
```

---

## 📝 Compatibility Notes

- Existing menu syntax does not need to be changed
- Menus without `Events.Tasks` behave the same as before
- Existing file menus, `/km open`, and `MenuManager` behavior remain compatible
- External in-memory menus do not need to come from `MenuManager`
- Paper currently does not provide a reliable Dialog close event. If players can close the menu with ESC, the server cannot immediately detect that client-side close. For strict lifecycle control, set `Settings.can_escape: false` and close the menu through `close` or `force-close`
- Periodic tasks do not receive live input responses and cannot read real-time `$(input_key)` values
- `return` inside a periodic task only ends the current action-chain round. Use `stop-current-task` or `stop-task` when the repeating task itself should stop

---

## 🚀 Upgrade Guide

### Upgrading from 1.4.5 / 1.4.6 test builds to 1.5.0

1. Replace the plugin jar with `KaMenu-1.5.0.jar`
2. Fully restart the server
3. Optionally use `KaMenuAPI.openYaml` / `openConfig` for dynamic menus from external plugins
4. Optionally add `Events.Tasks` to menus that need periodic logic
5. Optionally use `run-task` / `stop-task` / `stop-current-task` for manual tasks and countdown workflows
