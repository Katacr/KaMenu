package org.katacr.kamenu.migration

import org.katacr.kamenu.container.ContainerMenuType

/** 源菜单 图标在目标 Container 布局中的静态位置。 */
internal data class TrMenuButtonPlacement(
    val sourceId: String,
    val targetId: String,
    val sourceIndex: Int,
    val pageIndex: Int,
    val path: String,
    val section: TrMenuSourceSection,
    val slots: List<Int>,
    val slotExpression: String? = null,
    val slotFrames: List<String>? = null
)

/** 已转换并静态化的 源菜单 Container 布局。 */
internal data class TrMenuLayoutConversion(
    val type: ContainerMenuType,
    val pages: List<List<String>>,
    val defaultPage: Int,
    val buttons: List<TrMenuButtonPlacement>
) {
    val rows: List<String> get() = pages.firstOrNull().orEmpty()
}

/**
 * 将 源菜单 单页布局和图标位置转换为 KaMenu Container 静态布局。
 *
 * 多页、动态槽位和移动槽位动画没有等价运行时语义，因此按严格策略报告，
 * 不会在此处静默拆页或猜测目标位置。
 */
internal class TrMenuLayoutConverter {
    private val namedButtonId = Regex("^[\\p{L}\\p{N}_\\-/]+$")

    /** 转换菜单类型、布局行和图标位置；存在结构错误时返回 null。 */
    fun convert(
        source: TrMenuSourceMenu,
        diagnostics: TrMenuMigrationDiagnostics
    ): TrMenuLayoutConversion? {
        val root = source.root
        val renderType = root.value(TrMenuSourceProperty.RENDER_TYPE, "Render-Type", diagnostics)
            ?.toString()
            ?.trim()
        if (renderType.equals("DIALOG", ignoreCase = true)) {
            diagnostics.add(
                code = "TRM_RENDER_TYPE_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.ERROR,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = "Render-Type",
                message = "TrMenu Dialog menus are outside the Container migration scope."
            )
            return null
        }

        val type = parseType(root, diagnostics) ?: return null
        val pages = parseLayoutPages(
            root.value(TrMenuSourceProperty.LAYOUT, "Layout", diagnostics),
            diagnostics
        ) ?: return null

        reportPlayerInventory(root, diagnostics)
        val icons = parseIcons(root, diagnostics)
        val targetIds = buildTargetIds(icons)

        val defaultPage = parseDefaultPage(root, diagnostics)
        val placements = mutableListOf<TrMenuButtonPlacement>()

        pages.forEachIndexed { pageIndex, sourceRows ->
            val pageLabel = "Layout[page ${pageIndex + 1}]"
            val targetRowCount = resolveRowCount(root, type, sourceRows.size, diagnostics, pageLabel) ?: return null
            val sourceSlots = parseAndPadRows(sourceRows, type.columns, targetRowCount, diagnostics, pageLabel) ?: return null
            val knownSourceIds = icons.mapTo(linkedSetOf()) { it.first }
            sourceSlots.flatten().filterNotNull().distinct().forEach { sourceId ->
                if (sourceId !in knownSourceIds) {
                    diagnostics.add(
                        code = "TRM_LAYOUT_UNKNOWN_ICON",
                        severity = TrMenuMigrationSeverity.INFO,
                        compatibility = TrMenuMigrationCompatibility.EXACT,
                        path = pageLabel,
                        message = "Layout slot references undefined icon '$sourceId'; KaMenu leaves unconfigured slots empty."
                    )
                }
            }

            icons.forEachIndexed { index, (sourceId, section) ->
                val path = "Icons.$sourceId"
                val display = section.section(TrMenuSourceProperty.ICON_DISPLAY, "$path.display", diagnostics)
                val pagesValue = display?.value(TrMenuSourceProperty.ICON_PAGE, "$path.display.page", diagnostics)
                val allowedPages = resolveIconPages(pagesValue, "$path.display.page", diagnostics)
                if (allowedPages != null && pageIndex !in allowedPages) return@forEachIndexed

                val explicitSlots = display?.value(TrMenuSourceProperty.ICON_SLOT, "$path.display.slot", diagnostics)
                val (staticSlots, dynamicSlot, slotFrames) = if (explicitSlots != null) {
                    parseExplicitSlots(explicitSlots, "$path.display.slot", diagnostics)
                } else {
                    Triple(findLayoutSlots(sourceSlots, sourceId, type.columns), null, null)
                }

                if (dynamicSlot != null) {
                    val targetId = targetIds[index]
                    if (targetId != sourceId) {
                        diagnostics.add(
                            code = "TRM_ICON_ID_RENAMED",
                            severity = TrMenuMigrationSeverity.INFO,
                            compatibility = TrMenuMigrationCompatibility.EXACT,
                            path = path,
                            message = "Icon ID '$sourceId' was renamed to '$targetId' to satisfy KaMenu layout ID rules."
                        )
                    }
                    placements += TrMenuButtonPlacement(
                        sourceId, targetId, index, pageIndex, path, section, emptyList(), dynamicSlot
                    )
                    return@forEachIndexed
                }

                if (slotFrames != null) {
                    val targetId = targetIds[index]
                    if (targetId != sourceId) {
                        diagnostics.add(
                            code = "TRM_ICON_ID_RENAMED",
                            severity = TrMenuMigrationSeverity.INFO,
                            compatibility = TrMenuMigrationCompatibility.EXACT,
                            path = path,
                            message = "Icon ID '$sourceId' was renamed to '$targetId' to satisfy KaMenu layout ID rules."
                        )
                    }
                    placements += TrMenuButtonPlacement(
                        sourceId, targetId, index, pageIndex, path, section, emptyList(), null, slotFrames
                    )
                    return@forEachIndexed
                }

                val validSlots = staticSlots.orEmpty().distinct().filter { slot ->
                    if (slot in 0 until targetRowCount * type.columns) {
                        true
                    } else {
                        diagnostics.add(
                            code = "TRM_ICON_SLOT_OUT_OF_RANGE",
                            severity = TrMenuMigrationSeverity.WARNING,
                            compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                            path = "$path.display.slot",
                            message = "Slot $slot is outside the target ${type.name} inventory and was skipped."
                        )
                        false
                    }
                }
                if (validSlots.isEmpty()) return@forEachIndexed

                val targetId = targetIds[index]
                if (targetId != sourceId) {
                    diagnostics.add(
                        code = "TRM_ICON_ID_RENAMED",
                        severity = TrMenuMigrationSeverity.INFO,
                        compatibility = TrMenuMigrationCompatibility.EXACT,
                        path = path,
                        message = "Icon ID '$sourceId' was renamed to '$targetId' to satisfy KaMenu layout ID rules."
                    )
                }
                placements += TrMenuButtonPlacement(sourceId, targetId, index, pageIndex, path, section, validSlots)
            }
        }

        if (diagnostics.hasErrors) return null
        val convertedPages = pages.mapIndexed { pageIndex, sourceRows ->
            buildPageRows(root, type, sourceRows, placements, pageIndex, diagnostics)
        }
        if (convertedPages.any { it == null }) return null
        return TrMenuLayoutConversion(type, convertedPages.filterNotNull(), defaultPage, placements)
    }

    private fun buildPageRows(
        root: TrMenuSourceSection,
        type: ContainerMenuType,
        sourceRows: List<String>,
        placements: List<TrMenuButtonPlacement>,
        pageIndex: Int,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<String>? {
        val pageLabel = "Layout[page ${pageIndex + 1}]"
        val targetRowCount = resolveRowCount(root, type, sourceRows.size, diagnostics, pageLabel) ?: return null
        val sourceSlots = parseAndPadRows(sourceRows, type.columns, targetRowCount, diagnostics, pageLabel)
            ?: return null
        val targetSlots = arrayOfNulls<String>(targetRowCount * type.columns)
        placements.forEach { placement ->
            if (placement.pageIndex == pageIndex) {
                placement.slots.forEach { slot ->
                    if (slot in targetSlots.indices) {
                        val existing = targetSlots[slot]
                        if (existing != null && existing != placement.targetId) {
                            diagnostics.add(
                                code = "TRM_ICON_SLOT_COLLISION",
                                severity = TrMenuMigrationSeverity.ERROR,
                                compatibility = TrMenuMigrationCompatibility.INVALID,
                                path = placement.path,
                                message = "Icon '${placement.sourceId}' and target button '$existing' both resolve to slot $slot."
                            )
                        } else {
                            targetSlots[slot] = placement.targetId
                        }
                    }
                }
            }
        }
        if (diagnostics.hasErrors) return null
        return targetSlots.toList().chunked(type.columns).map { row ->
            row.joinToString("") { id -> encodeLayoutToken(id) }
        }
    }

    private fun parseType(
        root: TrMenuSourceSection,
        diagnostics: TrMenuMigrationDiagnostics
    ): ContainerMenuType? {
        val raw = root.value(TrMenuSourceProperty.INVENTORY_TYPE, "Type", diagnostics)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (raw.isEmpty()) return ContainerMenuType.CHEST
        return ContainerMenuType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
            ?: run {
                diagnostics.add(
                    code = "TRM_CONTAINER_TYPE_UNSUPPORTED",
                    severity = TrMenuMigrationSeverity.ERROR,
                    compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                    path = "Type",
                    message = "Inventory type '$raw' cannot be represented by KaMenu Container."
                )
                null
            }
    }

    private fun parseLayoutPages(
        raw: Any?,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<List<String>>? {
        if (raw == null) return listOf(emptyList())
        if (raw is String) return listOf(listOf(raw))
        if (raw !is List<*>) {
            invalidLayout(diagnostics, "Layout must be a row string, row list, or page list.")
            return null
        }
        if (raw.isEmpty()) return listOf(emptyList())

        return if (raw.first() is List<*>) {
            raw.mapIndexed { pageIndex, page ->
                val rows = page as? List<*> ?: run {
                    invalidLayout(diagnostics, "Layout page $pageIndex is not a row list.")
                    return null
                }
                rows.mapIndexed { rowIndex, row ->
                    row as? String ?: run {
                        invalidLayout(diagnostics, "Layout[$pageIndex][$rowIndex] must be a string.")
                        return null
                    }
                }
            }
        } else {
            listOf(raw.mapIndexed { rowIndex, row ->
                row as? String ?: run {
                    invalidLayout(diagnostics, "Layout[$rowIndex] must be a string.")
                    return null
                }
            })
        }
    }

    private fun resolveRowCount(
        root: TrMenuSourceSection,
        type: ContainerMenuType,
        layoutRows: Int,
        diagnostics: TrMenuMigrationDiagnostics,
        pageLabel: String = "Layout"
    ): Int? {
        val rawSize = root.value(TrMenuSourceProperty.SIZE, "Size", diagnostics)
        val configured = when (rawSize) {
            null -> 0
            is Number -> rawSize.toInt()
            else -> rawSize.toString().trim().toIntOrNull()
        }
        if (configured == null || configured < 0) {
            invalidLayout(diagnostics, "Size must be a non-negative integer row or slot count.", "Size")
            return null
        }

        if (type != ContainerMenuType.CHEST) {
            return type.minRows
        }
        val configuredRows = when {
            configured == 0 -> 0
            configured in 1..6 -> configured
            configured in 9..54 && configured % 9 == 0 -> configured / 9
            else -> {
                invalidLayout(diagnostics, "CHEST Size must be 1-6 rows or 9-54 slots in multiples of 9.", "Size")
                return null
            }
        }
        val rows = maxOf(1, configuredRows, layoutRows)
        if (rows !in type.minRows..type.maxRows) {
            invalidLayout(diagnostics, "CHEST layout resolves to $rows rows; only 1-6 rows are supported.")
            return null
        }
        return rows
    }

    private fun parseAndPadRows(
        sourceRows: List<String>,
        columns: Int,
        rowCount: Int,
        diagnostics: TrMenuMigrationDiagnostics,
        pageLabel: String = "Layout"
    ): List<List<String?>>? {
        if (sourceRows.size > rowCount) {
            invalidLayout(diagnostics, "Layout contains ${sourceRows.size} rows but the target container has $rowCount.")
            return null
        }
        val parsed = sourceRows.mapIndexed { rowIndex, row ->
            val tokens = tokenizeRow(row, rowIndex, diagnostics) ?: return null
            if (tokens.size > columns) {
                invalidLayout(
                    diagnostics,
                    "Layout row ${rowIndex + 1} has ${tokens.size} logical slots; expected at most $columns.",
                    "Layout[$rowIndex]"
                )
                return null
            }
            tokens + List(columns - tokens.size) { null }
        }.toMutableList()
        while (parsed.size < rowCount) parsed.add(List(columns) { null })
        return parsed
    }

    private fun tokenizeRow(
        row: String,
        rowIndex: Int,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<String?>? {
        val tokens = mutableListOf<String?>()
        var offset = 0
        while (offset < row.length) {
            if (row[offset] == '`') {
                val closing = row.indexOf('`', offset + 1)
                if (closing < 0) {
                    invalidLayout(diagnostics, "Layout row ${rowIndex + 1} contains an unclosed named icon.", "Layout[$rowIndex]")
                    return null
                }
                val id = row.substring(offset + 1, closing)
                tokens += id.takeIf(String::isNotEmpty)
                offset = closing + 1
                continue
            }
            val codePoint = row.codePointAt(offset)
            val token = String(Character.toChars(codePoint))
            tokens += token.takeUnless { it == " " }
            offset += Character.charCount(codePoint)
        }
        return tokens
    }

    private fun parseDefaultPage(
        root: TrMenuSourceSection,
        diagnostics: TrMenuMigrationDiagnostics
    ): Int {
        val options = root.section(TrMenuSourceProperty.OPTIONS, "Options", diagnostics) ?: return 0
        val raw = options.value(TrMenuSourceProperty.OPTION_DEFAULT_LAYOUT, "Options.Default-Layout", diagnostics)
            ?: return 0
        val text = when (raw) {
            is Number -> raw.toInt().toString()
            else -> raw.toString().trim()
        }
        return text.toIntOrNull() ?: 0
    }

    /** 解析图标 page 配置；null 表示出现在所有页，非空返回允许的页码集合。 */
    private fun resolveIconPages(
        raw: Any?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Set<Int>? {
        if (raw == null) return null
        val values = if (raw is List<*>) raw else listOf(raw)
        val pages = values.mapNotNull { it?.toString()?.trim()?.toIntOrNull() }
        if (pages.size != values.filterNotNull().size) {
            diagnostics.add(
                code = "TRM_ICON_PAGE_INVALID",
                severity = TrMenuMigrationSeverity.ERROR,
                compatibility = TrMenuMigrationCompatibility.INVALID,
                path = path,
                message = "Icon page '$raw' contains a non-integer page index."
            )
            return null
        }
        if (pages.isEmpty()) return null
        return pages.toSet()
    }

    private fun parseIcons(
        root: TrMenuSourceSection,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Pair<String, TrMenuSourceSection>> {
        val section = root.section(TrMenuSourceProperty.ICONS, "Icons", diagnostics) ?: return emptyList()
        return section.entries().mapNotNull { (id, value) ->
            if (value is TrMenuSourceSection) {
                id to value
            } else {
                diagnostics.add(
                    code = "TRM_ICON_INVALID",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.INVALID,
                    path = "Icons.$id",
                    message = "Icon '$id' must be a YAML section and was skipped."
                )
                null
            }
        }
    }

    private fun findLayoutSlots(
        rows: List<List<String?>>,
        sourceId: String,
        columns: Int
    ): List<Int> = buildList {
        rows.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { columnIndex, id ->
                if (id == sourceId) add(rowIndex * columns + columnIndex)
            }
        }
    }

    /**
     * 解析图标显式槽位，区分三种语义：
     * - 静态多副本：`[8, 9, 10]` 或单值，返回第一组（静态整数槽位列表）
     * - 单动态表达式：`%var%`，返回第二组（运行时槽位表达式）
     * - 动画帧：`[[8], [9], [10]]` 列表的列表，返回第三组（每帧一个表达式，运行时逐帧循环）
     */
    private fun parseExplicitSlots(
        raw: Any?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Triple<List<Int>?, String?, List<String>?> {
        val frames = when (raw) {
            is List<*> -> if (raw.firstOrNull() is List<*>) {
                raw.map { (it as? List<*>) ?: listOf(it) }
            } else {
                listOf(raw)
            }
            else -> listOf(listOf(raw))
        }

        if (frames.size > 1) {
            val frameExpressions = frames.mapNotNull { frame ->
                val texts = frame.mapNotNull { it?.toString()?.trim()?.takeIf { t -> t.isNotEmpty() } }
                if (texts.isEmpty()) null else texts.joinToString(",")
            }
            if (frameExpressions.size > 1) {
                diagnostics.add(
                    code = "TRM_ICON_SLOT_ANIMATION",
                    severity = TrMenuMigrationSeverity.INFO,
                    compatibility = TrMenuMigrationCompatibility.EXACT,
                    path = path,
                    message = "Icon slot frames ${frameExpressions.joinToString(" / ")} cycle on each refresh to animate the icon position."
                )
                return Triple(null, null, frameExpressions)
            }
        }

        var dynamicExpression: String? = null
        val slots = linkedSetOf<Int>()
        frames.firstOrNull().orEmpty().forEach { value ->
            val text = value?.toString()?.trim().orEmpty()
            if (text.isEmpty()) return@forEach
            val range = Regex("^(-?\\d+)-(-?\\d+)$").matchEntire(text)
            when {
                range != null -> {
                    val from = range.groupValues[1].toInt()
                    val to = range.groupValues[2].toInt()
                    if (from <= to) {
                        slots.addAll(from..to)
                    } else {
                        diagnostics.add(
                            code = "TRM_ICON_SLOT_INVALID",
                            severity = TrMenuMigrationSeverity.WARNING,
                            compatibility = TrMenuMigrationCompatibility.INVALID,
                            path = path,
                            message = "Reversed slot range '$text' was skipped."
                        )
                    }
                }
                text.toIntOrNull() != null -> slots += text.toInt()
                else -> {
                    dynamicExpression = if (dynamicExpression == null) text else "$dynamicExpression,$text"
                    diagnostics.add(
                        code = "TRM_DYNAMIC_SLOT_EXPRESSION",
                        severity = TrMenuMigrationSeverity.INFO,
                        compatibility = TrMenuMigrationCompatibility.EXACT,
                        path = path,
                        message = "Icon slot '$text' uses a runtime expression and will be resolved per player by KaMenu Container."
                    )
                }
            }
        }
        return Triple(slots.toList().takeIf { it.isNotEmpty() }, dynamicExpression, null)
    }

    private fun targetButtonId(sourceId: String, sourceIndex: Int): String {
        if (isSingleCodePoint(sourceId) && sourceId != " " && sourceId != "`") return sourceId
        if (namedButtonId.matches(sourceId)) return sourceId
        return "trm_icon_$sourceIndex"
    }

    private fun buildTargetIds(
        icons: List<Pair<String, TrMenuSourceSection>>
    ): List<String> {
        val used = linkedSetOf<String>()
        return icons.mapIndexed { index, (sourceId, _) ->
            val preferred = targetButtonId(sourceId, index)
            var candidate = preferred
            var suffix = 2
            while (!used.add(candidate)) {
                candidate = "${preferred}_$suffix"
                suffix++
            }
            candidate
        }
    }

    private fun encodeLayoutToken(id: String?): String = when {
        id == null -> " "
        isSingleCodePoint(id) && id != " " && id != "`" -> id
        else -> "`$id`"
    }

    private fun isSingleCodePoint(value: String): Boolean =
        value.isNotEmpty() && value.codePointCount(0, value.length) == 1

    private fun reportPlayerInventory(
        root: TrMenuSourceSection,
        diagnostics: TrMenuMigrationDiagnostics
    ) {
        val playerInventory = root.value(
            TrMenuSourceProperty.PLAYER_INVENTORY,
            "PlayerInventory",
            diagnostics
        ) ?: return
        val hasContent = when (playerInventory) {
            is List<*> -> playerInventory.isNotEmpty()
            else -> playerInventory.toString().isNotBlank()
        }
        if (hasContent) {
            diagnostics.add(
                code = "TRM_PLAYER_INVENTORY_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = "PlayerInventory",
                message = "PlayerInventory button rows are not migrated into KaMenu's protected Container inventory."
            )
        }
    }

    private fun invalidLayout(
        diagnostics: TrMenuMigrationDiagnostics,
        message: String,
        path: String = "Layout"
    ) {
        diagnostics.add(
            code = "TRM_LAYOUT_INVALID",
            severity = TrMenuMigrationSeverity.ERROR,
            compatibility = TrMenuMigrationCompatibility.INVALID,
            path = path,
            message = message
        )
    }
}
