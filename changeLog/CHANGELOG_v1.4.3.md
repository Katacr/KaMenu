 # KaMenu v1.4.3 更新报告

## 📋 版本信息
- **版本号**: 1.4.3

---

## 🔧 修复与优化

### 1. 修复对话框物品 `item_model` 配置错误

**问题说明:**
此前 `Body.item` 组件中的 `item_model` 配置在部分场景下存在错误，可能导致：
- 配置了 `item_model` 但未正确生效
- 配置值非法时缺乏更明确的行为预期
- 与资源包物品模型相关的配置体验不够稳定

**本次修复:**
- 修正了对话框物品 `item_model` 的处理逻辑
- 提升了非法配置值时的容错与日志提示表现
- 使 `item_model` 在菜单物品展示中的行为更加稳定、可预期

**适用场景:**
- 使用命名空间模型标识的 Paper 物品展示
- 依赖资源包自定义物品模型的 Dialog 菜单

**示例:**
```yaml
Body:
  weapon:
    type: 'item'
    material: 'DIAMOND_SWORD'
    item_model: 'minecraft:diamond_sword'
    name: '&b展示武器'
```

---

### 2. 新增对话框物品 `custom_model_data` 属性支持

**功能说明:**
现在 `Body.item` 组件新增支持 `custom_model_data` 属性，可用于兼容基于 `CustomModelData` 的资源包物品展示方案（例如部分自定义材质、资源包系统、ItemsAdder 类似用法）。

**新增字段:**
- `custom_model_data`

**适用组件:**
- `Body` → `type: 'item'`

**示例:**
```yaml
Body:
  coin:
    type: 'item'
    material: 'PAPER'
    custom_model_data: 10001
    name: '&6金币图标'
    description: '&7这是一个使用 CustomModelData 的展示物品'
```

**效果说明:**
- 若资源包中为该材质配置了对应的 `CustomModelData` 模型，菜单中的展示物品会渲染为对应外观
- 若填写的值不是合法整数，插件会输出警告日志并忽略该值

---

## 📝 配置示例

### 同时使用 `custom_model_data` 与 `item_model`

```yaml
Body:
  icon1:
    type: 'item'
    material: 'PAPER'
    custom_model_data: 20001
    name: '&a自定义图标A'
    description: '&7使用 CustomModelData 渲染'

  icon2:
    type: 'item'
    material: 'STICK'
    item_model: 'minecraft:stick'
    name: '&b自定义图标B'
    description: '&7使用 item_model 渲染'
```

### 结合变量使用

```yaml
Body:
  dynamic_item:
    type: 'item'
    material: 'PAPER'
    custom_model_data: '{data:item_model_id}'
    name: '&e动态展示物品'
```

---

## ⚠️ 注意事项

- `custom_model_data` 仅适用于 `Body.item` 组件
- `custom_model_data` 必须为合法整数，否则会被忽略并输出警告日志
- `item_model` 与 `custom_model_data` 分别适配不同的资源包模型方案，具体最终显示效果取决于客户端资源包配置
- 如果资源包未提供对应模型，物品会回退为原始材质显示

---

## 🚀 升级指南

### 从 1.4.2 升级到 1.4.3

**升级步骤:**

1. **更新插件**
   - 下载最新的 `KaMenu-1.4.3.jar`
   - 替换旧版本 jar 文件

2. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器

3. **（可选）更新配置**
   - 若你使用资源包自定义物品展示，可为 `Body.item` 添加 `custom_model_data`
   - 若你此前已配置 `item_model`，本次更新后其行为会更稳定

**兼容性:**
- ✅ 旧版菜单配置保持兼容
- ✅ 不使用 `custom_model_data` 时无需改动配置
- ✅ 本次更新以修复与增强为主，不影响现有常规菜单功能

---

## 📈 版本对比

| 功能 | 1.4.2 | 1.4.3 |
|------|-------|-------|
| `Body.item` 的 `item_model` 稳定性 | ⚠️ 存在问题 | ✅ 已修复 |
| `Body.item` 的 `custom_model_data` 支持 | ❌ | ✅ |

---

## 📚 相关文档

- **内容组件文档**: `docs/menu/body.md`
- **示例菜单**: `menus/example/` - 中文示例
- **示例菜单**: `menus/exampleEN/` - 英文示例

---

# KaMenu v1.4.3 Update Report

## 📋 Version Info
- **Version**: 1.4.3

---

## 🔧 Fixes and Improvements

### 1. Fixed dialog item `item_model` handling

**Issue:**
Previously, the `item_model` field in `Body.item` could behave incorrectly in some cases, which might cause:
- `item_model` being configured but not applied correctly
- unclear behavior when invalid values were provided
- unstable resource-pack-based item model display behavior

**This update fixes:**
- corrected the handling logic of dialog item `item_model`
- improved fault tolerance and warning logging for invalid values
- made menu item model rendering more stable and predictable

**Use Cases:**
- displaying Paper items using namespaced model identifiers
- dialog menus that rely on resource-pack-based custom item rendering

**Example:**
```yaml
Body:
  weapon:
    type: 'item'
    material: 'DIAMOND_SWORD'
    item_model: 'minecraft:diamond_sword'
    name: '&bDisplay Weapon'
```

---

### 2. Added `custom_model_data` support for dialog items

**Description:**
`Body.item` now supports the `custom_model_data` field, making it easier to work with resource-pack-based item rendering systems that rely on `CustomModelData` (such as many custom item pipelines and systems similar to ItemsAdder usage patterns).

**New Field:**
- `custom_model_data`

**Supported Component:**
- `Body` → `type: 'item'`

**Example:**
```yaml
Body:
  coin:
    type: 'item'
    material: 'PAPER'
    custom_model_data: 10001
    name: '&6Coin Icon'
    description: '&7A display item rendered using CustomModelData'
```

**Behavior:**
- if the resource pack provides a model for the specified `CustomModelData`, the dialog item will render with that appearance
- if the value is not a valid integer, KaMenu will log a warning and ignore it

---

## 📝 Configuration Examples

### Using both `custom_model_data` and `item_model`

```yaml
Body:
  icon1:
    type: 'item'
    material: 'PAPER'
    custom_model_data: 20001
    name: '&aCustom Icon A'
    description: '&7Rendered using CustomModelData'

  icon2:
    type: 'item'
    material: 'STICK'
    item_model: 'minecraft:stick'
    name: '&bCustom Icon B'
    description: '&7Rendered using item_model'
```

### Using variables

```yaml
Body:
  dynamic_item:
    type: 'item'
    material: 'PAPER'
    custom_model_data: '{data:item_model_id}'
    name: '&eDynamic Display Item'
```

---

## ⚠️ Notes

- `custom_model_data` only applies to the `Body.item` component
- `custom_model_data` must be a valid integer, otherwise it will be ignored and a warning will be logged
- `item_model` and `custom_model_data` target different resource-pack rendering approaches; the final visual result depends on the client resource pack
- if no matching model exists in the resource pack, the item will fall back to its base material appearance

---

## 🚀 Upgrade Guide

### Upgrading from 1.4.2 to 1.4.3

**Steps:**

1. **Update the plugin**
   - Download the latest `KaMenu-1.4.3.jar`
   - Replace the old jar file

2. **Restart the server**
   - Fully stop the server
   - Start it again

3. **(Optional) Update configs**
   - If you use resource-pack-based custom item display, you can now add `custom_model_data` to `Body.item`
   - If you already use `item_model`, its behavior should now be more stable

**Compatibility:**
- ✅ Existing menu configs remain compatible
- ✅ No config change is required if you do not use `custom_model_data`
- ✅ This release focuses on fixes and enhancements, without affecting normal existing menus

---

## 📈 Version Comparison

| Feature | 1.4.2 | 1.4.3 |
|---------|-------|-------|
| `Body.item` `item_model` stability | ⚠️ Problematic in some cases | ✅ Fixed |
| `Body.item` `custom_model_data` support | ❌ | ✅ |

---

## 📚 Related Docs

- **Body component documentation**: `docs/menu/body.md`
- **Example menus**: `menus/example/` - Chinese examples
- **Example menus**: `menus/exampleEN/` - English examples
