---
description: KaMenu - 专为 Minecraft Paper 服务器设计的现代化 Dialog 菜单插件
---

# 🏠 首页

> 基于 Paper Dialog API 的下一代 Minecraft GUI 插件，超越传统箱子菜单的全新交互体验

**KaMenu** 是一款专为现代 Minecraft Paper 服务器打造的 GUI 插件。它抛弃了传统的箱子 (Inventory) 菜单模式，转而采用 **Paper 1.21.6+ 原生 Dialog API**，为玩家提供包含文本输入、滑块、下拉框、复选框等丰富交互组件的现代化菜单界面。配置文件基于 YAML，简洁直观，开箱即用。

***

## ✨ 核心特性

### 🖥️ 基于 Dialog API 的现代化 GUI

告别传统箱子菜单，拥抱原生 UI：

* 全新的 Paper Dialog 菜单界面，外观更现代
* 支持多种富交互组件：文本输入框、数值滑块、下拉选择框、复选框
* 支持物品展示 (Item)、纯文字消息 (Message) 等内容组件
* 三种底部按钮布局模式：`notice`、`confirmation`、`multi`

### 🔧 高度可定制

* 完全基于 YAML 配置，无需任何编程知识
* 支持多层文件夹结构，轻松管理大量菜单
* 支持热重载，修改配置后无需重启服务器

### 🔀 强大的动作系统

支持丰富的按钮点击动作：

* `tell` / `actionbar` / `title` — 多种消息发送方式
* `command` / `console` — 执行玩家或控制台指令
* `sound` — 播放声音（支持音量、音调、分类参数）
* `open` / `close` — 菜单跳转与关闭
* `hovertext` — 可悬停和点击的聊天文本
* `wait` — 延迟执行后续动作
* `set-data` / `set-gdata` — 读写持久化数据
* `url` / `copy` — 打开链接或复制到剪贴板

### 🔍 通用条件判断

* 在**任意文本字段**（标题、按钮文字、组件文本）中使用条件判断
* 在**动作列表**中嵌套条件，实现分支执行逻辑
* 支持 PlaceholderAPI 变量、比较运算符（`==` `!=` `>` `<` `>=` `<=`）及逻辑运算符（`&&` `||`）

### 💾 内置数据存储

* 支持 **SQLite**（默认）和 **MySQL** 双数据库
* **玩家数据** (`{data:key}`)：按玩家 UUID 存储的独立键值对
* **全局数据** (`{gdata:key}`)：所有玩家共享的键值对
* 通过 **PlaceholderAPI** 扩展对外暴露数据变量

### 🌐 快捷打开方式

* `/km open <菜单ID>` — 标准指令打开
* **快捷键监听**：支持配置按 `F`（切换副手）触发打开指定菜单
* **自定义指令注册**：一行配置将任意单词变为打开菜单的快捷指令

### 📊 PlaceholderAPI 支持

* 完整支持 PAPI 变量解析（菜单标题、组件文本、动作中均可使用）
* 提供 `%kamenu_data_<key>%` 和 `%kamenu_gdata_<key>%` 变量

***

## 💰 支持

KaMenu 是一款免费开源插件，您可以在 GitHub 下载源代码并自行构建最新实验功能使用。

{% embed url="https://github.com/Katacr/KaMenu/" %}


***

## 🤝 社区与反馈

* **GitHub**: [Katacr/KaMenu](https://github.com/Katacr/KaMenu/)
* **问题反馈**: [GitHub Issues](https://github.com/Katacr/KaMenu/issues)

## 📄 许可证

本项目采用 [GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html) 许可证开源。
