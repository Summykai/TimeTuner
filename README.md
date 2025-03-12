# TimeTuner - Precision Time Control for Minecraft Servers

![TimeTuner Banner](images/TimeTuner-Banner.jpg)

A Minecraft plugin for Paper servers (1.21.4+) offering granular control over world time progression.

## Key Features

- **Multi-World Time Control**: Independent day/night speed settings per world
- **Real-Time Adjustments**: Modify speeds without server restart
- **Advanced Time Mathematics**: Fractional tick accumulation system
- **Smart Auto-Pause**: Optional automatic pausing when server empty
- **Overflow Protection**: 72-day cycle reset mechanism
- **Modern Permissions**: Hierarchical permission system with OP defaults
- **Localized Messages**: Fully customizable user feedback
- **Debug Mode**: Detailed logging for troubleshooting
- **Atomic Reloads**: Safe configuration reloading
- **World Load Detection**: Automatic management of new worlds

## Architecture Overview

```
TimeTuner/
├── src/
│   └── main/
│       ├── java/
│       │   └── me/
│       │       └── summykai/
│       │           └── timetuner/
│       │               ├── commands/
│       │               │   ├── CommandManager.java        # Command implementations
│       │               │   └── TimeTunerCommandExecutor.java # Command router
│       │               ├── listeners/
│       │               │   └── WorldListener.java         # World load/unload handler
│       │               ├── time/
│       │               │   ├── Time.java                  # Time mathematics core
│       │               │   ├── TimeAdjuster.java          # Safe time updates
│       │               │   └── WorldTimeManager.java      # Per-world controller
│       │               ├── utils/
│       │               │   ├── ErrorHandler.java          # Error reporting system
│       │               │   └── MessageManager.java        # Localized messaging
│       │               └── TimeTuner.java                 # Plugin bootstrap
│       └── resources/
│           ├── config.yml         # Speed settings
│           ├── messages.yml       # User messages
│           └── plugin.yml         # Plugin metadata
└── pom.xml                        # Build configuration
```

## Core Components

### 1. Time Management Engine
- **Time.java**: Mathematical model for fractional tick accumulation
  - Handles time comparisons and phase detection
  - Provides overflow-safe arithmetic operations
- **WorldTimeManager.java**: Per-world time controller
  - Manages time progression states (paused/running)
  - Implements speed multipliers for day/night phases
  - Syncs with world GameRules (doDaylightCycle)

### 2. Command System
- **CommandManager.java**: Business logic for all commands
  - Validates permissions and input parameters
  - Handles configuration updates and world modifications
- **TimeTunerCommandExecutor.java**: Command routing and tab completion
  - Maps commands to permissions
  - Provides context-aware suggestions

### 3. Support Infrastructure
- **WorldListener.java**: World lifecycle handler
  - Auto-initializes managers for new worlds
  - Cleans up unloaded worlds to prevent memory leaks
- **MessageManager.java**: User communication system
  - Loads messages from YAML with placeholder support
  - Uses Adventure API for modern text formatting
- **ErrorHandler.java**: Centralized error management
  - Logs errors to console with plugin context
  - Provides user-friendly feedback messages

## Configuration Guide

### config.yml
```yaml
# Base speeds (1.0 = vanilla speed)
day-speed: 1.5
night-speed: 0.75

# World-specific configurations
worlds:
  world_overworld:
    day-speed: 1.2      # Overrides global day speed
    night-speed: 0.8    # Overrides global night speed
    enabled: true       # Enable time control
  world_nether:
    enabled: false      # Leave vanilla behavior

# Safety systems
overflow-protection: true  # Prevent long-world-time issues
auto-pause-empty: false    # Pause when no players online

# Performance
tick-frequency: 1        # Update interval (1-20 ticks)
debug-mode: false         # Enable diagnostic logging
```

### messages.yml
```yaml
errors:
  no-permission: "&cYou lack permission for this action!"
  invalid-world: "&cWorld '{world}' not found!"
  
commands:
  speed:
    success: "&aGlobal speeds set: Day={day}, Night={night}"
    usage: "&eUsage: /timetuner speed <day> <night>"
```

## Command Reference

| Command | Description | Permission | Aliases |
|---------|-------------|------------|---------|
| `/timetuner reload` | Reload configurations | timetuner.reload | `/tt reload` |
| `/timetuner pause` | Toggle time freezing | timetuner.pause | `/tt pause` |
| `/timetuner speed <day> <night>` | Set global speeds | timetuner.speed | `/tt speed` |
| `/timetuner worldspeed <world> <day> <night>` | Set world-specific speeds | timetuner.worldspeed | `/tt worldspeed` |
| `/timetuner status` | Show current settings | timetuner.use | `/tt status` |

## Installation

1. **Requirements**
   - Paper 1.21.4+ Server
   - Java 21 JRE

2. **Installation Steps**
   ```bash
   # Download latest release
   wget https://example.com/TimeTuner.jar -O plugins/TimeTuner.jar
   
   # Restart server
   systemctl restart minecraft
   ```

3. **Initial Setup**
   ```yaml
   # Edit plugins/TimeTuner/config.yml
   day-speed: 1.0
   night-speed: 1.0
   ```

## Production Features

- **Memory Safe**: Automatic cleanup of unloaded worlds
- **Input Validation**: Rejects negative/NAN speed values
- **Atomic Operations**: Config changes apply atomically
- **Tick Optimization**: Adjustable processing frequency
- **Multi-World Sync**: Handles world loading/unloading
- **Safe Arithmetic**: Prevents long-world-time overflow
- **Diagnostic Logging**: Debug mode for troubleshooting

## Development

```bash
# Clone repository
git clone https://github.com/Summykai/TimeTuner.git

# Build plugin
mvn clean package

# Install to server
cp target/TimeTuner.jar /path/to/plugins/
```

## License
MIT License - See [LICENSE](https://github.com/Summykai/TimeTuner/blob/main/LICENSE)