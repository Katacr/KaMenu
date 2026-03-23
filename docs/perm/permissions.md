# 🔐 权限列表

KaMenu 使用精简的权限体系，共有两个核心权限节点。

---

## 权限一览

| 权限节点 | 说明 | 默认拥有者 |
|---------|------|----------|
| `kamenu.use` | 允许玩家使用 `/km open` 指令打开菜单 | 所有玩家 |
| `kamenu.admin` | 允许执行 `/km reload` 重载插件 | 仅 OP |

---

## 详细说明

### kamenu.use

允许玩家使用 KaMenu 的基础功能，包括：

- 执行 `/km open <菜单ID>` 打开菜单
- 使用 `custom-commands` 中注册的自定义快捷指令
- 通过快捷键（如 Swap-hand 监听器）触发菜单

**默认状态：** 所有玩家默认拥有此权限（`default: true`）

```yaml
# 在权限插件中撤销某玩家的使用权限
# 示例（LuckPerms）：
/lp user <玩家名> permission set kamenu.use false
```

---

### kamenu.admin

允许服务器管理员执行管理操作，包括：

- 执行 `/km reload` 重新加载所有菜单和配置
- 查看重载后加载的菜单数量

**默认状态：** 仅 OP 拥有此权限

```yaml
# 在权限插件中授予某玩家管理权限
# 示例（LuckPerms）：
/lp user <玩家名> permission set kamenu.admin true
```

{% hint style="info" %}
如果你希望特定的非 OP 玩家（如服主助手）也能重载菜单，可以通过权限插件单独授予 `kamenu.admin` 权限。
{% endhint %}
