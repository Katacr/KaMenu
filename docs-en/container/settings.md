# Settings

Container menus use selected shared `Settings` entries plus inventory-specific click throttling. Dialog-only `can_escape`, `after_action`, and `lifetime` do not apply to Containers; use actions and `Events.Close` for closing and transitions.

## need_placeholder

Checks PlaceholderAPI and the listed expansions before the menu opens. A failed check blocks rendering so incomplete displays or conditions are not used.

```yaml
Settings:
  need_placeholder:
    - player
    - vault
```

List expansion identifiers for variables such as `%player_name%` and `%vault_eco_balance%`. The setting does not scan placeholders automatically. See [PlaceholderAPI prerequisites](../modern-dialog/setting.md#need_placeholder).

## min_click_delay

```yaml
Settings:
  min_click_delay: 200
```

The value is an absolute interval in milliseconds, tracked per player and active Container session:

- `0` or omitted disables the limit.
- Only valid action-bearing button clicks are limited.
- Empty slots, hidden buttons, and actionless buttons do not consume the cooldown.
- Reopening, `reset`, or switching menus starts a new session interval.

`150` to `300` milliseconds is a practical range for shops, rewards, economy, and points operations. See [Global Settings](../modern-dialog/setting.md#min_click_delay) for shared click-delay details.

## pass_arguments

Container and Dialog menus share argument passing:

```yaml
Settings:
  pass_arguments:
    enable: true
    default: ['default', '%player_name%', '{meta:source}']
    must: 2
```

`default` fills missing positions by index and supports PAPI and KaMenu variables. `must` is the minimum argument count required after defaults are applied; the menu does not open when the count is still insufficient. The target menu resolves arguments when an opening action runs. See [Actions](../modern-dialog/actions.md) and [Events](events.md) for lifecycle behaviour.
