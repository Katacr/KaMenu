package org.katacr.kamenu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** 验证 tmpdata 的 ttl 时长字符串解析与边界处理。 */
class TimeValueParserTest {

    @Test
    fun `parses plain seconds`() {
        assertEquals(1_000L, TimeValueParser.parseMillis("1"))
        assertEquals(86_400_000L, TimeValueParser.parseMillis("86400"))
        assertEquals(1_000L, TimeValueParser.parseMillis("1s"))
    }

    @Test
    fun `parses combined suffix durations`() {
        assertEquals(86_400_000L, TimeValueParser.parseMillis("1d"))
        assertEquals(3_600_000L, TimeValueParser.parseMillis("1h"))
        assertEquals(60_000L, TimeValueParser.parseMillis("1m"))
        assertEquals(90_000L, TimeValueParser.parseMillis("1m30s"))
        assertEquals(86_400_000L + 7_200_000L + 1_800_000L, TimeValueParser.parseMillis("1d2h30m"))
        assertEquals(86_400_000L + 3_600_000L + 60_000L + 1_000L, TimeValueParser.parseMillis("1d1h1m1s"))
    }

    @Test
    fun `is case insensitive`() {
        assertEquals(86_400_000L, TimeValueParser.parseMillis("1D"))
        assertEquals(7_200_000L, TimeValueParser.parseMillis("2H"))
    }

    @Test
    fun `rejects invalid or negative values`() {
        assertNull(TimeValueParser.parseMillis(""))
        assertNull(TimeValueParser.parseMillis("-1"))
        assertNull(TimeValueParser.parseMillis("-1d"))
        assertNull(TimeValueParser.parseMillis("1.5d"))
        assertNull(TimeValueParser.parseMillis("1day"))
        assertNull(TimeValueParser.parseMillis("abc"))
        assertNull(TimeValueParser.parseMillis("d"))
        assertNull(TimeValueParser.parseMillis("1d2h3"))
    }

    @Test
    fun `saturates at Long max instead of overflowing`() {
        assertEquals(Long.MAX_VALUE, TimeValueParser.parseMillis(Long.MAX_VALUE.toString()))
        assertNull(TimeValueParser.parseMillis("9223372036854775808"))
    }
}