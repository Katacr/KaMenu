package org.katacr.kamenu.migration

import java.io.File

/** 已完成 YAML 读取但尚未转换为 KaMenu 字段的 源菜单 源菜单。 */
internal data class TrMenuSourceMenu(
    val source: File,
    val menuId: String,
    val root: TrMenuSourceSection
)

/** 源菜单 源文件解析结果；解析失败时 [menu] 为 null 且包含 ERROR 诊断。 */
internal data class TrMenuSourceParseResult(
    val menu: TrMenuSourceMenu?,
    val issues: List<TrMenuMigrationIssue>
)
