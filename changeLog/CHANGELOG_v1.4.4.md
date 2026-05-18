# KaMenu v1.4.4 更新报告

## 📋 版本信息
- **版本号**: 1.4.4

---

## ✨ 新功能

### 1. 新增 `skull_owner` 属性 — 显示玩家头颅

**功能说明:**
`Body.item` 组件新增 `skull_owner` 属性，可通过玩家名称直接显示对应玩家的头颅皮肤。支持 PlaceholderAPI 变量解析。

**新增字段:**
- `skull_owner` — 玩家名称（支持变量）

**适用组件:**
- `Body` → `type: 'item'`（material 需为 `PLAYER_HEAD`）

**示例:**
```yaml
Body:
  player_avatar:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_owner: '%player_name%'
    name: '&a我的头像'
    width: 32
    height: 32
```

**效果说明:**
- 自动获取指定玩家的皮肤并渲染为头颅物品
- 支持 PlaceholderAPI 变量，可动态显示不同玩家的头颅

---

### 2. 新增 `skull_texture` 属性 — 自定义纹理头颅

**功能说明:**
`Body.item` 组件新增 `skull_texture` 属性，可通过 Base64 编码的纹理值显示自定义头颅外观。适用于装饰性图标、自定义 UI 元素等场景。

**新增字段:**
- `skull_texture` — Base64 纹理值

**适用组件:**
- `Body` → `type: 'item'`（material 需为 `PLAYER_HEAD`）

**示例:**
```yaml
Body:
  custom_icon:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_texture: 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODc5ZTU0Y2JlODc4NjdkMTRiMmZiZGYzZjE4NzA4OTQzNTIwNDhkZmVjZDk2Mjg0NmRlYTg5M2IyMTU0Yzg1In19fQ=='
    name: '&6自定义图标'
    width: 32
    height: 32
```

**纹理值获取方式:**
- 可从 [minecraft-heads.com](https://minecraft-heads.com/) 获取 Base64 纹理值
- 复制头颅的 `Value` 字段即可直接使用

**性能优化:**
- 采用基于纹理值的固定 UUID 生成策略，客户端首次加载纹理后会自动缓存
- 后续再次打开相同菜单时无需重新下载纹理，实现即时显示

---

## 📝 配置示例

### 玩家信息面板

```yaml
Body:
  avatar:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_owner: '%player_name%'
    name: '&b%player_name%'
    description: '&7等级: %player_level%'
    width: 48
    height: 48

  coin_icon:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_texture: 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA...'
    name: '&6金币'
    width: 16
    height: 16
```

### 结合条件判断

```yaml
Body:
  vip_icon:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_texture:
      - '[permission=vip.gold] eyJ0ZXh0dXJlcyI6...gold...'
      - '[else] eyJ0ZXh0dXJlcyI6...default...'
    name: '&eVIP 状态'
    width: 24
    height: 24
```

---

## ⚠️ 注意事项

- `skull_texture` 优先级高于 `skull_owner`，两者同时配置时仅 `skull_texture` 生效
- `material` 必须设置为 `PLAYER_HEAD`，否则这两个属性不会生效
- `skull_texture` 首次加载时客户端需要从 Mojang 服务器下载纹理贴图，可能需要短暂等待；后续访问会使用客户端缓存
- `skull_owner` 支持离线玩家名称，但需要服务器能够解析该玩家的 UUID

---

## 🚀 升级指南

### 从 1.4.3 升级到 1.4.4

**升级步骤:**

1. **更新插件**
   - 下载最新的 `KaMenu-1.4.4.jar`
   - 替换旧版本 jar 文件

2. **重启服务器**
   - 完全停止服务器
   - 重新启动服务器

3. **（可选）使用新功能**
   - 在 `Body.item` 中添加 `skull_owner` 或 `skull_texture` 属性

**兼容性:**
- ✅ 旧版菜单配置完全兼容
- ✅ 不使用新属性时无需改动任何配置
- ✅ 原有的槽位引用 `[HEAD]` / `[HEAD:Player]` 功能不受影响

---

## 📈 版本对比

| 功能 | 1.4.3 | 1.4.4 |
|------|-------|-------|
| `Body.item` 的 `skull_owner` 支持 | ❌ | ✅ |
| `Body.item` 的 `skull_texture` 支持 | ❌ | ✅ |
| 自定义纹理客户端缓存优化 | — | ✅ |

---

## 📚 相关文档

- **内容组件文档**: `docs/menu/body.md`
- **示例菜单**: `menus/example/` - 中文示例
- **示例菜单**: `menus/exampleEN/` - 英文示例

---

# KaMenu v1.4.4 Update Report

## 📋 Version Info
- **Version**: 1.4.4

---

## ✨ New Features

### 1. Added `skull_owner` property — Display player heads

**Description:**
`Body.item` now supports the `skull_owner` property, which renders the specified player's skin as a head item. Supports PlaceholderAPI variables.

**New Field:**
- `skull_owner` — Player name (supports variables)

**Supported Component:**
- `Body` → `type: 'item'` (material must be `PLAYER_HEAD`)

**Example:**
```yaml
Body:
  player_avatar:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_owner: '%player_name%'
    name: '&aMy Avatar'
    width: 32
    height: 32
```

---

### 2. Added `skull_texture` property — Custom texture heads

**Description:**
`Body.item` now supports the `skull_texture` property, which renders a custom head appearance using a Base64-encoded texture value. Ideal for decorative icons and custom UI elements.

**New Field:**
- `skull_texture` — Base64 texture value

**Supported Component:**
- `Body` → `type: 'item'` (material must be `PLAYER_HEAD`)

**Example:**
```yaml
Body:
  custom_icon:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_texture: 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODc5ZTU0Y2JlODc4NjdkMTRiMmZiZGYzZjE4NzA4OTQzNTIwNDhkZmVjZDk2Mjg0NmRlYTg5M2IyMTU0Yzg1In19fQ=='
    name: '&6Custom Icon'
    width: 32
    height: 32
```

**Where to get texture values:**
- Visit [minecraft-heads.com](https://minecraft-heads.com/) to find Base64 texture values
- Copy the `Value` field from any head entry

**Performance:**
- Uses a deterministic UUID derived from the texture value, enabling client-side caching
- After the first load, subsequent menu opens display the texture instantly without re-downloading

---

## 📝 Configuration Examples

### Player Info Panel

```yaml
Body:
  avatar:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_owner: '%player_name%'
    name: '&b%player_name%'
    description: '&7Level: %player_level%'
    width: 48
    height: 48

  coin_icon:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_texture: 'eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA...'
    name: '&6Coins'
    width: 16
    height: 16
```

### With conditions

```yaml
Body:
  vip_icon:
    type: 'item'
    material: 'PLAYER_HEAD'
    skull_texture:
      - '[permission=vip.gold] eyJ0ZXh0dXJlcyI6...gold...'
      - '[else] eyJ0ZXh0dXJlcyI6...default...'
    name: '&eVIP Status'
    width: 24
    height: 24
```

---

## ⚠️ Notes

- `skull_texture` takes priority over `skull_owner`; if both are set, only `skull_texture` is applied
- `material` must be set to `PLAYER_HEAD` for these properties to take effect
- On first load, the client needs to download the texture from Mojang servers (may take a moment); subsequent loads use the client cache
- `skull_owner` supports offline player names, but the server must be able to resolve the player's UUID

---

## 🚀 Upgrade Guide

### Upgrading from 1.4.3 to 1.4.4

**Steps:**

1. **Update the plugin**
   - Download the latest `KaMenu-1.4.4.jar`
   - Replace the old jar file

2. **Restart the server**
   - Fully stop the server
   - Start it again

3. **(Optional) Use new features**
   - Add `skull_owner` or `skull_texture` to your `Body.item` components

**Compatibility:**
- ✅ Existing menu configs remain fully compatible
- ✅ No config changes required if you don't use the new properties
- ✅ Existing slot reference `[HEAD]` / `[HEAD:Player]` functionality is unaffected

---

## 📈 Version Comparison

| Feature | 1.4.3 | 1.4.4 |
|---------|-------|-------|
| `Body.item` `skull_owner` support | ❌ | ✅ |
| `Body.item` `skull_texture` support | ❌ | ✅ |
| Custom texture client-side caching | — | ✅ |

---

## 📚 Related Docs

- **Body component documentation**: `docs/menu/body.md`
- **Example menus**: `menus/example/` - Chinese examples
- **Example menus**: `menus/exampleEN/` - English examples
