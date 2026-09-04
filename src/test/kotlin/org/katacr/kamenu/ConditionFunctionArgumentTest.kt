package org.katacr.kamenu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 验证条件内置函数参数列表覆盖所有支持参数的判断方法。 */
class ConditionFunctionArgumentTest {
    @Test
    fun `player and item predicates protect variable arguments`() {
        val method = TextResolver::class.java.getDeclaredMethod("conditionFunctionNames")
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val names = method.invoke(TextResolver) as Set<String>

        assertEquals(
            setOf(
                "isNull",
                "isPass",
                "isTrue",
                "isNum",
                "isPosNum",
                "isInt",
                "isPosInt",
                "isPlayerOnline",
                "hasPerm",
                "hasMoney",
                "hasPoints",
                "hasStockItem",
                "hasItem",
                "hasEquipment",
                "inList",
                "inGlist",
                "getLength"
            ),
            names
        )
    }
}
