# OryzonNPCs

> Modern NPC plugin for Paper servers (1.20.6+). Zero YAML, in-game UI, and
> ZeroBase-backed persistence.

OryzonNPCs is the first plugin from **Oryzon Studios**. It exists because
every NPC plugin in 2026 still treats configuration like a job for sysadmins:
edit a YAML, restart, hope for the best. We treat it like a UX problem.

## Status

**Pre-release.** The repository currently contains M1 scaffolding only —
build infrastructure, plugin descriptor, and the empty plugin shell. The
roadmap below tracks what's coming.

## Roadmap

| Milestone | Deliverable                                                        |
|-----------|--------------------------------------------------------------------|
| M1 ✅     | Bootstrap plugin + repo + CI                                       |
| M2 ✅     | NPC core: spawn, despawn, Mojang skins (PacketEvents)              |
| M3 ✅     | JSON-file persistence (`plugins/OryzonNPCs/npcs.json`, atomic)     |
| M4        | In-game UI editor (inventory menus, no commands required)          |
| M5        | Action system + **Free v0.1.0** released to SpigotMC               |
| v0.2      | Swap JSON store for embedded ZeroBase (Java client SDK)            |

## Why another NPC plugin?

The existing options each fall short of one of these:

- **Citizens2** is powerful but its UX is from 2014 — Denizen scripts and
  YAML traits scare anyone who doesn't already know it.
- **FancyNpcs** and **ZNPCsPlus** are modern and free, but very basic —
  click-runs-command and not much beyond that.
- **None** of them sync state across servers without paid add-ons or
  external databases.

OryzonNPCs free is built around two things the others don't have:

1. **In-game UI editor.** Right-click an NPC, get an inventory menu, edit
   actions and skins from there. No `/npc trait`, no YAML.
2. **ZeroBase under the hood.** The plugin embeds a local
   [`zerobased`](https://github.com/ODev-M/zerobase) daemon for storage —
   encrypted at rest, signed log entries, no external DB required.

## Free vs Pro (planned)

| Feature                                  | Free | Pro |
|------------------------------------------|:----:|:---:|
| Unlimited NPCs                           |  ✅  |  ✅ |
| Mojang skins                             |  ✅  |  ✅ |
| In-game UI editor                        |  ✅  |  ✅ |
| ZeroBase local persistence               |  ✅  |  ✅ |
| Single click → command/message           |  ✅  |  ✅ |
| Web management panel                     |      |  ✅ |
| Branching dialog trees                   |      |  ✅ |
| Chained actions + conditions             |      |  ✅ |
| Skin manager (URL, upload, library)      |      |  ✅ |
| Cross-server sync via shared ZeroBase    |      |  ✅ |
| Performance pack (FoVCulling, cluster)   |      |  ✅ |

## Building from source

```bash
./gradlew build
```

The shaded plugin jar lands in `build/libs/`. Drop it into your server's
`plugins/` directory.

## Requirements

- Paper **1.20.6** or newer (every 1.20.6 → 1.21.x release is supported).
- Java **21+**.

## License

TBD — pending decision between MIT (max reach) and Apache-2.0 (matches
ZeroBase). Free tier will always be source-available.
