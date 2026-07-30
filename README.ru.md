<!-- Language: [English](README.md) | **Русский** -->

# ClansMC

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net)
[![Build](https://github.com/sheynor43/ClansMC-1.21.11/actions/workflows/build.yml/badge.svg)](https://github.com/sheynor43/ClansMC-1.21.11/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> [English](README.md) | **Русский**

Полнофункциональный плагин кланов **production-качества для Purpur / Paper 1.21.11**. Построен на современном Paper API (нативный Brigadier, Adventure MiniMessage везде, Paper-plugin), полностью асинхронное хранилище и двуязычная локализация — **ни одной захардкоженной строки**.

---

## Возможности

- 🏰 **Кланы** с названием (3–16 символов, пробелы разрешены) и уникальным тегом (`[a-zA-Z0-9]`, 2–5 символов). Всего две роли: **Лидер** и **Участник**.
- 💬 **Интерактивный диалог `/clan create`** в чате (название → тег → подтверждение) с кликабельными кнопками, плюс быстрая форма `/clan create <название> <тег>`.
- ✉️ **Приглашения** с кликабельными Принять/Отклонить, сроком жизни и антиспамом.
- ⚔️ **Ограничение friendly fire** — сокланевцы могут бить, но не убивать друг друга. Нокбэк, звук и партиклы сохраняются; косвенный урон (ТНТ, кристаллы, лава, падение, пустота…) убивает как обычно. Питомцы защищены. Опциональные зоны спавна принудительно отменяют урон.
- 🐉 **Опыт с боссов** — когда клан убивает **Дракона Края** или **Визера**, весь опыт делится поровну между участниками, нанёсшими урон, и начисляется напрямую (сферы подавляются). Оффлайн-участники получают долю при следующем входе.
- 🏷️ **Тег клана в табе** (и только там) — режимы `INTERNAL` или `PLACEHOLDER_ONLY`. Учитывает AuthMe.
- 🗨️ **Клан-чат** — `/cc <текст>` или режим переключения.
- 🤝 **Союзы (взаимные)** и **враги (односторонние)**.
- 📈 **Уровни и перки клана** (макс. союзов, вместимость банка, разблокировка glow) — целиком из конфига.
- 🏦 **Клановый банк** (опционально, через Vault).
- 🖥️ **GUI-меню** на ванильном Inventory API — участники (пагинация, кик/передача), отношения, банк, настройки, с диалогами подтверждения.
- ✨ **Подсветка клана** (опционально, по уровню).
- 🌍 **Полная локализация** — `en.yml` + `ru.yml` в jar; добавьте любой `<код>.yml` и укажите в конфиге. Пофразовый фолбэк на английский, поддержка языка по игроку.
- 🗄️ **SQLite или MySQL/MariaDB** с HikariCP, единой схемой и системой версионируемых миграций. Весь I/O асинхронный, в памяти — кэш.
- 🔌 Экспансия **PlaceholderAPI** и простой **`ClansAPI`** для аддонов.

---

## Требования

| | |
|---|---|
| Сервер | Purpur / Paper **1.21.11** |
| Java | **21+** |
| Опционально | PlaceholderAPI, Vault + плагин экономики, AuthMe |

## Установка

1. Скачайте `ClansMC-x.y.z.jar` из [Releases](https://github.com/sheynor43/ClansMC-1.21.11/releases) (или соберите — см. ниже).
2. Положите в папку `plugins/` сервера.
3. Запустите сервер один раз — создадутся `plugins/ClansMC/config.yml` и `plugins/ClansMC/lang/`.
4. **Хранилище по умолчанию — MySQL** — настройте `storage.mysql.*`, либо укажите `storage.type: SQLITE` для локального хранения без настройки.
5. `/clan reload` применяет изменения конфига/языков без рестарта.

---

## Команды

В аргументах команд клан указывается по **тегу** (одно слово); название может содержать пробелы и используется только для отображения.

| Команда | Кто | Описание |
|---|---|---|
| `/clan create [название] [тег]` | `clans.create` | Создать клан (диалог или быстрая форма) |
| `/clan add <ник>` (алиас `invite`) | лидер | Пригласить игрока |
| `/clan accept <тег>` / `deny <тег>` | все | Ответ на приглашение |
| `/clan invites` | все | Активные приглашения |
| `/clan info [тег]` | все | Название, тег, лидер, уровень/опыт, участники, дата основания, союзы/враги, статистика по боссам |
| `/clan list [страница]` | все | Список кланов с пагинацией |
| `/clan members` | в клане | Участники со статусом |
| `/clan leave` | участник | Выйти (лидеру нельзя) |
| `/clan kick <ник>` | лидер | Исключить |
| `/clan transfer <ник>` | лидер | Передать лидерство (с подтверждением) |
| `/clan disband` | лидер | Расформировать (с подтверждением) |
| `/clan chat` · `/cc <текст>` | в клане | Клан-чат |
| `/clan ally <тег>` / `unally <тег>` | лидер | Союзы (взаимные) |
| `/clan enemy <тег>` / `unenemy <тег>` | лидер | Враги (односторонние) |
| `/clan bank` · `deposit <сумма>` · `withdraw <сумма>` | в клане | Банк (снятие — лидер) |
| `/clan glow` | лидер | Подсветка клана |
| `/clan menu` (алиас `gui`) | все | Открыть GUI |
| `/clan help` | все | Справка с учётом прав |
| `/clan reload` | `clans.admin` | Перечитать конфиг и языки |
| `/clanadmin delete <тег>` | `clans.admin` | Удалить клан |
| `/clanadmin settag <тег> <новый>` | `clans.admin` | Сменить тег |
| `/clanadmin setname <тег> <название>` | `clans.admin` | Сменить название |
| `/clanadmin join <игрок> <тег>` | `clans.admin` | Добавить игрока в клан |
| `/clanadmin leave <игрок>` | `clans.admin` | Убрать игрока из клана |
| `/clanadmin setleader <тег> <игрок>` | `clans.admin` | Назначить лидера |

## Права

| Право | По умолчанию | Описание |
|---|---|---|
| `clans.use` | `true` | Базовые команды `/clan` (info, list, help, вступление) |
| `clans.chat` | `true` | Клан-чат (`/cc`, `/clan chat`) |
| `clans.create` | `false` | Создание клана — для VIP |
| `clans.bypass.friendlyfire` | `false` | Урон этого игрока по сокланам не урезается |
| `clans.admin` | `op` | `/clanadmin` и `/clan reload` |

---

## Конфигурация

Все сообщения — в языковых файлах, а не здесь. При обновлении новые ключи автоматически дописываются в ваш `config.yml` (значения и комментарии сохраняются), отслеживается по `config-version`.

<details>
<summary><b>Полный <code>config.yml</code></b></summary>

```yaml
config-version: 1

# Языковой файл (без расширения) из plugins/ClansMC/lang/.
language: ru
# Если true — язык выбирается по локали клиента с фолбэком на 'language'.
per-player-locale: false
# Подробное логирование для отладки.
debug: false

storage:
  # SQLITE = локальный файл без настройки. MYSQL = сервер MySQL/MariaDB.
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
    # Единый цвет MiniMessage для всех тегов. Игрок не выбирает.
    color: "<aqua>"
  create-permission: clans.create
  dialog:
    timeout-seconds: 60

friendly-fire:
  # CAP = бить можно, убить нельзя; CANCEL = полный запрет прямого урона; OFF = без обработки.
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
  # INTERNAL — плагин сам ставит имя в табе; PLACEHOLDER_ONLY — только плейсхолдеры.
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

## Как добавить свой перевод

1. Скопируйте `plugins/ClansMC/lang/en.yml` в новый файл, например `de.yml`.
2. Переведите значения (формат [MiniMessage](https://docs.advntr.dev/minimessage/format.html); сохраняйте плейсхолдеры `<player>`, `<clan>`, `<tag>`, `<amount>`). Значение может быть строкой или списком строк.
3. Укажите `language: de` в `config.yml`.
4. Выполните `/clan reload`.

Примечания:
- Встроенные `en.yml`/`ru.yml` извлекаются при первом запуске и **никогда не перезаписываются** при обновлении.
- Любой отсутствующий/пустой ключ берётся из `en.yml`; если и там нет — показывается сам ключ и выводится предупреждение в консоль (один раз).
- Не меняйте `lang-version` — при несовпадении в консоль выводится предупреждение со списком новых/удалённых ключей.

---

## Режимы таба и FlectonePulse

Тег клана показывается **только в табе**, никогда над головой или в чате.

- **`INTERNAL`** (по умолчанию) — плагин ставит имя через `Player#playerListName`. `tab.update-interval` периодически переприменяет его, чтобы перебить другие плагины.
- **`PLACEHOLDER_ONLY`** — **рекомендуется, если табом управляет другой плагин** (например, **FlectonePulse**). ClansMC не трогает таб, только отдаёт плейсхолдеры PlaceholderAPI. Настройте свой tab-плагин на `%clans_tag_formatted%`, например в формате таба FlectonePulse:

  ```
  %player_name% %clans_tag_formatted%
  ```

  Затем установите `tab.mode: PLACEHOLDER_ONLY` в ClansMC и позвольте FlectonePulse рисовать таб.

> **О конфликте чата:** диалог создания и клан-чат перехватывают чат с `EventPriority.LOWEST` и отменяют событие, чтобы оно не попало в общий чат. FlectonePulse тоже обрабатывает чат. Если сообщения диалога/клан-чата всё же утекают в общий чат — убедитесь, что ClansMC загружается раньше, или понизьте приоритет чата у FlectonePulse; ClansMC намеренно использует самый низкий приоритет, чтобы перехватить первым.

---

## Распределение опыта с боссов — как работает и ограничения

- Урон по **Дракону Края** и **Визеру** ведётся в журнале по каждому игроку. На смерть урон агрегируется по кланам, и **весь пул опыта достаётся одному клану — тому, кто нанёс больше всего урона**. Игроки без клана и другие кланы не получают ничего из этого пула. Если урон нанесли только игроки без клана — поведение полностью ванильное.
- Внутри клана-победителя пул делится **поровну** только между участниками, нанёсшими урон; остаток от деления идёт топ-дамагеру.
- Контрибьюторы, оффлайн на момент смерти, получают долю в `pending_xp` и забирают её при следующем входе (`boss-xp.hold-for-offline`).
- **Визер:** достаточно `setDroppedExp(0)`; выпавший опыт и есть пул.
- **Дракон Края:** `setDroppedExp(0)` **недостаточно** — дракон спавнит сферы опыта во время анимации смерти. ClansMC открывает окно подавления (`dragon.suppress-orbs-ticks`, по умолчанию 400) и отменяет спавн `ExperienceOrb` в радиусе `dragon.suppress-radius` (по умолчанию 24) блоков от точки смерти, суммируя их опыт в общий пул. При необходимости подстройте радиус/длительность.

> Лут (яйцо дракона, звёзды незера) не трогается.

---

## Совместимость

| Плагин | Примечания |
|---|---|
| **FlectonePulse** | Главная точка интеграции для чата и таба. Используйте `tab.mode: PLACEHOLDER_ONLY` и `%clans_tag_formatted%`. См. заметку о конфликте чата выше. |
| **AuthMe / FastLogin** | Имя в табе не применяется, пока игрок не авторизован (определяется через API AuthMe, рефлексией). Без AuthMe тег применяется через `tab.apply-delay-ticks`. |
| **LuckPerms** | Используется для прав вроде `clans.create`. |
| **PlaceholderAPI** | Экспансия регистрируется автоматически при наличии. |
| **Vault** | Нужен для кланового банка. Без Vault банк автоматически отключается с предупреждением в консоль. |
| **ProtocolLib / packetevents** | Не требуются и не используются. |
| **CoreProtect, GSit, ResizePlayers** | Известных конфликтов нет. |

**Известное ограничение — подсветка клана:** Bukkit API не позволяет показывать свечение только сокланевцам, поэтому включённая подсветка **видна всем**. Пер-игроковая реализация потребовала бы отправки пакетов метаданных сущностей, чего плагин не делает (возможное будущее улучшение).

---

## PlaceholderAPI

| Плейсхолдер | Значение |
|---|---|
| `%clans_tag%` | Тег клана (без цвета) |
| `%clans_tag_formatted%` | Тег с заданным цветом (строка MiniMessage) |
| `%clans_name%` | Название клана |
| `%clans_role%` | `LEADER` / `MEMBER` |
| `%clans_level%` | Уровень клана |
| `%clans_members_total%` | Всего участников |
| `%clans_members_online%` | Участников в сети |
| `%clans_bank%` | Форматированный баланс банка |
| `%clans_allies%` | Число активных союзов |

---

## Сборка из исходников

```bash
git clone https://github.com/sheynor43/ClansMC-1.21.11.git
cd ClansMC-1.21.11
./gradlew build
```

Готовый jar — `build/libs/ClansMC-<версия>.jar`. Требуется JDK 21.

## API для аддонов

```java
ClansAPI api = Bukkit.getServicesManager().load(ClansAPI.class);
api.getClanOf(player.getUniqueId()).ifPresent(clan -> {
    getLogger().info(player.getName() + " состоит в клане " + clan.name());
});
```

## Скриншоты

<!-- Вставьте свои скриншоты -->
> _Меню клана_ — `![menu](docs/menu.png)`
>
> _Информация о клане_ — `![info](docs/info.png)`
>
> _Тег в табе_ — `![tab](docs/tab.png)`

## Вклад в разработку

Issue и pull request'ы приветствуются. Придерживайтесь стиля [Conventional Commits](https://www.conventionalcommits.org/) и убедитесь, что `./gradlew build` проходит.

## Лицензия

Распространяется под [лицензией MIT](LICENSE).
