package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 正版玩家头颅纹理缓存（数据库持久化 + 跨服共享）。
 *
 * 背景：`Bukkit.getOfflinePlayer(name)` 只对本服有记录的玩家返回带皮肤的档案；
 * 从未在本服游玩过的正版玩家会被构造成离线模式假档案（无皮肤）。
 *
 * 在 **Velocity/BungeeCord 代理后端且 online-mode=false** 的环境下，
 * Paper 不会主动向 Mojang 查询皮肤（`createProfile`/`complete(true)`/`update()`
 * 均不联网，实测 properties=0、skin=null），因此本类**直接调用 Mojang 官方 API**：
 *
 *   1. `https://api.mojang.com/users/profiles/minecraft/<name>` → 正版 UUID
 *   2. `https://sessionserver.mojang.com/session/minecraft/profile/<uuid>?unsigned=false`
 *      → 返回 base64 的 `textures` 属性
 *
 * 缓存存放在 KaMenu 数据库表 `skull_textures`（主键 = 玩家名小写）：
 * - **持久化**：重启后无需重新联网拉取。
 * - **跨服共享**：所有子服共用同一 MySQL 时天然同步。
 *
 * 读路径全部走内存：启动时全量载入，之后由定期任务从数据库增量拉取其他子服新增的记录。
 * 所有网络请求都在异步线程执行，不阻塞主线程。
 */
class SkullProfileCache(private val plugin: KaMenu) {
    /** 玩家名（小写）-> base64 纹理字符串。 */
    private val textures = ConcurrentHashMap<String, String>()

    /** 玩家名（小写）-> 本地已知的数据库更新时间戳（用于增量同步）。 */
    private val knownTimestamps = ConcurrentHashMap<String, Long>()

    /** 正在加载中的名字，避免重复请求。 */
    private val pending = ConcurrentHashMap.newKeySet<String>()

    /** 加载失败的名字 -> 失败时间戳（带过期，避免临时故障后永不重试）。 */
    private val failed = ConcurrentHashMap<String, Long>()

    /** 加载完成后触发的回调（用于刷新菜单）。 */
    @Volatile
    private var onLoaded: ((String) -> Unit)? = null

    /** 数据库同步任务句柄。 */
    @Volatile
    private var syncTask: KaTaskHandle? = null

    /** 启动时从数据库全量载入，并启动增量同步任务。 */
    fun init() {
        CompletableFuture.runAsync {
            runCatching {
                val all = plugin.databaseManager.getAllSkullTextures()
                all.forEach { (name, pair) ->
                    textures[name] = pair.first
                    knownTimestamps[name] = pair.second
                }
                if (all.isNotEmpty()) {
                    plugin.logger.info("[KaMenu] 已从数据库载入头颅纹理缓存：${all.size} 条")
                }
            }.onFailure {
                plugin.logger.warning("[KaMenu] 头颅纹理缓存载入失败：${it.message}")
            }
        }

        // 每 30 秒从数据库增量拉取其他子服新增/更新的记录（跨服同步）。
        val intervalTicks = 30 * 20L
        syncTask = KaScheduler.runAsyncTimer(0L, intervalTicks * 50L, Runnable { syncFromDatabase() })
    }

    fun setOnLoaded(callback: ((String) -> Unit)?) {
        onLoaded = callback
    }

    /** 从数据库增量同步（异步）。 */
    private fun syncFromDatabase() {
        runCatching {
            val all = plugin.databaseManager.getAllSkullTextures()
            var changed = false
            all.forEach { (name, pair) ->
                val (texture, updatedAt) = pair
                val known = knownTimestamps[name]
                if (known == null || updatedAt > known) {
                    textures[name] = texture
                    knownTimestamps[name] = updatedAt
                    changed = true
                }
            }
            if (changed) {
                // 其他子服新增了纹理，刷新本服已打开菜单。
                KaScheduler.runGlobal(Runnable {
                    if (plugin.containerMenusReady) {
                        plugin.containerMenuService.refreshAll("skull-textures sync")
                    }
                })
            }
        }.onFailure {
            plugin.logger.fine("[KaMenu] 头颅纹理增量同步失败：${it.message}")
        }
    }

    /**
     * 获取玩家头颅的 base64 纹理。
     *
     * 缓存策略（以数据库 `update_time` 为时间戳）：
     * - **未缓存**：返回 null 并异步拉取（调用方先渲染兜底皮肤）。
     * - **已缓存且未超过 15 天**：直接返回，不重新缓存。
     * - **已缓存但超过 15 天**：**先返回旧纹理**（保证界面立即有皮肤），
     *   同时异步重新拉取，成功后更新内存与数据库并刷新菜单。
     *
     * @return 命中缓存时返回纹理；未命中返回 null 并触发后台加载。
     */
    fun getTexture(name: String): String? {
        val key = name.lowercase()
        if (key.isEmpty()) return null

        val cached = textures[key]
        if (cached != null) {
            val updatedAt = knownTimestamps[key] ?: 0L
            if (System.currentTimeMillis() - updatedAt > REFRESH_AFTER_MILLIS) {
                // 超过 15 天：先引用旧纹理，再异步尝试重新缓存。
                requestAsync(name, key)
            }
            return cached
        }

        // 失败缓存在有效期内直接返回，过期后允许重试。
        failed[key]?.let { at ->
            if (System.currentTimeMillis() - at < FAIL_RETRY_MILLIS) return null
            failed.remove(key)
        }

        requestAsync(name, key)
        return null
    }

    /**
     * 手动缓存指定玩家的头颅纹理（忽略 15 天有效期，强制重新拉取）。
     *
     * 供管理员命令调用：为指定玩家名预热/刷新纹理缓存。
     *
     * @param name 玩家名（正版 ID）
     * @param force true = 忽略已有缓存强制刷新；false = 已有缓存则跳过
     * @param callback 结果回调（在主线程执行）：true 表示已缓存成功
     */
    fun cacheManually(name: String, force: Boolean, callback: (Boolean) -> Unit) {
        val key = name.lowercase()
        if (key.isEmpty()) {
            KaScheduler.runGlobal(Runnable { callback(false) })
            return
        }
        if (!force && textures.containsKey(key)) {
            // 已有缓存且不强制刷新，直接视为成功。
            KaScheduler.runGlobal(Runnable { callback(true) })
            return
        }
        // 清掉失败标记，允许立即重试。
        failed.remove(key)

        CompletableFuture.supplyAsync {
            fetchTextureFromMojang(name)
        }.whenComplete { texture, error ->
            pending.remove(key)
            if (texture != null) {
                textures[key] = texture
                CompletableFuture.runAsync {
                    runCatching {
                        plugin.databaseManager.setSkullTexture(key, texture)
                        knownTimestamps[key] = System.currentTimeMillis()
                    }.onFailure {
                        plugin.logger.warning("[KaMenu] 头颅纹理写入数据库失败: $name (${it.message})")
                    }
                }
                KaScheduler.runGlobal(Runnable { callback(true) })
            } else {
                failed[key] = System.currentTimeMillis()
                plugin.logger.fine("[KaMenu] 手动缓存头颅纹理失败: $name (${error?.message})")
                KaScheduler.runGlobal(Runnable { callback(false) })
            }
        }
    }

    /** 当前已缓存的玩家名集合（用于命令展示）。 */
    fun cachedNames(): Set<String> = textures.keys.toSortedSet()

    /**
     * 将所有已缓存纹理标记为「过期」（手动刷新）。
     *
     * 用于管理员命令 `/km reload skullcache`：
     * 把内存中所有纹理的时间戳置零，使其在下一次被读取时（玩家打开菜单）
     * 触发异步重缓存，而**不是**在这里一次性批量联网拉取，
     * 从而避免瞬间发起大量请求。
     *
     * 旧纹理在重缓存完成前仍会被引用（界面不会空窗）。
     *
     * @return 被标记的条目数。
     */
    fun markAllExpired(): Int {
        val count = knownTimestamps.size
        knownTimestamps.keys.forEach { knownTimestamps[it] = 0L }
        plugin.logger.info("[KaMenu] 已将 $count 条头颅纹理标记为过期，将在玩家打开菜单时按需重缓存。")
        return count
    }

    /**
     * 在线玩家可直接取其档案中的纹理，无需联网。
     *
     * @return 在线且带皮肤时返回纹理，否则 null。
     */
    fun onlineTexture(player: Player): String? = runCatching {
        val profile = player.javaClass.getMethod("getPlayerProfile").invoke(player)
        extractTextureFromProperties(profile)
    }.getOrNull()

    /** 从档案对象的 properties 集合中取出 textures 属性的值。 */
    private fun extractTextureFromProperties(profile: Any?): String? = runCatching {
        if (profile == null) return null
        val properties = profile.javaClass.getMethod("getProperties").invoke(profile) as? Collection<*> ?: return null
        for (property in properties) {
            if (property == null) continue
            val name = property.javaClass.getMethod("getName").invoke(property) as? String ?: continue
            if (name.equals("textures", ignoreCase = true)) {
                return property.javaClass.getMethod("getValue").invoke(property) as? String
            }
        }
        null
    }.getOrNull()

    /** 后台异步请求 Mojang API。 */
    private fun requestAsync(name: String, key: String) {
        if (!pending.add(key)) return

        CompletableFuture.supplyAsync {
            fetchTextureFromMojang(name)
        }.whenComplete { texture, error ->
            pending.remove(key)
            if (texture != null) {
                val hadOld = textures.containsKey(key)
                textures[key] = texture
                // 写入数据库（持久化 + 跨服共享）。
                CompletableFuture.runAsync {
                    runCatching {
                        plugin.databaseManager.setSkullTexture(key, texture)
                        knownTimestamps[key] = System.currentTimeMillis()
                    }.onFailure {
                        plugin.logger.warning("[KaMenu] 头颅纹理写入数据库失败: $name (${it.message})")
                    }
                }
                // 回到主线程触发刷新回调（Folia 兼容由 KaScheduler 处理）。
                KaScheduler.runGlobal(Runnable { onLoaded?.invoke(name) })
                if (hadOld) {
                    plugin.logger.fine("[KaMenu] 头颅纹理已刷新（超期重缓存）: $name")
                }
            } else if (textures.containsKey(key)) {
                // 超期刷新失败：保留旧纹理，不记失败（避免影响下次尝试）。
                plugin.logger.fine("[KaMenu] 头颅纹理超期刷新失败，保留旧纹理: $name (${error?.message})")
            } else {
                failed[key] = System.currentTimeMillis()
                plugin.logger.fine("[KaMenu] 拉取头颅纹理失败: $name (${error?.message})")
            }
        }
    }

    /** 依次请求 Mojang 的两个端点，返回 base64 纹理。 */
    private fun fetchTextureFromMojang(name: String): String? {
        // 1) 名字 -> UUID
        val uuidJson = httpGet("https://api.mojang.com/users/profiles/minecraft/$name") ?: return null
        val uuid = Regex("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"").find(uuidJson)?.groupValues?.get(1) ?: return null

        // 2) UUID -> 档案（含 textures 属性）
        val profileJson = httpGet(
            "https://sessionserver.mojang.com/session/minecraft/profile/$uuid?unsigned=false"
        ) ?: return null
        return Regex("\"name\"\\s*:\\s*\"textures\"\\s*,\\s*\"value\"\\s*:\\s*\"([^\"]+)\"")
            .find(profileJson)?.groupValues?.get(1)
    }

    /** 简单的同步 HTTP GET（仅在异步线程调用）。 */
    private fun httpGet(url: String): String? = runCatching {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.setRequestProperty("User-Agent", "KaMenu/2.0 (skull texture resolver)")
        try {
            if (connection.responseCode != 200) return@runCatching null
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    /** 插件卸载时调用：停止同步任务并清理运行态集合（数据已在数据库，无需写盘）。 */
    fun shutdown() {
        syncTask?.cancel()
        syncTask = null
        pending.clear()
        failed.clear()
        textures.clear()
        knownTimestamps.clear()
    }

    private companion object {
        /** 加载失败后多久允许重试（毫秒）。 */
        const val FAIL_RETRY_MILLIS = 10 * 60 * 1000L

        /** 缓存有效期：超过 15 天后先引用旧纹理再异步重新缓存。 */
        const val REFRESH_AFTER_MILLIS = 15 * 24 * 60 * 60 * 1000L
    }
}
