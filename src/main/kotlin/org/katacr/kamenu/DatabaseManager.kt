package org.katacr.kamenu

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.util.*

/**
 * 数据库管理器
 * 用于管理玩家数据和全局数据
 */
class DatabaseManager(val plugin: KaMenu) {
    var dataSource: HikariDataSource? = null

    /**
     * 初始化数据库
     */
    fun setup() {
        val config = HikariConfig()
        val dbType = plugin.config.getString("storage.type", "SQLite") ?: "SQLite"

        if (dbType.equals("MySQL", ignoreCase = true)) {
            config.jdbcUrl = "jdbc:mysql://${plugin.config.getString("storage.host")}:${plugin.config.getInt("storage.port")}/${plugin.config.getString("storage.db")}"
            config.username = plugin.config.getString("storage.user")
            config.password = plugin.config.getString("storage.password")
            config.driverClassName = "com.mysql.cj.jdbc.Driver"
        } else {
            val file = plugin.dataFolder.resolve("storage.db")
            config.jdbcUrl = "jdbc:sqlite:${file.absolutePath}"
            config.driverClassName = "org.sqlite.JDBC"
        }

        config.maximumPoolSize = 10
        if (!dbType.equals("MySQL", ignoreCase = true)) config.maximumPoolSize = 1

        dataSource = HikariDataSource(config)
        createTables()
    }

    /**
     * 获取数据库连接
     */
    val connection: Connection
        get() = dataSource!!.connection

    /**
     * 创建数据库表
     */
    private fun createTables() {
        val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
        val isMySQL = dbType.equals("mysql", ignoreCase = true)
        val autoIncrement = if (isMySQL) "AUTO_INCREMENT" else ""
        val uniqueConstraint = if (isMySQL) "UNIQUE KEY" else "UNIQUE"

        connection.use { conn ->
            val statement = conn.createStatement()

            // 玩家数据表
            statement.execute("""
                CREATE TABLE IF NOT EXISTS player_data (
                    id INTEGER PRIMARY KEY $autoIncrement,
                    player_uuid VARCHAR(36) NOT NULL,
                    data_key VARCHAR(64) NOT NULL,
                    data_value TEXT,
                    update_time BIGINT,
                    $uniqueConstraint(player_uuid, data_key)
                )
            """)

            // 全局数据表
            statement.execute("""
                CREATE TABLE IF NOT EXISTS global_data (
                    id INTEGER PRIMARY KEY $autoIncrement,
                    data_key VARCHAR(64) NOT NULL UNIQUE,
                    data_value TEXT,
                    update_time BIGINT
                )
            """)
        }
    }

    /**
     * 设置玩家数据
     */
    fun setPlayerData(playerUuid: UUID, key: String, value: String) {
        connection.use { conn ->
            val currentTime = System.currentTimeMillis()
            val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
            val isMySQL = dbType.equals("mysql", ignoreCase = true)

            val sql = if (isMySQL) {
                """
                INSERT INTO player_data (player_uuid, data_key, data_value, update_time)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    data_value = VALUES(data_value),
                    update_time = VALUES(update_time)
            """.trimIndent()
            } else {
                """
                INSERT INTO player_data (player_uuid, data_key, data_value, update_time)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(player_uuid, data_key) DO UPDATE SET
                    data_value = excluded.data_value,
                    update_time = excluded.update_time
            """.trimIndent()
            }

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.setString(3, value)
                stmt.setLong(4, currentTime)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 获取玩家数据
     */
    fun getPlayerData(playerUuid: UUID, key: String): String? {
        connection.use { conn ->
            val sql = "SELECT data_value FROM player_data WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getString("data_value") else null
                }
            }
        }
    }

    /**
     * 设置全局数据
     */
    fun setGlobalData(key: String, value: String) {
        connection.use { conn ->
            val currentTime = System.currentTimeMillis()
            val dbType = plugin.config.getString("storage.type", "sqlite") ?: "sqlite"
            val isMySQL = dbType.equals("mysql", ignoreCase = true)

            val sql = if (isMySQL) {
                """
                INSERT INTO global_data (data_key, data_value, update_time)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    data_value = VALUES(data_value),
                    update_time = VALUES(update_time)
            """.trimIndent()
            } else {
                """
                INSERT INTO global_data (data_key, data_value, update_time)
                VALUES (?, ?, ?)
                ON CONFLICT(data_key) DO UPDATE SET
                    data_value = excluded.data_value,
                    update_time = excluded.update_time
            """.trimIndent()
            }

            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                stmt.setString(2, value)
                stmt.setLong(3, currentTime)
                stmt.executeUpdate()
            }
        }
    }

    /**
     * 获取全局数据
     */
    fun getGlobalData(key: String): String? {
        connection.use { conn ->
            val sql = "SELECT data_value FROM global_data WHERE data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                stmt.executeQuery().use { rs ->
                    return if (rs.next()) rs.getString("data_value") else null
                }
            }
        }
    }

    /**
     * 关闭数据源
     */
    fun close() {
        dataSource?.close()
        dataSource = null
    }
}
