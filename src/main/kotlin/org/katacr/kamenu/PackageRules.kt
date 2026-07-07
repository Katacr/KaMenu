package org.katacr.kamenu

object PackageRules {
    const val MAX_PACKAGE_SIZE_BYTES: Long = 1024L * 1024L
    const val MAX_PACKAGE_SIZE_LABEL: String = "1 MiB"

    private val packageIdPattern = Regex("[A-Za-z0-9_./-]+")

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

    enum class PackageIdError(val langKey: String) {
        EMPTY("packages.invalid_id_empty"),
        INVALID_CHARACTERS("packages.invalid_id_characters"),
        PARENT_PATH("packages.invalid_id_parent_path"),
        LEADING_SLASH("packages.invalid_id_leading_slash"),
        TRAILING_SLASH("packages.invalid_id_trailing_slash"),
        DOUBLE_SLASH("packages.invalid_id_double_slash")
    }
}
