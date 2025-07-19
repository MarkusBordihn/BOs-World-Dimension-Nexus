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
public class AutoTeleportConfig extends Config {

  public static final String CONFIG_FILE_NAME = "auto_teleport.cfg";
  public static final String CONFIG_FILE_HEADER =
"""
Auto-Teleport Configuration
This file contains the configuration for auto-teleport behavior and restrictions.

Settings:
- PreventTeleportAfterDeath: Prevents other auto-teleport rules from triggering when a player
  has been teleported due to a death rule and is still in the death target dimension.
- DeathTeleportProtectionTimeSeconds: How long (in seconds) after a death teleport other
  auto-teleport rules should be suppressed for that player.
- PreventTeleportFromDeathDimension: Prevents auto-teleport rules from triggering when a player
  is in a dimension that is configured as a death teleport target (persists across server restarts).
- RestrictTeleportCommandsInDeathDimension: Restricts manual teleport commands when a player
  is in a dimension configured as a death teleport target.

""";

  public static boolean PREVENT_TELEPORT_AFTER_DEATH = true;
  public static int DEATH_TELEPORT_PROTECTION_TIME_SECONDS = 300; // 5 minutes
  public static boolean PREVENT_TELEPORT_FROM_DEATH_DIMENSION = true;
  public static boolean RESTRICT_TELEPORT_COMMANDS_IN_DEATH_DIMENSION = false;

  public static void registerConfig() {
    registerConfigFile(CONFIG_FILE_NAME, CONFIG_FILE_HEADER);
    parseConfigFile();
  }

  public static void parseConfigFile() {
    File configFile = getConfigFile(CONFIG_FILE_NAME);
    Properties properties = readConfigFile(configFile);
    Properties unmodifiedProperties = (Properties) properties.clone();

    PREVENT_TELEPORT_AFTER_DEATH =
        parseConfigValue(
            properties, "AutoTeleport:PreventTeleportAfterDeath", PREVENT_TELEPORT_AFTER_DEATH);

    DEATH_TELEPORT_PROTECTION_TIME_SECONDS =
        parseConfigValue(
            properties,
            "AutoTeleport:DeathTeleportProtectionTimeSeconds",
            DEATH_TELEPORT_PROTECTION_TIME_SECONDS);

    PREVENT_TELEPORT_FROM_DEATH_DIMENSION =
        parseConfigValue(
            properties,
            "AutoTeleport:PreventTeleportFromDeathDimension",
            PREVENT_TELEPORT_FROM_DEATH_DIMENSION);

    RESTRICT_TELEPORT_COMMANDS_IN_DEATH_DIMENSION =
        parseConfigValue(
            properties,
            "AutoTeleport:RestrictTeleportCommandsInDeathDimension",
            RESTRICT_TELEPORT_COMMANDS_IN_DEATH_DIMENSION);

    updateConfigFileIfChanged(configFile, CONFIG_FILE_HEADER, properties, unmodifiedProperties);
  }
}
