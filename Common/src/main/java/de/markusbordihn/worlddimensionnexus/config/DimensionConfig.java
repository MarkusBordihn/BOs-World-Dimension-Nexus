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
public class DimensionConfig extends Config {

  public static final String CONFIG_FILE_NAME = "dimension.cfg";
  public static final String CONFIG_FILE_HEADER =
"""
Dimension Configuration
This file contains the configuration for dimensions, including default settings and limits.

""";

  // Default settings for new dimensions
  public static float DEFAULT_AMBIENT_LIGHT = 0.0f;
  public static boolean DEFAULT_HAS_SKYLIGHT = true;
  public static boolean DEFAULT_HAS_CEILING = false;
  public static boolean DEFAULT_ULTRAWARM = false;
  public static boolean DEFAULT_NATURAL = true;
  public static double DEFAULT_COORDINATE_SCALE = 1.0;
  public static boolean DEFAULT_PIGLIN_SAFE = false;
  public static boolean DEFAULT_BED_WORKS = true;
  public static boolean DEFAULT_RESPAWN_ANCHOR_WORKS = false;
  public static boolean DEFAULT_HAS_RAIDS = true;
  public static int DEFAULT_LOGICAL_HEIGHT = 384;
  public static int DEFAULT_MIN_Y = -64;
  public static int DEFAULT_HEIGHT = 384;
  public static float DEFAULT_MONSTER_SPAWN_LIGHT_LEVEL = 0.0f;
  public static int DEFAULT_MONSTER_SPAWN_BLOCK_LIGHT_LIMIT = 0;

  // Safety and Limits
  public static int MAX_DIMENSIONS = 100;
  public static int MAX_DIMENSION_NAME_LENGTH = 64;
  public static boolean PREVENT_DIMENSION_DELETION = true;
  public static boolean BACKUP_DELETED_DIMENSIONS = true;

  // Auto-creation and Management
  public static boolean EXAMPLE_DIMENSIONS_ON_FIRST_RUN = true;
  public static boolean DEFAULT_DIMENSION_AUTO_LOAD = true;

  public static void registerConfig() {
    registerConfigFile(CONFIG_FILE_NAME, CONFIG_FILE_HEADER);
    parseConfigFile();
  }

  public static void parseConfigFile() {
    File configFile = getConfigFile(CONFIG_FILE_NAME);
    Properties properties = readConfigFile(configFile);
    Properties unmodifiedProperties = (Properties) properties.clone();

    // Default settings for new dimensions
    DEFAULT_AMBIENT_LIGHT =
        parseConfigValue(properties, "Dimension:DefaultAmbientLight", DEFAULT_AMBIENT_LIGHT);
    DEFAULT_HAS_SKYLIGHT =
        parseConfigValue(properties, "Dimension:DefaultHasSkylight", DEFAULT_HAS_SKYLIGHT);
    DEFAULT_HAS_CEILING =
        parseConfigValue(properties, "Dimension:DefaultHasCeiling", DEFAULT_HAS_CEILING);
    DEFAULT_ULTRAWARM =
        parseConfigValue(properties, "Dimension:DefaultUltrawarm", DEFAULT_ULTRAWARM);
    DEFAULT_NATURAL = parseConfigValue(properties, "Dimension:DefaultNatural", DEFAULT_NATURAL);
    DEFAULT_COORDINATE_SCALE =
        parseConfigValue(properties, "Dimension:DefaultCoordinateScale", DEFAULT_COORDINATE_SCALE);
    DEFAULT_PIGLIN_SAFE =
        parseConfigValue(properties, "Dimension:DefaultPiglinSafe", DEFAULT_PIGLIN_SAFE);
    DEFAULT_BED_WORKS =
        parseConfigValue(properties, "Dimension:DefaultBedWorks", DEFAULT_BED_WORKS);
    DEFAULT_RESPAWN_ANCHOR_WORKS =
        parseConfigValue(
            properties, "Dimension:DefaultRespawnAnchorWorks", DEFAULT_RESPAWN_ANCHOR_WORKS);
    DEFAULT_HAS_RAIDS =
        parseConfigValue(properties, "Dimension:DefaultHasRaids", DEFAULT_HAS_RAIDS);
    DEFAULT_LOGICAL_HEIGHT =
        parseConfigValue(properties, "Dimension:DefaultLogicalHeight", DEFAULT_LOGICAL_HEIGHT);
    DEFAULT_MIN_Y = parseConfigValue(properties, "Dimension:DefaultMinY", DEFAULT_MIN_Y);
    DEFAULT_HEIGHT = parseConfigValue(properties, "Dimension:DefaultHeight", DEFAULT_HEIGHT);
    DEFAULT_MONSTER_SPAWN_LIGHT_LEVEL =
        parseConfigValue(
            properties,
            "Dimension:DefaultMonsterSpawnLightLevel",
            DEFAULT_MONSTER_SPAWN_LIGHT_LEVEL);
    DEFAULT_MONSTER_SPAWN_BLOCK_LIGHT_LIMIT =
        parseConfigValue(
            properties,
            "Dimension:DefaultMonsterSpawnBlockLightLimit",
            DEFAULT_MONSTER_SPAWN_BLOCK_LIGHT_LIMIT);

    // Safety and Limits
    MAX_DIMENSIONS = parseConfigValue(properties, "Dimension:MaxDimensions", MAX_DIMENSIONS);
    MAX_DIMENSION_NAME_LENGTH =
        parseConfigValue(properties, "Dimension:MaxDimensionNameLength", MAX_DIMENSION_NAME_LENGTH);
    PREVENT_DIMENSION_DELETION =
        parseConfigValue(
            properties, "Dimension:PreventDimensionDeletion", PREVENT_DIMENSION_DELETION);
    BACKUP_DELETED_DIMENSIONS =
        parseConfigValue(
            properties, "Dimension:BackupDeletedDimensions", BACKUP_DELETED_DIMENSIONS);

    // Auto-creation and Management
    EXAMPLE_DIMENSIONS_ON_FIRST_RUN =
        parseConfigValue(
            properties, "Dimension:ExampleDimensionsOnFirstRun", EXAMPLE_DIMENSIONS_ON_FIRST_RUN);
    DEFAULT_DIMENSION_AUTO_LOAD =
        parseConfigValue(
            properties, "Dimension:DefaultDimensionAutoLoad", DEFAULT_DIMENSION_AUTO_LOAD);

    // Update config file if needed
    updateConfigFileIfChanged(configFile, CONFIG_FILE_HEADER, properties, unmodifiedProperties);
  }
}
