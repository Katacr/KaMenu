@file:Suppress("UnstableApiUsage")

package org.katacr.kamenu

import com.google.common.io.ByteStreams
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.title.Title
import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.SoundCategory
import org.bukkit.entity.Player
import com.google.common.io.ByteArrayDataOutput
import java.time.Duration

/**
 * 具体动作处理工具。
 *
 * [MenuActions] 负责动作序列和生命周期，本对象只处理某一类动作的参数解析与落地执行，
 * 例如 sound、title、toast、money、stock-item、item、server、data/list 参数等。
 *
 * 新增动作时优先判断是否需要影响动作序列；若只是单个行为，通常应放在这里。
 */
object ActionHandlers {

    private var languageManager: LanguageManager? = null
    private var databaseManager: DatabaseManager? = null
    private var metaDataManager: MetaDataManager? = null
    private var economy: Economy? = null
    private var plugin: KaMenu? = null
    private var itemManager: ItemManager? = null
    private var bungeeCordEnabled: Boolean = false

    // ==================== 初始化 ====================

    fun init(plugin: KaMenu) {
        this.plugin = plugin
    }

    fun setLanguageManager(manager: LanguageManager) {
        languageManager = manager
    }

    fun setDatabaseManager(manager: DatabaseManager) {
        databaseManager = manager
    }

    fun setMetaDataManager(manager: MetaDataManager) {
        metaDataManager = manager
    }

    fun setEconomy(econ: Economy?) {
        economy = econ
    }

    fun setItemManager(manager: ItemManager) {
        itemManager = manager
    }

    fun setBungeeCordEnabled(enabled: Boolean) {
        bungeeCordEnabled = enabled
    }

    // ==================== 变量解析 ====================

    /**
     * 解析带输入变量的文本。
     *
     * 兼容旧入口，实际委托给 [TextResolver]。
     */
    fun resolveVariablesWithInput(player: Player, text: String, variables: Map<String, String> = emptyMap()): String {
        return TextResolver.resolve(player, text, variables)
    }

    /**
     * 解析普通动作文本中的内置变量和 PAPI。
     */
    fun resolveVariables(player: Player, text: String): String {
        return resolveVariablesWithInput(player, text, emptyMap())
    }

    // ==================== 具体动作处理器 ====================

    /**
     * 解析并播放声音。
     *
     * 参数格式：`sound_name;volume=1.0;pitch=1.0;category=master`。
     */
    fun parseAndPlaySound(player: Player, args: String) {
        var soundName = ""
        var volume = 1f
        var pitch = 1f
        var category = SoundCategory.MASTER

        val params = args.split(";")
        for (param in params) {
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "volume" -> volume = value.toFloatOrNull() ?: 1f
                    "pitch" -> pitch = value.toFloatOrNull() ?: 1f
                    "category" -> category = value.lowercase().let { cat ->
                        when (cat) {
                            "master" -> SoundCategory.MASTER
                            "music" -> SoundCategory.MUSIC
                            "record" -> SoundCategory.RECORDS
                            "weather" -> SoundCategory.WEATHER
                            "block" -> SoundCategory.BLOCKS
                            "hostile" -> SoundCategory.HOSTILE
                            "neutral" -> SoundCategory.NEUTRAL
                            "player" -> SoundCategory.PLAYERS
                            "ambient" -> SoundCategory.AMBIENT
                            "voice" -> SoundCategory.VOICE
                            "ui" -> SoundCategory.UI
                            else -> {
                                player.sendMessage(languageManager?.getMessage("actions.unknown_sound_category", cat) ?: "§c未知的声音类别: $cat")
                                SoundCategory.MASTER
                            }
                        }
                    }
                    else -> soundName = soundName.ifEmpty { parts[0].trim() }
                }
            } else if (parts.size == 1 && param.trim().isNotEmpty()) {
                if (soundName.isEmpty()) soundName = param.trim()
            }
        }

        if (soundName.isNotEmpty()) {
            val normalizedSoundName = soundName.lowercase()
            val sound = listOf(
                normalizedSoundName,
                normalizedSoundName.replace('_', '.')
            ).asSequence()
                .distinct()
                .mapNotNull { key -> parseSoundKey(key)?.let { org.bukkit.Registry.SOUND_EVENT.get(it) } }
                .firstOrNull()

            if (sound != null) {
                player.playSound(player.location, sound, category, volume, pitch)
            } else {
                // Registry 中不存在的声音可能来自资源包 sounds.json，字符串 API 可直接转发给客户端播放。
                player.playSound(player.location, normalizedSoundName, category, volume, pitch)
            }
        }
    }

    /**
     * 解析原版或带命名空间的声音 Key。
     *
     * 原版声音使用 minecraft namespace；资源包自定义声音可能是 `namespace:path`，
     * 这类声音通常不会存在于服务端 Registry 中，但解析时也不能让非法 Key 中断动作执行。
     */
    private fun parseSoundKey(soundName: String): NamespacedKey? {
        return try {
            if (soundName.contains(":")) {
                NamespacedKey.fromString(soundName)
            } else {
                NamespacedKey.minecraft(soundName)
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /**
     * 解析并发送标题。
     *
     * 参数格式：`title=主标题;subtitle=副标题;in=10;keep=60;out=20`，时间单位为 tick。
     */
    fun parseAndSendTitle(player: Player, args: String) {
        var title = ""
        var subtitle = ""
        var fadeIn = 0
        var stay = 60
        var fadeOut = 20

        val params = args.split(";")
        for (param in params) {
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "title" -> title = value
                    "subtitle" -> subtitle = value
                    "in" -> fadeIn = value.toIntOrNull() ?: 0
                    "keep" -> stay = value.toIntOrNull() ?: 60
                    "out" -> fadeOut = value.toIntOrNull() ?: 20
                }
            }
        }

        val titleComponent = if (title.isEmpty()) Component.empty() else TextParser.parseText(title)
        val subtitleComponent = if (subtitle.isEmpty()) Component.empty() else TextParser.parseText(subtitle)

        // 使用 Adventure API 的 Title (Paper 推荐方式)
        val titleTimes = Title.Times.times(
            Duration.ofMillis(fadeIn * 50L),   // ticks to milliseconds
            Duration.ofMillis(stay * 50L),     // ticks to milliseconds
            Duration.ofMillis(fadeOut * 50L)   // ticks to milliseconds
        )
        val adventureTitle = Title.title(titleComponent, subtitleComponent, titleTimes)
        player.showTitle(adventureTitle)
    }

    /**
     * 解析并发送 Toast 通知
     */
    fun parseAndSendToast(player: Player, args: String) {
        var frameType = "task"
        var iconItem = "minecraft:stone"
        var title = "提示"
        var description = ""

        // 解析参数
        args.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "type" -> frameType = value.lowercase()
                    "icon" -> iconItem = if (value.contains(":")) value else "minecraft:$value"
                    "msg" -> title = value
                    "description" -> description = value
                }
            }
        }

        // 生成唯一 Key
        val randomKey = NamespacedKey("kamenu", "toast_${System.currentTimeMillis()}")

        val titleJson = GsonComponentSerializer.gson().serialize(TextParser.parseText(title))
        val descJson = GsonComponentSerializer.gson().serialize(TextParser.parseText(description))

        val advancementJson = """
    {
      "display": {
        "icon": {
          "id": "${iconItem.lowercase()}"
        },
        "title": $titleJson,
        "description": $descJson,
        "frame": "${if (frameType in listOf("task", "goal", "challenge")) frameType else "task"}",
        "show_toast": true,
        "announce_to_chat": false,
        "hidden": true
      },
      "criteria": {
        "impossible": {
          "trigger": "minecraft:impossible"
        }
      }
    }
    """.trimIndent()

        try {
            val advancement = Bukkit.getUnsafe().loadAdvancement(randomKey, advancementJson)
            val progress = player.getAdvancementProgress(advancement)
            progress.awardCriteria("impossible")

            KaScheduler.runPlayerLater(player, 10L, Runnable {
                if (player.isOnline) {
                    progress.revokeCriteria("impossible")
                    Bukkit.getUnsafe().removeAdvancement(randomKey)
                }
            })
        } catch (e: Exception) {
            plugin?.logger?.warning("Toast 通知发送失败: ${e.message}")
        }
    }

    /**
     * 解析并处理金币操作
     */
    fun parseAndHandleMoney(player: Player, args: String, variables: Map<String, String> = emptyMap()) {
        if (economy == null) {
            plugin?.logger?.warning("经济系统未启用，无法执行 money 动作。玩家: ${player.name}")
            return
        }

        var type = ""
        var amountStr = "0"

        val params = args.split(";")
        for (param in params) {
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "type" -> type = value.lowercase()
                    "num" -> amountStr = value
                }
            }
        }

        // 使用通用的变量解析方法
        val finalAmountStr = resolveVariablesWithInput(player, amountStr, variables)
        val amount = finalAmountStr.toDoubleOrNull() ?: 0.0

        val balance = economy!!.getBalance(player)

        when (type) {
            "add" -> {
                economy!!.depositPlayer(player, amount)
            }
            "take" -> {
                if (balance >= amount) {
                    economy!!.withdrawPlayer(player, amount)
                } else {
                    plugin?.logger?.warning("玩家 ${player.name} 余额不足，无法扣除 $amount 金币。当前余额: $balance")
                }
            }
            "reset" -> {
                val difference = amount - balance
                if (difference > 0) {
                    economy!!.depositPlayer(player, difference)
                } else if (difference < 0) {
                    economy!!.withdrawPlayer(player, -difference)
                }
            }
            else -> {
                plugin?.logger?.warning("无效的金币操作类型: $type。玩家: ${player.name}")
            }
        }
    }

    /**
     * 解析旧的 set-data/set-gdata/set-meta 格式（向后兼容）
     */
    fun parseDataAction(
        args: String,
        uuid: String,
        dataType: String,
        action: (String, String, String) -> Unit
    ) {
        val parts = args.split(" ", limit = 2)
        if (parts.size >= 2) {
            val key = parts[0]
            val value = parts[1]
            action(uuid, key, value)
        }
    }

    /**
     * 解析并执行新的 data/gdata/meta 动作
     */
    fun parseAndExecuteDataAction(
        args: String,
        player: Player,
        dataType: String,
        setAction: (String, String) -> Unit,
        modifyAction: (String, String) -> Unit,
        deleteAction: (String) -> Unit = {}
    ) {
        var type = ""
        var key = ""
        var value = ""

        // 解析参数
        args.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val paramKey = parts[0].trim().lowercase()
                val paramValue = parts[1].trim().removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                when (paramKey) {
                    "type" -> type = paramValue.lowercase()
                    "key" -> key = paramValue
                    "var" -> value = paramValue
                }
            }
        }

        if (key.isEmpty()) {
            plugin?.logger?.warning("$dataType 操作失败: 缺少 key 参数。玩家: ${player.name}")
            return
        }

        when (type) {
            "set" -> {
                setAction(key, value)
            }
            "add" -> {
                val numValue = value.toDoubleOrNull()
                if (numValue != null) {
                    modifyAction(key, numValue.toString())
                } else {
                    plugin?.logger?.warning("$dataType 操作失败: add/take 操作的值 '$value' 不是纯数字。玩家: ${player.name}")
                }
            }
            "take" -> {
                val numValue = value.toDoubleOrNull()
                if (numValue != null) {
                    modifyAction(key, (-numValue).toString())
                } else {
                    plugin?.logger?.warning("$dataType 操作失败: add/take 操作的值 '$value' 不是纯数字。玩家: ${player.name}")
                }
            }
            "delete" -> {
                deleteAction(key)
            }
            else -> {
                plugin?.logger?.warning("$dataType 操作失败: 无效的 type 参数 '$type'，支持的类型: set, add, take, delete。玩家: ${player.name}")
            }
        }
    }

    /**
     * 解析并执行 list/glist 动作。
     *
     * 列表以 JSON 字符串数组存储，读取变量时可直接作为 repeat source 使用。
     */
    fun parseAndExecuteListAction(
        args: String,
        player: Player,
        dataType: String,
        setAction: (String, List<String>) -> Unit,
        addAction: (String, List<String>, Boolean) -> Unit,
        removeAction: (String, List<String>) -> Unit,
        clearAction: (String) -> Unit,
        deleteAction: (String) -> Unit
    ) {
        var type = ""
        var key = ""
        var value = ""
        var split = ""
        var unique = true

        args.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val paramKey = parts[0].trim().lowercase()
                val paramValue = parts[1].trim().removeSurrounding("`").removeSurrounding("'").removeSurrounding("\"")
                when (paramKey) {
                    "type" -> type = paramValue.lowercase()
                    "key" -> key = paramValue
                    "var", "value" -> value = paramValue
                    "split", "separator" -> split = paramValue
                    "unique" -> unique = !(paramValue.equals("false", ignoreCase = true) || paramValue == "0" || paramValue.equals("no", ignoreCase = true))
                }
            }
        }

        if (key.isEmpty()) {
            plugin?.logger?.warning("$dataType 操作失败: 缺少 key 参数。玩家: ${player.name}")
            return
        }

        fun parseValues(): List<String> {
            if (value.isEmpty()) return emptyList()
            if (split.isNotEmpty()) {
                val separator = when (split.lowercase()) {
                    "\\n", "newline", "line" -> "\n"
                    "\\t", "tab" -> "\t"
                    else -> split
                }
                return value.split(separator).map { it.trim() }.filter { it.isNotEmpty() }
            }
            if (value.trim().startsWith("[") && value.trim().endsWith("]")) {
                return DatabaseManager.decodeStringList(value)
            }
            return listOf(value)
        }

        when (type) {
            "set", "create" -> {
                setAction(key, parseValues())
            }
            "add", "append" -> {
                val values = parseValues()
                if (values.isEmpty()) {
                    plugin?.logger?.warning("$dataType 操作失败: add 操作缺少 var 参数。玩家: ${player.name}")
                } else {
                    addAction(key, values, unique)
                }
            }
            "remove", "take" -> {
                val values = parseValues()
                if (values.isEmpty()) {
                    plugin?.logger?.warning("$dataType 操作失败: remove/take 操作缺少 var 参数。玩家: ${player.name}")
                } else {
                    removeAction(key, values)
                }
            }
            "clear" -> {
                clearAction(key)
            }
            "delete" -> {
                deleteAction(key)
            }
            else -> {
                plugin?.logger?.warning("$dataType 操作失败: 无效的 type 参数 '$type'，支持的类型: set, add, remove, take, clear, delete。玩家: ${player.name}")
            }
        }
    }

    /**
     * 解析并处理存储库物品给予/扣除动作
     */
    fun parseAndHandleStockItem(player: Player, args: String, variables: Map<String, String> = emptyMap()) {
        if (itemManager == null) {
            languageManager?.getMessage("actions.stock_item_manager_not_init", player.name)?.let {
                plugin?.logger?.warning(it)
            }
            return
        }

        var type = ""
        var itemName = ""
        var amountStr = "1"

        // 解析参数
        args.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "type" -> type = value.lowercase()
                    "name" -> itemName = value
                    "amount" -> amountStr = value
                }
            }
        }

        // 解析变量替换
        val finalItemName = resolveVariablesWithInput(player, itemName, variables)
        val finalAmountStr = resolveVariablesWithInput(player, amountStr, variables)
        val amount = (finalAmountStr.toIntOrNull() ?: 1).coerceAtLeast(1)

        if (finalItemName.isEmpty()) {
            languageManager?.getMessage("actions.stock_item_missing_name", player.name)?.let {
                plugin?.logger?.warning(it)
            }
            return
        }

        // 从数据库获取物品
        val item = itemManager!!.getItem(finalItemName)
        if (item == null) {
            languageManager?.getMessage("condition.stock_item_not_exist", finalItemName)?.let {
                player.sendMessage(TextParser.parseText(it))
            }
            return
        }

        when (type) {
            "give" -> {
                // 给予物品
                val itemToGive = item.clone()
                itemToGive.amount = amount
                val leftover = player.inventory.addItem(itemToGive)

                if (leftover.isNotEmpty()) {
                    // 物品栏已满，将剩余物品掉落在地上
                    var droppedAmount = 0
                    leftover.values.forEach { item ->
                        player.world.dropItem(player.location, item)
                        droppedAmount += item.amount
                    }

                    // 发送 actionbar 提示和拾取音效
                    val actionbarMessage = languageManager?.getMessage("actions.inventory_full_actionbar", droppedAmount.toString())
                    if (actionbarMessage != null) {
                        player.sendActionBar(TextParser.parseText(actionbarMessage))
                    }
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f)
                }
            }
            "take" -> {
                // 扣除物品
                val inventory = player.inventory
                var remaining = amount

                // 遍历背包中的所有物品（包括盔甲、副手物品栏和背包）
                val allItems = mutableListOf<org.bukkit.inventory.ItemStack>()
                allItems.addAll(inventory.storageContents.filterNotNull())
                allItems.addAll(inventory.armorContents.filterNotNull())
                allItems.add(inventory.itemInOffHand)
                allItems.add(inventory.itemInMainHand)

                for (stack in allItems) {
                    if (remaining <= 0) break
                    if (!stack.isEmpty && stack.isSimilar(item)) {
                        val stackAmount = stack.amount
                        if (stackAmount <= remaining) {
                            // 整个堆叠都扣除
                            stack.amount = 0
                            remaining -= stackAmount
                        } else {
                            // 部分扣除
                            stack.amount -= remaining
                            remaining = 0
                        }
                    }
                }

                // 更新玩家背包
                player.updateInventory()
            }
            else -> {
                languageManager?.getMessage("actions.stock_item_unknown_type", type, player.name)?.let {
                    plugin?.logger?.warning(it)
                }
            }
        }
    }

    /**
     * 解析并处理服务器传送
     */
    fun parseAndHandleServer(player: Player, serverName: String) {
        if (serverName.isEmpty()) {
            return
        }

        if (bungeeCordEnabled) {
            // 使用 BungeeCord 插件消息系统（不需要玩家权限）
            try {
                val out: ByteArrayDataOutput = ByteStreams.newDataOutput()
                out.writeUTF("Connect")
                out.writeUTF(serverName)

                plugin?.let { player.sendPluginMessage(it, "BungeeCord", out.toByteArray()) }
            } catch (e: Exception) {
                plugin?.logger?.severe("Connect ${player.name} server $serverName error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * 解析并处理普通物品给予/扣除动作
     * 格式: type=give;mats=材质;amount=数量 或 type=take;mats=材质;amount=数量;lore=描述;model=模型
     */
    fun parseAndHandleItem(player: Player, args: String, variables: Map<String, String> = emptyMap()) {
        var type = ""
        var materialName = ""
        var amountStr = "1"
        var loreText: String? = null
        var itemModel: String? = null

        // 解析参数
        args.split(";").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim().lowercase()
                val value = parts[1].trim()
                when (key) {
                    "type" -> type = value.lowercase()
                    "mats" -> materialName = value
                    "amount" -> amountStr = value
                    "lore" -> loreText = value
                    "model" -> itemModel = value
                }
            }
        }

        // 解析变量替换
        val finalMaterialName = resolveVariablesWithInput(player, materialName, variables)
        val finalAmountStr = resolveVariablesWithInput(player, amountStr, variables)
        val finalLoreText = loreText?.let { resolveVariablesWithInput(player, it, variables) }
        val finalItemModel = itemModel?.let { resolveVariablesWithInput(player, it, variables) }
        val amount = (finalAmountStr.toIntOrNull() ?: 1).coerceAtLeast(1)

        // 材质是必需的
        if (finalMaterialName.isEmpty()) {
            languageManager?.getMessage("actions.item_missing_material", player.name)?.let {
                plugin?.logger?.warning(it)
            }
            return
        }

        // 获取材质（使用规范化的材质匹配）
        val material = MaterialUtils.matchMaterial(finalMaterialName)
        if (material == null) {
            languageManager?.getMessage("actions.item_invalid_material", finalMaterialName, player.name)?.let {
                plugin?.logger?.warning(it)
            }
            return
        }

        when (type) {
            "give" -> {
                // 给予物品（只需要材质和数量，忽略lore和model）
                val itemToGive = org.bukkit.inventory.ItemStack(material, amount)
                val leftover = player.inventory.addItem(itemToGive)

                if (leftover.isNotEmpty()) {
                    // 物品栏已满，将剩余物品掉落在地上
                    var droppedAmount = 0
                    leftover.values.forEach { item ->
                        player.world.dropItem(player.location, item)
                        droppedAmount += item.amount
                    }

                    // 发送 actionbar 提示和拾取音效
                    val actionbarMessage = languageManager?.getMessage("actions.inventory_full_actionbar", droppedAmount.toString())
                    if (actionbarMessage != null) {
                        player.sendActionBar(TextParser.parseText(actionbarMessage))
                    }
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f)
                }
            }
            "take" -> {
                // 扣除物品（支持lore和model判断）
                val inventory = player.inventory
                var remaining = amount

                // 遍历背包中的所有物品（包括盔甲、副手物品栏和背包）
                val allItems = mutableListOf<org.bukkit.inventory.ItemStack>()
                allItems.addAll(inventory.storageContents.filterNotNull())
                allItems.addAll(inventory.armorContents.filterNotNull())
                allItems.add(inventory.itemInOffHand)
                allItems.add(inventory.itemInMainHand)

                for (stack in allItems) {
                    if (remaining <= 0) break
                    if (!stack.isEmpty && stack.type == material) {
                        // 检查 lore 是否匹配（如果指定了 lore）
                        if (finalLoreText != null) {
                            val itemMeta = stack.itemMeta
                            if (itemMeta != null && itemMeta.hasLore()) {
                                val lore = itemMeta.lore()
                                // 检查 lore 中是否包含指定字符串（忽略大小写）
                                val loreMatched = lore?.any { line ->
                                    val plainText = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(line)
                                    plainText.contains(finalLoreText, ignoreCase = true)
                                } ?: false
                                if (!loreMatched) {
                                    continue
                                }
                            } else {
                                // 没有 lore，跳过
                                continue
                            }
                        }

                        // 检查 item_model 是否匹配（如果指定了 model）
                        if (finalItemModel != null) {
                            val itemMeta = stack.itemMeta
                            if (itemMeta != null && itemMeta.hasItemModel()) {
                                val modelKey = itemMeta.itemModel
                                if (modelKey != null) {
                                    // 格式化为 namespace:key
                                    val modelStr = "${modelKey.namespace()}:${modelKey.value()}"
                                    if (!modelStr.equals(finalItemModel, ignoreCase = true)) {
                                        continue
                                    }
                                } else {
                                    // 没有模型，跳过
                                    continue
                                }
                            } else {
                                // 没有模型，跳过
                                continue
                            }
                        }

                        // 符合所有条件，执行扣除
                        val stackAmount = stack.amount
                        if (stackAmount <= remaining) {
                            // 整个堆叠都扣除
                            stack.amount = 0
                            remaining -= stackAmount
                        } else {
                            // 部分扣除
                            stack.amount -= remaining
                            remaining = 0
                        }
                    }
                }

                // 更新玩家背包
                player.updateInventory()
            }
            else -> {
                languageManager?.getMessage("actions.item_unknown_type", type, player.name)?.let {
                    plugin?.logger?.warning(it)
                }
            }
        }
    }

    /**
     * 解析并处理坐标传送
     * 格式: world,x,y,z[,yaw,pitch]，yaw/pitch 可选，未指定则保留玩家当前朝向
     */
    fun parseAndHandleTppos(player: Player, args: String) {
        val location = SerializationUtil.deserializeLocation(args) ?: return
        val parts = args.split(",")
        if (parts.size < 6) { // yaw 或 pitch 未指定，用玩家当前朝向
            if (parts.size <= 4) location.yaw = player.location.yaw
            if (parts.size <= 5) location.pitch = player.location.pitch
        }
        if (KaScheduler.folia) {
            player.teleportAsync(location)
        } else {
            player.teleport(location)
        }
    }
}
