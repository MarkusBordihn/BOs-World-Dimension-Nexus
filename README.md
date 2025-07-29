# 🌐 World Dimension Nexus

[![World Dimension Nexus Versions](http://cf.way2muchnoise.eu/versions/Minecraft_1278368_all.svg)](https://www.curseforge.com/minecraft/mc-mods/world-dimension-nexus)

[![Download on CurseForge](http://cf.way2muchnoise.eu/title/1278368.svg)](https://www.curseforge.com/minecraft/mc-mods/world-dimension-nexus)
[![CurseForge Downloads](http://cf.way2muchnoise.eu/full_1278368_downloads.svg)](https://www.curseforge.com/minecraft/mc-mods/world-dimension-nexus)

[![Download on Modrinth](https://img.shields.io/badge/dynamic/json?labelColor=black&color=grey&label=&query=title&url=https://api.modrinth.com/v2/project/nUBOuZRL&style=flat&logo=modrinth)](https://modrinth.com/mod/world-dimension-nexus)
[![Modrinth Downloads](https://img.shields.io/badge/dynamic/json?labelColor=black&color=grey&label=&suffix=%20downloads&query=downloads&url=https://api.modrinth.com/v2/project/nUBOuZRL&style=flat&logo=modrinth)](https://modrinth.com/mod/world-dimension-nexus)

[![Report an Issue](https://img.shields.io/badge/dynamic/json?label=Report%20an%20Issue%20%2F%20Bug%20%2F%20Crash%20%2F%20Feature%20Request&labelColor=black&color=grey&query=title&url=https://api.modrinth.com/v2/project/nUBOuZRL&style=flat&logo=github)][issues]

[![Wiki](https://img.shields.io/badge/dynamic/json?label=Wiki&labelColor=black&color=grey&query=title&url=https://api.modrinth.com/v2/project/nUBOuZRL&style=flat&logo=github)][wiki]
[![Support me on Ko-fi](https://img.shields.io/badge/Support_me_on_Ko--fi-!?labelColor=black&style=flat&logo=ko-fi)][ko-fi]

![World Dimension Nexus Logo][logo]

**The ultimate serverside tool for managing dimensions, worlds, and player data without requiring
any client-side installation.**

🛠️ *This mod is created as part of
the [NeoForged Serverside Summer Modjam 2025][neoforged-modjam].*

⚠️ **Beta Version Notice**: This mod is currently in beta. Features and commands may change before
the stable release.

## ✨ Key Features

### 🌍 Dimension Management

- **Multi-World Support** - Create and manage unlimited custom dimensions with various world types
- **World Import/Export** - Easy dimension sharing and migration between servers
- **Game Mode Management** - Set different game modes per dimension (creative, survival, adventure,
  spectator)
- **Cross-Dimension Safety** - Void protection, safe landing, and automatic fallback systems

### 🚀 Teleportation & Movement

- **Cross-Dimensional Teleportation** - Seamless travel between worlds with built-in safety features
- **📍 Warp System** - Create private and public teleportation points for quick travel
    - Private warps for personal bases and locations
    - Public warps for community areas and landmarks
    - Configurable limits and cooldown protection
    - Cross-dimension warp support
- **🤖 Auto-Teleport System** - Automated player movement based on events and triggers
    - First join, death, respawn triggers
    - Customizable destination coordinates
    - Event-based player routing

### 🏛️ Portal Network

- **Physical Portals** - Create visible gateways between dimensions
- **Portal Management** - Bind and configure portal connections
- **Visual Portal System** - Interactive portal blocks for immersive travel

### ⚙️ Server Administration

- **Fully Serverside** - Works with vanilla clients - no client mods required
- **Comprehensive Commands** - Intuitive command system with tab completion
- **Flexible Configuration** - Extensive config options for all systems
- **Permission Integration** - Supports server permission levels and moderation tools

## 🚀 Quick Start (Demo)

This quick start guide will show you a limited set of commands to get you started with the
World Dimension Nexus mod.

Please use a test world to try out the commands and features before using them in your main world.

### 🌍 Dimension Setup

The following commands will help you create and manage dimensions:

```bash
# Import a new dimension like lobby and fishing
/wdn dimension import lobby_dimension.wdn
/wdn dimension import fishing_dimension.wdn

# Optionally, create a new dimension with a preset
/wdn dimension create my_skyblock skyblock

# List all dimensions
/wdn dimension list
```

### 🚀 Teleportation & Auto-Teleport Workflow

Set up automated player movement between dimensions using auto-teleport rules:

```bash
# Teleport to a dimension
/wdn teleport dimension world_dimension_nexus:my_skyblock

# Set up auto-teleport rules
/wdn autoteleport add always world_dimension_nexus:lobby_dimension      # 🏠 Send new players to lobby
/wdn autoteleport add on_death world_dimension_nexus:fishing_dimension  # 🎣 Send players to fishing on death

# List all auto-teleport rules
/wdn autoteleport list
```

After running these commands:

- 🏠 Players will be sent to the lobby dimension when they join for the first time.
- 🎣 Players will be sent to the fishing dimension when they die.

💡 *Tip: You can customize triggers, target dimensions, and even coordinates!
See* [Auto-Teleport Commands][commands-auto-teleport] *for advanced options.*

Note: The hot-creation of dimensions is limited, so you might need to restart the server
to get the dimensions fully functional with all features and entities.

### 🏛️ Portal Setup

Create and manage portals between dimensions:

```bash
# Teleport to the fishing dimension (if not already in the dimension)
/wdn teleport dimension world_dimension_nexus:fishing_dimension

# Use a good location for the portal, e.g. at one of the trees
/wdn portal create unbound green 43 49 20

# Teleport to the lobby dimension
/wdn teleport dimension world_dimension_nexus:lobby_dimension

# Use a good location for the portal, e.g. in the woods
/wdn portal create unbound green -36 9 -27
```

After running these commands, you will have two portals created in the lobby and fishing.
You can now use the portals to teleport between the dimensions.

## 🎯 Use Cases

- **Creative Servers** - Separate building worlds with different themes
- **Survival Networks** - Multiple survival worlds with different difficulties
- **Minigame Servers** - Dedicated arenas and lobbies
- **Hub Servers** - Central lobby connecting to various game modes
- **Event Hosting** - Temporary dimensions for special occasions

## 📚 Documentation

📖 **[Complete Wiki Documentation][wiki]** - Comprehensive guides and tutorials

### Quick Links

- [⚡ Quick Start Guide][quick-start] - Get started in 5 minutes
- [🌍 Dimension Commands][commands-dimension] - Create and manage dimensions
- [🚀 Teleport Commands][commands-teleport] - Player teleportation
- [🤖 Auto-Teleport Commands][commands-auto-teleport] - Automated rules
- [🏗️ Example Tutorials][examples-creative] - Step-by-step setups

## 🤝 Contributing

We welcome contributions! Please:

1. Check existing [issues][issues] and [pull requests][pull-requests]
2. Follow the existing code style and conventions
3. Test your changes thoroughly
4. Update documentation as needed

## 🐛 Support

- **Bug Reports**: [GitHub Issues][issues]
- **Feature Requests**: [GitHub Discussions][discussions]
- **Documentation**: [Wiki][wiki-home]
- **Troubleshooting**: [Debug Guide][debug-guide]

## 📜 License

This project is open source under the [MIT License][license].

**Ready to get started?** Check out the [Quick Start Guide][quick-start] to create your
first dimension in minutes!

[ko-fi]: https://ko-fi.com/Kaworru

[wiki]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/wiki

[wiki-home]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/wiki/Home

[issues]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/issues

[pull-requests]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/pulls

[discussions]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/discussions

[license]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/blob/main/LICENSE.md

[neoforged-modjam]: https://neoforged.net/news/2025serversidesummer/

[quick-start]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/wiki/Quick-Start

[commands-dimension]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/wiki/Commands-Dimension

[commands-teleport]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/wiki/Commands-Teleport

[commands-auto-teleport]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/wiki/Commands-Auto-Teleport

[examples-creative]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/wiki/Examples-Creative-World

[debug-guide]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/wiki/Commands-Debug

[logo]: https://github.com/MarkusBordihn/BOs-World-Dimension-Nexus/wiki/images/logo_header.png
