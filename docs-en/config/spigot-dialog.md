# 🧩 Spigot Dialog Compatibility

KaMenu v1.7.0 supports Paper, Folia, and Spigot from the same plugin JAR. It detects the current server at startup and loads only the matching Dialog and scheduler adapters. No separate compatibility plugin is required.

## Requirements

| Server | Minimum | Scope |
|--------|---------|-------|
| Paper / Folia | 1.21.7+ | Complete feature set |
| Spigot | 1.21.6+ | Complete menu runtime except for the platform boundaries below |

The same `KaMenu-1.7.0.jar` has been verified on Paper 26.2 and Spigot 26.2 for startup, menu rendering, input callbacks, action execution, and close handling.

## Supported on Spigot

- Loaded menus, `/km open`, custom commands, and in-memory YAML APIs
- `Events.Open`, `Events.Close`, `Events.Click`, and `Events.Tasks`
- Full action queues, conditions, `wait`, `return`, menu navigation, and task controls
- JavaScript, global JS packages, global action packages, and PlaceholderAPI
- Common data, list, item, economy, action, and condition features
- External `KaMenuAPI` menus and action handlers
- All Inputs with values submitted to actions
- `notice`, `confirmation`, `multi`, and generated `repeat` buttons
- Public `Body.item` visual properties, including names, lore, enchantments, damage, models, glint, tooltip settings, leather colors, and player heads
- Server callbacks and action arguments for body `<text ...;actions=...>` segments
- `Settings.can_escape`, `pause`, `after_action`, and `lifetime`
- Common actions such as `tell`, `actionbar`, `title`, `sound`, `command`, `console`, `open`, and `close`

Spigot buttons and body `actions=` segments use one-shot server-side callbacks. The client may submit only fields declared by the current menu. KaMenu validates text length, slider ranges, option IDs, and checkbox values before executing the trusted action path stored on the server.

## Platform Boundaries

The following features require Paper APIs or have no equivalent public Spigot API:

- `toast:` does not create an Advancement toast on Spigot and sends a localized unsupported message instead.
- The ESC pause-screen entry, `/km pause`, and the `pause_menu.yml` datapack integration remain Paper/Folia-only.
- Clickable-text `hover_item` shares the same Bukkit public-property mapper as `Body.item` and supports hands, inventory slots, armor, saved items, vanilla materials, and external item sources.
- `Body.sprite` and `Body.player_head` still require Paper/Folia.
- A single `url:` or `copy:` action runs entirely on the client and does not notify the server. If it closes a Dialog through `after_action: CLOSE`, Tasks and the Close lifecycle are cleaned up by the `Settings.lifetime` fallback.

The native Dialog API has no general server event for every silent ESC close path. Use `can_escape: false` with explicit `close` buttons when immediate exit logic is required. `Settings.lifetime` always actively closes expired menus and clears their lifecycle state.

On Spigot, `Body.item` explicitly maps properties through Bukkit/Spigot public APIs without calling CraftBukkit or NMS. Supported properties include custom names, fixed item names, lore, regular/stored enchantments, damage, maximum damage, unbreakable, custom-model data, `item_model`, enchantment glint, rarity, tooltip style and hidden components, maximum stack size, leather color, and player-head profiles.

Slot references and external items retain the visual properties exposed through those public APIs. PDC, plugin-private components, and data components not exposed by Bukkit are not written to the Dialog. These values normally do not affect the icon, but specialized tooltip content may be absent. A mapping error falls back only that component to its basic material and amount and logs one localized warning.

## Actions and Inputs Example

```yaml
Inputs:
  target:
    type: input
    text: '&fPlayer name'
    max_length: 16

Bottom:
  type: multi
  columns: 3
  buttons:
    execute:
      text: '&aExecute'
      actions:
        - condition: 'isPass.$(target)'
          deny:
            - 'console: say $(target) submitted an input from a Spigot Dialog'
            - 'sound: entity.experience_orb.pickup;volume=1;pitch=1.2'
        - 'reset'
    website:
      text: '&bWebsite'
      actions:
        - 'url: https://example.com'
    copy:
      text: '&eCopy address'
      actions:
        - 'copy: play.example.com'
```

`command:` is a server-side KaMenu action with variables, conditions, and player context. `url:` and `copy:` use client-side static behavior only when each is the sole action on its button.
