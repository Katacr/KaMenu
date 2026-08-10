package org.katacr.kamenu.dialog

/** 扫描 `<text=...>` 标签，并避免把引号内或嵌套字形标签的 `>` 当作外层结束符。 */
object ClickableTextTagScanner {

    /** 一个完整可点击文本标签在原字符串中的位置及其内部属性文本。 */
    data class Tag(
        val startIndex: Int,
        val endIndex: Int,
        val content: String
    )

    /** 按原始顺序提取所有结构完整的可点击文本标签。 */
    fun scan(rawText: String): List<Tag> {
        val tags = mutableListOf<Tag>()
        var searchFrom = 0

        while (searchFrom < rawText.length) {
            val startIndex = rawText.indexOf("<text=", searchFrom, ignoreCase = true)
            if (startIndex < 0) break

            val endIndex = findClosingBracket(rawText, startIndex)
            if (endIndex < 0) {
                searchFrom = startIndex + 1
                continue
            }

            tags += Tag(
                startIndex = startIndex,
                endIndex = endIndex,
                content = rawText.substring(startIndex + 1, endIndex)
            )
            searchFrom = endIndex + 1
        }

        return tags
    }

    /** 在引号外按尖括号深度寻找结束符，引号内允许出现第三方字形标签。 */
    private fun findClosingBracket(text: String, startIndex: Int): Int {
        var depth = 0
        var quote: Char? = null
        var escaped = false

        for (index in startIndex until text.length) {
            val character = text[index]
            if (quote != null) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == quote -> quote = null
                }
                continue
            }

            when (character) {
                '\'', '"', '`' -> quote = character
                '<' -> depth++
                '>' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }

        return -1
    }
}
