# 📦 actions Folder

`plugins/KaMenu/actions/` stores global actions packages. Use actions packages for action queues shared by multiple menus, such as common feedback, reward delivery, permission checks, and cross-menu navigation.

Global actions packages can be called from any menu action list, `Events.Click`, clickable text, periodic tasks, and custom command action lists.

---

## File Location

Runtime folder:

```text
plugins/KaMenu/actions/
```

One `.yml` file is one actions package. The package ID is the relative path without the `.yml` suffix, using `/` as the path separator:

```text
plugins/KaMenu/actions/reward/daily.yml -> reward/daily
plugins/KaMenu/actions/common/close.yml -> common/close
```

When the server starts and `plugins/KaMenu/actions/` does not exist yet, KaMenu releases a built-in example:

```text
plugins/KaMenu/actions/example/welcome.yml -> example/welcome
```

If the folder already exists, KaMenu does not write example files automatically, avoiding changes to existing server files.

---

## Naming And Size Limits

Package IDs may only use letters, numbers, `_`, `-`, `.`, and `/`. They cannot contain `..`, cannot start or end with `/`, and cannot contain consecutive `//`.

Each actions package file is limited to `1 MiB`. Files over the limit are skipped and a localized warning is printed to the console.

---

## File Format

The root node must be an `actions` list:

```yaml
actions:
  - 'toast: type=task;msg=Claimed;icon=emerald'
  - 'sound: entity.experience_orb.pickup;volume=1.0;pitch=1.2'
  - 'money: type=add;num={arg:0}'
```

Packages support regular actions, conditional branches, `wait`, `return`, nested `actions:` calls, and the full action queue behavior.

---

## Calling Packages

```yaml
actions:
  - 'actions: reward/daily,100'
  - 'actions: common/close'
```

Arguments may be separated by commas or spaces:

```yaml
- 'actions: reward/daily,100,vip'
- 'actions: reward/daily 100 vip'
```

Wrap an argument with single quotes, double quotes, or backticks when it contains a space or comma:

```yaml
- 'actions: reward/message,"100 coins",`vip,plus`'
```

Inside the package, read arguments with `{arg:0}`, `{arg:1}`, and so on.

---

## Lookup Priority

When calling:

```yaml
- 'actions: reward/daily'
```

KaMenu checks:

1. Current menu `Events.Click.reward/daily`
2. Global package `plugins/KaMenu/actions/reward/daily.yml`

If a menu `Events.Click` entry and a global package use the same name, the menu action list has priority.

---

## Recursion Rule

To avoid direct recursion, an action list cannot directly call itself:

```yaml
# Do not write this inside reward/daily.yml:
- 'actions: reward/daily'
```

When a direct self-call is detected, KaMenu skips that `actions:` call and continues with following actions.

---

## Reload

Global actions packages are loaded on server startup, `/km reload`, or `/km reload actions`.

Only `.yml` files are loaded. `.yaml` files are not loaded.

---

## Example

File:

```text
plugins/KaMenu/actions/reward/daily.yml
```

Content:

```yaml
actions:
  - condition: 'hasPerm.reward.daily'
    allow:
      - 'toast: type=task;msg=Claimed;icon=emerald'
      - 'money: type=add;num={arg:0}'
      - 'sound: entity.experience_orb.pickup;volume=1.0;pitch=1.2'
    deny:
      - 'toast: type=challenge;msg=No permission;icon=barrier'
```

Call from a menu:

```yaml
Bottom:
  type: multi
  buttons:
    daily:
      text: '&a[ Daily Reward ]'
      actions:
        - 'actions: reward/daily,100'
        - 'reset'
```

---

## Troubleshooting

**The package does not run**

- Check that the file is under `plugins/KaMenu/actions/`
- Check that the suffix is `.yml`
- Check that the package ID only uses letters, numbers, `_`, `-`, `.`, and `/`
- Check that the file size is no more than `1 MiB`
- Check that the root node is `actions`
- Run `/km reload actions`

**Action list not found**

In a menu context, KaMenu checks current menu `Events.Click.xxx` and global `actions/xxx.yml`. In custom commands or other no-menu contexts, only global actions packages are checked.
