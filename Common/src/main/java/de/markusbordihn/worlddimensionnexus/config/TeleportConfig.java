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
public class TeleportConfig extends Config {

  public static final String CONFIG_FILE_NAME = "teleport.cfg";
  public static final String CONFIG_FILE_HEADER =
"""
Teleport Configuration
This file contains the configuration for teleport commands, including cooldowns and restrictions.

Commands:
- EnableBackTeleportCommand: Enables or disables the /wdn teleport back command for players
- EnableOverworldTeleportCommand: Enables or disables the /wdn teleport overworld command for players

Cooldowns:
- BackTeleportCooldown: Cooldown in seconds for the back teleport command (0 = disabled)
- ModeratorsBypassCooldown: Whether moderators bypass teleport cooldowns

""";

  public static int BACK_TELEPORT_COOLDOWN = 30;
  public static boolean MODERATORS_BYPASS_COOLDOWN = true;
  public static boolean ENABLE_BACK_TELEPORT_COMMAND = true;
  public static boolean ENABLE_OVERWORLD_TELEPORT_COMMAND = true;

  public static void registerConfig() {
    registerConfigFile(CONFIG_FILE_NAME, CONFIG_FILE_HEADER);
    parseConfigFile();
  }

  public static void parseConfigFile() {
    File configFile = getConfigFile(CONFIG_FILE_NAME);
    Properties properties = readConfigFile(configFile);
    Properties unmodifiedProperties = (Properties) properties.clone();

    BACK_TELEPORT_COOLDOWN =
        parseConfigValue(properties, "Teleport:BackTeleportCooldown", BACK_TELEPORT_COOLDOWN);

    MODERATORS_BYPASS_COOLDOWN =
        parseConfigValue(
            properties, "Teleport:ModeratorsBypassCooldown", MODERATORS_BYPASS_COOLDOWN);

    ENABLE_BACK_TELEPORT_COMMAND =
        parseConfigValue(
            properties, "Teleport:EnableBackTeleportCommand", ENABLE_BACK_TELEPORT_COMMAND);

    ENABLE_OVERWORLD_TELEPORT_COMMAND =
        parseConfigValue(
            properties,
            "Teleport:EnableOverworldTeleportCommand",
            ENABLE_OVERWORLD_TELEPORT_COMMAND);

    updateConfigFileIfChanged(configFile, CONFIG_FILE_HEADER, properties, unmodifiedProperties);
  }
}
