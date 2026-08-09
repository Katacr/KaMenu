# Menu References (References)

Menu references reuse text, numbers, and simple lists within one menu file. They work in Dialog and Container menus and reduce duplication in button labels, lore, action parameters, and condition values.

## Reference Forms

| Syntax | Scope | Typical use |
|---|---|---|
| `{ref:path}` | `References.path` in the current menu | Shared text or templates |
| `{config:path}` | Root of the current menu | Existing fields such as `Title` or `Settings` |
| `{self:<field-path>}` | Current component or button node | Button-local custom data |
| `{self:id}` | Current component or button ID | Include the current ID in a shared format |
| `{self:path}` | Full configuration path of the current component or button | Diagnostics or dynamic path composition |
| `{refarg:n}` | Argument `n` passed to the active reference | Template parameters inside `References` |

Paths are case-insensitive, although matching the actual YAML key casing is recommended. References are limited to the current menu file and cannot read another menu.

## Shared References

Define `References` at the menu root and read it with `{ref:path}`:

```yaml
Title: '{ref:text.title}'

References:
  text:
    title: '&8Server Shop'
    currency: '&6Coins'
    product_lore:
      - '&7Price: {refarg:0} {ref:text.currency}'
      - '&7Left-click to buy'

Body:
  info:
    type: message
    text: '&7Currency: {ref:text.currency}'

Bottom:
  type: notice
  confirm:
    text: '{ref:[text.product_lore;100]}'
    actions:
      - 'close'
```

A scalar becomes text directly. A simple list is joined with newlines, so it can be reused as a multiline message, lore, or tooltip.

## Parameterized Templates

Template arguments use zero-based indexes:

```yaml
References:
  buy: '&aBuy {refarg:0} {refarg:1} for {refarg:2}'

Body:
  info:
    type: message
    text: '{ref:[buy;5;diamonds;100 coins]}'
```

The result is:

```text
&aBuy 5 diamonds for 100 coins
```

Arguments are separated by semicolons. Wrap an argument in backticks, single quotes, or double quotes when it contains a semicolon:

```yaml
text: '{ref:[buy;5;`limited;diamonds`;100 coins]}'
```

A template may contain other references. Arguments may also contain PAPI placeholders, KaMenu variables, or references. A missing required `{refarg:n}` throws a configuration error instead of producing incomplete text silently.

## Reading The Current Configuration

`{config:path}` reads from the current menu root:

```yaml
Title: '&8Main Menu'

Body:
  debug:
    type: message
    text: '&7Configured title: {config:Title}'
```

Use this for an existing simple field. Store shared templates under `References` instead, and do not reference an entire Map section.

## Current Component References

`{self:*}` is automatically scoped to the component currently being resolved, where `*` is a field path relative to that component:

- Dialog: `Body.<id>`, `Inputs.<id>`, `Bottom.confirm`, `Bottom.deny`, `Bottom.buttons.<id>`, `Bottom.exit`, and repeat `Bottom.buttons.<id>.item`.
- Container: `Buttons.<id>`. A button using `variants` still uses its parent button ID and path.

```yaml
Type: CHEST
Title: '&8Shop'

Layout:
  - '    S    '

Buttons:
  S:
    data:
      product: DIAMOND
      price: 100
    display:
      material: '{self:data.product}'
      name: '&bProduct {self:id}'
      lore:
        - '&7Price: {self:data.price}'
    actions:
      left:
        - 'actionbar: &aBought {self:data.product} for {self:data.price}'
```

`self` lets several components share a format while retaining local data. It only exists in a component context; using `{self:*}` in `Events.Open` or another context without a current component throws an error.

## References In Conditions

`{ref:*}`, `{config:*}`, and `{self:*}` are expanded before a condition expression is parsed, and each resolved result participates as one safely encoded value:

```yaml
References:
  minimum_level: 10

Settings:
  max_price: 500

Buttons:
  S:
    data:
      price: 100
    view_condition: '%player_level% >= {ref:minimum_level} && {self:data.price} <= {config:Settings.max_price}'
```

This works in conditional maps, button visibility conditions, `variants.condition`, and the `{condition: ...}` line suffix. `ref` and `config` only require the current menu configuration. `self` additionally requires a Dialog component or Container button context. `{self:id}` returns the component ID, `{self:path}` returns a full path such as `Buttons.S`, and `{self:data.price}` reads a field under that path.

## Limits And Errors

- Supported values are strings, numbers, booleans, and lists containing only scalars.
- Lists are joined with newlines. Maps, configuration sections, nested lists, and other structures cannot be embedded in text.
- References do not inherit complete buttons, Body components, or other YAML structures.
- Cross-menu references are not supported.
- The maximum recursive depth is 16, and circular references throw a clear error.
- Structural fields such as `Type`, `Layout`, and YAML key names do not become dynamic through text references.

An invalid path, unsupported structure, missing template argument, or cycle preserves KaMenu's normal fail-fast behavior and reports the underlying configuration error.
