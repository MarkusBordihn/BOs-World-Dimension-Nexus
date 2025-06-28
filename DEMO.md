# Demo

This file shows some examples of how to use the World Dimension Nexus mod.

## Creating a New Dimension

### Basic Dimension Creation

To create a new dimension with default settings, you can use the `/wdn dimension create` command:

```
/wdn dimension create my_new_dimension
```

### Creating Dimensions with Specific Types

You can now create dimensions with specific chunk generator types by adding the type parameter:

```
/wdn dimension create <n> <type>
```

**Available Chunk Generator Types:**

- `flat` - Standard flat world (default)
- `void` - Empty void world
- `lobby` - Lobby-style world with barrier blocks
- `skyblock` - Minimal world for skyblock gameplay
- `cave` - Underground cave world
- `noise` - Standard noise-based terrain generation
- `debug` - Debug world with all blocks
- `floating_islands` - End-like floating islands
- `amplified` - Amplified terrain generation
- `custom` - Custom configuration-based generation

**Examples:**

```bash
# Create a void dimension for building
/wdn dimension create void_world void

# Create a lobby dimension
/wdn dimension create server_lobby lobby

# Create a skyblock dimension
/wdn dimension create skyblock_survival skyblock

# Create a cave dimension
/wdn dimension create underground_world cave

# Create floating islands
/wdn dimension create sky_islands floating_islands
```

### Managing Chunk Generator Types

**List all available types:**

```
/wdn dimension types
```

**View loaded worldgen configurations:**

```
/wdn dimension worldgen list
```

**Reload worldgen configurations:**

```
/wdn dimension worldgen reload
```

## Dimension Management

### List Dimensions

To list all available dimensions:

```
/wdn dimension list
```

### Remove Dimensions

To remove a dimension from the server (data remains):

```
/wdn dimension remove <n>
```

### Dimension Information

To get detailed information about a dimension:

```
/wdn dimension info <n>
```

### Teleporting Between Dimensions

**Teleport yourself:**

```
/wdn teleport dimension <n>
```

**Teleport another player:**

```
/wdn teleport dimension <n> <player>
```

**Teleport back to previous location:**

```
/wdn teleport back
```

**Teleport another player back:**

```
/wdn teleport back <player>
```

**Using vanilla execute command:**

```
/execute in world_dimension_nexus:my_new_dimension run tp @p ~ ~ ~
```

## Advanced Features

### Auto-Teleport

Set auto-teleport on join:

```
/wdn dimension autoteleport <n>
```

Clear auto-teleport:

```
/wdn dimension autoteleport off
```

### Import/Export Dimensions

**Export a dimension:**

```
/wdn dimension export <dimension>
```

**Import a dimension:**

```
/wdn dimension import <file>
/wdn dimension import <file> <dimension_id>
/wdn dimension import <file> <dimension_id> <dimension_type_id>
```

## Custom Worldgen Configuration

You can create custom worldgen configurations by placing JSON files in:
`Common/src/main/resources/data/world_dimension_nexus/worldgen/`

**Example configuration file (custom_type.json):**

```json
{
  "type": "custom_type",
  "custom_settings": {
    "layers": [
      {"height": 1, "block": "minecraft:bedrock"},
      {"height": 50, "block": "minecraft:stone"},
      {"height": 5, "block": "minecraft:dirt"},
      {"height": 1, "block": "minecraft:grass_block"}
    ],
    "biome": "minecraft:plains",
    "structures": ["minecraft:villages"],
    "lakes": true
  }
}
```

After adding new configurations, reload them with:

```
/wdn dimension worldgen reload
```

## Portal Blocks

To create a portal block that allows players to teleport between dimensions, you can use the
special portal block provided by the World Dimension Nexus mod.

Just place colored wool blocks in a 5x4 rectangle and place diamond blocks in the corners.
This will create a portal that players can use to teleport between dimensions.

## Permission Levels

Commands require different permission levels:

- **Moderators** (Level 2): `list`, `info`, `teleport`, `types`
- **Gamemasters** (Level 3): `create`
- **Admins** (Level 4): `remove`, `autoteleport`, `worldgen`
- **Owners** (Level 5): `export`, `import`

## Examples Showcase

### Building Server Setup

```bash
# Create a lobby dimension
/wdn dimension create spawn_lobby lobby

# Create building worlds
/wdn dimension create creative_flat flat
/wdn dimension create creative_void void

# Set auto-teleport to lobby
/wdn dimension autoteleport spawn_lobby
```

### Survival Server Setup

```bash
# Create different survival worlds
/wdn dimension create survival_overworld noise
/wdn dimension create skyblock_challenge skyblock
/wdn dimension create cave_adventure cave
/wdn dimension create floating_realm floating_islands

# List all dimensions
/wdn dimension list

# Teleport players to different worlds
/wdn teleport dimension skyblock_challenge Steve
/wdn teleport dimension cave_adventure Alex
```

### Testing New Types

```bash
# Check available types
/wdn dimension types

# Test different generators
/wdn dimension create test_flat flat
/wdn dimension create test_void void
/wdn dimension create test_debug debug

# Check configurations
/wdn dimension worldgen list

# Clean up test dimensions
/wdn dimension remove test_flat
/wdn dimension remove test_void
/wdn dimension remove test_debug
```

# World Dimension Nexus - Demo Commands

This document demonstrates the available commands for the World Dimension Nexus mod.

## Auto-Teleport Commands

The auto-teleport system allows you to configure automatic teleportation for players based on
various triggers.

### Command Syntax

All auto-teleport commands require operator permissions (level 2).

```
/wdn autoteleport <subcommand> [arguments]
```

### Available Triggers

- `always` - Teleport on every login (no persistent data)
- `daily` - Teleport once per day (persistent data)
- `weekly` - Teleport once per week (persistent data)
- `monthly` - Teleport once per month (persistent data)
- `join` - Teleport once per server join session (persistent data)
- `restart` - Teleport once after server restart (no persistent data)

### Subcommands

#### Add Auto-Teleport

Adds a new auto-teleport configuration for a player.

```
/wdn autoteleport add <player> <trigger> <dimension> <x> <y> <z>
```

**Examples:**

```bash
# Player always teleports to spawn on login
/wdn autoteleport add Steve always minecraft:overworld 0 64 0

# Player teleports to nether base once per day
/wdn autoteleport add Alex daily minecraft:nether 100 64 200

# Player teleports to end city once per week
/wdn autoteleport add Bob weekly minecraft:the_end 1000 50 1000

# Player teleports to marketplace on server join
/wdn autoteleport add Alice join minecraft:overworld 500 65 -300
```

#### Remove Auto-Teleport

Removes a specific auto-teleport trigger for a player.

```
/wdn autoteleport remove <player> <trigger>
```

**Examples:**

```bash
# Remove daily teleport for Alex
/wdn autoteleport remove Alex daily

# Remove always teleport for Steve
/wdn autoteleport remove Steve always
```

#### Clear All Auto-Teleports

Removes all auto-teleport configurations for a player.

```
/wdn autoteleport clear <player>
```

**Examples:**

```bash
# Clear all auto-teleports for Bob
/wdn autoteleport clear Bob
```

#### List Auto-Teleports

Shows all configured auto-teleports for a player.

```
/wdn autoteleport list <player>
```

**Examples:**

```bash
# List all auto-teleports for Alice
/wdn autoteleport list Alice
```

**Sample Output:**

```
Auto-teleports for Alice:
• Daily → minecraft:nether at 100.0 64.0 200.0
• On Join → minecraft:overworld at 500.0 65.0 -300.0
• Weekly → minecraft:the_end at 1000.0 50.0 1000.0
```

### Auto-Teleport (Global Rules)

**Auto-teleport applies to ALL players but is tracked individually per player.**

**Add a global auto-teleport rule:**

```
/wdn autoteleport add <trigger> <dimension> <x> <y> <z>
```

**Available triggers:**

- `always` - Teleport every time player joins
- `daily` - Teleport once per day
- `weekly` - Teleport once per week
- `monthly` - Teleport once per month
- `join` - Teleport once per server join session
- `restart` - Teleport once after each server restart

**Supported dimensions:**

- Standard: `minecraft:overworld`, `minecraft:nether`, `minecraft:the_end`
- Custom: `worlddimensionnexus:<dimension_name>` (all your custom dimensions)

**Examples:**

```bash
# Teleport all players to spawn on server restart
/wdn autoteleport add restart minecraft:overworld 0 64 0

# Teleport all players to a custom lobby daily
/wdn autoteleport add daily worlddimensionnexus:lobby 10 65 10

# Always teleport new players to tutorial
/wdn autoteleport add join worlddimensionnexus:tutorial 0 100 0
```

**Remove an auto-teleport rule:**

```
/wdn autoteleport remove <trigger>
```

**List all active auto-teleport rules:**

```
/wdn autoteleport list
```

**Clear all auto-teleport rules:**

```
/wdn autoteleport clear
```

**How it works:**

- Rules are set globally for ALL players
- Each player's trigger history is tracked individually
- Example: If you set `daily` trigger, each player will be teleported once per day
- Players who join later will still get teleported according to their individual schedule

```
