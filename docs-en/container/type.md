# Type

`Type` selects the Bukkit inventory type. When `Type` is omitted but `Layout` exists, the parser defaults to `CHEST`; writing it explicitly is recommended.

| Value | Logical size | Description |
|---|---:|---|
| `CHEST` | 9 columns, 1-6 rows | Standard chest menu |
| `HOPPER` | 5 slots | Hopper menu |
| `DISPENSER` | 3 x 3 | Dispenser menu |
| `DROPPER` | 3 x 3 | Dropper menu |
| `FURNACE` | 3 slots | Furnace progress properties |
| `BLAST_FURNACE` | 3 slots | Blast furnace progress properties |
| `SMOKER` | 3 slots | Smoker progress properties |
| `ANVIL` | 3 slots | Anvil input and result properties |

Each `Layout` row must match the logical slot count for the selected type. Container menus use Bukkit's public inventory API rather than an NMS fake-window layer, so display and click support follow the public Bukkit API boundary.

See [Properties](properties.md) for furnace and anvil fields. Static chest-like menus usually need only `Type`, `Layout`, and `Buttons`.
