# ⌨️ 指令列表

KaMenu 提供了简洁的指令体系，主指令为 `/km`。

---

## 主指令

```
/km <子指令> [参数]
```

**权限要求：** `kamenu.use`（基础使用）

---

## 子指令详情

### /km open

打开一个指定的菜单。

**格式：** `/km open <菜单ID>`

**权限：** `kamenu.use`

**Tab 补全：** 输入 `/km open ` 后按 Tab 键，会自动补全所有已加载的菜单 ID（包括子文件夹路径）

**示例：**

```bash
# 打开根目录的菜单
/km open main_menu

# 打开子文件夹中的菜单
/km open shop/weapons

# 打开示例演示菜单
/km open example/actions_demo
```

**错误提示：**
- 若菜单 ID 不存在，玩家将收到错误消息
- 该指令只能由玩家执行，控制台无法使用

---

### /km reload

重新加载插件的所有配置文件和菜单文件，无需重启服务器。

**格式：** `/km reload`

**权限：** `kamenu.admin`

**重载内容：**
1. `config.yml` 全局配置
2. 语言文件（`lang/` 目录）
3. `menus/` 目录下的所有菜单文件（包括子文件夹）

**示例：**

```bash
/km reload
# 输出: [KaMenu] 重载完成，已加载 12 个菜单。
```

{% hint style="info" %}
修改菜单文件后，只需执行 `/km reload` 即可立即生效，无需重启服务器。
{% endhint %}

---

## 自定义快捷指令

除了 `/km open` 之外，你还可以在 `config.yml` 中注册自定义快捷指令，直接将一个简短的指令映射到打开某个菜单：

```yaml
custom-commands:
  shop: 'server_shop'   # 玩家执行 /shop 即打开 server_shop 菜单
  menu: 'main_menu'     # 玩家执行 /menu 即打开 main_menu 菜单
```

详细配置请参阅 [配置文件: config.yml](../config/config.md)。
