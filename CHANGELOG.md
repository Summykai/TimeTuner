# TimeTuner Changelog

---

## Version 1.1.0 (2025-03-16)

### Overview
Sleep mechanics, time progression fixes, and improved configuration management

### Features
#### Sleep Skip System
- Configure how many players must sleep to skip night
  - Percentage-based (default: 50%) or fixed player count thresholds
  - Auto-skip for single players
  - World-specific tracking
  - Configurable in `sleep` section in config.yml

### Fixes
#### Time Progression
- Fractional time increments now correctly applied
  - Critical fix for worlds using non-integer speed multipliers (e.g., default 0.5)
  - Removed premature casting to `long` in time delta calculations

#### World States
- Paused worlds stay paused through plugin reloads

#### Command Messages
- All messages now use proper hierarchical keys

### Configuration
#### Structure
```yaml
global-speeds:   # Base multipliers for all worlds
  day-speed: 0.5
  night-speed: 1.0

sleep:           # Night skip mechanics
  allow-skip: true
  percentage: 0.50
  use-required-players: false
  required-players: 3

safety:          # Protection features
  overflow-protection: true
  precision-mode: true

advanced:        # Technical settings
  tick-frequency: 1
  debug-mode: false
  auto-pause-empty: false
```

### Commands & Permissions
#### Documentation
- plugin.yml now shows only implemented commands
  - Added: `resume` command
  - Removed: references to unimplemented `worlds` and `worldspeed` commands

#### Status Command
- Shows more detailed world information
  - Current day/night speeds
  - Day/night state
  - Paused status
  - Current world time

#### Permissions
- Added: `timetuner.resume` permission
- Removed: `timetuner.worldspeed` permission

### Technical Improvements
#### Caching
- Time check caching with configurable intervals
- Config reload cooldown

#### World Management
- New `WorldConfig` class for better settings management
- Improved initialization and cleanup

#### Messaging
- Added world-wide broadcast capability
- Standardized message formatting

#### Code Quality
- Better separation of concerns
- Improved type safety
- Enhanced overflow protection

---