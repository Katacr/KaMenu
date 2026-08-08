# TrMenu Migration

KaMenu can compile classic TrMenu stable-v3 inventory menus into standard KaMenu V2 Container menus. The migrator only reads YAML. It does not depend on TrMenu and never executes source Kether, JavaScript, commands, or click actions during migration.

## Running A Migration

```bash
/km migrate trmenu [source-file-or-directory] [output-directory] [overwrite]
```

- `trm` is an alias of `trmenu`.
- The default source is `plugins/TrMenu/menus`.
- The default output is `plugins/KaMenu/menus/trmenu_migrated`.
- The output directory must remain under `plugins/KaMenu/menus`.
- Existing menus, custom commands, and item bindings are preserved unless `overwrite` is supplied.
- Plain root commands from `Bindings.Commands` are merged into `custom_commands.yml`.
- Compatible `Bindings.Items` entries are merged into `item_bindings.yml`.
- Menus, custom commands, item bindings, and online player command trees are reloaded after migration.

```bash
/km migrate trmenu
/km migrate trm overwrite
/km migrate trmenu /path/to/TrMenu/menus trmenu_migrated overwrite
```

## Migration Process

The migrator scans the whole batch first, maps TrMenu file names to KaMenu target IDs, and then converts cross-menu `open:` actions. TrMenu files with the same file name in different directories fail with `TRM_DUPLICATE_MENU_ID`, because TrMenu itself uses the file name without its extension as the menu ID.

Every generated file is parsed again by KaMenu's `ContainerMenuParser`. A file with any ERROR is not written. Successful output is written to a temporary file in the target directory and then atomically replaces the target.

## Supported Scope

The main supported subset includes:

- Single-page CHEST, HOPPER, DISPENSER, DROPPER, FURNACE, BLAST_FURNACE, SMOKER, and ANVIL layouts
- TrMenu property-key regular expressions for case, singular/plural, and short aliases
- Vanilla materials, player heads, player equipment slots, ItemsAdder, Oraxen, CraftEngine, and CustomModelData
- Nested `icons` to `Buttons.<id>.variants`, including priority and inherit/append action order
- Common `any`, `all`, `not`, `check`, `perm`, `var/vars`, PAPI, money, points, and basic item conditions
- Statically recognized `utils.isPlayerOnline`, `utils.hasEquipment`, and `utils.getEquipment` calls, plus simple-argument `utils.hasMoney` and `utils.hasItem` calls
- Common message, command, sound, title, menu-open, refresh, menu-argument update/clear, delay, chance, economy, points, data, and basic item actions
- `Events.Open`, `Events.Close`, and automatic `Tasks`
- Pure return-value `Functions` to menu `JavaScript`, including `${function_arg}` calls
- Plain `Bindings.Commands` entries to `custom_commands.yml`
- `material`, `lore`, `name`, `data`, and `model-data` from `Bindings.Items` to `item_bindings.yml`

Untargeted TrMenu `refresh` / `update` actions become bare KaMenu `refresh` actions that refresh every button icon; explicit icon IDs become targeted refreshes. TrMenu `reset` is approximated as a bare `refresh`, but its animation indexes cannot be retained and therefore still produce an approximate-migration diagnostic.

TrMenu ignores `amount` while matching bound items, and the migrated binding does the same. Generated entries use `cooldown-ms: 2000`, matching TrMenu's default `Bound-Item-Interval`. The migrator does not read global `settings.yml`, so adjust this value manually when the source server used a custom interval. See [Right-Click Item Bindings](../config/item-bindings.md) for the complete target format.

## Not Automatically Migrated

These features reject a file, skip a branch, or produce a WARNING:

- TrMenu `Render-Type: DIALOG` and multi-page layouts
- Packet-based PlayerInventory, Free-Slots, and Hide-Player-Inventory features
- catcher, staged chat input, and menu-page actions
- Arbitrary Kether flows, JEXL, NovaScript, inline TrMenu JS, and private `session/config/utils` bindings outside the fixed mappings listed above
- Repo/private item sources, NBT, and properties without an explicit Bukkit API mapping
- Regex command bindings, client-localized Lang nodes, and item-binding traits without a safe mapping
- TrMenu drag and outside-window click types

Opposed item traits, skull owner/texture matching, unknown traits, amount-only matchers, and conflicting matcher groups are skipped and reported instead of being weakened into a broader item match.

An unsupported condition is never removed while its allow branch remains executable. If an Open condition cannot be converted, the migrator keeps migratable deny actions and appends `return`, preventing players from opening an incomplete or accidentally unprotected menu.

The stable-v3 built-in example places `Free-Slots` under `Options`, while the corresponding runtime source reads it from the menu root. The migrator detects both root `Free-Slots` and `Options.Free-Slots`; both are reported and skipped instead of introducing movable slots into KaMenu's read-only Container model.

## Diagnostics

Each diagnostic includes severity, compatibility, a stable code, and the source YAML path:

```text
[WARNING/APPROXIMATE/TRM_ICON_ANIMATION_FIRST_FRAME]
Icons.demo.display.name: Animated name was reduced to the first frame.
```

- `WARNING`: the file may still be generated, but the behavior must be reviewed.
- `ERROR`: the target file is not generated.
- `APPROXIMATE`: the output runs, but timing, animation, or lifecycle behavior differs.
- `UNSUPPORTED`: the affected field, action, or branch was filtered.

After migration, verify third-party items, Vault, PlayerPoints, proxy transfers, permissions, and every button action on a test server.
