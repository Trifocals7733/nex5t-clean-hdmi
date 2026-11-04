@AGENTS.md

## Claude-specific

- Reply to the user in Swedish. Code and docs are in English.
- The camera may be USB-connected. Before installing, check that it is in Mass
  Storage mode (`pmca-console.py info -d native`).
- Firmware research runs in WSL Ubuntu (user `peepo`). The unpacked firmware is
  at `~/fw/out`. For anything non-trivial, write an LF shell script to the
  scratchpad and run it with `wsl bash /mnt/c/...`. Inline `wsl bash -c` with
  `$vars` gets mangled by quoting.
- Ask before running anything that reboots the camera into updater mode
  (updatershell) or writes to its flash.
