# KaMenu v2.0.0 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.0
- **发布日期 / Release Date:** 2026年8月7日 / August 7, 2026

---

## 中文

### V2 容器菜单

- 新增基于 Bukkit 虚拟库存的 Container 菜单运行时，默认不依赖 ProtocolLib、PacketEvents 或 NMS 发包层。
- 支持 `CHEST`、`HOPPER`、`DISPENSER`、`DROPPER`、`FURNACE`、`BLAST_FURNACE`、`SMOKER` 和 `ANVIL` 容器类型。
- 新增 `Type`、`Layout`、`Buttons`、`Properties`、`Update`、`Title-Update` 和按钮级 `update` 配置。
- Container 菜单复用 KaMenu 的变量、PlaceholderAPI、条件、JavaScript、actions 包、生命周期事件和动作队列。
- 增加熔炉火焰/加工箭头百分比刷新，以及 `Events.Progress` 边沿触发事件。
- 增加铁砧输入、重命名结果和 `$(input)` 上下文支持。
- 所有容器交互、拖拽和快捷键库存操作均按只读菜单处理，防止展示物品进入玩家库存。

### DeluxeMenus 迁移

- 新增 `/km migrate dm`，默认读取 `plugins/DeluxeMenus/gui_menus`，也支持手动指定源文件和目录。
- 支持 DM 菜单布局、槽位、物品显示、条件、点击动作、分页和 `open_command` 迁移。
- `open_command` 会写入 KaMenu V2 标准菜单和 `custom_commands.yml`。
- 支持 `overwrite` 覆盖目标菜单和冲突的自定义指令；默认保留现有配置并报告冲突。

### 自定义指令独立化

- 自定义指令从 `config.yml` 独立到插件根目录的 `custom_commands.yml`。
- 新增独立文件管理器，负责默认文件释放、配置读取、保存、旧配置迁移和备份。
- `/km reload config` 和 `/km reload all` 会同步重载 `config.yml`、`custom_commands.yml` 和在线玩家的命令补全。
- 旧版 `config.yml > custom-commands` 会在启动时自动迁移，并生成 `custom_commands_legacy_backup_<时间戳>.yml`。
- 配置版本升级为 v6。

### 兼容性边界

- 保留 Paper、Folia 和 Spigot 的现有 Dialog 支持、平台适配和生命周期清理。
- Container 菜单当前使用 Bukkit 公共虚拟库存方案，不提供后台熔炉模拟，也不包含基于 NMS 的假窗口实现。
- 公共运行时编译基线降至 Bukkit/Spigot 1.16.5，并将公共运行时字节码目标降至 Java 16；仅在现代核心上加载的 Dialog 适配器仍单独使用 Java 21。
- 构建环境仍使用 Java 21；Paper/Folia 1.21.7+ 服务端本身继续要求 Java 21。
- Paper/Folia 1.21.7+ 和 Spigot 1.21.6+ 才启用原生 Dialog；较低核心自动降级为无 Dialog 模式，Container、actions、变量、JavaScript、存储和自定义指令仍可用。
- 仅将 Libby 随插件 JAR 提供；Kotlin 会在无 Kotlin 依赖的 `onLoad` 引导段优先热加载，随后再挂载 Adventure/MiniMessage 及其他运行依赖。
- 旧版 Paper 内置的 Adventure 不满足新版 MiniMessage 时会自动回退到 Legacy 文本，避免依赖冲突导致插件无法启用；`sprite`、`head` 等现代标签仅在兼容的现代核心上生效。
- bStats 运行库通过 Libby 下载并重定位到 KaMenu 私有命名空间，避免触发 bStats 的未重定位检查，也不增加插件 JAR 体积。

---

## English

### V2 Container Menus

- Added a Bukkit virtual-inventory Container menu runtime without requiring ProtocolLib, PacketEvents, or an NMS packet layer by default.
- Supports `CHEST`, `HOPPER`, `DISPENSER`, `DROPPER`, `FURNACE`, `BLAST_FURNACE`, `SMOKER`, and `ANVIL` container types.
- Added `Type`, `Layout`, `Buttons`, `Properties`, `Update`, `Title-Update`, and button-level `update` configuration.
- Container menus reuse KaMenu variables, PlaceholderAPI, conditions, JavaScript, action packages, lifecycle events, and action queues.
- Added furnace flame/cook-arrow percentage refresh and edge-triggered `Events.Progress` watchers.
- Added anvil input, rename-result, and `$(input)` context support.
- Container clicks, drags, and inventory hotkeys are treated as read-only menu interactions so display items cannot enter player inventories.

### DeluxeMenus Migration

- Added `/km migrate dm`, which scans `plugins/DeluxeMenus/gui_menus` by default and also accepts an explicit file or directory.
- Supports migration of DM layouts, slots, item display properties, conditions, click actions, pagination, and `open_command`.
- `open_command` entries are written to KaMenu V2 menus and `custom_commands.yml`.
- `overwrite` replaces target menus and conflicting custom commands; existing configuration is preserved and conflicts are reported by default.

### Separate Custom Commands

- Moved custom commands from `config.yml` into the plugin-root `custom_commands.yml`.
- Added an independent file manager for default-file release, loading, saving, legacy migration, and backups.
- `/km reload config` and `/km reload all` now reload `config.yml`, `custom_commands.yml`, and online players' command completions together.
- Legacy `config.yml > custom-commands` entries are migrated on startup and backed up as `custom_commands_legacy_backup_<timestamp>.yml`.
- Configuration version is now v6.

### Compatibility Boundaries

- Existing Paper, Folia, and Spigot Dialog support, platform adapters, and lifecycle cleanup are retained.
- Container menus currently use Bukkit's public virtual-inventory implementation. Background furnace simulation and an NMS fake-window backend are outside this release.
- The shared runtime now compiles against the Bukkit/Spigot 1.16.5 API baseline and targets Java 16 bytecode; isolated Dialog adapters loaded only on modern cores continue to target Java 21.
- The build environment still uses Java 21, and Paper/Folia 1.21.7+ servers continue to require Java 21 themselves.
- Native Dialogs are enabled only on Paper/Folia 1.21.7+ and Spigot 1.21.6+; older cores automatically use no-Dialog mode while retaining Container menus, actions, variables, JavaScript, storage, and custom commands.
- Only Libby is bundled in the plugin JAR. A Kotlin-free `onLoad` bootstrap first hot-loads Kotlin, then mounts Adventure/MiniMessage and the remaining runtime dependencies.
- When an older Paper build provides an Adventure API that is incompatible with current MiniMessage, KaMenu falls back to Legacy text instead of failing during startup. Modern tags such as `sprite` and `head` remain available only on compatible modern cores.
- bStats is downloaded by Libby and relocated into KaMenu's private namespace, satisfying bStats relocation checks without increasing the plugin JAR size.

---

## Upgrade Guide

### Upgrading from 1.7.3 to 2.0.0

1. Replace the plugin JAR with `KaMenu-2.0.0.jar`.
2. Fully restart the server.
3. On the first startup, old `config.yml > custom-commands` entries are moved to `plugins/KaMenu/custom_commands.yml`.
4. Review the generated `custom_commands_legacy_backup_<timestamp>.yml` backup if you need to compare the migrated entries.
5. Existing Dialog menus do not require syntax changes. Container menus use the new V2 `Type`/`Layout`/`Buttons` format.
