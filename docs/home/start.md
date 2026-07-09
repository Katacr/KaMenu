# 🚀 快速开始

本指南将帮助你快速安装和配置 KaMenu 插件。

---

## 🎮 系统要求

| 项目 | 支持详情                       |
|------|----------------------------|
| Minecraft 版本 | 1.21.7+                    |
| Java 版本 | Java 21+                   |
| 服务器类型 | **Paper**、**Folia** 及兼容 Paper Dialog API 的衍生核心 |
| 数据库 | SQLite（默认）、MySQL 5.7+      |

{% hint style="info" %}
**版本功能支持**： 

- ✅ Paper 1.21.7+：基础功能完整支持
- ✨ Paper 1.21.8+：推荐版本，API 更加稳定
- 🎨 Paper 1.21.9+：完整功能（支持 sprite 物品图标、player head 头像等高级特性）
- ⚡ Folia 1.21.7+：支持区域线程调度；建议使用与目标 Minecraft 版本匹配的最新构建
{% endhint %}

{% hint style="info" %}
**Folia 兼容说明**：KaMenu 会自动识别 Folia，并将玩家菜单、`wait`、`Events.Tasks`、JavaScript `delay()`、菜单 API 等任务调度到正确的玩家或全局线程。自定义 JavaScript、外部 action handler、PlaceholderAPI 扩展及由 `console:` 调用的其他插件指令，仍取决于对应代码或插件自身是否兼容 Folia。
{% endhint %}

{% hint style="warning" %}
KaMenu 依赖 Paper Dialog API，**不支持**： 

- Paper 1.21.6 及以下版本（API 尚未完全实现）
- Spigot、CraftBukkit 等非 Paper 服务端

请确保服务器基于 **Paper/Folia 1.21.7 或更高版本**，并提供对应版本的 Dialog API！
{% endhint %}

---

## 📥 安装步骤

### 1. 下载插件

从 GitHub 下载源码构建插件：

{% embed url="https://github.com/Katacr/KaMenu/releases" %}

在以下插件发布平台下载插件：

{% embed url="https://www.spigotmc.org/resources/133736/" %}

{% embed url="https://www.minebbs.com/resources/15814/" %}


### 2. 安装可选依赖

KaMenu 的所有功能均可独立运行，无强制依赖。以下为可选依赖：

**可选依赖：**
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) — 在菜单中使用 `%变量%` 格式的 PAPI 占位符
- [Vault](https://www.spigotmc.org/resources/vault.34315/) — 经济系统集成（如需在动作中操作玩家余额）

### 3. 安装插件

1. 将下载的 KaMenu `.jar` 文件放入服务器的 `plugins` 文件夹
2. 启动服务器
3. 插件会自动：
   - 创建 `plugins/KaMenu/` 配置目录
   - 释放默认配置文件 `config.yml`
   - 初始化数据库（默认为 SQLite）

{% hint style="info" %}
首次使用建议在游戏内执行 `/kamenu guide`（或 `/km guide`）打开入门向导。向导菜单直接从插件 jar 内部加载到内存，不会写入 `menus` 目录。
{% endhint %}

### 4. 打开入门向导

服务器启动后，拥有 `kamenu.admin` 权限的玩家可以执行：

```bash
/kamenu guide
```

入门向导会引导你设置插件语言，并按语言释放示例菜单。示例菜单会写入：

```text
plugins/KaMenu/menus/example/
```

你也可以直接使用指令释放示例菜单：

```bash
# 按当前插件语言释放示例
/kamenu examples

# 释放中文示例
/kamenu examples zh_CN

# 释放英文示例
/kamenu examples en_US
```

{% hint style="info" %}
当服务器当前没有加载任何菜单，且 OP 玩家进入服务器时，KaMenu 会发送一条可点击的入门向导提示，方便首次配置。
{% endhint %}

---

## 🧪 验证安装

服务器启动后，控制台应显示 KaMenu 的启动 Logo，包含版本、数据库类型、已加载菜单数量等信息。

你也可以在游戏中执行以下指令验证安装是否成功：

```
/kamenu guide
```

如果成功弹出入门向导，则表示安装正常。释放示例菜单后，也可以执行 `/km open example/actions_demo` 打开动作示例菜单。

---

## 🔄 热重载

修改配置文件或菜单文件后，无需重启服务器即可重新加载。未指定目标时会重载全部模块：

```
/km reload
```

常用的定向重载：

```bash
/km reload menu      # 仅重载菜单
/km reload config    # 重载 config.yml、语言文件和自定义指令
/km reload actions   # 仅重载全局动作包
/km reload js        # 仅重载全局 JavaScript 包
/km reload lang      # 仅重载当前语言文件
```

需要 `kamenu.admin` 权限。
