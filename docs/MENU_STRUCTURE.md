# KaMenu 菜单结构说明

## 📁 文件夹结构

插件支持多层级的菜单文件夹结构，便于管理和组织大量菜单。

### 默认结构示例

```
plugins/KaMenu/menus/
├── demo.yml              # 演示菜单（根目录）
├── actions_demo.yml       # 功能演示菜单（根目录）
├── examples/             # 示例文件夹
│   └── tutorial.yml      # 教程菜单
└── admin/                # 管理员文件夹
    └── tools.yml          # 管理工具菜单
```

## 🎯 菜单 ID 规则

- **根目录菜单**: 直接使用文件名
  - `/km open demo`
  - `/km open actions_demo`

- **子文件夹菜单**: 使用路径（用 `/` 分隔）
  - `/km open examples/tutorial`
  - `/km open admin/tools`

## 🚀 自动释放

首次加载插件时，会自动从 jar 包内释放 `menus/` 目录下的所有文件和文件夹到服务器。

### 支持的释放内容

- ✅ 所有 `.yml` 菜单文件
- ✅ 子文件夹结构
- ✅ 嵌套文件夹（多层级）


## 📝 Tab 补全

输入命令时按 `Tab` 键可以补全所有已加载的菜单 ID：

```
/km open <Tab>
```

会显示：
```
demo
actions_demo
examples/tutorial
admin/tools
```

## 🔧 菜单内引用其他菜单

在菜单的 action 中引用其他菜单时，同样使用完整路径：

```yaml
action:
  - 'open: examples/tutorial'    # 打开子文件夹菜单
  - 'open: demo'               # 打开根目录菜单
```

### 添加自定义菜单

#### 在服务器创建

在 `plugins/KaMenu/menus/` 下创建或修改文件，然后使用：

```
/km reload
```

#### 文件和命名说明

菜单文件和文件夹可以使用任何名称，包括中文：

- ✅ 支持中文文件名和文件夹名
- ✅ 支持任意字符
- ✅ 唯一要求：文件扩展名必须是 `.yml`

#### 示例

```
plugins/KaMenu/menus/
├── 演示菜单.yml
├── shop/
│   ├── 商店主菜单.yml
│   └── 武器店.yml
└── admin/
    └── 管理工具.yml
```

对应的打开命令：
```
/km open 演示菜单
/km open shop/商店主菜单
/km open shop/武器店
/km open admin/管理工具
```

## ⚠️ 注意事项

1. **文件扩展名**: 必须是 `.yml`（不是 `.yaml`）
2. **路径分隔符**: 使用 `/` 而非 `\`
3. **ID 唯一性**: 同一路径下不能有重复的菜单 ID
4. **热重载**: 修改文件后记得执行 `/km reload`

## 🔍 调试

### 查看已加载的菜单

加载时控制台会显示：

```
[KaMenu] 已加载菜单: demo
[KaMenu] 已加载菜单: actions_demo
[KaMenu] 已加载菜单: examples/tutorial
[KaMenu] 已加载菜单: admin/tools
```

### 使用 Tab 补全查看所有菜单

```
/km open <Tab>
```

### 打开不存在的菜单

如果尝试打开不存在的菜单，会收到错误提示：

```
/km open 不存在的菜单名
```

提示信息：
```
§c[KaMenu] 菜单 '不存在的菜单名' 不存在！
```
