## Routinize

Routinize is a Fabric client-side mod for creating routines that automate commands, menus, keyboard inputs, and mouse actions.

> **Beta:** Routinize may contain bugs or compatibility issues. Report them at: https://github.com/WhereDidMyDivGo/routinize/issues

## Dependencies

- Fabric API
- Mod Menu (optional)

## Getting Started

Run `/routinize` to open the routine manager. From there you can create, edit, and delete routines, assign run and pause keys, and configure routine settings.

Mod Menu also provides a shortcut to open the routine manager.

## Commands

| Command                    | Action                                                      |
| -------------------------- | ----------------------------------------------------------- |
| `/routinize`               | Open the routine manager.                                   |
| `/routinize edit <name>`   | Edit a routine.                                             |
| `/routinize delete <name>` | Delete a routine.                                           |
| `/routinize dump`          | Save the current menu's item names and lore to a text file. |
| `/routinize reload`        | Reload saved routines and settings.                         |

Examples:

```text
/routinize edit My Routine
/routinize delete My Routine
```

Menu dumps are saved to `.minecraft/routinize-dumps/`.

The config is saved to `./minecraft/config/routinize.json`.

## Routine Syntax

Empty lines and lines beginning with `#` are ignored.

| Function               | Action                                                    |
| ---------------------- | --------------------------------------------------------- |
| `command [...]`        | Run a command.                                            |
| `wait (...)`           | Wait for a duration, a menu to open, or a menu to change. |
| `close`                | Close the current menu.                                   |
| `stop`                 | Stop the routine.                                         |
| `action [...]`         | Press or release keys and click matching items.           |
| `if`, `else`, `elseif` | Choose blocks based on a condition.                       |
| `while`                | Repeat a block while its condition is met.                |
| `loop`                 | Repeat a block a fixed number of times or indefinitely.   |
| `continue`             | Skip to the next loop iteration.                          |
| `break`                | Exit the current loop.                                    |

### Commands and timing

Run a command. The slash is optional.

```text
command [/home]
command [home]
```

Wait for a fixed or random duration in milliseconds:

```text
wait (500)
wait (500-1500)
```

Wait for a menu to open or change:

```text
wait (open)
wait (change)
```

These waits time out after five seconds if the expected event does not occur.

Close the current menu or stop the routine:

```text
close
stop
```

### Actions

An `action` can contain one or more tokens. Key actions use `down` and `up`:

```text
action [forward down] [jump down]
wait (1000)
action [forward up] [jump up]
```

Supported held keys:

```text
forward   backward   left   right
jump      sneak      lclick   rclick
```

Inventory clicks use an optional item name, lore, or `shift` filter:

```text
action [lclick item name="Stone"]
action [rclick item lore="Right-click to open"]
action [lclick item shift name="Diamond"]
action [mclick item name="Upgrade" lore="Cost: 10 coins"]
```

Inventory click buttons are `lclick`, `rclick`, and `mclick`.

`mclick` cannot be combined with `shift`.

`name="X"` and `lore="X"` use **contains matching**, so they match names or lore that include `X` rather than requiring an exact match.

### Conditions

Conditions determine whether a block should run:

```text
if (name="Confirm")
    action [lclick item name="Confirm"]
end
```

Use `not`, `else`, and `elseif` as needed:

```text
if not (name="Cancel")
    command [tell steve No cancel button found]
elseif (lore="Click to continue")
    action [lclick item lore="Click to continue"]
else
    wait (500)
end
```

### Loops

Run a block while its condition is met:

```text
while (name="Refresh")
    action [lclick item name="Refresh"]
    wait (change)
end
```

Use `not` to invert the condition:

```text
while not (name="Done")
    wait (change)
end
```

Repeat a block a fixed number of times or indefinitely:

```text
loop (3)
    action [lclick item name="Next"]
    wait (change)
end

loop
    action [lclick item name="Refresh"]
    wait (change)
end
```

Inside a loop, `continue` skips to the next iteration and `break` exits the loop.

## Settings

### Click retry timeout

How long an item click can remain unsuccessful before the routine times out. Default: `5000` milliseconds.

### Chat feedback

Shows routine status messages in chat. Errors and command responses are shown regardless of this setting. Default: enabled.

### Release keys on stop

Releases keys held by a routine when it stops. Default: enabled.

### Auto-resume on GUI close

Resumes routines that were automatically paused when a GUI opened after that GUI closes. Default: disabled.

## Safety

Routines that use world actions are paused or stopped when a GUI opens, depending on whether a pause key is assigned.
