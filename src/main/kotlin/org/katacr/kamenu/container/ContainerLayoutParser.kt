package org.katacr.kamenu.container

/**
 * 按容器类型将 `Layout` 文本解析为固定规格的逻辑槽位。
 *
 * 普通 Unicode 字符占一个槽位；反引号包裹的多字符 ID（例如 `close`）整体占一个槽位。
 * 空格表示空槽位，其他字符都被视为按钮 ID。
 */
object ContainerLayoutParser {
    private val namedButtonId = Regex("^[\\p{L}\\p{N}_\\-/]+$")

    /** 解析并校验一组容器布局行。 */
    fun parse(
        rows: List<String>,
        type: ContainerMenuType = ContainerMenuType.CHEST,
        dynamicSlotButtons: Set<String> = emptySet()
    ): ContainerLayoutParseResult {
        val diagnostics = mutableListOf<ContainerMenuDiagnostic>()
        if (rows.size !in type.minRows..type.maxRows) {
            val expectedRows = if (type.minRows == type.maxRows) {
                type.minRows.toString()
            } else {
                "${type.minRows} to ${type.maxRows}"
            }
            diagnostics += error(
                code = "layout.invalid_row_count",
                path = "Layout",
                message = "${type.name} Layout must contain $expectedRows row(s), but found ${rows.size}."
            )
            return ContainerLayoutParseResult(null, diagnostics)
        }

        val slots = mutableListOf<ContainerLayoutSlot>()
        rows.forEachIndexed { rowIndex, row ->
            val tokens = tokenizeRow(row, rowIndex, diagnostics)
            if (tokens.size != type.columns) {
                diagnostics += error(
                    code = "layout.invalid_column_count",
                    path = "Layout[$rowIndex]",
                    message = "${type.name} Layout row ${rowIndex + 1} must contain ${type.columns} logical slots, " +
                        "but found ${tokens.size}."
                )
            }

            tokens.take(type.columns).forEachIndexed { columnIndex, buttonId ->
                slots += ContainerLayoutSlot(
                    index = rowIndex * type.columns + columnIndex,
                    row = rowIndex,
                    column = columnIndex,
                    buttonId = buttonId
                )
            }
        }

        if (diagnostics.any { it.severity == ContainerDiagnosticSeverity.ERROR }) {
            return ContainerLayoutParseResult(null, diagnostics)
        }

        val slotsByButton = linkedMapOf<String, MutableList<Int>>()
        slots.forEach { slot ->
            slot.buttonId?.let { id -> slotsByButton.getOrPut(id, ::mutableListOf).add(slot.index) }
        }
        return ContainerLayoutParseResult(
            ContainerLayoutDefinition(
                rows = rows.size,
                columns = type.columns,
                slots = slots.toList(),
                slotsByButton = slotsByButton.mapValues { (_, value) -> value.toList() },
                dynamicSlotButtons = dynamicSlotButtons
            ),
            diagnostics.toList()
        )
    }

    /** 将一行文本拆成逻辑按钮 ID，避免按原始字符串长度误算反引号 ID 和 Unicode 字符。 */
    private fun tokenizeRow(
        row: String,
        rowIndex: Int,
        diagnostics: MutableList<ContainerMenuDiagnostic>
    ): List<String?> {
        val tokens = mutableListOf<String?>()
        var offset = 0
        while (offset < row.length) {
            if (row[offset] == '`') {
                val closing = row.indexOf('`', offset + 1)
                if (closing < 0) {
                    diagnostics += error(
                        code = "layout.unclosed_named_button",
                        path = "Layout[$rowIndex]",
                        message = "Layout row ${rowIndex + 1} contains an unclosed named button at character ${offset + 1}."
                    )
                    break
                }

                val id = row.substring(offset + 1, closing)
                if (!namedButtonId.matches(id)) {
                    diagnostics += error(
                        code = "layout.invalid_named_button",
                        path = "Layout[$rowIndex]",
                        message = "Named button '$id' may only contain letters, numbers, '_', '-', or '/'."
                    )
                }
                tokens += id.takeIf { it.isNotEmpty() }
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

    private fun error(code: String, path: String, message: String): ContainerMenuDiagnostic =
        ContainerMenuDiagnostic(ContainerDiagnosticSeverity.ERROR, code, path, message)
}

/** Layout 独立解析结果，供 YAML 菜单解析器和聚焦测试复用。 */
data class ContainerLayoutParseResult(
    val definition: ContainerLayoutDefinition?,
    val diagnostics: List<ContainerMenuDiagnostic>
)
