# Global Codex Instructions

  ## Language
  - 默认使用中文回复，除非我明确要求英文。
  - 回答要直接、务实，少说空话。

  ## Working Style
  - 修改代码前，先阅读相关文件并遵循现有风格。
  - 优先做最小、聚焦的改动，不做无关重构。
  - 遇到不确定或高风险操作时先说明判断依据。

  ## Safety
  - 不要删除、覆盖或回滚我已有的改动，除非我明确要求。
  - 执行破坏性命令、安装依赖、修改系统服务前必须先确认。

  ## Coding
  - 优先使用项目已有框架、工具和约定。
  - 搜索文件优先用 rg。
  - 改完后尽量运行相关测试；如果没运行，要说明原因。
  - 最后简短说明改了什么、验证了什么。
  - 若新建方法或类，要备注说明方法或类的用途。

  ## Linux
  - 在 Linux 环境优先给 bash 命令。
  - 路径有歧义时使用绝对路径。
  - 涉及系统服务时优先使用 systemctl 和 journalctl 排查。

## KaMenu 项目信息

- 项目路径：`/home/plugins/KaMenu`。
- 当前主线版本：`2.0.5`。
- 主语言：Kotlin；使用 Gradle 构建，发布产物为 `build/libs/KaMenu-2.0.5.jar`。
- 兼容目标：Minecraft `1.16.5+`，运行时最低 Java `16`。Dialog 仅在当前核心提供可用适配器时启用；Container、动作、变量和自定义指令是低版本的主要功能路径。
- 菜单默认按 Dialog 编写；只有用户明确要求箱子、漏斗、发射器、投掷器、熔炉、高炉、烟熏炉、铁砧等容器界面时，才使用 Container 语法。
- Paper/Folia 和 Spigot 共用一个插件 JAR，通过运行时适配处理差异。`toast` 等 Paper 专属能力不能作为跨平台菜单的必需反馈，跨平台反馈优先使用 `actionbar` 或 `title`。

## 功能与文档同步

- 新增功能或修改既有语法时，必须同步更新：`docs/` 中文文档、`docs-en/` 英文文档，以及 `/root/.codex/skills/kamenu-menu-author/` Skill。新增文档页面时同时更新两份 `SUMMARY.md`。
- Container 按钮变体使用 `Buttons.<id>.variants`：变体拥有完整 `display` 和 `actions`；`priority` 越小越优先，相同优先级保持 YAML 声明顺序；全部未指定优先级时严格按 YAML 从上到下；不要与按钮顶层 `display/actions` 混用。
- Container 玩家真实物品输入只使用顶层 `Free-Slots`：对应 Layout 槽位必须留空，普通按钮仍是只读展示；多材料奖励前先执行原子 `free-slot: type=consume`，关闭与停服恢复依赖持久化托管记录。
- Container 渲染和点击都重新选择当前变体，以避免权限、冷却或其他条件变化后执行旧状态动作。按钮级 `view_condition` 是外层显示条件，变体 `condition` 是候选状态条件。
- DeluxeMenus 迁移默认读取 `plugins/DeluxeMenus/gui_menus`，输出到 `plugins/KaMenu/menus/dm_migrated`，通过 `/km migrate dm [源文件或目录] [输出目录] [overwrite]` 执行；迁移结果必须在实际测试服复核第三方物品、经济和动作。
- TrMenu stable-v3 迁移默认读取 `plugins/TrMenu/menus`，输出到 `plugins/KaMenu/menus/trmenu_migrated`，通过 `/km migrate trmenu [源文件或目录] [输出目录] [overwrite]` 执行。迁移器只生成 KaMenu V2 标准 Container YAML，不在运行时引入 TrMenu、Kether 或 TabooLib；未知条件和动作必须安全失败并输出结构化诊断。
- 物品右键菜单入口统一由 `ItemBindingManager` 加载：旧 `config.yml > listeners.item-lore` 保持兼容，新绑定和 TrMenu `Bindings.Items` 迁移结果写入独立 `item_bindings.yml`。只映射 Bukkit API 可验证的 material、lore、name、data 和 model-data；反向、头颅纹理及未知 trait 必须跳过，不能放宽匹配。
- `data`、`gdata`、`list`、`glist` 可能经过 SQLite/MySQL 异步读写。需要同一动作链立即读取的短期状态优先使用 `meta`；持久化写入后，数据相关条件或动作按需要等待 1 个或数个 tick。
- 每次构建后至少执行 YAML 解析、`git diff --check` 和相关 Gradle 编译/构建；新增类或方法需要添加简洁的用途注释。不要把测试服生成的迁移产物复制回 `src/main/resources`。

## 构建与测试

```bash
cd /home/plugins/KaMenu
bash ./gradlew compileKotlin --no-daemon
bash ./gradlew shadowJar --no-daemon
```

四个测试服均为手动启动，不设置开机自启。四个 tmux socket 和会话如下：

| 核心 | 目录 | Java | 游戏端口 | tmux socket | 会话 |
|---|---|---|---:|---|---|
| Paper 1.16.5 | `/data/MinecraftServer/Paper1.16.5` | `/opt/jdks/jdk-16.0.2+7/bin/java` | `25569` | `minecraft-paper-1.16.5` | `paper1_16_5` |
| Spigot 1.16.5 | `/data/MinecraftServer/Spigot1.16.5` | `/opt/jdks/jdk-16.0.2+7/bin/java` | `25568` | `minecraft-spigot-1.16.5` | `spigot1_16_5` |
| Paper 26.2 | `/home/minecraft/Paper26.2` | `/usr/lib/jvm/jdk-25/bin/java` | `25565` | `minecraft-paper` | `paper26_2` |
| Spigot 26.2 | `/home/minecraft/Spigot26.2` | `/usr/lib/jvm/jdk-25/bin/java` | `25566` | `minecraft-spigot` | `spigot26_2` |

以 `minecraft` 用户操作 tmux。例如查看 Paper 1.16.5 控制台：

```bash
runuser -u minecraft -- tmux -L minecraft-paper-1.16.5 attach -t paper1_16_5
runuser -u minecraft -- tmux -L minecraft-paper-1.16.5 capture-pane -pt paper1_16_5 -S -100
```

启动已停止的测试服：

```bash
runuser -u minecraft -- tmux -L minecraft-paper-1.16.5 new-session -d -s paper1_16_5 -c /data/MinecraftServer/Paper1.16.5 ./start.sh
```

其他服务器替换为对应表格中的 socket、会话和目录。替换插件前先保留带时间戳的 JAR 备份；仅在用户明确要求部署或重启时操作测试服。

## Git 工作区约定

- 用户通常在另一台已配置 GitHub 密钥的设备上完成提交和推送，并通过 Syncthing 同步源码到本机。
- 当用户说明更新版本号时，默认视为最新代码已经推送到 GitHub；本机显示的 Git 脏状态可能只是同步后的提交差异，不应因此阻塞版本更新、构建或测试。
- 不要因版本更新主动执行 `git clean`、`git reset`、回滚文件或删除未提交改动，也不要要求用户先清理本机工作区。
- 只有用户明确说明需要保留本地脏状态、检查差异、清理工作区或执行提交时，才按该要求处理 Git 状态。
- 版本更新时仍需检查版本声明、JAR 名称、更新日志、双语文档和 Skill；Git 状态本身不作为版本是否完成的判断依据。
