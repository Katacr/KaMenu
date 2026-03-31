# 🚀 快速开始

本指南将帮助你快速安装和配置 KaMenu 插件。

---

## 🎮 系统要求

| 项目 | 支持详情                       |
|------|----------------------------|
| Minecraft 版本 | 1.21.7+                    |
| Java 版本 | Java 21+                   |
| 服务器类型 | **Paper** 及其衍生核心（Purpur 等） |
| 数据库 | SQLite（默认）、MySQL 5.7+      |

{% hint style="info" %}
**版本功能支持**：
- ✅ Paper 1.21.7+：基础功能完整支持
- ✨ Paper 1.21.8+：推荐版本，API 更加稳定
- 🎨 Paper 1.21.9+：完整功能（支持 sprite 物品图标、player head 头像等高级特性）
{% endhint %}

{% hint style="warning" %}
KaMenu 依赖 Paper Dialog API，**不支持**：
- Paper 1.21.6 及以下版本（API 尚未完全实现）
- Spigot、CraftBukkit 等非 Paper 服务端

请确保服务器版本为 **Paper 1.21.7 或更高版本**！
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
   - 释放默认配置文件 `config.yml` 和示例菜单
   - 初始化数据库（默认为 SQLite）

{% hint style="info" %}
首次启动后，`plugins/KaMenu/menus/` 目录会自动生成示例菜单文件，你可以直接参考或修改它们。
{% endhint %}

---

## 🧪 验证安装

服务器启动后，控制台应显示 KaMenu 的启动 Logo，包含版本、数据库类型、已加载菜单数量等信息。

你也可以在游戏中执行以下指令验证安装是否成功：

```
/km open example/actions_demo
```

如果成功弹出一个演示菜单，则表示安装正常。

---

## 🔄 热重载

修改配置文件或菜单文件后，无需重启服务器，执行以下指令即可重新加载：

```
/km reload
```

需要 `kamenu.admin` 权限。
