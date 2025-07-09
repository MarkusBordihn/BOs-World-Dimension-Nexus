/*
 * Copyright 2025 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.worlddimensionnexus.config;

import de.markusbordihn.worlddimensionnexus.data.block.BlockRegistry;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalType;
import java.io.File;
import java.util.Properties;
import net.minecraft.world.level.block.Block;

@SuppressWarnings({"java:S1104", "java:S1444", "java:S3008"})
public class PortalConfig extends Config {

  public static final String CONFIG_FILE_NAME = "portal.cfg";
  public static final String CONFIG_FILE_HEADER =
"""
Portal Configuration
This file contains the configuration for portals, including teleport delays, cooldowns, and effects.

Portal Types Configuration:
- Player portals are bound to specific players and can only link to portals from the same creator
- World portals are bound to the same dimension and can only link within that dimension
- Unbound portals can link to any other unbound portal across dimensions
- Event portals are one-way teleports with no return portal (moderator only)

Corner Block Configuration:
You can change the corner blocks for each portal type to match your server requirements.
Use minecraft block IDs (e.g., minecraft:diamond_block, minecraft:emerald_block)

""";

  // Teleport timing settings (in ticks, 20 ticks = 1 second)
  public static int TELEPORT_DELAY = 20 * 3; // 3 seconds before teleport
  public static int TELEPORT_COOLDOWN = 20 * 2; // 2 seconds cooldown after teleport
  public static int CONFUSION_DURATION = 20 * 5; // 5 seconds of confusion effect

  // Portal effect settings
  public static int PORTAL_PARTICLE_COUNT = 30;
  public static double PARTICLE_OFFSET_Y = 1.0;
  public static double PARTICLE_SPREAD_XZ = 0.5;
  public static double PARTICLE_SPREAD_Y = 1.0;
  public static double PARTICLE_SPEED = 0.1;

  // Portal sound settings
  public static float SOUND_VOLUME = 1.0F;
  public static float SOUND_PITCH = 1.0F;

  // Auto-Link settings
  public static boolean AUTO_LINK_PORTALS = true;
  public static boolean AUTO_LINK_ACROSS_DIMENSIONS = true;

  // Portal type activation settings
  public static boolean ENABLE_PLAYER_PORTALS = true;
  public static boolean ENABLE_WORLD_PORTALS = true;
  public static boolean ENABLE_UNBOUND_PORTALS = true;
  public static boolean ENABLE_EVENT_PORTALS = true;

  // Portal corner block settings (using block names for config compatibility)
  public static String PLAYER_PORTAL_CORNER_BLOCK = "minecraft:diamond_block";
  public static String WORLD_PORTAL_CORNER_BLOCK = "minecraft:emerald_block";
  public static String UNBOUND_PORTAL_CORNER_BLOCK = "minecraft:netherite_block";
  public static String EVENT_PORTAL_CORNER_BLOCK = "minecraft:beacon";

  // Portal limits configuration
  public static int PLAYER_PORTAL_MAX_LINKS = 2;
  public static int WORLD_PORTAL_MAX_LINKS = 2;
  public static int UNBOUND_PORTAL_MAX_LINKS = 2;
  public static int EVENT_PORTAL_MAX_LINKS = -1; // -1 means unlimited

  public static void registerConfig() {
    registerConfigFile(CONFIG_FILE_NAME, CONFIG_FILE_HEADER);
    parseConfigFile();
  }

  public static void parseConfigFile() {
    File configFile = getConfigFile(CONFIG_FILE_NAME);
    Properties properties = readConfigFile(configFile);
    Properties unmodifiedProperties = (Properties) properties.clone();

    // Teleport timing settings
    TELEPORT_DELAY = parseConfigValue(properties, "Portal:TeleportDelay", TELEPORT_DELAY);
    TELEPORT_COOLDOWN = parseConfigValue(properties, "Portal:TeleportCooldown", TELEPORT_COOLDOWN);
    CONFUSION_DURATION =
        parseConfigValue(properties, "Portal:ConfusionDuration", CONFUSION_DURATION);

    // Portal effect settings
    PORTAL_PARTICLE_COUNT =
        parseConfigValue(properties, "Portal:ParticleCount", PORTAL_PARTICLE_COUNT);
    PARTICLE_OFFSET_Y = parseConfigValue(properties, "Portal:ParticleOffsetY", PARTICLE_OFFSET_Y);
    PARTICLE_SPREAD_XZ =
        parseConfigValue(properties, "Portal:ParticleSpreadXZ", PARTICLE_SPREAD_XZ);
    PARTICLE_SPREAD_Y = parseConfigValue(properties, "Portal:ParticleSpreadY", PARTICLE_SPREAD_Y);
    PARTICLE_SPEED = parseConfigValue(properties, "Portal:ParticleSpeed", PARTICLE_SPEED);

    // Portal sound settings
    SOUND_VOLUME = parseConfigValue(properties, "Portal:SoundVolume", SOUND_VOLUME);
    SOUND_PITCH = parseConfigValue(properties, "Portal:SoundPitch", SOUND_PITCH);

    // Auto-Link settings
    AUTO_LINK_PORTALS = parseConfigValue(properties, "Portal:AutoLinkPortals", AUTO_LINK_PORTALS);
    AUTO_LINK_ACROSS_DIMENSIONS =
        parseConfigValue(
            properties, "Portal:AutoLinkAcrossDimensions", AUTO_LINK_ACROSS_DIMENSIONS);

    // Portal type activation settings
    ENABLE_PLAYER_PORTALS =
        parseConfigValue(properties, "PortalTypes:EnablePlayerPortals", ENABLE_PLAYER_PORTALS);
    ENABLE_WORLD_PORTALS =
        parseConfigValue(properties, "PortalTypes:EnableWorldPortals", ENABLE_WORLD_PORTALS);
    ENABLE_UNBOUND_PORTALS =
        parseConfigValue(properties, "PortalTypes:EnableUnboundPortals", ENABLE_UNBOUND_PORTALS);
    ENABLE_EVENT_PORTALS =
        parseConfigValue(properties, "PortalTypes:EnableEventPortals", ENABLE_EVENT_PORTALS);

    // Portal corner block settings
    PLAYER_PORTAL_CORNER_BLOCK =
        parseConfigValue(
            properties, "PortalBlocks:PlayerPortalCornerBlock", PLAYER_PORTAL_CORNER_BLOCK);
    WORLD_PORTAL_CORNER_BLOCK =
        parseConfigValue(
            properties, "PortalBlocks:WorldPortalCornerBlock", WORLD_PORTAL_CORNER_BLOCK);
    UNBOUND_PORTAL_CORNER_BLOCK =
        parseConfigValue(
            properties, "PortalBlocks:UnboundPortalCornerBlock", UNBOUND_PORTAL_CORNER_BLOCK);
    EVENT_PORTAL_CORNER_BLOCK =
        parseConfigValue(
            properties, "PortalBlocks:EventPortalCornerBlock", EVENT_PORTAL_CORNER_BLOCK);

    // Portal limits configuration
    PLAYER_PORTAL_MAX_LINKS =
        parseConfigValue(properties, "PortalLimits:PlayerPortalMaxLinks", PLAYER_PORTAL_MAX_LINKS);
    WORLD_PORTAL_MAX_LINKS =
        parseConfigValue(properties, "PortalLimits:WorldPortalMaxLinks", WORLD_PORTAL_MAX_LINKS);
    UNBOUND_PORTAL_MAX_LINKS =
        parseConfigValue(
            properties, "PortalLimits:UnboundPortalMaxLinks", UNBOUND_PORTAL_MAX_LINKS);
    EVENT_PORTAL_MAX_LINKS =
        parseConfigValue(properties, "PortalLimits:EventPortalMaxLinks", EVENT_PORTAL_MAX_LINKS);

    // Update config file if needed
    updateConfigFileIfChanged(configFile, CONFIG_FILE_HEADER, properties, unmodifiedProperties);
  }

  public static Block getCornerBlockForPortalType(PortalType portalType) {
    String blockName =
        switch (portalType) {
          case PLAYER -> PLAYER_PORTAL_CORNER_BLOCK;
          case WORLD -> WORLD_PORTAL_CORNER_BLOCK;
          case UNBOUND -> UNBOUND_PORTAL_CORNER_BLOCK;
          case EVENT -> EVENT_PORTAL_CORNER_BLOCK;
        };

    return BlockRegistry.getBlockFromName(blockName);
  }

  public static boolean isPortalTypeEnabled(PortalType portalType) {
    return switch (portalType) {
      case PLAYER -> ENABLE_PLAYER_PORTALS;
      case WORLD -> ENABLE_WORLD_PORTALS;
      case UNBOUND -> ENABLE_UNBOUND_PORTALS;
      case EVENT -> ENABLE_EVENT_PORTALS;
    };
  }

  public static int getMaxLinksForPortalType(PortalType portalType) {
    return switch (portalType) {
      case PLAYER -> PLAYER_PORTAL_MAX_LINKS;
      case WORLD -> WORLD_PORTAL_MAX_LINKS;
      case UNBOUND -> UNBOUND_PORTAL_MAX_LINKS;
      case EVENT -> EVENT_PORTAL_MAX_LINKS;
    };
  }
}
