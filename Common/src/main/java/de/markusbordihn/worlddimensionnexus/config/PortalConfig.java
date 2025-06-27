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

import java.io.File;
import java.util.Properties;

@SuppressWarnings({"java:S1104", "java:S1444", "java:S3008"})
public class PortalConfig extends Config {

  public static final String CONFIG_FILE_NAME = "portal.cfg";
  public static final String CONFIG_FILE_HEADER =
"""
Portal Configuration
This file contains the configuration for portals, including teleport delays, cooldowns, and effects.

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

    // Update config file if needed
    updateConfigFileIfChanged(configFile, CONFIG_FILE_HEADER, properties, unmodifiedProperties);
  }
}
