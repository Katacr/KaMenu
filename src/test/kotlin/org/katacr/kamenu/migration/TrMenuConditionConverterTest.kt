package org.katacr.kamenu.migration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 验证受限 TrMenu Kether 条件到 KaMenu 表达式的安全转换。 */
class TrMenuConditionConverterTest {
    @Test
    fun `converts common predicates and values`() {
        assertConversion("perm *server.shop", "hasPerm.server.shop")
        assertConversion("check vars *%player_level% >= *10", "%player_level% >= 10")
        assertConversion("check &shop_mode is *sell", "{meta:shop_mode} == sell")
        assertConversion("mtc int &amount", "isInt.{meta:amount}")
        assertConversion("money *100", "hasMoney.100")
        assertConversion("points *25", "hasPoints.25")
        assertConversion("check {data: count} > {0}", "{data:count} > {arg:0}")
    }

    @Test
    fun `converts nested any all and not groups`() {
        assertConversion(
            "all [ mtc int &amount any [ perm *shop.vip not perm *shop.blocked ] check &amount > *0 ]",
            "(isInt.{meta:amount} && (hasPerm.shop.vip || !(hasPerm.shop.blocked)) && {meta:amount} > 0)"
        )
    }

    @Test
    fun `reports strict equality as approximate`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = TrMenuConditionConverter().convert("check *Admin == *admin", "condition", diagnostics)

        assertEquals("Admin == admin", result)
        assertTrue(diagnostics.issues.any { it.code == "TRM_CONDITION_CASE_APPROXIMATE" })
    }

    @Test
    fun `rejects private scripts and collection operators`() {
        val scriptDiagnostics = TrMenuMigrationDiagnostics()
        assertNull(TrMenuConditionConverter().convert("\$ utils.isPlayerOnline()", "condition", scriptDiagnostics))
        assertTrue(scriptDiagnostics.issues.any { it.code == "TRM_CONDITION_UNSUPPORTED" })

        val collectionDiagnostics = TrMenuMigrationDiagnostics()
        assertNull(TrMenuConditionConverter().convert("check *abc has *a", "condition", collectionDiagnostics))
        assertTrue(collectionDiagnostics.issues.any { it.code == "TRM_CONDITION_UNSUPPORTED" })
    }

    @Test
    fun `converts statically known TrMenu utility conditions`() {
        val converter = TrMenuConditionConverter(TrMenuVariableConverter())

        assertConversion(
            converter,
            "\$ utils.isPlayerOnline(vars(\"{0}\"))",
            "isPlayerOnline.{arg:0}"
        )
        assertConversion(
            converter,
            "\$ !utils.hasEquipment(vars(\"{0}\"), \"FEET\")",
            "!(hasEquipment.[BOOTS;{arg:0}])"
        )
        assertConversion(
            converter,
            "js: utils.hasMoney(player, funInt(3))",
            "hasMoney.{arg:3}"
        )
        assertConversion(
            converter,
            "js: utils.hasItem(player, vars(\"mat:{1},amt:{3}\"))",
            "hasItem.[mats={arg:1};amount={arg:3}]"
        )
    }

    @Test
    fun `rejects utility money calls with inline arithmetic`() {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = TrMenuConditionConverter(TrMenuVariableConverter()).convert(
            "js: utils.hasMoney(player, funInt(3) - 1)",
            "condition",
            diagnostics
        )

        assertNull(result)
        assertTrue(diagnostics.issues.any { it.code == "TRM_CONDITION_UNSUPPORTED" })
    }

    private fun assertConversion(source: String, expected: String) {
        assertConversion(TrMenuConditionConverter(), source, expected)
    }

    private fun assertConversion(
        converter: TrMenuConditionConverter,
        source: String,
        expected: String
    ) {
        val diagnostics = TrMenuMigrationDiagnostics()
        val result = converter.convert(source, "condition", diagnostics)
        assertEquals(expected, result)
        assertTrue(diagnostics.issues.none { it.code == "TRM_CONDITION_UNSUPPORTED" })
    }
}
