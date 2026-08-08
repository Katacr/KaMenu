package org.katacr.kamenu.migration

/**
 * TrMenu stable-v3 源配置的语义键定义。
 *
 * 每个正则来自当前分析的 TrMenu `Property` 源码快照，仅供迁移器读取源菜单；
 * 生成的 KaMenu 菜单始终使用标准键名。
 */
internal enum class TrMenuSourceProperty(
    val canonicalKey: String,
    pattern: String
) {
    TITLE("Title", "(title|name)s?"),
    TITLE_UPDATE("Title-Update", "titles?-?updates?"),
    LAYOUT("Layout", "(layout|shape)s?"),
    PLAYER_INVENTORY("PlayerInventory", "(layout|shape)?-?player-?inv(entory)?s?"),
    INVENTORY_TYPE("Type", "(inv(entory)?)?-?types?"),
    SIZE("Size", "(size|row)s?"),
    PROPERTIES("Properties", "propert(y|ies?)"),
    OPTIONS("Options", "(option|setting)s?"),
    RENDER_TYPE("Render-Type", "render-?types?"),

    OPTION_ARGUMENTS("Arguments", "(transfer)?-?arg(ument)?s?"),
    OPTION_DEFAULT_ARGUMENTS("Default-Arguments", "def(ault)?-?arg(ument)?s?"),
    OPTION_FREE_SLOTS("Free-Slots", "free-?slots?"),
    OPTION_DEFAULT_LAYOUT("Default-Layout", "def(ault)?-?lay(out)?s?"),
    OPTION_HIDE_PLAYER_INVENTORY("Hide-Player-Inventory", "hide-?player-?inv(entory)?s?"),
    OPTION_PURE_PACKET("Pure-Packet", "pure-?pack(et)?s?"),
    OPTION_MIN_CLICK_DELAY("Min-Click-Delay", "min-?click-?delay"),
    OPTION_DEPEND_EXPANSIONS("Depend-Expansions", "depend-?expansions?"),
    OPTION_COMMAND_FAKE_OP("Command-Fake-Op", "command-?fake-?op"),

    BINDINGS("Bindings", "(bind(ing)?|bound)s?"),
    BINDING_COMMANDS("Commands", "(command|cmd)s?"),
    BINDING_ITEMS("Items", "items?"),

    EVENTS("Events", "events?"),
    EVENT_OPEN("Open", "opens?"),
    EVENT_CLOSE("Close", "closes?"),
    EVENT_CLICK("Click", "clicks?"),
    CONDITION("condition", "(require(ment)?|cond(ition)?)s?"),
    PRIORITY("priority", "pri(ority)?s?"),
    INHERIT("inherit", "inherits?"),
    APPEND("append", "appends?"),
    PERIOD("period", "(period|time)s?"),
    ACTIONS("actions", "(list|action|click|execute|cmd)s?"),
    DENY_ACTIONS("deny-actions", "deny-?(list|action|click|execute|cmd)?s?"),

    ICONS("Icons", "(icon|button)s?"),
    ICON_REFRESH("refresh", "refreshs?"),
    ICON_UPDATE("update", "updates?"),
    ICON_DISPLAY("display", "displays?"),
    ICON_PAGE("page", "pages?"),
    ICON_SLOT("slot", "(slot|pos(ition)?)s?"),
    ICON_NAME("name", "(display)?-?names?"),
    ICON_MATERIAL("material", "(mat(erial)?|texture)s?"),
    ICON_LORE("lore", "(display)?-?lores?"),
    ICON_DATA("data", "(display)?-?data"),
    ICON_AMOUNT("amount", "(amt|amount)s?"),
    ICON_ENCHANT("enchant", "enchant(ment)?s?"),
    ICON_SHINY("shiny", "(shiny|glow)s?"),
    ICON_FLAGS("flags", "flags?"),
    ICON_NBT("nbt", "nbts?"),
    ICON_TOOLTIP("tooltip", "tooltip(_style)?"),
    ICON_ITEM_MODEL("model", "(item)?_?model"),
    ICON_HIDE_TOOLTIP("hide_tooltip", "hide_?tool(tip)?"),
    ICON_UNBREAKABLE("unbreakable", "unbreak(able)?"),
    ICON_SUB_ICONS("icons", "(sub|priority)?icons?"),

    TASKS("Tasks", "(task|schedule)s?"),
    FUNCTIONS("Functions", "(fun(ction)?|script)s?"),
    LANG("Lang", "lang(uage)?|internationalization|i18n");

    /** 忽略大小写且由 `Regex.matches` 执行完整匹配。 */
    val regex: Regex = Regex("(?i)$pattern")
}
