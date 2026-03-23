# KaMenu API 文档

KaMenu 提供了公开 API，允许其他插件打开 KaMenu 菜单。

## API 类

### `org.katacr.kamenu.api.KaMenuAPI`

#### 方法

##### `openMenu(Player player, String menuId)`

打开指定的菜单。

**参数：**
- `player` - 目标玩家
- `menuId` - 菜单 ID（如 "main_menu" 或 "shop/weapons"）

**返回值：**
- `boolean` - 是否成功打开菜单

**示例：**

```kotlin
import org.katacr.kamenu.api.KaMenuAPI
import org.bukkit.entity.Player

// 打开主菜单
val success = KaMenuAPI.openMenu(player, "main_menu")
if (!success) {
    player.sendMessage("§c打开菜单失败！")
}

// 打开子文件夹菜单
KaMenuAPI.openMenu(player, "shop/weapons")
```

```java
import org.katacr.kamenu.api.KaMenuAPI;
import org.bukkit.entity.Player;

// 打开主菜单
boolean success = KaMenuAPI.openMenu(player, "main_menu");
if (!success) {
    player.sendMessage("§c打开菜单失败！");
}

// 打开子文件夹菜单
KaMenuAPI.openMenu(player, "shop/weapons");
```

##### `isAvailable()`

检查 KaMenu 是否已加载。

**返回值：**
- `boolean` - KaMenu 是否可用

**示例：**

```kotlin
if (KaMenuAPI.isAvailable()) {
    KaMenuAPI.openMenu(player, "main_menu")
} else {
    player.sendMessage("§cKaMenu 插件未安装或未启用")
}
```

##### `getPlugin()`

获取 KaMenu 插件实例。

**返回值：**
- `KaMenuPlugin` - KaMenu 插件实例，如果未加载则返回 null

## 在其他插件中使用

### 方法 1：直接使用反射（无需依赖）

如果你的插件不需要直接依赖 KaMenu，可以使用反射调用 API：

```kotlin
fun openKaMenu(player: Player, menuId: String): Boolean {
    try {
        val kaMenuPlugin = player.server.pluginManager.getPlugin("KaMenu")
        if (kaMenuPlugin == null) {
            player.sendMessage("§cKaMenu 插件未安装或未启用")
            return false
        }

        val apiClass = Class.forName("org.katacr.kamenu.api.KaMenuAPI")
        val openMenuMethod = apiClass.getMethod("openMenu", org.bukkit.entity.Player::class.java, String::class.java)
        val result = openMenuMethod.invoke(null, player, menuId)
        return result == true
    } catch (e: Exception) {
        e.printStackTrace()
        return false
    }
}
```

### 方法 2：添加依赖（推荐）

如果希望使用类型安全的 API，可以在 `plugin.yml` 中添加 KaMenu 作为依赖：

```yaml
name: YourPlugin
version: 1.0.0
main: com.yourname.yourplugin.Main
api-version: "1.21"
depend:
  - KaMenu
```

然后直接使用：

```kotlin
import org.katacr.kamenu.api.KaMenuAPI

fun openShop(player: Player) {
    KaMenuAPI.openMenu(player, "shop/weapons")
}
```

## 在动作中使用

KaGuilds 插件已经集成了 KaMenu API，可以在菜单配置中使用 `kamenu:` 动作：

```yaml
# KaGuilds 菜单配置示例
guild_manage:
  type: NOTICE
  layout: ...
  buttons:
    kamenu_demo:
      icon: BOOK
      text: "&a打开 KaMenu 演示菜单"
      actions:
        right:
          - 'kamenu: condition_demo'
        left:
          - 'tell: &a正在打开 KaMenu 菜单...'
          - 'kamenu: example/actions_demo'
```

## 注意事项

1. **插件依赖**：确保 KaMenu 已安装并启用
2. **菜单存在**：确保要打开的菜单 ID 存在
3. **异步调用**：API 方法本身是同步的，但在某些情况下可能需要异步调用
4. **错误处理**：建议添加适当的错误处理，以便在 API 调用失败时提供反馈

## 完整示例

以下是一个完整的插件示例，展示如何使用 KaMenu API：

```kotlin
package com.example.plugin

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.katacr.kamenu.api.KaMenuAPI

class ExamplePlugin : JavaPlugin() {
    override fun onEnable() {
        logger.info("ExamplePlugin 已启用")

        // 检查 KaMenu 是否可用
        if (!KaMenuAPI.isAvailable()) {
            logger.warning("KaMenu 插件未安装，部分功能将不可用")
        }
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (command.name.equals("openmenu", ignoreCase = true)) {
            if (sender !is Player) {
                sender.sendMessage("§c该指令只能由玩家执行")
                return true
            }

            if (!KaMenuAPI.isAvailable()) {
                sender.sendMessage("§cKaMenu 插件未安装或未启用")
                return true
            }

            val menuId = if (args.isNotEmpty()) args[0] else "main_menu"
            val success = KaMenuAPI.openMenu(sender, menuId)

            if (success) {
                sender.sendMessage("§a成功打开菜单: $menuId")
            } else {
                sender.sendMessage("§c打开菜单失败，请检查菜单 ID 是否正确")
            }
            return true
        }
        return false
    }
}
```

对应的 `plugin.yml`：

```yaml
name: ExamplePlugin
version: 1.0.0
main: com.example.plugin.ExamplePlugin
api-version: "1.21"
depend:
  - KaMenu

commands:
  openmenu:
    description: 打开 KaMenu 菜单
    usage: /openmenu [菜单ID]
    permission: exampleplugin.openmenu
    permission-message: "§c你没有权限执行此指令"
```
