<!-- Language: **English** | [Русский](README.ru.md) -->

# ClansMC

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![Build](https://github.com/sheynor43/ClansMC-1.21.11/actions/workflows/build.yml/badge.svg)](https://github.com/sheynor43/ClansMC-1.21.11/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **English** | [Русский](README.ru.md)

A feature-rich, production-quality **clans plugin for Purpur / Paper 1.21.11**. Built on the modern Paper API (native Brigadier commands, Adventure MiniMessage everywhere, Paper-plugin), fully asynchronous storage, and completely bilingual/localizable — **no hard-coded strings**.

---

## Features

- 🏰 **Clans** with a name (3–16 chars, spaces allowed) and a unique tag (`[a-zA-Z0-9]`, 2–5 chars). Two roles only: **Leader** and **Member**.
- 💬 **Interactive `/clan create` dialog** in chat (name → tag → confirm) with clickable buttons, plus a quick `/clan create <name> <tag>` form.
- ✉️ **Invitations** with clickable Accept/Decline, expiry and anti-spam.
- ⚔️ **Friendly fire cap** — clan mates can hit but never kill each other. Knockback, sound and particles are preserved; indirect damage (TNT, crystals, lava, fall, void…) still kills. Pets are protected. Optional spawn regions force a hard cancel.
- 🐉 **Boss XP sharing** — when a clan slays the **Ender Dragon** or **Wither**, all XP is split equally between the contributing members and granted directly (orbs suppressed). Offline members get their share on next join.
- 🏷️ **Clan tag in the tab list** (and only there) — `INTERNAL` or `PLACEHOLDER_ONLY` mode. AuthMe-aware.
- 🗨️ **Clan chat** — `/cc <text>` or a toggle mode.
- 🤝 **Alliances (mutual)** and **enemies (unilateral)**.
- 📈 **Clan levels & perks** (max allies, bank capacity, glow unlock) — fully config-driven.
- 🏦 **Clan bank** (optional, Vault-based).
- 🖥️ **GUI menu** on the vanilla Inventory API — members (paginated, kick/transfer), relations, bank, settings, with confirmation dialogs.
- ✨ **Clan glow** (optional, level-gated).
- 🌍 **Full localization** — `en.yml` + `ru.yml` ship in the jar; add any `<code>.yml` and select it in the config. Per-key fallback to English, per-player locale support.
- 🗄️ **SQLite or MySQL/MariaDB** with HikariCP, a single shared schema and a versioned migration system. All I/O is asynchronous with an in-memory cache.
- 🔌 **PlaceholderAPI** expansion and a simple **`ClansAPI`** for add-ons.

---

## Requirements

| | |
|---|---|
| Server | Purpur / Paper **1.21.11** |
| Java | **21+** |
| Optional | PlaceholderAPI, Vault + an economy plugin, AuthMe |

## Installation

1. Download `ClansMC-x.y.z.jar` from [Releases](https://github.com/sheynor43/ClansMC-1.21.11/releases) (or build it — see below).
2. Drop it into your server's `plugins/` folder.
3. Start the server once to generate `plugins/ClansMC/config.yml` and `plugins/ClansMC/lang/`.
4. **The default storage is MySQL** — edit `config.yml` → `storage.mysql.*`, or set `storage.type: SQLITE` for zero-setup local storage.
5. `/clan reload` to apply config/language changes without a restart.

---

## Commands

Clans are referenced by **tag** (a single word) in command arguments; names may contain spaces and are used for display only.

| Command | Who | Description |
|---|---|---|
| `/clan create [name] [tag]` | `clans.create` | Create a clan (interactive dialog, or quick form) |
| `/clan add <player>` (alias `invite`) | leader | Invite a player |
| `/clan accept <tag>` / `deny <tag>` | everyone | Respond to an invitation |
| `/clan invites` | everyone | List your pending invitations |
| `/clan info [tag]` | everyone | Name, tag, leader, level/XP, members, founded date, allies/enemies, boss stats |
| `/clan list [page]` | everyone | Paginated clan list |
| `/clan members` | in clan | Member list with online status |
| `/clan leave` | member | Leave your clan (leader can't) |
| `/clan kick <player>` | leader | Kick a member |
| `/clan transfer <player>` | leader | Transfer leadership (confirm) |
| `/clan disband` | leader | Disband the clan (confirm) |
| `/clan chat` · `/cc <text>` | in clan | Clan chat |
| `/clan ally <tag>` / `unally <tag>` | leader | Alliances (mutual) |
| `/clan enemy <tag>` / `unenemy <tag>` | leader | Enemies (unilateral) |
| `/clan bank` · `deposit <amount>` · `withdraw <amount>` | in clan | Clan bank (withdraw = leader) |
| `/clan glow` | leader | Toggle clan glow |
| `/clan menu` (alias `gui`) | everyone | Open the GUI |
| `/clan help` | everyone | Permission-aware help |
| `/clan reload` | `clans.admin` | Reload config & languages |
| `/clanadmin delete <tag>` | `clans.admin` | Delete a clan |
| `/clanadmin settag <tag> <newtag>` | `clans.admin` | Change a clan's tag |
| `/clanadmin setname <tag> <newname>` | `clans.admin` | Rename a clan |
| `/clanadmin join <player> <tag>` | `clans.admin` | Add a player to a clan |
| `/clanadmin leave <player>` | `clans.admin` | Remove a player from their clan |
| `/clanadmin setleader <tag> <player>` | `clans.admin` | Set a clan's leader |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `clans.use` | `true` | Basic `/clan` commands (info, list, help, join) |
| `clans.chat` | `true` | Clan chat (`/cc`, `/clan chat`) |
| `clans.create` | `false` | Found a new clan — intended for VIPs |
| `clans.bypass.friendlyfire` | `false` | This player's hits on clan mates are never capped |
| `clans.admin` | `op` | `/clanadmin` and `/clan reload` |

---

## Configuration

Every message lives in the language files, not here. On update, new keys are merged into your `config.yml` automatically (your values and comments are kept), tracked by `config-version`.

<details>
<summary><b>Full <code>config.yml</code></b></summary>

```yaml
config-version: 1

# Language file (without extension) from plugins/ClansMC/lang/.
language: ru
# When true, each player sees messages in their client locale, falling back to 'language'.
per-player-locale: false
# Verbose logging for troubleshooting.
debug: false

storage:
  # SQLITE = zero-setup local file. MYSQL = MySQL/MariaDB server.
  type: MYSQL
  sqlite:
    file: clans.db
  mysql:
    host: 127.0.0.1
    port: 3306
    database: clans
    user: root
    password: ""
    pool-size: 10
    properties:
      useUnicode: "true"
      characterEncoding: "utf8"

clan:
  name:
    min-length: 3
    max-length: 16
    blacklist: [ "admin", "server", "staff", "fuck", "nigger" ]
  tag:
    min-length: 2
    max-length: 5
    # Single MiniMessage colour applied to every tag. Players cannot pick it.
    color: "<aqua>"
  create-permission: clans.create
  dialog:
    timeout-seconds: 60

friendly-fire:
  # CAP = hit but not kill; CANCEL = block all direct clan-mate damage; OFF = no handling.
  mode: CAP
  min-health: 0.5
  protect-pets: true
  include-allies: false
  regions:
    # - world: world
    #   corner1: { x: -50, y: 0, z: -50 }
    #   corner2: { x: 50, y: 255, z: 50 }

boss-xp:
  enabled: true
  hold-for-offline: true
  damage-timeout-seconds: 600
  dragon:
    enabled: true
    suppress-orbs-ticks: 400
    suppress-radius: 24
  wither:
    enabled: true

tab:
  # INTERNAL sets the tab name itself; PLACEHOLDER_ONLY only exposes placeholders.
  mode: INTERNAL
  format: "<player_name> <gray>[<clan_tag_colored><gray>]"
  update-interval: 200
  apply-delay-ticks: 20

clan-chat:
  log-to-console: true

notifications:
  join-quit: false

relations:
  ally-request-timeout-seconds: 120

invite:
  expire-seconds: 120
  antispam-seconds: 30

levels:
  enabled: true
  xp-rewards:
    wither: 100
    dragon: 500
  levels:
    1: { xp: 0,     max-allies: 1, bank-capacity: 10000.0,   glow-unlocked: false }
    2: { xp: 500,   max-allies: 2, bank-capacity: 50000.0,   glow-unlocked: false }
    3: { xp: 1500,  max-allies: 3, bank-capacity: 150000.0,  glow-unlocked: true }
    4: { xp: 4000,  max-allies: 5, bank-capacity: 500000.0,  glow-unlocked: true }
    5: { xp: 10000, max-allies: 8, bank-capacity: 2000000.0, glow-unlocked: true }

bank:
  enabled: false

glow:
  enabled: false

limits:
  max-members: -1
```

</details>

---

## Adding your own translation

1. Copy `plugins/ClansMC/lang/en.yml` to a new file, e.g. `de.yml`.
2. Translate the values (they are [MiniMessage](https://docs.advntr.dev/minimessage/format.html); keep placeholders like `<player>`, `<clan>`, `<tag>`, `<amount>` intact). A value may be a single string or a list of lines.
3. Set `language: de` in `config.yml`.
4. Run `/clan reload`.

Notes:
- Bundled `en.yml`/`ru.yml` are extracted on first run and **never overwritten** on update.
- Any missing/empty key falls back to `en.yml`; if it's missing there too, the raw key is shown and logged once.
- Keep `lang-version` as shipped — a mismatch triggers a console warning listing new/removed keys.

---

## Tab modes & FlectonePulse

The clan tag appears **only in the tab list**, never over the head or in chat.

- **`INTERNAL`** (default) — the plugin sets the tab name via `Player#playerListName`. `tab.update-interval` periodically re-applies it to override other plugins.
- **`PLACEHOLDER_ONLY`** — **recommended when another plugin manages the tab list** (e.g. **FlectonePulse**). ClansMC never touches the tab; it only exposes PlaceholderAPI placeholders. Configure your tab plugin to render `%clans_tag_formatted%`, for example in FlectonePulse's tab format:

  ```
  %player_name% %clans_tag_formatted%
  ```

  Then set `tab.mode: PLACEHOLDER_ONLY` in ClansMC and let FlectonePulse draw the tab.

> **Chat conflict note:** the clan-creation dialog and clan-chat capture chat at `EventPriority.LOWEST` and cancel the event so it never reaches global chat. FlectonePulse also processes chat. If you observe dialog/clan-chat messages leaking into global chat, ensure ClansMC loads first or lower FlectonePulse's chat priority; ClansMC intentionally uses the lowest priority to win the race.

---

## Boss XP distribution — how it works & limits

- Damage to the **Ender Dragon** and **Wither** is journaled per player. On death, damage is aggregated per clan and **the whole XP pool goes to the single clan that dealt the most damage**. Non-clan players and other clans get nothing from that pool. If only clan-less players dealt damage, vanilla behaviour is untouched.
- Within the winning clan, only the members who dealt damage share the pool **equally**; the integer remainder goes to the top damager.
- Contributors who are offline at death get their share stored (`pending_xp`) and receive it on next join (`boss-xp.hold-for-offline`).
- **Wither:** `setDroppedExp(0)` is enough; the dropped amount is the pool.
- **Ender Dragon:** `setDroppedExp(0)` is **not** enough — the dragon spawns XP orbs during its death animation. ClansMC opens a suppression window (`dragon.suppress-orbs-ticks`, default 400) and cancels `ExperienceOrb` spawns within `dragon.suppress-radius` (default 24) blocks of the death point, summing their XP into the shared pool. Tune the radius/duration if your setup differs.

> Loot (dragon egg, nether stars) is never touched.

---

## Compatibility

| Plugin | Notes |
|---|---|
| **FlectonePulse** | Main integration point for chat & tab. Use `tab.mode: PLACEHOLDER_ONLY` and render `%clans_tag_formatted%`. See the chat-conflict note above. |
| **AuthMe / FastLogin** | The tab name is not applied until the player authenticates (detected via AuthMe's API, reflectively). Without AuthMe, the tag is applied after `tab.apply-delay-ticks`. |
| **LuckPerms** | Used for permissions like `clans.create`. |
| **PlaceholderAPI** | Expansion auto-registers when present. |
| **Vault** | Required for the clan bank. Missing Vault → bank auto-disables with a console warning. |
| **ProtocolLib / packetevents** | Not required and not used. |
| **CoreProtect, GSit, ResizePlayers** | No known conflicts. |

**Known limitation — clan glow:** the Bukkit API cannot restrict a glow outline to clan mates only, so an enabled clan glow is **visible to everyone**. A per-viewer implementation would require sending entity-metadata packets, which this plugin does not do (a possible future enhancement).

---

## PlaceholderAPI

| Placeholder | Value |
|---|---|
| `%clans_tag%` | The clan tag (plain) |
| `%clans_tag_formatted%` | The tag with the configured colour (MiniMessage string) |
| `%clans_name%` | The clan name |
| `%clans_role%` | `LEADER` / `MEMBER` |
| `%clans_level%` | Clan level |
| `%clans_members_total%` | Total members |
| `%clans_members_online%` | Online members |
| `%clans_bank%` | Formatted bank balance |
| `%clans_allies%` | Number of active allies |

---

## Building from source

```bash
git clone https://github.com/sheynor43/ClansMC-1.21.11.git
cd ClansMC-1.21.11
./gradlew build
```

The shaded plugin jar is produced at `build/libs/ClansMC-<version>.jar`. Requires JDK 21.

## API for add-ons

```java
ClansAPI api = Bukkit.getServicesManager().load(ClansAPI.class);
api.getClanOf(player.getUniqueId()).ifPresent(clan -> {
    getLogger().info(player.getName() + " is in " + clan.name());
});
```

## Screenshots

<!-- Add your screenshots here -->
> _Clan menu_ — `![menu](docs/menu.png)`
>
> _Clan info_ — `![info](docs/info.png)`
>
> _Tab tag_ — `![tab](docs/tab.png)`

## Contributing

Issues and pull requests are welcome. Please keep commits in the [Conventional Commits](https://www.conventionalcommits.org/) style and make sure `./gradlew build` passes.

## License

Released under the [MIT License](LICENSE).
