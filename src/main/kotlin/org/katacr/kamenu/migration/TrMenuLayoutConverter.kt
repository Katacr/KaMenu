package org.katacr.kamenu.migration

import org.katacr.kamenu.container.ContainerMenuType

/** 源菜单 图标在目标 Container 布局中的静态位置。 */
internal data class TrMenuButtonPlacement(
    val sourceId: String,
    val targetId: String,
    val sourceIndex: Int,
    val path: String,
    val section: TrMenuSourceSection,
    val slots: List<Int>
)

/** 已转换并静态化的 源菜单 Container 布局。 */
internal data class TrMenuLayoutConversion(
    val type: ContainerMenuType,
    val rows: List<String>,
    val buttons: List<TrMenuButtonPlacement>
)

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
        if (pages.size > 1) {
            diagnostics.add(
                code = "TRM_MULTI_PAGE_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.ERROR,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = "Layout",
                message = "TrMenu contains ${pages.size} layout pages; KaMenu Container currently supports one static page."
            )
            return null
        }

        reportPlayerInventory(root, diagnostics)
        val sourceRows = pages.firstOrNull().orEmpty()
        val targetRowCount = resolveRowCount(root, type, sourceRows.size, diagnostics) ?: return null
        val sourceSlots = parseAndPadRows(sourceRows, type.columns, targetRowCount, diagnostics) ?: return null
        val icons = parseIcons(root, diagnostics)
        val targetIds = buildTargetIds(icons)

        val targetSlots = arrayOfNulls<String>(targetRowCount * type.columns)
        val placements = mutableListOf<TrMenuButtonPlacement>()
        val knownSourceIds = icons.mapTo(linkedSetOf()) { it.first }
        sourceSlots.flatten().filterNotNull().distinct().forEach { sourceId ->
            if (sourceId !in knownSourceIds) {
                diagnostics.add(
                    code = "TRM_LAYOUT_UNKNOWN_ICON",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                    path = "Layout",
                    message = "Layout references undefined icon '$sourceId'; its slots will be left empty."
                )
            }
        }

        icons.forEachIndexed { index, (sourceId, section) ->
            val path = "Icons.$sourceId"
            val display = section.section(TrMenuSourceProperty.ICON_DISPLAY, "$path.display", diagnostics)
            val pagesValue = display?.value(TrMenuSourceProperty.ICON_PAGE, "$path.display.page", diagnostics)
            if (!usesOnlyFirstPage(pagesValue, "$path.display.page", diagnostics)) return@forEachIndexed

            val explicitSlots = display?.value(TrMenuSourceProperty.ICON_SLOT, "$path.display.slot", diagnostics)
            val slots = if (explicitSlots != null) {
                parseExplicitSlots(explicitSlots, "$path.display.slot", diagnostics)
            } else {
                findLayoutSlots(sourceSlots, sourceId, type.columns)
            }
            if (slots == null) return@forEachIndexed

            val validSlots = slots.distinct().filter { slot ->
                if (slot in targetSlots.indices) {
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
            if (validSlots.isEmpty()) {
                diagnostics.add(
                    code = "TRM_ICON_UNPLACED",
                    severity = TrMenuMigrationSeverity.WARNING,
                    compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                    path = path,
                    message = "Icon '$sourceId' has no static slot on the migrated page and was skipped."
                )
                return@forEachIndexed
            }

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
            val collisions = validSlots.mapNotNull { slot ->
                targetSlots[slot]?.takeIf { it != targetId }?.let { existing -> slot to existing }
            }
            if (collisions.isNotEmpty()) {
                collisions.forEach { (slot, existing) ->
                    diagnostics.add(
                        code = "TRM_ICON_SLOT_COLLISION",
                        severity = TrMenuMigrationSeverity.ERROR,
                        compatibility = TrMenuMigrationCompatibility.INVALID,
                        path = "$path.display.slot",
                        message = "Icon '$sourceId' and target button '$existing' both resolve to slot $slot."
                    )
                }
                return@forEachIndexed
            }
            validSlots.forEach { slot -> targetSlots[slot] = targetId }
            placements += TrMenuButtonPlacement(sourceId, targetId, index, path, section, validSlots)
        }

        if (diagnostics.hasErrors) return null
        val rows = targetSlots.toList().chunked(type.columns).map { row ->
            row.joinToString("") { id -> encodeLayoutToken(id) }
        }
        return TrMenuLayoutConversion(type, rows, placements)
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
        diagnostics: TrMenuMigrationDiagnostics
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
        diagnostics: TrMenuMigrationDiagnostics
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

    private fun parseExplicitSlots(
        raw: Any?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): List<Int>? {
        val frames = when (raw) {
            is List<*> -> if (raw.firstOrNull() is List<*>) {
                raw.map { (it as? List<*>) ?: listOf(it) }
            } else {
                listOf(raw)
            }
            else -> listOf(listOf(raw))
        }
        if (frames.size > 1) {
            diagnostics.add(
                code = "TRM_ICON_SLOT_ANIMATION_FIRST_FRAME",
                severity = TrMenuMigrationSeverity.WARNING,
                compatibility = TrMenuMigrationCompatibility.APPROXIMATE,
                path = path,
                message = "Animated icon positions were reduced to the first frame."
            )
        }

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
                    diagnostics.add(
                        code = "TRM_DYNAMIC_SLOT_UNSUPPORTED",
                        severity = TrMenuMigrationSeverity.ERROR,
                        compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                        path = path,
                        message = "Dynamic or invalid slot '$text' cannot be represented by a static Container layout."
                    )
                    return null
                }
            }
        }
        return slots.toList()
    }

    private fun usesOnlyFirstPage(
        raw: Any?,
        path: String,
        diagnostics: TrMenuMigrationDiagnostics
    ): Boolean {
        if (raw == null) return true
        val values = if (raw is List<*>) raw else listOf(raw)
        val pages = values.mapNotNull { it?.toString()?.trim()?.toIntOrNull() }
        if (pages.size != values.filterNotNull().size || pages.any { it != 0 }) {
            diagnostics.add(
                code = "TRM_MULTI_PAGE_UNSUPPORTED",
                severity = TrMenuMigrationSeverity.ERROR,
                compatibility = TrMenuMigrationCompatibility.UNSUPPORTED,
                path = path,
                message = "Icon page selection '$raw' cannot be represented by a single-page Container menu."
            )
            return false
        }
        return true
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
