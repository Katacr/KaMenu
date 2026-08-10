# Container Menus

Container menus use Bukkit virtual inventories to render chests, hoppers, dispensers, droppers, furnaces, blast furnaces, smokers, and anvils. They are the main menu path for Minecraft 1.16.5 and cores without a usable native Dialog API.

## Decide Whether To Use A Container

| Requirement | Recommended structure | Start here |
|---|---|---|
| Native dialog text, inputs, and buttons | Dialog | [Modern Dialog Menus](../modern-dialog/README.md) |
| Chest, hopper, dispenser, or dropper inventory UI | Container | This documentation group |
| Furnace flame/arrow or anvil rename UI | Container | [Properties](properties.md) |
| A server below the native Dialog versions | Container | This documentation group |

A typical Container workflow is: choose `Type`, configure `Title` and `Settings`, arrange slots with `Layout`, define each entry under `Buttons`, then add `Properties`, `Events`, and refresh intervals only when needed.

## Documentation

- [Creating A Container Menu](creating_menu.md): a complete beginner workflow from file creation to reload and open.
- [Container Structure](structure.md): top-level keys and the complete skeleton.
- [Type](type.md): container types, sizes, and version boundaries.
- [Title](title.md): container titles and dynamic resolution.
- [Settings](settings.md): dependency checks, arguments, and click throttling.
- [Layout](layout.md): inventory rows, slots, empty spaces, and multi-character IDs.
- [Free Slots](free-slots.md): real item input, conditions, previews, atomic consumption, and recovery.
- [Buttons](buttons.md): item display, visibility, click actions, and `variants`.
- [Properties](properties.md): furnace progress, anvil input, and type-specific fields.
- [Refresh](refresh.md): full, title, button, and progress refreshes.
- [Events](events.md): Container-specific lifecycle differences.
- [Menu Migration Overview](migration.md): migrate DeluxeMenus or TrMenu menus and interpret migration reports.

## Dialog Boundary

Container files use `Type`, `Layout`, `Buttons`, and optional `Properties`. They must not define Dialog-only `Body`, `Inputs`, or `Bottom` sections. `Events`, actions, conditions, variables, PlaceholderAPI, and JavaScript use KaMenu's shared runtime.

Paper/Folia and supported Spigot adapters can use Dialogs. Older cores disable Dialogs while retaining Containers. `toast` depends on Paper/Folia; cross-platform menus should use `actionbar` or `title` feedback. Use `Free-Slots` for real player-owned item input; rendered buttons remain server-controlled displays.

## Minimal Example

```yaml
Type: CHEST
Title: '&8Shop'

Layout:
  - '         '
  - '    `shop`    '
  - '         '

Buttons:
  shop:
    display:
      material: DIAMOND
      name: '&bDiamond'
    actions:
      left:
        - 'tell: &aDiamond clicked'
        - 'close'
```

See [Modern Dialog Menu File Structure](../modern-dialog/structure.md) for menu IDs, reload behaviour, and common YAML rules.

## Where To Start

1. Read [Container Structure](structure.md) to understand top-level keys and indentation.
2. For a normal chest menu, continue with [Type](type.md), [Layout](layout.md), and [Buttons](buttons.md).
3. Read [Settings](settings.md) when you need click throttling or menu arguments.
4. Read [Title](title.md), [Refresh](refresh.md), and [Properties](properties.md) for dynamic titles, periodic updates, furnace progress, or anvil input.
5. Add opening, closing, and reusable action flows with [Events](events.md).
