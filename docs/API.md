# KaMenu API 文档

KaMenu 提供公开 API，允许其他插件打开文件菜单、渲染内存 YAML 菜单，并注册自定义动作命名空间。

## API 类

### `org.katacr.kamenu.api.KaMenuAPI`

#### `openMenu(Player player, String menuId)`

打开由 KaMenu `MenuManager` 加载的菜单。

**参数：**
- `player` - 目标玩家
- `menuId` - 菜单 ID，例如 `"main_menu"` 或 `"shop/weapons"`

**返回值：**
- `boolean` - 是否成功提交打开请求

```kotlin
import org.katacr.kamenu.api.KaMenuAPI

val success = KaMenuAPI.openMenu(player, "main_menu")
KaMenuAPI.openMenu(player, "shop/weapons")
```

```java
import org.katacr.kamenu.api.KaMenuAPI;

boolean success = KaMenuAPI.openMenu(player, "main_menu");
KaMenuAPI.openMenu(player, "shop/weapons");
```

#### `openYaml(Player player, String yaml, String contextId = "external")`

解析内存中的 YAML 字符串，并作为 KaMenu 菜单打开。该 YAML 不会写入 `menus` 目录，也不需要执行菜单重载。

**参数：**
- `player` - 目标玩家
- `yaml` - 完整的 KaMenu 菜单 YAML 内容
- `contextId` - 外部上下文标识，用于日志定位来源

**返回值：**
- `boolean` - 是否成功解析 YAML 并提交打开请求

```kotlin
val yaml = """
Title: "&a游戏中心"
Body:
  welcome:
    type: message
    text: "&7欢迎你，%player_name%！"
Bottom:
  type: notice
  confirm:
    text: "&a加入"
    actions:
      - "kgc:join lobby"
""".trimIndent()

KaMenuAPI.openYaml(player, yaml, "kagamecenter:main")
```

如果 YAML 解析失败，KaMenu 会在警告日志中写入 `contextId`。

#### `openConfig(Player player, YamlConfiguration config, String contextId = "external")`

打开内存中的 `YamlConfiguration`。该配置不要求来自 `MenuManager`。

**参数：**
- `player` - 目标玩家
- `config` - 完整的 KaMenu 菜单配置
- `contextId` - 外部上下文标识，用于日志定位来源

**返回值：**
- `boolean` - 是否成功提交打开请求

```kotlin
import org.bukkit.configuration.file.YamlConfiguration
import org.katacr.kamenu.api.KaMenuAPI

val config = YamlConfiguration()
config.loadFromString(yaml)

KaMenuAPI.openConfig(player, config, "myplugin:dynamic-shop")
```

`Events.Open` 会在渲染前执行。如果 `Open` 事件中执行了 `return`，菜单不会继续打开。外部内存菜单使用 `reset` 且不存在文件菜单 ID 时，KaMenu 会忽略该动作。

#### `registerActionHandler(String namespace, KaMenuActionHandler handler)`

注册自定义动作命名空间。形如 `namespace:payload` 的动作会先交给已注册的 handler 处理，再决定是否继续走 KaMenu 内置动作逻辑。

**参数：**
- `namespace` - 不包含 `:` 的动作命名空间，例如 `"kgc"`
- `handler` - 自定义动作处理器

**返回值：**
- `boolean` - 是否注册成功

```kotlin
import org.katacr.kamenu.api.KaMenuAPI

KaMenuAPI.registerActionHandler("kgc") { player, action, variables, config ->
    val payload = action.removePrefix("kgc:").trim()

    when {
        payload == "open-main" -> {
            KaMenuAPI.openYaml(player, buildMainMenuYaml(player), "kagamecenter:main")
            true
        }
        payload.startsWith("join ") -> {
            val arenaId = payload.removePrefix("join ").trim()
            joinArena(player, arenaId)
            true
        }
        else -> false
    }
}
```

菜单 YAML 中可以在任意支持动作列表的位置使用该命名空间：

```yaml
Bottom:
  type: notice
  confirm:
    text: "&a加入"
    actions:
      - "kgc:join lobby"
```

handler 会收到已解析变量后的动作字符串、输入变量（如 `$(name)`）以及当前菜单配置。返回 `true` 表示动作已处理；返回 `false` 表示未处理，KaMenu 会继续尝试内置动作逻辑。handler 抛出异常时，KaMenu 会记录命名空间和动作内容。

#### `unregisterActionHandler(String namespace)`

注销自定义动作命名空间。

```kotlin
KaMenuAPI.unregisterActionHandler("kgc")
```

#### `isAvailable()`

检查 KaMenu 是否已加载并可用。

```kotlin
if (KaMenuAPI.isAvailable()) {
    KaMenuAPI.openMenu(player, "main_menu")
}
```

#### `getPlugin()`

获取 KaMenu 插件实例。如果 KaMenu 未加载，返回 `null`。

## 动作处理器接口

```kotlin
package org.katacr.kamenu.api

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player

fun interface KaMenuActionHandler {
    fun execute(
        player: Player,
        action: String,
        variables: Map<String, String>,
        rawConfig: YamlConfiguration?
    ): Boolean
}
```

建议为你的插件选择唯一命名空间。除非你明确希望拦截内置动作，否则不要使用 `tell`、`open`、`command`、`console`、`js` 等内置动作名称作为命名空间。

## 在其他插件中使用

### 方法 1：反射调用

如果你的插件不需要直接编译依赖 KaMenu，可以使用反射调用 API：

```kotlin
fun openKaMenu(player: Player, menuId: String): Boolean {
    return try {
        val kaMenuPlugin = player.server.pluginManager.getPlugin("KaMenu") ?: return false
        val apiClass = Class.forName("org.katacr.kamenu.api.KaMenuAPI")
        val openMenuMethod = apiClass.getMethod("openMenu", Player::class.java, String::class.java)
        openMenuMethod.invoke(null, player, menuId) == true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
```

### 方法 2：添加插件依赖

如果希望使用类型安全的 API，可以在 `plugin.yml` 中添加 KaMenu 作为依赖：

```yaml
name: YourPlugin
version: 1.0.0
main: com.yourname.yourplugin.Main
api-version: "1.21"
depend:
  - KaMenu
```

然后直接调用：

```kotlin
import org.katacr.kamenu.api.KaMenuAPI

fun openShop(player: Player) {
    KaMenuAPI.openMenu(player, "shop/weapons")
}
```

## 注意事项

1. **插件依赖**：调用 API 前请确保 KaMenu 已安装并启用。
2. **菜单来源**：文件菜单使用 `openMenu`，动态内存菜单使用 `openYaml` 或 `openConfig`。
3. **线程**：`openConfig` 和 `openYaml` 可以从异步代码调用；KaMenu 会将实际 Dialog 渲染调度回服务器主线程。
4. **Open 事件**：内存菜单同样会在渲染前执行 `Events.Open`。
5. **错误处理**：建议检查布尔返回值，并在 API 调用失败时给玩家或日志提供反馈。

## 完整示例

```kotlin
package com.example.plugin

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.katacr.kamenu.api.KaMenuAPI

class ExamplePlugin : JavaPlugin() {
    override fun onEnable() {
        if (!KaMenuAPI.isAvailable()) {
            logger.warning("KaMenu 插件未安装，部分功能将不可用")
        }

        KaMenuAPI.registerActionHandler("example") { player, action, variables, config ->
            val payload = action.removePrefix("example:").trim()
            if (payload == "reward") {
                player.sendMessage("§a奖励动作已由 ExamplePlugin 处理")
                true
            } else {
                false
            }
        }
    }

    override fun onDisable() {
        KaMenuAPI.unregisterActionHandler("example")
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (!command.name.equals("openmenu", ignoreCase = true)) {
            return false
        }
        if (sender !is Player) {
            sender.sendMessage("§c该指令只能由玩家执行")
            return true
        }

        val yaml = """
        Title: "&a示例菜单"
        Body:
          text:
            type: message
            text: "&7该菜单来自内存 YAML。"
        Bottom:
          type: notice
          confirm:
            text: "&a领取奖励"
            actions:
              - "example:reward"
        """.trimIndent()

        val success = KaMenuAPI.openYaml(sender, yaml, "example:command")
        if (!success) {
            sender.sendMessage("§c打开菜单失败")
        }
        return true
    }
}
```
