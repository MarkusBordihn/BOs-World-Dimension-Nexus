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
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@SuppressWarnings({"java:S1104", "java:S1444", "java:S3008"})
public class WarpConfig extends Config {

  public static final String CONFIG_FILE_NAME = "warp.cfg";
  public static final String CONFIG_FILE_HEADER =
"""
Warp Configuration
This file contains the configuration for the warp system, including limits, cooldowns, and restrictions.

Warp System Configuration:
- Private warps are only accessible by their creator
- Public warps are accessible by all players
- Warps can be temporarily disabled before being permanently deleted to prevent abuse
- Reserved names prevent players from creating warps with certain names

Deletion System:
- When a warp is deleted, it is first disabled for a configurable period
- After this period, the warp is permanently removed
- This prevents players from using warps as quick save/load systems

""";

  // Warp system activation
  public static boolean ENABLE_WARP_SYSTEM = true;

  // Warp limits
  public static int MAX_PRIVATE_WARPS_PER_PLAYER = 5;
  public static int MAX_PUBLIC_WARPS_PER_PLAYER = 2;
  public static int MAX_TOTAL_PUBLIC_WARPS = 50;

  // Timing settings (in milliseconds)
  public static long WARP_DELETION_DELAY = 5 * 60 * 1000; // 5 minutes
  public static long WARP_TELEPORT_COOLDOWN = 3 * 1000; // 3 seconds

  // Name restrictions
  public static int MIN_WARP_NAME_LENGTH = 3;
  public static int MAX_WARP_NAME_LENGTH = 16;
  public static int MAX_WARP_DESCRIPTION_LENGTH = 64;

  // Reserved names (comma-separated list)
  public static String RESERVED_WARP_NAMES =
      "spawn,home,admin,moderator,server,lobby,hub,world,dimension,portal,teleport,warp,help,info,list,create,delete,remove,set,get";

  // Permission settings
  public static boolean ALLOW_CROSS_DIMENSION_WARPS = true;
  public static boolean REQUIRE_PERMISSION_FOR_PUBLIC_WARPS = false;

  private static Set<String> reservedNamesSet;

  public static void registerConfig() {
    registerConfigFile(CONFIG_FILE_NAME, CONFIG_FILE_HEADER);
    parseConfigFile();
  }

  public static void parseConfigFile() {
    File configFile = getConfigFile(CONFIG_FILE_NAME);
    Properties properties = readConfigFile(configFile);
    Properties unmodifiedProperties = (Properties) properties.clone();

    // Warp system activation
    ENABLE_WARP_SYSTEM = parseConfigValue(properties, "WarpSystem:Enable", ENABLE_WARP_SYSTEM);

    // Warp limits
    MAX_PRIVATE_WARPS_PER_PLAYER =
        parseConfigValue(
            properties, "WarpLimits:MaxPrivateWarpsPerPlayer", MAX_PRIVATE_WARPS_PER_PLAYER);
    MAX_PUBLIC_WARPS_PER_PLAYER =
        parseConfigValue(
            properties, "WarpLimits:MaxPublicWarpsPerPlayer", MAX_PUBLIC_WARPS_PER_PLAYER);
    MAX_TOTAL_PUBLIC_WARPS =
        parseConfigValue(properties, "WarpLimits:MaxTotalPublicWarps", MAX_TOTAL_PUBLIC_WARPS);

    // Timing settings
    WARP_DELETION_DELAY =
        parseConfigValue(properties, "WarpTiming:DeletionDelayMillis", WARP_DELETION_DELAY);
    WARP_TELEPORT_COOLDOWN =
        parseConfigValue(properties, "WarpTiming:TeleportCooldownMillis", WARP_TELEPORT_COOLDOWN);

    // Name restrictions
    MIN_WARP_NAME_LENGTH =
        parseConfigValue(properties, "WarpNames:MinNameLength", MIN_WARP_NAME_LENGTH);
    MAX_WARP_NAME_LENGTH =
        parseConfigValue(properties, "WarpNames:MaxNameLength", MAX_WARP_NAME_LENGTH);
    MAX_WARP_DESCRIPTION_LENGTH =
        parseConfigValue(properties, "WarpNames:MaxDescriptionLength", MAX_WARP_DESCRIPTION_LENGTH);
    RESERVED_WARP_NAMES =
        parseConfigValue(properties, "WarpNames:ReservedNames", RESERVED_WARP_NAMES);

    // Permission settings
    ALLOW_CROSS_DIMENSION_WARPS =
        parseConfigValue(
            properties, "WarpPermissions:AllowCrossDimensionWarps", ALLOW_CROSS_DIMENSION_WARPS);
    REQUIRE_PERMISSION_FOR_PUBLIC_WARPS =
        parseConfigValue(
            properties,
            "WarpPermissions:RequirePermissionForPublicWarps",
            REQUIRE_PERMISSION_FOR_PUBLIC_WARPS);

    // Parse reserved names
    parseReservedNames();

    // Update config file if needed
    updateConfigFileIfChanged(configFile, CONFIG_FILE_HEADER, properties, unmodifiedProperties);
  }

  private static void parseReservedNames() {
    reservedNamesSet = new HashSet<>();
    if (RESERVED_WARP_NAMES != null && !RESERVED_WARP_NAMES.trim().isEmpty()) {
      String[] names = RESERVED_WARP_NAMES.split(",");
      for (String name : names) {
        reservedNamesSet.add(name.trim().toLowerCase());
      }
    }
  }

  public static boolean isWarpSystemEnabled() {
    return ENABLE_WARP_SYSTEM;
  }

  public static boolean isNameReserved(String name) {
    if (reservedNamesSet == null) {
      parseReservedNames();
    }
    return reservedNamesSet.contains(name.toLowerCase());
  }

  public static boolean isValidWarpName(String name) {
    if (name == null || name.trim().isEmpty()) {
      return false;
    }

    String trimmedName = name.trim();
    return trimmedName.length() >= MIN_WARP_NAME_LENGTH
        && trimmedName.length() <= MAX_WARP_NAME_LENGTH
        && !isNameReserved(trimmedName)
        && trimmedName.matches("^[a-zA-Z0-9_-]+$");
  }

  public static boolean isValidDescription(String description) {
    if (description == null) {
      return true;
    }
    return description.trim().length() <= MAX_WARP_DESCRIPTION_LENGTH;
  }
}
