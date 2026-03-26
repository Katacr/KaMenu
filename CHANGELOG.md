# KaMenu 更新日志 / Changelog

## v1.2.2 - 2026-03-27

### 中文 / Chinese

#### 新增功能
- **新增 action 指令**：用于快捷测试和执行各种菜单动作，支持内置变量和PlaceholderAPI变量
  - 使用格式：`/kamenu action <玩家> <动作>`
  - 支持所有动作类型：tell, actionbar, title, hovertext, command, console, sound, open, close, data, gdata, meta, set-data, set-gdata, set-meta, toast, money 等
  - 支持玩家变量、全局数据、元数据以及PAPI变量

- **新增 list 指令**：显示服务器内所有已加载的菜单列表
  - 支持分页显示，每页10个菜单
  - 菜单项可点击直接打开
  - 提供上一页/下一页翻页按钮
  - 使用格式：`/kamenu list [页码]`

- **新增 help 指令**：显示插件的所有指令帮助信息
  - 包含所有指令的详细用法说明
  - 底部提供可点击的文档链接
  - 支持多语言（根据设置语言显示对应的文档链接）

- **实装自定义指令注册功能**：
  - 可在菜单配置中定义自定义指令
  - 自动注册自定义指令，无需手动配置
  - 支持指令别名
  - 支持权限配置
  - 重载时自动更新自定义指令

#### 优化改进
- 优化 open 指令：支持为指定玩家打开菜单
  - 格式：`/kamenu open <菜单名> [玩家]`
  - 若不指定玩家则为自己打开
  - 控制台使用时必须指定玩家

- 改进控制台兼容性：action 和 open 指令支持控制台使用
- 优化日志输出：简化自定义指令注册日志
- 在启动和重载时显示已注册的自定义指令数量

#### 权限更新
- open 指令和 list 指令现在需要 `kamenu.admin` 权限

---

### English

#### New Features
- **Added action command**: Quick test and execute various menu actions, supporting built-in variables and PlaceholderAPI variables
  - Usage: `/kamenu action <player> <action>`
  - Supports all action types: tell, actionbar, title, hovertext, command, console, sound, open, close, data, gdata, meta, set-data, set-gdata, set-meta, toast, money, etc.
  - Supports player variables, global data, metadata, and PAPI variables

- **Added list command**: Display all loaded menus on the server
  - Paginated display with 10 menus per page
  - Clickable menu items to open directly
  - Previous/Next page navigation buttons
  - Usage: `/kamenu list [page]`

- **Added help command**: Display all plugin command help information
  - Contains detailed usage instructions for all commands
  - Clickable documentation link at the bottom
  - Multi-language support (shows documentation link based on language setting)

- **Implemented custom command registration feature**:
  - Define custom commands in menu configuration
  - Automatic registration without manual configuration
  - Supports command aliases
  - Supports permission configuration
  - Auto-update custom commands on reload

#### Improvements
- Enhanced open command: Support opening menu for specified player
  - Format: `/kamenu open <menu_name> [player]`
  - If no player specified, opens for self
  - Console must specify player when using this command

- Improved console compatibility: action and open commands now support console usage
- Optimized logging output: Simplified custom command registration logs
- Display registered custom command count on startup and reload

#### Permission Updates
- open command and list command now require `kamenu.admin` permission
