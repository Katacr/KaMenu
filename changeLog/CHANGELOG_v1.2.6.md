# KaMenu v1.2.6 更新报告

## 📋 版本信息
|- **版本号**: 1.2.6
|- **发布日期**: 2026年3月30日

---

## 🐛 Bug 修复

### 1. 修复主线程阻塞导致服务器超时问题

**问题描述：**
在执行 `data:` 和 `gdata:` 动作时，数据库操作在主线程中同步执行，导致服务器主线程阻塞，触发 15 秒超时警告。特别是在 Events.Open 中设置数据后，菜单可能先于数据设置完成就渲染，导致 `{data:key}` 变量显示不正确。

**影响范围：**
- `data:` 动作（玩家数据操作）
- `gdata:` 动作（全局数据操作）
- `take`/`add` 子操作（数据增减）
- `delete` 子操作（数据删除）
- Events.Open/Close 事件中的数据库操作

**原因分析：**
1. `data:` 和 `gdata:` 动作内部调用了数据库修改方法（`modifyPlayerData`/`modifyGlobalData`），这些方法会执行 SELECT → 计算值 → UPDATE/INSERT 流程
2. 原有的 `set-data` 和 `set-gdata` 只执行单次数据库写入，不会阻塞
3. 所有数据库操作都在主线程中同步执行，导致数据库连接池等待时主线程被阻塞
4. 异步执行后，菜单渲染可能在数据写入前完成

**修复方案：**

实现了智能异步/同步执行策略，根据上下文自动选择最佳的执行模式：

**1. 数据库操作异步化**
- 所有数据库操作（`set-data`、`set-gdata`、`data:`、`gdata:`）都改为异步执行
- 使用 `Bukkit.getScheduler().runTaskAsynchronously()` 在独立线程中执行
- 避免阻塞主线程

**2. 智能执行模式**

| 执行场景 | 模式 | 数据操作 | 其他操作 | wait 动作 |
|---------|------|---------|---------|----------|
| Events.Open（无 wait） | 半同步 | 异步 | 同步 | 同步 |
| Events.Open（有 wait） | 完全异步 | 异步 | 异步 | 异步 |
| Events.Close（无 wait） | 半同步 | 异步 | 同步 | 同步 |
| Events.Close（有 wait） | 完全异步 | 异步 | 异步 | 异步 |
| 按钮点击 | 完全异步 | 异步 | 异步 | 异步 |

**3. wait 动作支持优化**

当 Events.Open 中包含 `wait` 动作时：
- 整个打开流程异步执行
- wait 延迟也在异步线程中完成
- 所有动作执行完成后，切回主线程渲染菜单
- 确保数据在菜单渲染前已完成设置

**示例配置：**
```yaml
Events:
  Open:
    - 'set-data: text 你好世界'
    - 'wait: 1'
    - 'set-data: text2 第二个文本'
    - 'wait: 1'
    # 开始渲染菜单
Body:
  message:
    type: 'message'
    text: 'text设置的值为{data:text}'
  message2:
    type: 'message'
    text: 'text2设置的值为{data:text2}'
```

**执行流程：**
```
Open 事件开始（异步）
  ↓
set-data: text = "你好世界"（异步）
  ↓
wait: 1 tick（异步延迟）
  ↓
set-data: text2 = "第二个文本"（异步）
  ↓
wait: 1 tick（异步延迟）
  ↓
所有动作完成 → 切换到主线程
  ↓
渲染菜单，此时 text 和 text2 已正确设置
```

**4. 性能改进**

- **避免阻塞**：数据库操作不再阻塞主线程，服务器性能提升
- **连接池优化**：避免了嵌套连接请求，减少连接池压力
- **智能调度**：根据上下文选择最优执行策略

---

## 🔧 优化改进

### 1. 整数显示优化

**改进内容：**
对 `data:` 和 `gdata:` 动作的 `add`/`take` 操作结果进行优化，移除整数后的 `.0` 后缀。

**优化前：**
```
玩家金币: 100
增加: 50
结果: 150.0  ← 多余的 .0
```

**优化后：**
```
玩家金币: 100
增加: 50
结果: 150  ← 干净的整数
```

**实现逻辑：**
```kotlin
val newValueDouble = currentNum + numDelta
val newValue = if (newValueDouble == newValueDouble.toLong().toDouble()) {
    newValueDouble.toLong().toString()  // 移除 .0
} else {
    newValueDouble.toString()  // 保留小数
}
```

**保留小数的情况：**
- 真正的小数值：`100.5` → `100.5`
- 浮点运算结果：`10.0 + 0.1` → `10.1`

---

### 2. Slider 组件验证增强

**改进内容：**
为 Inputs 中的 slider 组件添加了严格的 min/max 值验证。

**验证规则：**
- min 必须 **小于** max
- 验证失败时使用默认值（min=0.0, max=10.0）
- 记录警告日志提示用户

**错误示例：**
```yaml
Inputs:
  test_slider:
    type: 'slider'
    min: 10.0    # ❌ 错误：min 必须小于 max
    max: 5.0
```

**自动修正：**
- 使用默认值：min=0.0, max=10.0
- 记录警告日志：
  ```
  [KaMenu] Slider 配置错误: 菜单 'test_menu' 的输入组件 'test_slider' 的 min (10.0) 必须小于 max (5.0)，已使用默认值 min=0.0, max=10.0
  ```

**正确示例：**
```yaml
Inputs:
  test_slider:
    type: 'slider'
    min: 0.0      # ✅ 正确：min < max
    max: 10.0
```

---

### 3. 国际化改进

**改进内容：**
将 Slider 配置错误消息改为国际化格式，支持多语言。

**中文：**
```
[KaMenu] Slider 配置错误: 菜单 '{0}' 的输入组件 '{1}' 的 min ({2}) 必须小于 max ({3})，已使用默认值 min=0.0, max=10.0
```

**英文：**
```
[KaMenu] Slider configuration error: Menu '{0}' input component '{1}' min ({2}) must be less than max ({3}), using default values min=0.0, max=10.0
```

---

## 📚 技术细节

### 1. CompletableFuture 异步机制

使用 Java 的 `CompletableFuture` 实现异步操作管理和结果等待：

```kotlin
fun executeActionList(...): CompletableFuture<Boolean> {
    // 执行动作列表
    val actionFutures = actions.map { deferred ->
        executeDeferredAction(...)  // 返回 CompletableFuture
    }

    // 等待所有动作完成
    return CompletableFuture.allOf(*actionFutures.toTypedArray())
        .thenApply { shouldReturn }
}
```

### 2. 智能模式检测

通过递归检查动作列表，判断是否包含 `wait` 动作：

```kotlin
private fun hasWaitAction(actionList: List<*>): Boolean {
    for (action in actionList) {
        when (action) {
            is Map<*, *> -> {
                // 递归检查条件判断中的动作
                if (hasWaitAction(successActions) || hasWaitAction(denyActions)) {
                    return true
                }
            }
            is String -> {
                if (action.trim().lowercase().startsWith("wait:")) {
                    return true
                }
            }
        }
    }
    return false
}
```

### 3. 数据库操作优化

避免嵌套数据库调用，直接使用 UPDATE/INSERT：

```kotlin
// ❌ 旧方案：嵌套调用
modifyPlayerData() {
    val current = getPlayerData()  // 第一次连接
    val newValue = current + delta
    setPlayerData(newValue)         // 第二次连接（嵌套）
}

// ✅ 新方案：直接操作
modifyPlayerData() {
    connection.use { conn ->
        // SELECT（第一次连接）
        val current = conn.prepareStatement("SELECT...")

        // 直接 UPDATE/INSERT（复用连接）
        if (exists) {
            conn.prepareStatement("UPDATE...")  // 同一连接
        } else {
            conn.prepareStatement("INSERT...")  // 同一连接
        }
    }
}
```

---

## 🎯 兼容性说明

- ✅ 本版本完全兼容旧版本配置
- ✅ 异步改进不影响现有菜单的正常运行
- ✅ 整数显示优化自动应用，无需修改配置
- ✅ Slider 验证自动生效，不合规配置会自动修正
- ✅ 建议更新后测试包含数据库操作的菜单
- ✅ 建议：Events.Open 中使用 wait 动作时，确保菜单依赖于这些数据

---

## 📝 已知问题

- 暂无

---

## 🚀 下一步计划

- 继续优化性能
- 扩展条件判断方法
- 优化监听器功能
- 添加更多实用功能

---

**感谢您使用 KaMenu！如有问题或建议，欢迎反馈。**

---

---

# KaMenu v1.2.6 Update Notes

## 📋 Version Information
|- **Version**: 1.2.6
|- **Release Date**: March 30, 2026

---

## 🐛 Bug Fixes

### 1. Fixed Server Timeout Caused by Main Thread Blocking

**Issue Description:**
When executing `data:` and `gdata:` actions, database operations were executed synchronously on the main thread, causing server main thread blocking and triggering 15-second timeout warnings. Particularly, after setting data in Events.Open, menus might render before data setup completes, resulting in incorrect `{data:key}` variable display.

**Impact Scope:**
- `data:` actions (player data operations)
- `gdata:` actions (global data operations)
- `take`/`add` sub-operations (data increment/decrement)
- `delete` sub-operations (data deletion)
- Database operations in Events.Open/Close

**Root Cause Analysis:**
1. `data:` and `gdata:` actions internally call database modification methods (`modifyPlayerData`/`modifyGlobalData`), which execute SELECT → calculate value → UPDATE/INSERT flow
2. Original `set-data` and `set-gdata` only perform single database write, not blocking
3. All database operations were executed synchronously on main thread, causing main thread blocking while waiting for database connection pool
4. After async execution, menu rendering might complete before data write finishes

**Fix Implementation:**

Implemented intelligent async/sync execution strategy that automatically selects optimal execution mode based on context:

**1. Database Operation Asynchronization**
- All database operations (`set-data`, `set-gdata`, `data:`, `gdata:`) changed to async execution
- Use `Bukkit.getScheduler().runTaskAsynchronously()` to execute in separate thread
- Avoid blocking main thread

**2. Smart Execution Mode**

| Execution Context | Mode | Data Ops | Other Ops | wait Action |
|-------------------|------|----------|-----------|-------------|
| Events.Open (no wait) | Semi-sync | Async | Sync | Sync |
| Events.Open (has wait) | Fully Async | Async | Async | Async |
| Events.Close (no wait) | Semi-sync | Async | Sync | Sync |
| Events.Close (has wait) | Fully Async | Async | Async | Async |
| Button Click | Fully Async | Async | Async | Async |

**3. wait Action Support Optimization**

When Events.Open contains `wait` actions:
- Entire opening process executes asynchronously
- wait delays also complete in async thread
- After all actions complete, switch back to main thread to render menu
- Ensure data is set before menu rendering

**Example Configuration:**
```yaml
Events:
  Open:
    - 'set-data: text Hello World'
    - 'wait: 1'
    - 'set-data: text2 Second text'
    - 'wait: 1'
    # Start rendering menu
Body:
  message:
    type: 'message'
    text: 'text value is {data:text}'
  message2:
    type: 'message'
    text: 'text2 value is {data:text2}'
```

**Execution Flow:**
```
Open event starts (async)
  ↓
set-data: text = "Hello World" (async)
  ↓
wait: 1 tick (async delay)
  ↓
set-data: text2 = "Second text" (async)
  ↓
wait: 1 tick (async delay)
  ↓
All actions complete → Switch to main thread
  ↓
Render menu, text and text2 are now correctly set
```

**4. Performance Improvements**

- **Avoid Blocking**: Database operations no longer block main thread, server performance improved
- **Connection Pool Optimization**: Avoid nested connection requests, reduce connection pool pressure
- **Smart Scheduling**: Select optimal execution strategy based on context

---

## 🔧 Optimizations

### 1. Integer Display Optimization

**Optimization Content:**
Optimize `data:` and `gdata:` action `add`/`take` operation results by removing `.0` suffix from integers.

**Before Optimization:**
```
Player coins: 100
Add: 50
Result: 150.0  ← Extra .0
```

**After Optimization:**
```
Player coins: 100
Add: 50
Result: 150  ← Clean integer
```

**Implementation Logic:**
```kotlin
val newValueDouble = currentNum + numDelta
val newValue = if (newValueDouble == newValueDouble.toLong().toDouble()) {
    newValueDouble.toLong().toString()  // Remove .0
} else {
    newValueDouble.toString()  // Keep decimal
}
```

**Decimal Preserved Cases:**
- True decimals: `100.5` → `100.5`
- Float operation results: `10.0 + 0.1` → `10.1`

---

### 2. Slider Component Validation Enhancement

**Optimization Content:**
Added strict min/max value validation for slider components in Inputs.

**Validation Rules:**
- min must be **less than** max
- Use default values (min=0.0, max=10.0) on validation failure
- Log warning to inform user

**Error Example:**
```yaml
Inputs:
  test_slider:
    type: 'slider'
    min: 10.0    # ❌ Error: min must be less than max
    max: 5.0
```

**Auto Correction:**
- Use default values: min=0.0, max=10.0
- Log warning:
  ```
  [KaMenu] Slider configuration error: Menu 'test_menu' input component 'test_slider' min (10.0) must be less than max (5.0), using default values min=0.0, max=10.0
  ```

**Correct Example:**
```yaml
Inputs:
  test_slider:
    type: 'slider'
    min: 0.0      # ✅ Correct: min < max
    max: 10.0
```

---

### 3. Internationalization Improvement

**Optimization Content:**
Changed Slider configuration error message to internationalized format, supporting multiple languages.

**Chinese:**
```
[KaMenu] Slider 配置错误: 菜单 '{0}' 的输入组件 '{1}' 的 min ({2}) 必须小于 max ({3})，已使用默认值 min=0.0, max=10.0
```

**English:**
```
[KaMenu] Slider configuration error: Menu '{0}' input component '{1}' min ({2}) must be less than max ({3}), using default values min=0.0, max=10.0
```

---

## 📚 Technical Details

### 1. CompletableFuture Async Mechanism

Use Java's `CompletableFuture` for async operation management and result waiting:

```kotlin
fun executeActionList(...): CompletableFuture<Boolean> {
    // Execute action list
    val actionFutures = actions.map { deferred ->
        executeDeferredAction(...)  // Returns CompletableFuture
    }

    // Wait for all actions to complete
    return CompletableFuture.allOf(*actionFutures.toTypedArray())
        .thenApply { shouldReturn }
}
```

### 2. Smart Mode Detection

Recursively check action list to determine if it contains `wait` actions:

```kotlin
private fun hasWaitAction(actionList: List<*>): Boolean {
    for (action in actionList) {
        when (action) {
            is Map<*, *> -> {
                // Recursively check actions in conditions
                if (hasWaitAction(successActions) || hasWaitAction(denyActions)) {
                    return true
                }
            }
            is String -> {
                if (action.trim().lowercase().startsWith("wait:")) {
                    return true
                }
            }
        }
    }
    return false
}
```

### 3. Database Operation Optimization

Avoid nested database calls, use direct UPDATE/INSERT:

```kotlin
// ❌ Old approach: Nested calls
modifyPlayerData() {
    val current = getPlayerData()  // First connection
    val newValue = current + delta
    setPlayerData(newValue)         // Second connection (nested)
}

// ✅ New approach: Direct operation
modifyPlayerData() {
    connection.use { conn ->
        // SELECT (First connection)
        val current = conn.prepareStatement("SELECT...")

        // Direct UPDATE/INSERT (Reuse connection)
        if (exists) {
            conn.prepareStatement("UPDATE...")  // Same connection
        } else {
            conn.prepareStatement("INSERT...")  // Same connection
        }
    }
}
```

---

## 🎯 Compatibility Notes

- ✅ Fully backward compatible with older configurations
- ✅ Async improvements don't affect existing menu functionality
- ✅ Integer display optimization applies automatically, no config changes needed
- ✅ Slider validation takes effect automatically, invalid configs auto-corrected
- ✅ Recommended to test menus with database operations after update
- ✅ Suggestion: When using wait actions in Events.Open, ensure menu depends on that data

---

## 📝 Known Issues

- None

---

## 🚀 Next Steps

- Continue performance optimization
- Expand condition checking methods
- Optimize listener functionality
- Add more utility features

---

**Thank you for using KaMenu! If you have any questions or suggestions, please feel free to provide feedback.**

---
