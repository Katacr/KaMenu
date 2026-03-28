# 🔐 权限设计理念

KaMenu 采用极简的权限设计理念，仅保留核心管理权限，将所有访问控制交给配置系统灵活实现。

---

## 💡 设计理念

### 为什么只有一个权限？

KaMenu 相信**配置大于权限**的设计理念。与其使用繁琐的权限节点来控制菜单访问，不如通过灵活的配置系统实现更精细、更直观的访问控制。

**核心原则：**
- **权限系统**：仅用于插件管理操作（如重载配置）
- **配置系统**：用于所有菜单访问控制（指令、物品、条件判断）

### 为什么这样设计？

#### 传统的权限系统问题

```yaml
# 传统插件通常需要大量权限节点
menu.main.open
menu.shop.open
menu.teleport.open
menu.admin.open
# ... 可能需要几十个权限节点
```

**问题：**
- 权限节点数量较多，难以管理
- 权限与菜单文件分离，配置分散
- 需要记忆大量权限节点名称
- 无法实现复杂的条件访问控制

#### KaMenu 的配置驱动设计
我们可以在菜单内添加开启动作，判断玩家是否符合条件。
```yaml
# 通过条件判断实现访问控制
Events:
  open:
    actions:
      - condition: 'hasPerm.user.vip'
        allow:
          - 'tell: 你有 user.vip 权限，允许打开此菜单。'  # 允许打开菜单
        deny:
          - 'tell: &c你没有 user.vip 权限，无法打开此菜单'
          - 'cancel'    # 阻止打开菜单
```

**优势：**
- 所有访问控制逻辑集中在菜单文件中
- 支持复杂的多条件判断
- 无需配置权限节点，简化管理
- 更直观、更灵活

---

## 📋 权限一览

| 权限节点 | 说明 | 默认拥有者 |
|---------|------|----------|
| `kamenu.admin` | 允许执行管理操作（如重载配置） | 仅 OP |

---

## 🔧 kamenu.admin 权限

### 功能说明

允许服务器管理员执行插件管理操作：

- 允许执行 `/kamenu` 所有的指令

**默认状态：** 仅 OP 拥有此权限

---

## 🎯 访问控制的灵活实现

KaMenu 通过多种配置方式实现菜单访问控制，无需依赖权限系统。

### 1. 自定义指令 - 谁可以使用指令打开菜单？

通过 `config.yml` 中的 `custom-commands` 注册自定义指令，默认所有玩家都可以使用。

```yaml
custom-commands:
  menu: 'main_menu'       # /menu -> 所有玩家都可以执行
  shop: 'server_shop'     # /shop -> 所有玩家都可以执行
```

### 2. 监听器 - 谁可以通过物品/按键打开菜单？

通过 `config.yml` 中的监听器配置，自动触发菜单打开。

```yaml
listeners:
  # 副手键触发
  swap-hand:
    enabled: true
    menu: 'main_menu'
    require-sneaking: true  # 需要 Shift + F

  # 右键物品触发
  item-lore:
    main-menu:
      enabled: true
      material: 'CLOCK'
      target-lore: '菜单'
      menu: 'main_menu'
      require-sneaking: false
```

**默认行为：**
- 所有持有对应物品或按对应按键的玩家都会触发


### 3. Events.open 条件 - 谁可以打开菜单？

通过菜单文件中的 `Events.open` 配置，实现精细的访问控制。

#### 基础示例：权限检查

```yaml
Events:
  open:
    actions:
      - condition: 'hasPerm.kamenu.vip'
        allow:
          - 'tell: &a你有 kamenu.vip 权限，允许打开菜单。'  # 继续打开菜单
        deny:
          - 'tell: &c你需要 kamenu.vip 权限才能访问此菜单'
          - 'return'    # 阻止打开菜单
```

#### 进阶示例：多条件判断

```yaml
Events:
  open:
    actions:
      # 检查多个条件
      - condition: 'hasPerm.kamenu.vip && %vault_eco_balance% >= 100'
        allow:
          - 'tell: &a你有 kamenu.vip 权限和 100 金币，允许打开菜单。'
        deny:
          - 'tell: &c你需要 kamenu.vip 权限和 100 金币才能访问此菜单'
          - 'return'
```

---

## 📝 对比总结

### 传统权限系统 vs KaMenu 配置驱动

| 特性 | 传统权限系统 | KaMenu 配置驱动 |
|------|-------------|----------------|
| 权限节点数量 | 大量（几十个） | 极少（仅 1 个） |
| 配置位置 | 分散（权限插件 + 插件配置） | 集中（菜单文件） |
| 条件判断 | 不支持复杂条件 | 支持任意复杂条件 |
| 灵活性 | 低 | 高 |
| 学习成本 | 需要记忆大量权限节点 | 配置直观易懂 |
| 维护成本 | 高（修改权限需多处配置） | 低（配置集中） |


---

## 📚 相关文档

- [⌨️ 自定义指令](../config/customCommands.md) - 了解如何注册自定义指令
- [⚙️ 事件系统](../menu/events.md) - 了解 Events.open 的详细用法
- [🔧 条件判断](../menu/conditions.md) - 了解各种条件判断方法
- [📋 配置文件](../config/config.md) - 了解 config.yml 的完整配置
