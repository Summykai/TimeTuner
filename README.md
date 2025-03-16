# TimeTuner - Precision Time Control for Minecraft Servers

![TimeTuner Banner](images/TimeTuner-Banner.jpg)

## About
This is the source code repository for the TimeTuner Minecraft plugin, a Paper server plugin that provides granular control over world time progression and sleep mechanics. This repository is public for development purposes and to allow users to:
- Report issues
- Contribute to development
- Understand how the plugin works

## Features

### Core Features
- **Custom Time Speeds**: Set different speeds for day and night
- **Per-World Control**: Configure time settings for each world individually
- **Pause/Resume**: Freeze time in specific worlds or globally

### Sleep Mechanics
- **Flexible Night Skipping**: Choose between percentage-based or fixed player count requirements
- **Single Player Optimization**: Automatic night skip when alone
- **World-Specific Rules**: Customize sleep settings per world

### Safety & Reliability
- **Overflow Protection**: Prevent time-related crashes and bugs
- **Memory Management**: Automatic cleanup of unloaded worlds
- **Input Validation**: Reject invalid speed configurations

## Getting Started

### Configuration
Edit `plugins/TimeTuner/config.yml` to customize settings. Key options include:

```yaml
global-speeds:
  day-speed: 0.5
  night-speed: 1.0

sleep:
  allow-skip: true
  percentage: 0.50
  use-required-players: false
  required-players: 3

safety:
  overflow-protection: true
  precision-mode: true

advanced:
  tick-frequency: 1
  debug-mode: false
  auto-pause-empty: false
```

## Commands
| Command | Description |
|---------|-------------|
| `/timetuner help` | Show help message |
| `/timetuner reload` | Reload configurations |
| `/timetuner pause [world]` | Pause time in all or specific world |
| `/timetuner resume [world]` | Resume time in all or specific world |
| `/timetuner speed <day> <night>` | Set global speeds |
| `/timetuner status` | Show current settings |

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