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

import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.io.File;
import java.util.Properties;

@SuppressWarnings({"java:S1104", "java:S1444", "java:S3008"})
public class FloatingIslandsChunkGeneratorConfig extends Config {

  private static final PrefixLogger log =
      ModLogger.getPrefixLogger("Floating Islands Chunk Generator Config");

  private static final String CONFIG_FILE_NAME = "floating_islands_generator.properties";
  private static final String CONFIG_HEADER =
      """
      Floating Islands Chunk Generator Configuration
      This file contains the configuration for the floating islands chunk generator.

      Island Generation Settings:
      - Island density controls how many islands are generated (0.0 = none, 1.0 = maximum)
      - Island spacing determines the distance between island clusters
      - Height settings control the vertical range where islands are generated

      Island Shape and Size:
      - Thickness settings control how thick the islands are
      - Noise scale affects the detail level of island shapes
      - Vegetation chance controls how much plant life appears on islands

      Performance Settings:
      - Higher detail levels provide more realistic islands but use more resources
      - Lower spacing values create more islands but may impact performance
      """;

  // Island generation settings
  public static double ISLAND_DENSITY = 0.3;
  public static int ISLAND_SPACING = 128;
  public static double NOISE_SCALE = 0.02;

  // Island height settings
  public static int MIN_ISLAND_HEIGHT = 60;
  public static int MAX_ISLAND_HEIGHT = 120;
  public static int DEFAULT_ISLAND_THICKNESS = 20;

  // Island shape settings
  public static double ISLAND_RADIUS_MULTIPLIER = 0.3;
  public static int MIN_ISLAND_THICKNESS = 3;
  public static int HEIGHT_VARIATION_RANGE = 3;

  // Vegetation settings
  public static double VEGETATION_CHANCE = 0.1;
  public static double GRASS_CHANCE = 0.7;
  public static double TREE_CHANCE = 0.3;

  // Noise generation settings
  public static long ISLAND_NOISE_SEED = 12345L;
  public static long HEIGHT_NOISE_SEED = 54321L;
  public static long DETAIL_NOISE_SEED = 98765L;

  // Performance settings
  public static boolean ENABLE_HEIGHT_CACHING = true;
  public static boolean ENABLE_DETAILED_GENERATION = true;

  private FloatingIslandsChunkGeneratorConfig() {}

  public static void registerConfig() {
    registerConfigFile(CONFIG_FILE_NAME, CONFIG_HEADER);
    loadConfig();
  }

  private static void loadConfig() {
    File configFile = getConfigFile(CONFIG_FILE_NAME);
    if (configFile == null || !configFile.exists()) {
      log.warn("Configuration file {} not found, using defaults", CONFIG_FILE_NAME);
      return;
    }

    Properties properties = readConfigFile(configFile);
    Properties unmodifiedProperties = (Properties) properties.clone();

    // Island generation settings
    ISLAND_DENSITY = parseConfigValue(properties, "IslandGeneration:Density", ISLAND_DENSITY);
    ISLAND_SPACING = parseConfigValue(properties, "IslandGeneration:Spacing", ISLAND_SPACING);
    NOISE_SCALE = parseConfigValue(properties, "IslandGeneration:NoiseScale", NOISE_SCALE);

    // Island height settings
    MIN_ISLAND_HEIGHT = parseConfigValue(properties, "IslandHeight:MinHeight", MIN_ISLAND_HEIGHT);
    MAX_ISLAND_HEIGHT = parseConfigValue(properties, "IslandHeight:MaxHeight", MAX_ISLAND_HEIGHT);
    DEFAULT_ISLAND_THICKNESS =
        parseConfigValue(properties, "IslandHeight:DefaultThickness", DEFAULT_ISLAND_THICKNESS);

    // Island shape settings
    ISLAND_RADIUS_MULTIPLIER =
        parseConfigValue(properties, "IslandShape:RadiusMultiplier", ISLAND_RADIUS_MULTIPLIER);
    MIN_ISLAND_THICKNESS =
        parseConfigValue(properties, "IslandShape:MinThickness", MIN_ISLAND_THICKNESS);
    HEIGHT_VARIATION_RANGE =
        parseConfigValue(properties, "IslandShape:HeightVariationRange", HEIGHT_VARIATION_RANGE);

    // Vegetation settings
    VEGETATION_CHANCE =
        parseConfigValue(properties, "Vegetation:VegetationChance", VEGETATION_CHANCE);
    GRASS_CHANCE = parseConfigValue(properties, "Vegetation:GrassChance", GRASS_CHANCE);
    TREE_CHANCE = parseConfigValue(properties, "Vegetation:TreeChance", TREE_CHANCE);

    // Noise generation settings
    ISLAND_NOISE_SEED =
        parseConfigValue(properties, "NoiseGeneration:IslandNoiseSeed", ISLAND_NOISE_SEED);
    HEIGHT_NOISE_SEED =
        parseConfigValue(properties, "NoiseGeneration:HeightNoiseSeed", HEIGHT_NOISE_SEED);
    DETAIL_NOISE_SEED =
        parseConfigValue(properties, "NoiseGeneration:DetailNoiseSeed", DETAIL_NOISE_SEED);

    // Performance settings
    ENABLE_HEIGHT_CACHING =
        parseConfigValue(properties, "Performance:EnableHeightCaching", ENABLE_HEIGHT_CACHING);
    ENABLE_DETAILED_GENERATION =
        parseConfigValue(
            properties, "Performance:EnableDetailedGeneration", ENABLE_DETAILED_GENERATION);

    updateConfigFileIfChanged(configFile, CONFIG_HEADER, properties, unmodifiedProperties);

    log.info("Loaded floating islands chunk generator configuration");
  }
}
