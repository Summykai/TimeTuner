# TimeTuner Changelog

---

## Version 1.2.0 (2025-04-08)

### Features
- **Thunderstorm Sleep:** Players can now sleep to skip thunderstorms, even during the day, if enabled via the new `allow-thunderstorm-sleep` world configuration option.
  - Configurable per-world in `config.yml` under the `worlds.<world_name>` section. Defaults to `true`.
- **Weather Clearing on Sleep Skip:** When enough players sleep to skip the night (or a thunderstorm), the weather in that world will now automatically be cleared (both rain/storm and thunder).

### Improvements
- **Sleep Logic Robustness:**
  - Sleep checks now correctly ignore players with the `isSleepingIgnored` flag (e.g., players in spectator mode).
  - Sleep skip checks are now scheduled 1 tick after bed entry to ensure player state is consistent.
  - Refined logic for handling single-player sleep skips.
  - Improved debug logging for sleep events, including reasons for denial and success details.
- **Bed Explosion Handling:** The `allow-bed-explosions` check now correctly prevents bed *interaction* in dimensions where explosions are disallowed, rather than just skipping sleep logic after interaction.
- **World Time Management:**
  - Optimized time update logic and caching in `WorldTimeManager`.
  - When resuming a paused world, the internal time tracker now correctly syncs with the actual world time to prevent sudden jumps.
- **Configuration:** Added comments in `config.yml` explaining the new world options (`allow-bed-explosions` and `allow-thunderstorm-sleep`).

### Fixes
- Player state changes (leaving bed, quitting, changing worlds) now more reliably remove players from the internal sleeping player cache.
