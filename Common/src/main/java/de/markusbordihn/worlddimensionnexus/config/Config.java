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

import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

public class Config {
  protected static final PrefixLogger log = ModLogger.getPrefixLogger("[Config]");
  private static boolean isLoaded = false;

  private static Path configPath =
      Paths.get("").toAbsolutePath().resolve("config").resolve(Constants.MOD_ID);

  protected Config() {}

  public static void register(boolean isServer) {
    prepareConfiguration();
    registerCommonConfig();
    if (isServer) {
      registerServerConfig();
    } else {
      registerClientConfig();
    }
  }

  public static void registerCommonConfig() {
    log.info("Registering common configuration ...");
    PortalConfig.registerConfig();
    DimensionConfig.registerConfig();
  }

  public static void registerClientConfig() {
    log.info("Registering client configuration ...");
  }

  public static void registerServerConfig() {
    log.info("Registering server configuration ...");
  }

  public static void prepareConfiguration() {
    // Validate game folder path.
    if (Constants.CONFIG_DIR != null) {
      configPath = Constants.CONFIG_DIR.resolve(Constants.MOD_ID);
      log.info("Updated configuration path to {}", configPath);
    }

    // Validate configuration folder
    if (!configPath.toFile().exists()) {
      log.info("Creating configuration folder {}", getConfigDirectory());
    }

    // Simple reload protection
    if (isLoaded) {
      log.error("Configuration is already loaded");
      log.warn("Check if configuration is loaded multiple times!");
      return;
    }
    isLoaded = true;
  }

  public static void registerConfigFile(
      final String configFileName, final String configFileHeader) {
    File configFile = getConfigFile(configFileName.trim());
    if (configFile == null || !configFile.exists()) {
      createConfigFile(getConfigFile(configFileName.trim()), configFileHeader.trim());
    }
  }

  public static Properties readConfigFile(final File configFile) {
    Properties properties = new Properties();
    try (var reader = Files.newBufferedReader(configFile.toPath())) {
      properties.load(reader);
    } catch (Exception e) {
      log.error("Failed to read configuration file {}:", configFile, e);
    }
    return properties;
  }

  public static void createConfigFile(final File configFile, final String header) {
    Properties properties = new Properties();
    log.info("Creating configuration file {}", configFile);
    try (FileWriter writer = new FileWriter(configFile)) {
      properties.store(writer, header.trim());
    } catch (Exception e) {
      log.error("Failed to create configuration file {} for {}", configFile, properties);
    }
  }

  public static File getConfigFile(final String configFileName) {
    Path path = getConfigDirectory();
    if (path != null) {
      return path.resolve(configFileName).toFile();
    }
    return null;
  }

  private static Path getConfigDirectory() {
    Path resultPath = null;
    try {
      resultPath = Files.createDirectories(configPath);
    } catch (Exception e) {
      log.error("Failed to create configuration folder {}:", configPath, e);
    }
    return resultPath;
  }

  public static void updateConfigFileIfChanged(
      File configFile,
      String configFileHeader,
      Properties properties,
      Properties unmodifiedProperties) {
    if (!properties.equals(unmodifiedProperties)) {
      log.info("Updating configuration file {} {}: {}", configFile, configFileHeader, properties);
      try (FileWriter writer = new FileWriter(configFile)) {
        properties.store(writer, configFileHeader.trim());
      } catch (Exception e) {
        log.error("Failed to update configuration file {} with {}", configFile, properties);
      }
    } else {
      log.info("{} is up to date: {}", configFileHeader, properties);
    }
  }

  protected static int parseConfigValue(
      final Properties properties, final String key, final int defaultValue) {
    if (properties.containsKey(key)) {
      try {
        return Integer.parseInt(properties.getProperty(key).trim());
      } catch (Exception e) {
        log.error("Failed to parse Integer value for key {}:", key, e);
      }
    }
    properties.setProperty(key, Integer.toString(defaultValue));
    return defaultValue;
  }

  protected static short parseConfigValue(
      final Properties properties, final String key, final short defaultValue) {
    if (properties.containsKey(key)) {
      try {
        return Short.parseShort(properties.getProperty(key).trim());
      } catch (Exception e) {
        log.error("Failed to parse Short value for key {}:", key, e);
      }
    }
    properties.setProperty(key, Short.toString(defaultValue));
    return defaultValue;
  }

  protected static boolean parseConfigValue(
      final Properties properties, final String key, final boolean defaultValue) {
    if (properties.containsKey(key)) {
      try {
        return Boolean.parseBoolean(properties.getProperty(key).trim());
      } catch (Exception e) {
        log.error("Failed to parse Boolean value for key {}:", key, e);
      }
    }
    properties.setProperty(key, Boolean.toString(defaultValue));
    return defaultValue;
  }

  protected static double parseConfigValue(
      final Properties properties, final String key, final double defaultValue) {
    if (properties.containsKey(key)) {
      try {
        return Double.parseDouble(properties.getProperty(key).trim());
      } catch (Exception e) {
        log.error("Failed to parse Double value for key {}:", key, e);
      }
    }
    properties.setProperty(key, Double.toString(defaultValue));
    return defaultValue;
  }

  protected static float parseConfigValue(
      final Properties properties, final String key, final float defaultValue) {
    if (properties.containsKey(key)) {
      try {
        return Float.parseFloat(properties.getProperty(key).trim());
      } catch (Exception e) {
        log.error("Failed to parse Float value for key {}:", key, e);
      }
    }
    properties.setProperty(key, Float.toString(defaultValue));
    return defaultValue;
  }

  protected static Set<String> parseConfigValue(
      final Properties properties, final String key, final Set<String> defaultValue) {
    if (properties.containsKey(key)) {
      try {
        String value = properties.getProperty(key).trim();
        return value.isEmpty() ? Set.of() : Set.of(value.split(",\\s*"));
      } catch (Exception e) {
        log.error("Failed to parse Set[String] for key {}:", key, e);
      }
    }
    properties.setProperty(key, String.join(",", defaultValue));
    return defaultValue;
  }
}
