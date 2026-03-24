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
     * 删除玩家数据
     */
    fun deletePlayerData(playerUuid: UUID, key: String): Boolean {
        connection.use { conn ->
            val sql = "DELETE FROM player_data WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 修改玩家数据（增加或减少数值）
     * @param playerUuid 玩家 UUID
     * @param key 数据键
     * @param delta 变化量（正数增加，负数减少，String 类型）
     */
    fun modifyPlayerData(playerUuid: UUID, key: String, delta: String) {
        val numDelta = delta.toDoubleOrNull()
        if (numDelta == null) {
            plugin.logger.warning("玩家数据修改失败: 变化量 '$delta' 不是数字")
            return
        }

        connection.use { conn ->
            // 获取当前值
            val sql = "SELECT data_value FROM player_data WHERE player_uuid = ? AND data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, playerUuid.toString())
                stmt.setString(2, key)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val currentValue = rs.getString("data_value")
                        val currentNum = currentValue.toDoubleOrNull()
                        if (currentNum != null) {
                            // 当前值是数字，可以进行加减操作
                            val newValue = (currentNum + numDelta).toString()
                            setPlayerData(playerUuid, key, newValue)
                        } else {
                            // 当前值不是数字，无法进行加减操作
                            plugin.logger.warning("玩家数据修改失败: 键 '$key' 的当前值 '$currentValue' 不是数字")
                        }
                    } else {
                        // 键不存在，直接设置为 delta 的值
                        setPlayerData(playerUuid, key, numDelta.toString())
                    }
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
     * 删除全局数据
     */
    fun deleteGlobalData(key: String): Boolean {
        connection.use { conn ->
            val sql = "DELETE FROM global_data WHERE data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                return stmt.executeUpdate() > 0
            }
        }
    }

    /**
     * 修改全局数据（增加或减少数值）
     * @param key 数据键
     * @param delta 变化量（正数增加，负数减少，String 类型）
     */
    fun modifyGlobalData(key: String, delta: String) {
        val numDelta = delta.toDoubleOrNull()
        if (numDelta == null) {
            plugin.logger.warning("全局数据修改失败: 变化量 '$delta' 不是数字")
            return
        }

        connection.use { conn ->
            // 获取当前值
            val sql = "SELECT data_value FROM global_data WHERE data_key = ?"
            conn.prepareStatement(sql).use { stmt ->
                stmt.setString(1, key)
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        val currentValue = rs.getString("data_value")
                        val currentNum = currentValue.toDoubleOrNull()
                        if (currentNum != null) {
                            // 当前值是数字，可以进行加减操作
                            val newValue = (currentNum + numDelta).toString()
                            setGlobalData(key, newValue)
                        } else {
                            // 当前值不是数字，无法进行加减操作
                            plugin.logger.warning("全局数据修改失败: 键 '$key' 的当前值 '$currentValue' 不是数字")
                        }
                    } else {
                        // 键不存在，直接设置为 delta 的值
                        setGlobalData(key, numDelta.toString())
                    }
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
