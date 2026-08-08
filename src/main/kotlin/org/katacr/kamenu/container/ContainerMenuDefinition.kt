package org.katacr.kamenu.container

/**
 * 容器菜单支持的服务端库存类型。
 *
 * 每种类型声明 Bukkit 库存类型及固定布局规格；箱子允许 1 至 6 行，其他类型使用原版固定尺寸。
 */
enum class ContainerMenuType(
    val columns: Int,
    val minRows: Int,
    val maxRows: Int
) {
    CHEST(9, 1, 6),
    HOPPER(5, 1, 1),
    DISPENSER(3, 3, 3),
    DROPPER(3, 3, 3),
    FURNACE(3, 1, 1),
    BLAST_FURNACE(3, 1, 1),
    SMOKER(3, 1, 1),
    ANVIL(3, 1, 1);

    /** 是否为支持火焰和加工箭头进度的熔炉类容器。 */
    val isFurnace: Boolean
        get() = this == FURNACE || this == BLAST_FURNACE || this == SMOKER
}

/**
 * 容器按钮支持的点击类型。
 *
 * [ALL] 表示每次有效按钮点击都执行的公共动作，具体点击动作会在它之后执行。
 */
enum class ContainerClickType(val configKey: String) {
    ALL("all"),
    LEFT("left"),
    RIGHT("right"),
    SHIFT_LEFT("shift_left"),
    SHIFT_RIGHT("shift_right"),
    MIDDLE("middle"),
    DROP("drop"),
    CONTROL_DROP("control_drop"),
    DOUBLE_CLICK("double_click"),
    OFFHAND("offhand"),
    NUMBER_KEY("number_key"),
    NUMBER_KEY_1("number_key_1"),
    NUMBER_KEY_2("number_key_2"),
    NUMBER_KEY_3("number_key_3"),
    NUMBER_KEY_4("number_key_4"),
    NUMBER_KEY_5("number_key_5"),
    NUMBER_KEY_6("number_key_6"),
    NUMBER_KEY_7("number_key_7"),
    NUMBER_KEY_8("number_key_8"),
    NUMBER_KEY_9("number_key_9");

    companion object {
        private val byConfigKey = entries.associateBy(ContainerClickType::configKey)

        /** 根据 YAML 键名取得点击类型；键名大小写不自动转换。 */
        fun fromConfigKey(key: String): ContainerClickType? = byConfigKey[key]
    }
}

/**
 * 从 YAML 深拷贝得到的只读配置值。
 *
 * 容器菜单在加载阶段不能解析玩家变量和条件，因此显示属性需要保留其标量、条件 Map 或列表结构，
 * 但不应继续持有可变的 Bukkit ConfigurationSection。
 */
sealed interface ContainerConfigValue {
    data object Null : ContainerConfigValue
    data class Scalar(val value: Any) : ContainerConfigValue
    data class Sequence(val values: List<ContainerConfigValue>) : ContainerConfigValue
    data class Mapping(val values: Map<String, ContainerConfigValue>) : ContainerConfigValue
}

/** 容器菜单中一个逻辑槽位。空槽位的 [buttonId] 为 null。 */
data class ContainerLayoutSlot(
    val index: Int,
    val row: Int,
    val column: Int,
    val buttonId: String?
)

/**
 * 已校验的容器布局。
 *
 * [slotsByButton] 预先保存按钮到槽位的反向索引，渲染和刷新时无需重复扫描字符串布局。
 */
data class ContainerLayoutDefinition(
    val rows: Int,
    val columns: Int,
    val slots: List<ContainerLayoutSlot>,
    val slotsByButton: Map<String, List<Int>>
) {
    val size: Int = rows * columns

    /** 返回指定原始槽位对应的按钮 ID。 */
    fun buttonAt(slot: Int): String? = slots.getOrNull(slot)?.buttonId
}

/**
 * 按钮物品的原始显示定义。
 *
 * 属性值会在玩家打开或刷新菜单时由统一物品工厂解析，因此支持 PAPI、KaMenu 变量和条件值。
 */
data class ContainerItemDefinition(
    val properties: Map<String, ContainerConfigValue>
) {
    operator fun get(property: String): ContainerConfigValue? = properties[property]
}

/** 菜单级自动刷新周期；null 表示不启用对应刷新。 */
data class ContainerUpdateDefinition(
    val menuIntervalTicks: Long?,
    val titleIntervalTicks: Long?,
    val progressIntervalTicks: Long?
)

/** 容器类型专属的动态属性，例如熔炉进度和铁砧输入设置。 */
data class ContainerPropertiesDefinition(
    val values: Map<String, ContainerConfigValue>
) {
    operator fun get(property: String): ContainerConfigValue? = values[property]
    fun contains(property: String): Boolean = values.containsKey(property)

    companion object {
        val EMPTY = ContainerPropertiesDefinition(emptyMap())
    }
}

/** 一个已解析的按钮定义。 */
data class ContainerButtonDefinition(
    val id: String,
    val viewCondition: String?,
    val updateIntervalTicks: Long?,
    val display: ContainerItemDefinition,
    val actions: Map<ContainerClickType, List<Any>>,
    val variants: List<ContainerButtonVariantDefinition> = emptyList()
)

/**
 * Container 按钮的完整显示变体。
 *
 * 变体按 priority 升序选择；priority 相同或均未指定时保持 YAML 中的顺序。
 * 每个变体同时拥有 display 和 actions，避免同一槽位的物品属性被不同条件分别选中。
 */
data class ContainerButtonVariantDefinition(
    val priority: Int?,
    val order: Int,
    val condition: String?,
    val display: ContainerItemDefinition,
    val actions: Map<ContainerClickType, List<Any>>
)

/** 熔炉进度监听器定义；条件只在未满足到满足的状态变化时触发。 */
data class ContainerProgressWatcherDefinition(
    val id: String,
    val source: String,
    val condition: String,
    val triggerInitial: Boolean,
    val actions: List<Any>
)

/**
 * 文件加载阶段生成的不可变容器菜单定义。
 *
 * 玩家相关变量尚未在此阶段解析；原始 YamlConfiguration 由 MenuManager 单独保留给事件、JS 和动作系统。
 */
data class ContainerMenuDefinition(
    val id: String,
    val type: ContainerMenuType,
    val title: ContainerConfigValue,
    val layout: ContainerLayoutDefinition,
    val properties: ContainerPropertiesDefinition,
    val buttons: Map<String, ContainerButtonDefinition>,
    val update: ContainerUpdateDefinition,
    val minClickDelayMillis: Long,
    val progressWatchers: Map<String, ContainerProgressWatcherDefinition>
)

/** 菜单配置诊断级别。 */
enum class ContainerDiagnosticSeverity {
    WARNING,
    ERROR
}

/**
 * 一条可定位到 YAML 路径的容器菜单配置诊断。
 *
 * [code] 是稳定机器标识，后续可映射到 i18n 文本；[message] 供开发阶段直接阅读。
 */
data class ContainerMenuDiagnostic(
    val severity: ContainerDiagnosticSeverity,
    val code: String,
    val path: String,
    val message: String
)

/** 容器菜单解析结果；存在 ERROR 时 [definition] 必须为空。 */
data class ContainerMenuParseResult(
    val definition: ContainerMenuDefinition?,
    val diagnostics: List<ContainerMenuDiagnostic>
) {
    val succeeded: Boolean
        get() = definition != null && diagnostics.none { it.severity == ContainerDiagnosticSeverity.ERROR }
}
