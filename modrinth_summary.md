![TimeTuner Banner](https://cdn.modrinth.com/data/cached_images/7f1168f55c04bc91e9379df2105215ebe879ee95_0.webp)

# TimeTuner - Precision Time Control for Minecraft Servers

Transform your server's day/night cycle with TimeTuner, a powerful Paper plugin giving you complete control over time progression and related mechanics. Perfect for survival servers, creative worlds, adventure maps, or any server needing customized time and world rule management.

### Why TimeTuner?
- **Custom Time Speeds**: Make days longer and nights shorter (or vice versa) globally or per world.
- **Per-World Control**: Apply different speeds and rules (sleep, explosions, thunderstorms) to different worlds.
- **Smart Sleep System**: Configurable sleep skipping (percentage/fixed count), thunderstorm sleeping control, and bed explosion prevention.
- **No Restart Required**: Reload configuration changes instantly using `/timetuner reload`.
- **Performance Friendly**: Optimized with optional auto-pause for empty worlds and efficient updates.
- **Reliable**: Includes time overflow protection and synchronization on resume.

### Standout Features

#### 🌍 World-Specific Control
- Set unique day/night speed multipliers for each world.
- Enable/disable TimeTuner management per world (`enabled: false` lets vanilla rules apply).
- Control specific rules like `allow-bed-explosions` and `allow-thunderstorm-sleep` per world.
- Pause/Resume time globally or for specific worlds.

#### 🛏️ Enhanced Sleep & World Mechanics
- Choose between player percentage or fixed player count to skip the night/storm.
- **NEW:** Configure per-world whether players can sleep through **thunderstorms** (like vanilla) or only at night via `allow-thunderstorm-sleep`.
- **NEW:** Configure per-world whether beds should **explode** (like vanilla) or be prevented from exploding via `allow-bed-explosions`.
- Automatic skip for single players (who aren't ignoring sleep).

#### ⚡ Performance & Reliability
- Auto-pause time in empty worlds to save resources (configurable via `auto-pause-empty`).
- Built-in protection against time value overflow (`overflow-protection`).
- Automatic cleanup for unloaded worlds.
- Resuming from pause correctly syncs time to prevent jumps.

### Configuration Overview
TimeTuner's `config.yml` allows detailed customization:
* **`global-speeds`**: Set default `day-speed` and `night-speed` multipliers (`1.0` = vanilla; `<1.0` = slower/longer; `>1.0` = faster/shorter).
* **`sleep`**: Configure global sleep skipping (`allow-skip`), `percentage` or fixed `required-players` needed.
* **`worlds.<world_name>`**: Override global settings per world. Set `enabled: false` to disable TimeTuner for a world. Customize `day-speed`, `night-speed`, `allow-bed-explosions`, and `allow-thunderstorm-sleep`.
* **`safety`**: Toggle `overflow-protection`.
* **`advanced`**: Adjust `tick-frequency`, enable `debug-mode`, or enable `auto-pause-empty` worlds.

### Commands
- `/timetuner help`: Show help message.
- `/timetuner reload`: Reload configurations (`config.yml` & `messages.yml`).
- `/timetuner pause [world]`: Pause time globally or in a specific world.
- `/timetuner resume [world]`: Resume time globally or in a specific world.
- `/timetuner speed <type> <speed> [world]`: Set `day`/`night`/`both` speed multiplier globally or per world.
- `/timetuner reset [world]`: Skip to day (time 0) globally or per world (clears weather if applicable).
- `/timetuner status`: Show current status for managed worlds.

*(Alias: `/tt`)*