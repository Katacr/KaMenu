# Container Menus

Container menus use Bukkit virtual inventories to render chests, hoppers, dispensers, droppers, furnaces, blast furnaces, smokers, and anvils. They are the main menu path for Minecraft 1.16.5 and cores without a usable native Dialog API.

## Documentation

- [Container Structure](structure.md): top-level keys and the complete skeleton.
- [Type](type.md): container types, sizes, and version boundaries.
- [Title](title.md): container titles and dynamic resolution.
- [Settings](settings.md): dependency checks, arguments, and click throttling.
- [Layout](layout.md): inventory rows, slots, empty spaces, and multi-character IDs.
- [Buttons](buttons.md): item display, visibility, click actions, and `variants`.
- [Properties](properties.md): furnace progress, anvil input, and type-specific fields.
- [Refresh](refresh.md): full, title, button, and progress refreshes.
- [Events](events.md): Container-specific lifecycle differences.

## Dialog Boundary

Container files use `Type`, `Layout`, `Buttons`, and optional `Properties`. They must not define Dialog-only `Body`, `Inputs`, or `Bottom` sections. `Events`, actions, conditions, variables, PlaceholderAPI, and JavaScript use KaMenu's shared runtime.

Paper/Folia and supported Spigot adapters can use Dialogs. Older cores disable Dialogs while retaining Containers. `toast` depends on Paper/Folia; cross-platform menus should use `actionbar` or `title` feedback.

## Minimal Example

```yaml
Type: CHEST
Title: '&8Shop'

Layout:
  - '#########'
  - '####`shop`####'
  - '#########'

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
