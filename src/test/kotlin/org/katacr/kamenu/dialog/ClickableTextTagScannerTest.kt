package org.katacr.kamenu.dialog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 验证 Spigot 可点击文本扫描不会被第三方字形标签提前截断。 */
class ClickableTextTagScannerTest {

    @Test
    fun `scans ordinary clickable text`() {
        val rawText = "<text=\"你好\";hover=\"你好\";actions=btn_1>"

        val tag = ClickableTextTagScanner.scan(rawText).single()

        assertEquals(0, tag.startIndex)
        assertEquals(rawText.lastIndex, tag.endIndex)
        assertEquals("text=\"你好\";hover=\"你好\";actions=btn_1", tag.content)
    }

    @Test
    fun `keeps quoted CraftEngine glyph inside clickable text`() {
        val rawText = "before <text=\"<image:ce:text_1>\";hover=\"你好\";actions=btn_1> after"

        val tag = ClickableTextTagScanner.scan(rawText).single()

        assertEquals(rawText.indexOf("<text="), tag.startIndex)
        assertEquals(rawText.lastIndexOf('>'), tag.endIndex)
        assertEquals("text=\"<image:ce:text_1>\";hover=\"你好\";actions=btn_1", tag.content)
    }

    @Test
    fun `keeps unquoted nested glyph and greater-than sign inside quotes`() {
        val rawText = "<text=<glyph:menu_icon>;hover=\"等级 > 10\";command=/menu>"

        val tag = ClickableTextTagScanner.scan(rawText).single()

        assertEquals(rawText.lastIndex, tag.endIndex)
        assertEquals("text=<glyph:menu_icon>;hover=\"等级 > 10\";command=/menu", tag.content)
    }

    @Test
    fun `scans multiple clickable tags in source order`() {
        val rawText =
            "<text=\"<image:ia:shop>\";actions=shop> / <text=\"<glyph:oraxen_icon>\";actions=icons>"

        val tags = ClickableTextTagScanner.scan(rawText)

        assertEquals(2, tags.size)
        assertEquals("text=\"<image:ia:shop>\";actions=shop", tags[0].content)
        assertEquals("text=\"<glyph:oraxen_icon>\";actions=icons", tags[1].content)
    }
}
