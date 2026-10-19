package org.katacr.kamenu

import org.bukkit.entity.Player
import java.lang.reflect.Method

/**
 * 跨核心的 Post Effect（屏幕后期效果）兼容层。
 *
 * 仅 Paper 26.3+ 提供 `Player#postEffects()` 与 `io.papermc.paper.entity.PlayerPostEffects`。
 * 全部通过反射调用，保持对 1.16.5 的编译兼容；旧核心返回不支持。
 *
 * 效果 key 形如 `minecraft:blur`、`minecraft:creeper`、`minecraft:spider`、`minecraft:invert`、
 * `minecraft:entity_outline`，也可由资源包在 `assets/<namespace>/post_effect/<name>.json` 自定义。
 */
object PostEffectCompat {
    /** 缓存解析出的 `Player#postEffects()` 方法；null 表示探测过但当前核心不支持。 */
    @Volatile
    private var postEffectsMethod: Method? = null
    @Volatile
    private var postEffectsResolved = false

    /** 当前核心是否支持 Post Effect API（Paper 26.3+）。 */
    fun isSupported(): Boolean = resolvePostEffectsMethod() != null

    /** 返回玩家当前生效的后期效果 key 列表；不支持时返回空列表。 */
    fun values(player: Player): List<String> {
        val effects = postEffects(player) ?: return emptyList()
        return runCatching {
            val method = effects.javaClass.methods.firstOrNull {
                it.name == "values" && it.parameterCount == 0
            } ?: return emptyList()
            val raw = method.invoke(effects) as? Iterable<*> ?: return emptyList()
            raw.mapNotNull { it?.toString() }
        }.getOrDefault(emptyList())
    }

    /** 叠加一个后期效果；已存在或核心不支持时返回 false。 */
    fun add(player: Player, key: String): Boolean {
        val effects = postEffects(player) ?: return false
        return invokeEffect(effects, "add", listOf(key))
    }

    /** 移除一个后期效果；不存在或核心不支持时返回 false。 */
    fun remove(player: Player, key: String): Boolean {
        val effects = postEffects(player) ?: return false
        return invokeEffect(effects, "remove", listOf(key))
    }

    /** 清空全部后期效果；核心不支持时返回 false。 */
    fun clear(player: Player): Boolean {
        val effects = postEffects(player) ?: return false
        return invokeEffect(effects, "clear", emptyList())
    }

    /** 顺序敏感地整体设置后期效果列表；核心不支持时返回 false。 */
    fun set(player: Player, keys: List<String>): Boolean {
        val effects = postEffects(player) ?: return false
        return invokeEffect(effects, "set", keys)
    }

    /** 强制刷新效果发送；核心不支持时返回 false。 */
    fun update(player: Player): Boolean {
        val effects = postEffects(player) ?: return false
        return invokeEffect(effects, "update", emptyList())
    }

    /** 调用 `Player#postEffects()`；不支持时返回 null。 */
    private fun postEffects(player: Player): Any? {
        val method = resolvePostEffectsMethod() ?: return null
        return runCatching { method.invoke(player) }.getOrNull()
    }

    /**
     * 调用 `PlayerPostEffects` 的对应方法。
     *
     * `set` 需要一个 `SequencedCollection<Key>`，这里按 `List` 传入（`List` 是 `SequencedCollection` 子类型）；
     * key 字符串经反射转换为 Adventure `Key`（`Key.key(String)`）。
     */
    private fun invokeEffect(effects: Any, methodName: String, keys: List<String>): Boolean {
        return runCatching {
            when (methodName) {
                "add", "remove" -> {
                    val key = toAdventureKey(keys.firstOrNull()) ?: return false
                    val method = effects.javaClass.methods.firstOrNull {
                        it.name == methodName && it.parameterCount == 1
                    } ?: return false
                    method.invoke(effects, key) as? Boolean ?: false
                }

                "set" -> {
                    val converted = keys.mapNotNull { toAdventureKey(it) }
                    val method = effects.javaClass.methods.firstOrNull {
                        it.name == "set" && it.parameterCount == 1
                    } ?: return false
                    method.invoke(effects, converted) as? Boolean ?: false
                }

                else -> {
                    val method = effects.javaClass.methods.firstOrNull {
                        it.name == methodName && it.parameterCount == 0
                    } ?: return false
                    method.invoke(effects)
                    true
                }
            }
        }.getOrDefault(false)
    }

    /** 将 `namespace:path` 字符串转换为 Adventure `Key`；非法输入返回 null。 */
    private fun toAdventureKey(raw: String?): Any? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching {
            val keyClass = Class.forName("net.kyori.adventure.key.Key")
            val keyMethod = keyClass.methods.firstOrNull {
                it.name == "key" && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java
            } ?: return null
            keyMethod.invoke(null, value)
        }.getOrNull()
    }

    /** 惰性解析 `Player#postEffects()`；当前核心缺少该方法时缓存为不支持。 */
    private fun resolvePostEffectsMethod(): Method? {
        if (postEffectsResolved) return postEffectsMethod
        synchronized(this) {
            if (postEffectsResolved) return postEffectsMethod
            postEffectsMethod = runCatching {
                Player::class.java.methods.firstOrNull {
                    it.name == "postEffects" && it.parameterCount == 0
                }
            }.getOrNull()
            postEffectsResolved = true
            return postEffectsMethod
        }
    }
}
