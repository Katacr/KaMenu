# Menu Migration Overview

KaMenu can convert DeluxeMenus and TrMenu inventory menus into standard KaMenu V2 Container menus. A migrator only reads YAML; it does not load the source plugin or execute source commands, JavaScript, or click actions.

## Supported Sources

| Source | Command | Default source | Default output |
|---|---|---|---|
| DeluxeMenus | `/km migrate dm` | `plugins/DeluxeMenus/gui_menus` | `plugins/KaMenu/menus/dm_migrated` |
| TrMenu stable-v3 | `/km migrate trmenu` | `plugins/TrMenu/menus` | `plugins/KaMenu/menus/trmenu_migrated` |

Both commands use the same complete form:

```text
/km migrate <dm|trmenu> [source-file-or-directory] [output-directory] [overwrite]
```

- The source may be one YAML file or a directory.
- The output must remain under `plugins/KaMenu/menus`.
- Existing targets and conflicting configuration are preserved unless `overwrite` is explicitly supplied.
- Menus and related entry configuration are reloaded after migration.
- Chat only shows summaries for migration counts, entry merges, runtime reloads, and the report path.
- Per-file results, all warnings/errors, and configuration conflicts are written to a separate `.log` file under `plugins/KaMenu/logs/migration/` for every run.
- Successful generation does not guarantee identical behavior. Review every `WARNING`.

## DeluxeMenus Migration Tutorial

### 1. Migrate The Default Directory

Place or keep the DM menus under:

```text
plugins/DeluxeMenus/gui_menus/
```

Run:

```text
/km migrate dm
```

The source directory structure is preserved under:

```text
plugins/KaMenu/menus/dm_migrated/
```

For example, `gui_menus/shop/main.yml` becomes menu ID `dm_migrated/shop/main`:

```text
/km open dm_migrated/shop/main
```

DM `open_command` entries are merged into `custom_commands.yml`. Existing commands with the same name are preserved and reported as conflicts by default.

### 2. Select A Source Or Replace Old Output

```text
/km migrate dm /path/to/DeluxeMenus/gui_menus
/km migrate dm /path/to/DeluxeMenus/gui_menus dm_migrated
/km migrate dm /path/to/DeluxeMenus/gui_menus dm_migrated overwrite
```

Use `overwrite` only when the old menus and same-name custom commands should be replaced.

### 3. Migrated DM Content

- Chest menus from 9 to 54 slots, titles, `slot`, `slots`, and slot ranges.
- Material, name, lore, amount, enchantments, flags, model IDs, heads, and unbreakable state.
- Same-slot `priority` candidates converted into `Buttons.<id>.variants`.
- Permission, money, item, and basic string/numeric requirements.
- General, left, right, shift-left, shift-right, and middle click actions.
- Message, player/console command, menu open, server connect, close, refresh, sound, and Vault economy actions.
- `open_requirement`, `open_commands`, `open_command`, and item `update`.

## TrMenu Migration

Run the default migration with:

```text
/km migrate trmenu
```

`trm` is an alias of `trmenu`. Plain `Bindings.Commands` entries are merged into `custom_commands.yml`; safely mapped `Bindings.Items` entries are merged into `item_bindings.yml`.

The main supported subset includes single-page standard container layouts, regular and explicit-slot icons, nested icon states, common item fields, common Kether conditions/actions, lifecycle events, automatic tasks, cross-menu opens, and statically auditable return-value `Functions`.

Static `{node:path}`, `{nodes:path}`, and `{n:path}` expressions become KaMenu `{ref:trmenu.path}` references. Only source nodes that are actually used are copied under `References.trmenu`. Positional placeholders such as `{0}` and `{1}` inside a source node become `{refarg:0}` and `{refarg:1}`. An `@iconId@` token inside an icon is resolved to that source icon ID during migration:

```yaml
# TrMenu source
lore:
  - '{node:Icons.@iconId@.display.material}'

# KaMenu output
lore:
  - '{ref:trmenu.Icons.shop.display.material}'
```

A dynamic node path containing runtime values cannot be determined statically and is reported for manual conversion.

Select a directory or replace old output with:

```text
/km migrate trmenu /path/to/TrMenu/menus trmenu_migrated
/km migrate trmenu /path/to/TrMenu/menus trmenu_migrated overwrite
```

## Unsupported And Incompatible Content

### DeluxeMenus

| Content | Migration result |
|---|---|
| Unsupported requirement | Treated as false and reported; the original restriction is not bypassed |
| Unknown action | Skipped with a warning |
| Unsupported click type | No matching click action is generated; add it manually |
| Legacy material `data` | Ignored with a warning |
| HeadDatabase `hdb-*` | Replaced with `PAPER` and requires manual conversion |
| `[json]` | Reduced to plain `tell` text; review formatting |
| `[broadcastsound]` | Approximated as a sound played only to the current viewer |
| `[commandevent]` | Approximated as a normal player command |
| Item-name matching in `has item` | The name is ignored; only safely mapped material, amount, and lore remain |

### TrMenu

The following features reject the file, skip the affected branch, or produce a warning:

- `Render-Type: DIALOG` and multi-page `Layout`.
- Packet-based `PlayerInventory`, `Free-Slots`, and `Hide-Player-Inventory` features.
- Catchers, staged chat input, page actions, drag, and outside-window clicks.
- Arbitrary Kether flows, JEXL, NovaScript, inline TrMenu JavaScript, and private objects or methods outside fixed mappings.
- Repo/private item sources, NBT, and properties without an explicit Bukkit API mapping.
- Regex command bindings, client-localized `Lang`, and unsafe item-binding traits.

An unsupported condition is never removed while its protected action remains executable. If an open condition cannot be converted, the incomplete menu is prevented from opening.

## Reports And Diagnostic Codes

Every migration creates a file similar to:

```text
plugins/KaMenu/logs/migration/migration-20260810-153012-123-trmenu.log
```

The UTF-8 report contains the migration type, source, output directory, overwrite mode, per-file status, complete diagnostics, command conflicts, item-binding conflicts, and runtime reload results. Chat no longer prints each detail and only provides the report's absolute path. If the report cannot be written, KaMenu reports the failure in chat and falls back to the server console for the complete output.

### DeluxeMenus Reports

DM reports do not have stable codes. They contain a severity, source YAML path, and message:

```text
Migration warning [items.shop.click_commands]: Unsupported DeluxeMenus action [takeexp] was skipped.
```

- `WARNING`: the target may still be generated, but the field was skipped or approximated.
- `ERROR`: the target is not generated. Correct the source YAML at the bracketed path and migrate it again.

### TrMenu Reports

A TrMenu report uses this format:

```text
[WARNING/UNSUPPORTED/TRM_ACTION_UNSUPPORTED] Icons.shop.actions: Unknown action was skipped.
```

The four parts are:

1. `INFO`, `WARNING`, or `ERROR`; an `ERROR` prevents that file from being generated.
2. `EXACT`, `APPROXIMATE`, `UNSUPPORTED`, or `INVALID`; this describes compatibility with the source behavior.
3. A stable `TRM_*` diagnostic code.
4. The YAML path and message describing what must be reviewed.

Common codes:

| Code | Meaning | Resolution |
|---|---|---|
| `TRM_SOURCE_INVALID` / `TRM_YAML_INVALID` | Missing source, non-YAML source, or YAML parse failure | Check the path, indentation, and YAML syntax |
| `TRM_TARGET_EXISTS` | The target already exists | Confirm and use `overwrite`, or select another output directory |
| `TRM_DUPLICATE_MENU_ID` | Multiple TrMenu files have the same file name | Rename the conflicting source files and rerun the whole batch |
| `TRM_LAYOUT_INVALID` | Invalid rows, slots, or icon references | Correct source `Layout` and `Icons` |
| `TRM_RENDER_TYPE_UNSUPPORTED` / `TRM_MULTI_PAGE_UNSUPPORTED` | A Dialog or multi-page menu cannot become one static Container menu | Split or rewrite the menu manually |
| `TRM_CONDITION_UNSUPPORTED` / `TRM_ACTION_UNSUPPORTED` | The condition or action has no fixed mapping | Rewrite it with KaMenu conditions or actions |
| `TRM_ITEM_SOURCE_UNSUPPORTED` / `TRM_ITEM_META_UNSUPPORTED` | A private item source or property cannot be mapped safely | Use a Bukkit material or supported KaMenu item format |
| `TRM_NODE_DYNAMIC_UNSUPPORTED` | The node path contains runtime or nested dynamic content | Use a static path or rewrite it with KaMenu variables/references |
| `TRM_NODE_ICON_CONTEXT_MISSING` | `@iconId@` appears outside an icon, so its ID is unknown | Use an explicit static node path |
| `TRM_NODE_PATH_MISSING` | The source menu does not contain the referenced node | Check the source path, key casing, and file content |
| `TRM_NODE_STRUCTURE_UNSUPPORTED` | The node points to a Map, nested list, or another non-text structure | Reference a concrete scalar or simple-list field |
| `TRM_NODE_REFERENCE_UNSUPPORTED` | The current field cannot safely retain an unconverted node reference | Rewrite that field manually and migrate again |
| `TRM_TARGET_VALIDATION_FAILED` | Generated output failed KaMenu's second parse | Review preceding diagnostics; report the issue with the full report if reproducible |

Use the YAML path and message to interpret every other `TRM_*` code. `APPROXIMATE` means the target can run but behaves differently. `UNSUPPORTED` means the affected content was filtered. A successful file count alone is not sufficient for production use.

## Post-Migration Checklist

Verify at least the following on a test server:

- Every menu opens with the expected layout and title.
- Permission, money, item requirements, and deny branches still work.
- Every click type, menu transition, close, and refresh action works.
- ItemsAdder, Oraxen, CraftEngine, Vault, PlayerPoints, and proxy integrations work.
- `custom_commands.yml` and `item_bindings.yml` have no unresolved conflicts or missing entries.
