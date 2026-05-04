package org.katacr.kamenu

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * 更新检查器
 * 从 GitHub 获取最新版本号，对比当前版本，向 OP 玩家发送更新提示
 */
object UpdateChecker {

    private const val PLUGIN_YML_URL =
        "https://raw.githubusercontent.com/Katacr/KaMenu/refs/heads/main/src/main/resources/plugin.yml"
    private const val MINEBBS_URL = "https://www.minebbs.com/resources/15814/"
    private const val SPIGOTMC_URL = "https://www.spigotmc.org/resources/133736/"

    private var latestVersion: String? = null
    private var currentVersion: String? = null
    private var checkComplete = false
    private var languageManager: LanguageManager? = null

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * 启动异步版本检查
     */
    fun check(plugin: KaMenu) {
        currentVersion = plugin.description.version
        languageManager = plugin.languageManager
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(PLUGIN_YML_URL))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    val body = response.body()
                    val versionRegex = Regex("version:\\s*'([^']+)'")
                    latestVersion = versionRegex.find(body)?.groupValues?.get(1)
                }
            } catch (_: Exception) {
                // 网络不可达，静默忽略
            }
            checkComplete = true
        })
    }

    /**
     * 向 OP 玩家发送更新提示（如果有新版本）
     */
    fun notifyIfUpdateAvailable(player: Player) {
        if (!checkComplete || !player.isOp) return
        val latest = latestVersion ?: return
        val current = currentVersion ?: return
        if (!isNewer(latest, current)) return

        val msg = languageManager?.getMessage("plugin.update_available", latest, current)
            ?: "&e[KaMenu] &fNew version available: &a$latest&f, current: &7$current"
        val hoverText = parseUpdateMessage(msg)
        player.sendMessage(MenuActions.parseClickableText(hoverText))
    }

    /**
     * 构建带点击链接的 hovertext 消息
     */
    private fun parseUpdateMessage(plain: String): String {
        val minebbsHover = languageManager?.getMessage("plugin.update_minebbs_hover") ?: "&7MineBBS"
        val spigotmcHover = languageManager?.getMessage("plugin.update_spigotmc_hover") ?: "&7SpigotMC"
        return plain.replace(
            "&a[MineBBS]",
            "<text='&a[MineBBS]';hover='$minebbsHover';url='$MINEBBS_URL'>"
        ).replace(
            "&b[SpigotMC]",
            "<text='&b[SpigotMC]';hover='$spigotmcHover';url='$SPIGOTMC_URL'>"
        )
    }

    /**
     * 比较版本号，latest > current 则返回 true
     * 支持标准 semver 格式 x.y.z
     */
    private fun isNewer(latest: String, current: String): Boolean {
        val lv = parseVersion(latest) ?: return false
        val cv = parseVersion(current) ?: return false
        for (i in 0 until maxOf(lv.size, cv.size)) {
            val l = lv.getOrElse(i) { 0 }
            val c = cv.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun parseVersion(version: String): List<Int>? {
        return try {
            version.split(".").map { it.toInt() }
        } catch (_: Exception) {
            null
        }
    }
}
