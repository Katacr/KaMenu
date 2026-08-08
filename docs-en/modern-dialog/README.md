# Modern Dialog Menus

KaMenu's modern Dialog menus use Minecraft's native Dialog API, support multi-level folder structures, and allow rich component combinations. Each `.yml` file is an independent Dialog menu.

## Feature Navigation

- [Menu File Structure](structure.md): Learn menu IDs, folder layout, top-level nodes, and the basic YAML structure.
- [Container Menus](../container/README.md): Configure chest-like interfaces, button state variants, priorities, and refresh behaviour.
- [Global Settings](setting.md): Configure ESC closing, client behaviour after button actions, and required PlaceholderAPI expansions.
- [JavaScript Features](javascript.md): Define reusable scripts and use `{js:...}` to output dynamic values in text, conditions, and actions.
- [actions Folder](../config/actions-packages.md), [js Folder](../config/javascript-packages.md): Manage reusable global actions packages and JavaScript packages.
- [Events](events.md): Configure `Open`, `Close`, `Click`, and `Tasks`, including pre-open checks, reusable action groups, and periodic tasks.
- [Body Components](body.md), [Inputs](inputs.md), [Bottom Buttons](bottom.md): Combine Dialog body content, input controls, and button layouts.
- [Actions](actions.md): Use messages, commands, menu navigation, data writes, `wait`, `return`, nested `actions`, and more.
- [Conditions](conditions.md): Use multi-layer conditions in text and actions with PlaceholderAPI, data variables, and JavaScript expressions.
