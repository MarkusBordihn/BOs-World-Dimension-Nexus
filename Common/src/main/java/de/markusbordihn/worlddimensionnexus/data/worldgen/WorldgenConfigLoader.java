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

package de.markusbordihn.worlddimensionnexus.data.worldgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldgenConfigLoader {
  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final Map<ChunkGeneratorType, WorldgenConfig> configs =
      new EnumMap<>(ChunkGeneratorType.class);

  public static void loadConfigsFromResources() {
    log.info("Loading worldgen configurations from resources...");

    // Load all available chunk generator types
    for (ChunkGeneratorType type : ChunkGeneratorType.values()) {
      try {
        loadConfigForType(type);
      } catch (Exception e) {
        log.warn("Could not load configuration for {}: {}", type.getName(), e.getMessage());
      }
    }
  }

  private static void loadConfigForType(final ChunkGeneratorType type) {
    String resourcePath = "/data/" + Constants.MOD_ID + "/worldgen/" + type.getName() + ".json";

    try (InputStream inputStream = WorldgenConfigLoader.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        log.debug("No configuration found for: {}", type.getName());
        return;
      }

      JsonElement element =
          JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

      if (element instanceof JsonObject jsonObject) {
        WorldgenConfig config = parseConfig(type, jsonObject);
        configs.put(type, config);
        log.debug("Worldgen configuration loaded for: {}", type.getName());
      }
    } catch (IOException e) {
      log.error("Error loading worldgen configuration for {}: {}", type.getName(), e.getMessage());
    }
  }

  public static void loadConfigsFromPath(final Path worldgenPath) {
    if (!Files.exists(worldgenPath)) {
      log.debug("Worldgen path does not exist: {}", worldgenPath);
      return;
    }

    log.info("Loading worldgen configurations from path: {}", worldgenPath);
    try {
      try (var fileStream = Files.walk(worldgenPath)) {
        fileStream
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".json"))
            .forEach(WorldgenConfigLoader::loadConfigFromFile);
      }
    } catch (IOException e) {
      log.error("Error scanning worldgen path: {}", e.getMessage());
    }
  }

  private static void loadConfigFromFile(final Path configFile) {
    try {
      String fileName = configFile.getFileName().toString();
      String typeName = fileName.substring(0, fileName.lastIndexOf('.'));
      ChunkGeneratorType type = ChunkGeneratorType.fromString(typeName);

      String content = Files.readString(configFile, StandardCharsets.UTF_8);
      JsonElement element = JsonParser.parseString(content);

      if (element instanceof JsonObject jsonObject) {
        WorldgenConfig config = parseConfig(type, jsonObject);
        configs.put(type, config);
        log.debug("Worldgen configuration loaded from file: {}", configFile);
      }
    } catch (Exception e) {
      log.error("Error loading configuration file {}: {}", configFile, e.getMessage());
    }
  }

  private static WorldgenConfig parseConfig(
      final ChunkGeneratorType type, final JsonObject jsonObject) {
    Optional<ResourceLocation> noiseSettings = Optional.empty();
    Optional<ResourceLocation> biomeSource = Optional.empty();
    Map<String, String> customSettings = new HashMap<>();

    if (jsonObject.has("noise_settings")) {
      noiseSettings =
          Optional.of(
              ResourceLocation.fromNamespaceAndPath(
                  "minecraft", jsonObject.get("noise_settings").getAsString()));
    }

    if (jsonObject.has("biome_source")) {
      biomeSource =
          Optional.of(
              ResourceLocation.fromNamespaceAndPath(
                  "minecraft", jsonObject.get("biome_source").getAsString()));
    }

    if (jsonObject.has("custom_settings")) {
      JsonObject custom = jsonObject.getAsJsonObject("custom_settings");
      custom
          .entrySet()
          .forEach(entry -> customSettings.put(entry.getKey(), entry.getValue().getAsString()));
    }

    return new WorldgenConfig(type, noiseSettings, biomeSource, customSettings);
  }

  public static Optional<WorldgenConfig> getConfig(final ChunkGeneratorType type) {
    return Optional.ofNullable(configs.get(type));
  }

  public static Map<ChunkGeneratorType, WorldgenConfig> getAllConfigs() {
    return Map.copyOf(configs);
  }

  public static void clear() {
    configs.clear();
    log.debug("All worldgen configurations cleared");
  }
}
