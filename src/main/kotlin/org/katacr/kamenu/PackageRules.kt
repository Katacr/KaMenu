package org.katacr.kamenu

/**
 * 全局包 ID 校验规则。
 *
 * actions 包和 js 包共用同一套 ID 规则，防止路径穿越、重复斜杠和不可预期字符。
 * 包 ID 来自相对文件路径，不包含扩展名。
 */
object PackageRules {
    const val MAX_PACKAGE_SIZE_BYTES: Long = 1024L * 1024L
    const val MAX_PACKAGE_SIZE_LABEL: String = "1 MiB"

    private val packageIdPattern = Regex("[A-Za-z0-9_./-]+")

    /**
     * 校验包 ID，返回 null 表示合法。
     */
    fun validatePackageId(packageId: String): PackageIdError? {
        return when {
            packageId.isEmpty() -> PackageIdError.EMPTY
            !packageIdPattern.matches(packageId) -> PackageIdError.INVALID_CHARACTERS
            packageId.contains("..") -> PackageIdError.PARENT_PATH
            packageId.startsWith("/") -> PackageIdError.LEADING_SLASH
            packageId.endsWith("/") -> PackageIdError.TRAILING_SLASH
            packageId.contains("//") -> PackageIdError.DOUBLE_SLASH
            else -> null
        }
    }

    /**
     * 包 ID 错误类型。
     *
     * langKey 对应语言文件中的错误提示键。
     */
    enum class PackageIdError(val langKey: String) {
        EMPTY("packages.invalid_id_empty"),
        INVALID_CHARACTERS("packages.invalid_id_characters"),
        PARENT_PATH("packages.invalid_id_parent_path"),
        LEADING_SLASH("packages.invalid_id_leading_slash"),
        TRAILING_SLASH("packages.invalid_id_trailing_slash"),
        DOUBLE_SLASH("packages.invalid_id_double_slash")
    }
}
