@file:Suppress("DEPRECATION", "UnstableApiUsage")

package org.katacr.kamenu.container

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.katacr.kamenu.ExternalItemAdapter
import org.katacr.kamenu.ItemPropertyReader
import org.katacr.kamenu.MenuUI

/**
 * 将自由槽中的真实 ItemStack 转换为条件、显示和动作可读取的只读变量。
 *
 * 该对象不修改库存；多物理槽组的数量会求和，其他物品属性取声明顺序中的首个非空物品。
 */
object FreeSlotItemContext {
    private val legacySerializer by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LegacyComponentSerializer.legacyAmpersand()
    }
    private val plainSerializer by lazy(LazyThreadSafetyMode.PUBLICATION) {
        PlainTextComponentSerializer.plainText()
    }

    /** 返回当前库存中全部具名自由槽位的 `{free:<id>.*}` 变量。 */
    fun sessionVariables(
        definition: ContainerFreeSlotsDefinition,
        inventory: Inventory
    ): Map<String, String> {
        return buildMap {
            definition.byId.values.forEach { freeSlot ->
                val items = freeSlot.slots.mapNotNull(inventory::getItem).filter(::isPresent)
                val first = items.firstOrNull()
                val prefix = "free:${freeSlot.id}"
                putAll(itemVariables(prefix, first))
                put("$prefix.amount", items.sumOf(ItemStack::getAmount).toString())
                put("$prefix.empty", items.isEmpty().toString())
                put(
                    "$prefix.material",
                    when {
                        items.isEmpty() -> Material.AIR.name
                        items.map(ItemStack::getType).distinct().size == 1 -> first!!.type.name
                        else -> "MIXED"
                    }
                )
            }
        }
    }

    /** 返回候选物品使用的稳定标量属性；空物品不会产生 null。 */
    fun itemVariables(prefix: String, item: ItemStack?): Map<String, String> {
        val present = item?.takeIf(::isPresent)
        val meta = present?.itemMeta
        val enchantments = meta?.enchants.orEmpty().entries.sortedBy { it.key.key.toString() }
        return buildMap {
            put("$prefix.empty", (present == null).toString())
            put("$prefix.material", present?.type?.name ?: Material.AIR.name)
            put("$prefix.amount", present?.amount?.toString() ?: "0")
            put("$prefix.name", present?.let(MenuUI::itemName)?.let(legacySerializer::serialize).orEmpty())
            put("$prefix.plain_name", present?.let(MenuUI::itemName)?.let(plainSerializer::serialize).orEmpty())
            put(
                "$prefix.lore",
                meta?.let(MenuUI::itemLore)?.joinToString("\n", transform = legacySerializer::serialize).orEmpty()
            )
            put(
                "$prefix.enchantments",
                enchantments.joinToString(",") { (enchantment, level) -> "${enchantment.key}:$level" }
            )
            put("$prefix.enchantment_count", enchantments.size.toString())
            Enchantment.values().forEach { enchantment ->
                put("$prefix.enchantment.${enchantment.key.key}", "0")
                put("$prefix.enchantment.${enchantment.key}", "0")
            }
            enchantments.forEach { (enchantment, level) ->
                put("$prefix.enchantment.${enchantment.key.key}", level.toString())
                put("$prefix.enchantment.${enchantment.key}", level.toString())
            }
            put("$prefix.custom_model_data", ItemPropertyReader.getCustomModelId(meta)?.toString() ?: "0")
            put("$prefix.item_model", ItemPropertyReader.getItemModel(meta).orEmpty())
            put("$prefix.max_stack_size", present?.maxStackSize?.toString() ?: "0")
            put("$prefix.provider", present?.let(ExternalItemAdapter::providerName).orEmpty())
            put("$prefix.id", present?.let(ExternalItemAdapter::nativeId).orEmpty())
        }
    }

    /** 取得逻辑自由槽位中声明顺序的首个非空真实物品克隆。 */
    fun firstItem(
        definition: ContainerFreeSlotsDefinition,
        inventory: Inventory,
        id: String
    ): ItemStack? {
        val freeSlot = definition.byId[id] ?: return null
        return freeSlot.slots.asSequence()
            .mapNotNull(inventory::getItem)
            .firstOrNull(::isPresent)
            ?.clone()
    }

    private fun isPresent(item: ItemStack): Boolean = item.type != Material.AIR && item.amount > 0
}
