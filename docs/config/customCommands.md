# 自定义指令

KaMenu 提供了强大的自定义指令功能，让你能够为每个菜单创建独立、简洁的打开指令，极大提升了玩家的使用体验。

## 为什么使用自定义指令？

### ❌ 传统方式的弊端

使用 `/kamenu open <菜单名>` 打开菜单存在以下问题：

- **指令冗长**：玩家需要输入完整的菜单路径，记忆负担重
- **权限问题**：必须给予玩家 `kamenu.admin` 权限，玩家可以打开所有菜单，存在安全隐患
- **管理复杂**：想限制特定玩家访问特定菜单时，需要配置复杂的权限系统

### ✅ 自定义指令的优势

使用注册指令打开菜单：

- **简洁快捷**：每个菜单都有独立的短指令，如 `/main`、`/shop` 等，记忆轻松
- **权限灵活**：无需配置权限，所有玩家都能看到指令
- **条件控制**：在菜单的 `Events.Open` 事件中判断条件，自由控制谁能打开菜单

## 配置自定义指令

在 `config.yml` 中配置自定义指令：

```yaml
custom-commands:
  # 自定义指令名: 菜单ID
  main: example/main_menu
  shop: example/shop_menu
  vip: example/vip_menu
  admin: example/admin_menu
```

## 示例：限制特定玩家访问

假设你只想让 VIP 玩家打开 VIP 菜单，无需配置权限，只需在菜单的 `Events.Open` 事件中添加条件判断：

```yaml
Events:
  Open:
    - condition: "hasPerm.user.vip"
      actions:
        - 'tell: 欢迎访问 VIP 菜单！'
      deny:
        - 'tell: §c你没有 user.vip 权限，无法访问此菜单'
        - 'return'
```

这样配置后：
- ✅ 所有玩家都可以尝试执行 `/vip` 指令
- ✅ 只有拥有 `vip` 权限的玩家才能成功打开菜单
- ✅ 其他玩家会收到提示信息，菜单不会打开

## 使用场景

### 场景 1：常用菜单快捷访问

为常用菜单创建简短指令：

```yaml
custom-commands:
  menu: example/main_menu      # 主菜单
  warp: example/warp_menu      # 传送菜单
  shop: example/shop_menu      # 商店菜单
  bank: example/bank_menu      # 银行菜单
```

玩家只需输入 `/menu`、`/warp` 等简单指令即可快速访问。

### 场景 2：特权菜单条件访问

为 VIP、管理员等创建特殊菜单：

```yaml
custom-commands:
  vip: example/vip_menu
  admin: example/admin_menu
  owner: example/owner_menu
```

然后在各自的 `Events.Open` 中判断玩家权限，实现条件访问。

### 场景 3：功能菜单独立入口

为特定功能创建独立入口：

```yaml
custom-commands:
  daily: example/daily_reward     # 每日签到
  mail: example/mail_system        # 邮件系统
  quest: example/quest_system      # 任务系统
```

## 最佳实践

### 1. 命名规范

- 使用简短、易记的英文名称
- 避免使用特殊符号
- 建议使用小写字母，兼容性更好

```yaml
# ✅ 推荐
custom-commands:
  menu: ...
  shop: ...
  vip: ...

# ❌ 不推荐
custom-commands:
  @shop: ...           # 特殊符号可能冲突
  VERYLONGNAME: ...    # 太长不方便输入
  help: ...         # 尽量避免与其他插件冲突
```

### 2. 条件判断建议

在 `Events.Open` 中使用条件判断时，建议：

- 提供清晰的提示信息
- 使用 `return` 动作阻止不符合条件的玩家打开菜单
- 结合 `data` 或 `meta` 实现更灵活的访问控制

```yaml
Events:
  Open:
    # 多条件组合判断
    - condition: "hasPerm.user.vip && %player_level% >= 10"
      actions:
        - 'tell: '欢迎访问 VIP 菜单'
      deny:
        - 'tell: §c你需要 user.vip 权限且等级达到 10 级才能访问此菜单'
        - 'return'
```

### 3. 重载指令

修改 `config.yml` 后，执行以下指令重载：

```
/kamenu reload
```

系统会自动注册所有自定义指令，无需重启服务器。

## 技术细节

- 自定义指令在服务器启动时自动注册
- 支持 `/kamenu reload` 热重载，无需重启服务器
- 指令名称不区分大小写（`/menu` 和 `/MENU` 效果相同）
- 自定义指令与主指令独立，互不影响

## 总结

自定义指令功能让菜单访问变得简单、安全、灵活。通过合理使用，你可以：

1. 简化玩家的操作流程
2. 避免复杂的权限配置
3. 通过条件判断实现精细的访问控制
4. 提升整体用户体验

开始使用自定义指令，让你的插件更加易用和强大吧！
