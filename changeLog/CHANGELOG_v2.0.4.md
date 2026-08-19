# KaMenu v2.0.4 更新日志 / Changelog

## 版本信息 / Release Information

- **版本号 / Version:** 2.0.4
- **发布日期 / Release Date:** 2026年8月19日 / August 19, 2026

---

## 中文

### 临时数据 tmpdata

- 新增 `tmpdata:` 动作，支持带到期时间的玩家级键值对，到期后自动清空，适合冷却、限时加成、限时称号等场景。
- 数据持久化到 SQLite/MySQL，玩家下线或服务器重启后仍按原到期时间计算，不会提前失效。
- 支持 `set`、`add`、`take`、`addtime`、`taketime`、`refresh`、`delete` 七种操作类型。
- `ttl` 参数支持纯数字（按秒）与 `1d2h30m15s` 组合（d/h/m/s，不区分大小写）两种写法，`ttl=0` 可作立即作废。

### 时间与数值边界

- `addtime` 只延长到期时间，`taketime` 提前到期；`taketime` 后剩余时间小于等于 0 时立即删除该键（扣时间溢出处理）。
- `refresh` 把到期时间重置为 now + `ttl`，不保留剩余时间。
- `add` / `take` 命中有效键时只改动数值，提供 `ttl` 时同时续期；键不存在或已过期时必须有 `ttl` 才能按新键写入。
- `addtime` / `taketime` / `refresh` 作用于不存在或已过期的键时安全跳过并输出警告，不会复活旧键。
- 到期时间戳运算使用饱和加法，极端数值不会导致 Long 溢出。

### 读取与剩余时间展示

- 新增 `{tmpdata:键名}`（存储值）、`{tmpdata_time:键名}`（剩余秒数，纯整数，已过期返回 `0`）、`{tmpdata_time_format:键名}`（可读文本如 `1天 2时 30分 15秒`）三个内置变量，可在普通文本与条件表达式中使用。
- 新增 PAPI 占位符 `%kamenu_tmpdata_键名%`、`%kamenu_tmpdata_time_键名%`（纯数字秒数，方便其他插件直接数值判断）、`%kamenu_tmpdata_timeformat_键名%`（用户可读文本）。
- 新增 JavaScript 便捷函数 `tmpdata(key, targetPlayer)`，脚本内也可用 `kvar("tmpdata_time:key")`、`kvar("tmpdata_time_format:key")` 读取剩余时间。
- 到期值读取时立即返回空，数据库中的过期行由周期任务（`config.yml` 的 `tmpdata-purge-interval`，默认 60 秒）在异步线程物理清理。

### 文档与 Skill

- 补充中英文 tmpdata 动作、内置变量、PAPI 占位符与 JavaScript 说明，并同步 KaMenu 菜单编写 Skill。

---

## English

### Temporary Data (tmpdata)

- Added the `tmpdata:` action for player-scoped key-value pairs with an expiry time that clears automatically, suitable for cooldowns, limited-time bonuses, and temporary titles.
- Data is persisted to SQLite/MySQL, so the remaining time still follows the original expiry after the player logs off or the server restarts.
- Supports seven operation types: `set`, `add`, `take`, `addtime`, `taketime`, `refresh`, and `delete`.
- The `ttl` parameter accepts plain numbers (seconds) or combinations like `1d2h30m15s` (d/h/m/s, case-insensitive); `ttl=0` acts as an immediate void.

### Time And Numeric Boundaries

- `addtime` only extends the expiry while `taketime` expires earlier; when the remaining time reaches 0 or below after `taketime`, the key is deleted immediately.
- `refresh` resets the expiry to now + `ttl` without preserving the remaining time.
- `add` / `take` only change the value on a valid key; providing `ttl` also renews the expiry, and a missing/expired key needs `ttl` to be created.
- `addtime` / `taketime` / `refresh` skip safely with a warning when the key is missing or already expired, so stale keys are never revived.
- Expiry arithmetic uses saturating addition so extreme values never overflow `Long`.

### Read And Remaining-Time Display

- Added `{tmpdata:key}` (stored value), `{tmpdata_time:key}` (remaining whole seconds, `0` when expired), and `{tmpdata_time_format:key}` (readable text such as `1d 2h 30m 15s`) built-in variables for both text and condition expressions.
- Added PlaceholderAPI placeholders `%kamenu_tmpdata_key%`, `%kamenu_tmpdata_time_key%` (plain seconds for direct numeric checks in other plugins), and `%kamenu_tmpdata_timeformat_key%` (human-readable text).
- Added the `tmpdata(key, targetPlayer)` JavaScript helper; scripts can also use `kvar("tmpdata_time:key")` and `kvar("tmpdata_time_format:key")` for remaining time.
- Expired values return empty immediately on read, while expired rows are physically cleaned on an async thread by the periodic task (`tmpdata-purge-interval` in `config.yml`, default 60 seconds).

### Documentation And Skill

- Added complete Chinese/English tmpdata action, built-in variable, PlaceholderAPI placeholder, and JavaScript references, and synchronized the KaMenu authoring Skill.
