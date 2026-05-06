# KaMenu v1.4.1 更新报告

## 📋 版本信息
- **版本号**: 1.4.1

---

## ✨ 新增功能

### 1. 新增 force-open: 动作

**功能说明:**
强制打开指定菜单，**跳过目标菜单的 Events.Open 动作列表**。

**使用场景:**
- 需要打开菜单但不想触发 Open 事件中的初始化逻辑（如权限检查、前置动作等）
- 在 Events.Click 中嵌套打开菜单时避免重复执行 Open 事件

**格式:** `force-open: <菜单ID>`

```yaml
# 普通打开（会执行目标菜单的 Open 事件）
- 'open: shop'

# 强制打开（跳过 Open 事件）
- 'force-open: shop'
```

---

### 2. 新增 force-close 动作

**功能说明:**
强制关闭当前菜单，**不执行 Events.Close 动作列表**。

**使用场景:**
- 需要立即关闭菜单而不触发 Close 事件中的清理/记录逻辑

**格式:** `force-close`

```yaml
# 普通关闭（会执行 Close 事件）
- 'close'

# 强制关闭（跳过 Close 事件）
- 'force-close'
```

---

### 3. 新增 reset 动作

**功能说明:**
重新打开当前菜单（相当于刷新），**不执行 Events.Open 动作列表**。

**使用场景:**
- 刷新当前菜单内容（如更新了变量显示）
- 重置按钮状态

**格式:** `reset`

```yaml
# 刷新当前菜单
- 'reset'
```

---

## 🔧 优化改进

### 4. actions 动作组内 open 动作的 wait/return 支持

**问题:**
在 `actions:` 引用的动作组中或条件分支内使用 `open:` 动作时，`wait` 延迟和 `return` 中断指令无法正常工作。这是因为异步执行链的类型传递存在缺陷——`return` 标志无法从子链正确传播回父链。

**修复:**
重构 `executeActionListAsync` 和 `executeActionList` 的返回类型为 `CompletableFuture<Boolean>`，使 `return` 中断标志能够沿整个异步调用链正确传播。

**影响范围:**

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| actions 组内的 wait | ❌ 不生效 | ✅ 正常等待 |
| actions 组内的 return | ❌ 不中断后续动作 | ✅ 正确中断 |
| 条件分支中的 open + wait | ❌ 不生效 | ✅ 正常等待 |
| 条件分支中的 open + return | ❌ 不中断 | ✅ 正确中断 |

**示例:**
```yaml
Events.Click:
  confirm:
    actions:
      - 'tell: 即将打开商店...'
      - 'open: shop'        # 现在可以正常配合 wait 和 return 使用
      - 'tell: 这行不会执行（如果商店内有 return）'
      - 'wait: 20'          # 现在可以被正确累积并等待

# 菜单 shop.yml 内部
Events.Open:
  - 'wait: 40'              # 会被正确等待
  - 'tell: 欢迎来到商店！'
  - 'return'                # 会中断 actions 组中后续的动作
```

---

## 📝 配置示例

### force-open / force-close / reset 示例

```yaml
Body:
  type: 'multi'
  buttons:
    # 使用 force-open 打开子菜单，不触发其 Open 事件
    shop:
      text: '&a商店'
      actions:
        - 'force-open: shop_menu'

    # 刷新当前菜单内容
    refresh:
      text: '&e刷新'
      actions:
        - 'reset'

    # 直接关闭，不执行 Close 事件
    exit:
      text: '&c退出'
      actions:
        - 'force-close'

# 对比：普通 close 会先执行 Events.Close
# 对比：普通 open 会先执行目标的 Events.Open
```

### actions 组内 open 配合 wait/return 示例

```yaml
Events.Click:
  teleport_hub:
    actions:
      - 'command: tp hub'
      - 'wait: 30'           # 传送后等待 1.5 秒
      - 'open: main_menu'    # 打开主菜单（会等待上面的 wait）
      - 'tell: 已完成！'     # 如果 main_menu 内有 return，此行不会执行
```

---

## ⚠️ 注意事项

- `force-open`、`force-close`、`reset` 均跳过对应的事件动作，请确认不需要执行该事件时才使用
- Tab 补全已支持新动作的关键字匹配：输入 `force-open`、`force-close`、`reset` 均可自动补全
- 本次修改为内部行为优化，现有配置无需更改即可获得 `actions` 组内 `open` 的 `wait`/`return` 修复效果

---

## 🚀 升级指南

### 从 1.4.0 升级到 1.4.1

**升级步骤:**

1. **更新插件**
   - 下载最新的 `KaMenu-1.4.1.jar`
   - 替换旧版本 jar 文件

2. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器

**兼容性:**
- ✅ **完全兼容**：无需修改任何配置即可享受所有优化和新功能
- ✅ **新增 API**：`MenuUI.forceOpenMenu()`、`MenuManager.getMenuId()` 可供其他插件调用

---

## 📈 版本对比

|| 1.4.0 | 1.4.1 |
||-------|-------|
| force-open 动作 | ❌ | ✅ |
| force-close 动作 | ❌ | ✅ |
| reset 动作 | ❌ | ✅ |
| actions 组内 open 的 wait 支持 | ❌ | ✅ |
| actions 组内 open 的 return 支持 | ❌ | ✅ |

---

## 📚 相关文档

- **动作系统文档**: `docs/menu/actions.md`
- **示例菜单**: `menus/example/` - 中文示例
- **示例菜单**: `menus/exampleEN/` - 英文示例

---

# KaMenu v1.4.1 Update Report

## 📋 Version Info
- **Version**: 1.4.1

---

## ✨ New Features

### 1. New force-open: Action

**Description:**
Force opens a specified menu, **skipping the target menu's Events.Open action list**.

**Use Cases:**
- Opening menus without triggering initialization logic (permission checks, pre-actions) in the Open event
- Nesting menu opens in Events.Click without re-executing the target's Open event

**Format:** `force-open: <menuID>`

```yaml
# Normal open (triggers target's Open event)
- 'open: shop'

# Force open (skips Open event)
- 'force-open: shop'
```

---

### 2. New force-close Action

**Description:**
Force closes the current menu, **without executing the Events.Close action list**.

**Use Cases:**
- Immediately closing the menu without triggering cleanup/recording logic in the Close event

**Format:** `force-close`

```yaml
# Normal close (executes Close event)
- 'close'

# Force close (skips Close event)
- 'force-close'
```

---

### 3. New reset Action

**Description:**
Reopens the current menu (refresh), **without executing the Events.Open action list**.

**Use Cases:**
- Refreshing current menu content (e.g., updated variable displays)
- Resetting button states

**Format:** `reset`

```yaml
# Refresh current menu
- 'reset'
```

---

## 🔧 Improvements

### 4. wait/return Support for open Inside actions Groups

**Issue:**
When using `open:` inside `action groups` or conditional branches, `wait` delays and `return` interrupts did not work properly. The async execution chain had a type propagation issue — the `return` flag could not correctly propagate from child chains to parent chains.

**Fix:**
Refactored `executeActionListAsync` and `executeActionList` return types to `CompletableFuture<Boolean>`, allowing `return` interrupt flags to propagate correctly along the entire async call chain.

**Impact:**

| Scenario | Before Fix | After Fix |
|----------|-----------|-----------|
| wait inside actions group | ❌ Not working | ✅ Works correctly |
| return inside actions group | ❌ Does not interrupt | ✅ Interrupts correctly |
| open + wait in conditions | ❌ Not working | ✅ Works correctly |
| open + return in conditions | ❌ No interrupt | ✅ Interrupts correctly |

**Example:**
```yaml
Events.Click:
  confirm:
    actions:
      - 'tell: Opening shop...'
      - 'open: shop'        # Now works with wait and return
      - 'tell: This wont execute if shop has return'
      - 'wait: 20'          # Now properly accumulated and waited

# Inside shop.yml
Events.Open:
  - 'wait: 40'             # Will be waited properly now
  - 'tell: Welcome to shop!'
  - 'return'               # Will interrupt subsequent actions in parent group
```

---

## ⚠️ Notes

- `force-open`, `force-close`, and `reset` skip their respective events — only use when you don't need those events to execute
- Tab completion supports keyword matching for new actions: `force-open`, `force-close`, `reset`
- This is an internal behavior optimization — existing configs automatically benefit from the `actions` group `open` wait/return fix without changes

---

## 🚀 Upgrade Guide

### Upgrade from 1.4.0 to 1.4.1

**Steps:**

1. **Update Plugin**
   - Download latest `KaMenu-1.4.1.jar`
   - Replace old jar file

2. **Restart Server**
   - Stop server completely
   - Restart server

**Compatibility:**
- ✅ **Fully compatible**: No config changes needed
- ✅ **New APIs**: `MenuUI.forceOpenMenu()`, `MenuManager.getMenuId()` available for other plugins

---

## 📈 Version Comparison

|| 1.4.0 | 1.4.1 |
||-------|-------|
| force-open action | ❌ | ✅ |
| force-close action | ❌ | ✅ |
| reset action | ❌ | ✅ |
| wait support for open in actions groups | ❌ | ✅ |
| return support for open in actions groups | ❌ | ✅ |

---

## 📚 Related Documentation

- **Action System Docs**: `docs/menu/actions.md`
- **Example Menus**: `menus/example/` - Chinese examples
- **Example Menus**: `menus/exampleEN/` - English examples
