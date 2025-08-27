# TimeTuner - Precision Time Control for Minecraft Servers

![TimeTuner Banner](images/TimeTuner-Banner.jpg)

## About

This is the source code repository for the TimeTuner Minecraft plugin, a Paper server plugin that provides granular control over world time progression and sleep mechanics. This repository is public for development purposes and to allow users to:

- Report issues
- Contribute to development
- Understand how the plugin works

## Features

### Core Features

- **Custom Time Speeds**: Set different speeds for day and night globally or per world.
- **Per-World Control**: Configure time speeds, specific sleep rules (like bed explosions, thunderstorm sleeping), and enable/disable TimeTuner management for each world individually.
- **Pause/Resume**: Freeze time progression in specific managed worlds or globally.

### Sleep & World Mechanics

- **Flexible Night Skipping**: Choose between percentage-based or fixed player count requirements for skipping the night or thunderstorms.
- **Configurable Thunderstorm Sleeping**: Enable or disable the vanilla behavior allowing players to sleep during thunderstorms on a per-world basis (defaults to enabled).
- **Configurable Bed Explosions**: Prevent beds from exploding in dimensions like the Nether or End, configurable per world (defaults to vanilla behavior).
- **Single Player Optimization**: Automatic night/storm skip when only one player (who isn't ignoring sleep) is online in a world.

### Safety & Reliability

- **Overflow Protection**: Prevents potential time-related issues on servers with extremely high uptime by managing the underlying time values.
- **Memory Management**: Automatic cleanup of data for unloaded worlds.
- **Input Validation**: Rejects invalid command inputs like non-numeric speeds or invalid types.
- **Pause Synchronization**: Resuming time correctly syncs with the current world time to avoid jumps if time was changed externally while paused.

## Getting Started

### Configuration

Edit `plugins/TimeTuner/config.yml` to customize settings. The available options are:

**`global-speeds`**: Defines default time speeds used unless overridden in a specific world.

- `day-speed`: (Number) Default multiplier for daytime speed. `1.0` = normal vanilla speed. Values `< 1.0` make the day longer (slower progression), values `> 1.0` make the day shorter (faster progression).
- `night-speed`: (Number) Default multiplier for nighttime speed. `1.0` = normal vanilla speed. Values `< 1.0` make the night longer, values `> 1.0` make the night shorter.

**`sleep`**: Global settings related to skipping the night or thunderstorms.

- `allow-skip`: (`true`/`false`) Globally enables or disables the sleep skipping functionality.
- `percentage`: (Decimal, e.g., `0.5`) The fraction (0.0 to 1.0) of online (non-ignored) players required to sleep for a skip, used if `use-required-players` is `false`.
- `use-required-players`: (`true`/`false`) If `true`, the plugin uses the fixed `required-players` count instead of the `percentage`.
- `required-players`: (Integer) The absolute number of players required to sleep for a skip, used if `use-required-players` is `true`.

**`worlds`**: Contains subsections for each world where you want to override global settings.

- `<world_name>`: (e.g., `world`, `world_nether`) Create a section named after the specific world folder.
  - `enabled`: (`true`/`false`) Set to `false` to completely disable TimeTuner management (time speed, sleep rules) for this world. Vanilla gamerules will apply.
  - `day-speed`: (Number) Overrides the `global-speeds.day-speed` specifically for this world.
  - `night-speed`: (Number) Overrides the `global-speeds.night-speed` specifically for this world.
  - `allow-bed-explosions`: (`true`/`false`) If `false`, prevents beds from exploding in this world (useful for Nether/End). Defaults to `true` (vanilla behavior).
  - `allow-thunderstorm-sleep`: (`true`/`false`) If `true`, allows players to sleep during thunderstorms (vanilla behavior). If `false`, sleep is only possible at night. Defaults to `true`.

**`safety`**: Settings related to plugin stability.

- `overflow-protection`: (`true`/`false`) Recommended `true`. Helps prevent issues related to Minecraft's internal `fullTime` counter on servers with very long uptime.

**`advanced`**: Settings for fine-tuning and debugging.

- `tick-frequency`: (Integer, >= 1) How often, in server ticks, the plugin updates world time. `1` provides the smoothest time flow. Higher values update less frequently.
- `debug-mode`: (`true`/`false`) Enables detailed logging in the server console, useful for troubleshooting.
- `auto-pause-empty`: (`true`/`false`) If `true`, time progression automatically pauses in managed worlds when they have no players and resumes when a player enters.

## Commands

| Command                          | Description                                                                         |
|----------------------------------|-------------------------------------------------------------------------------------|
| `/timetuner help`                | Show help message listing available commands.                                       |
| `/timetuner reload`              | Reload the plugin's `config.yml` and `messages.yml`.                                |
| `/timetuner pause [world]`       | Pause time progression in all managed worlds or just the specified `[world]`.         |
| `/timetuner resume [world]`      | Resume time progression in all managed worlds or just the specified `[world]`.        |
| `/timetuner speed <type> <speed> [world]` | Set time speed multiplier for `day`, `night`, or `both` to `<speed>` globally or only for `[world]`. |
| `/timetuner reset [world]`       | Instantly skip to the start of the day (time 0) in all managed worlds or just `[world]`. Clears weather if skipping night/storm. |
| `/timetuner status`              | Show current time, configured speeds, and paused status for all managed worlds.     |

## Directory Structure

```
TimeTuner/
├── src/
│   └── main/
│       ├── java/
│       │   └── me/
│       │       └── summykai/
│       │           └── timetuner/
│       │               ├── commands/
│       │               ├── listeners/
│       │               ├── time/
│       │               ├── utils/
│       │               └── TimeTuner.java
│       └── resources/
│           ├── config.yml
│           ├── messages.yml
│           └── plugin.yml
└── pom.xml
```

## License

MIT License - See [LICENSE](https://github.com/Summykai/TimeTuner/blob/main/LICENSE)
