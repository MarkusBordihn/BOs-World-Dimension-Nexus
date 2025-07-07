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

package de.markusbordihn.worlddimensionnexus.levelgen;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class JsonChunkGeneratorLoader {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("JSON ChunkGenerator Loader");
  private static final String WORLDGEN_PATH_TEMPLATE = "/data/%s/worldgen/%s_worldgen.json";
  private static final String DIMENSION_PATH_TEMPLATE =
      "/data/%s/dimension_preset/%s_dimension.json";
  private static final String DIMENSION_TYPE_PATH_TEMPLATE =
      "/data/%s/dimension_type/%s_dimension_type.json";

  private JsonChunkGeneratorLoader() {}

  public static Optional<ChunkGenerator> loadFromJson(
      final MinecraftServer server, final ChunkGeneratorType type) {
    return loadFromWorldgenJson(server, type).or(() -> loadFromDimensionJson(server, type));
  }

  private static Optional<ChunkGenerator> loadFromWorldgenJson(
      final MinecraftServer server, final ChunkGeneratorType type) {
    String resourcePath = String.format(WORLDGEN_PATH_TEMPLATE, Constants.MOD_ID, type.getName());
    return loadFromResource(server, resourcePath, ChunkGenerator.CODEC, "worldgen ChunkGenerator");
  }

  private static Optional<ChunkGenerator> loadFromDimensionJson(
      final MinecraftServer server, final ChunkGeneratorType type) {
    String resourcePath = String.format(DIMENSION_PATH_TEMPLATE, Constants.MOD_ID, type.getName());
    return loadFromResource(server, resourcePath, LevelStem.CODEC, "dimension LevelStem")
        .map(LevelStem::generator);
  }

  public static Optional<DimensionType> loadDimensionTypeFromJson(
      final MinecraftServer server, final ChunkGeneratorType type) {
    String resourcePath =
        String.format(DIMENSION_TYPE_PATH_TEMPLATE, Constants.MOD_ID, type.getName());
    return loadFromResource(server, resourcePath, DimensionType.DIRECT_CODEC, "DimensionType");
  }

  private static <T> Optional<T> loadFromResource(
      final MinecraftServer server,
      final String resourcePath,
      final Codec<T> codec,
      final String logContext) {
    try (InputStream inputStream =
        JsonChunkGeneratorLoader.class.getResourceAsStream(resourcePath)) {
      if (inputStream == null) {
        log.debug("No {} JSON file found at: {}", logContext, resourcePath);
        return Optional.empty();
      }

      return parseJsonContent(
          server,
          new String(inputStream.readAllBytes(), StandardCharsets.UTF_8),
          codec,
          resourcePath,
          logContext);
    } catch (IOException e) {
      log.error("Failed to read {} JSON file: {}", logContext, resourcePath, e);
      return Optional.empty();
    }
  }

  public static Optional<ChunkGenerator> loadFromExternalFile(
      final MinecraftServer server, final Path filePath) {
    if (!Files.exists(filePath)) {
      log.debug("External JSON file does not exist: {}", filePath);
      return Optional.empty();
    }

    try {
      String jsonContent = Files.readString(filePath, StandardCharsets.UTF_8);
      return parseJsonContent(
          server,
          jsonContent,
          ChunkGenerator.CODEC,
          filePath.toString(),
          "external ChunkGenerator");
    } catch (IOException e) {
      log.error("Failed to read external JSON file: {}", filePath, e);
      return Optional.empty();
    }
  }

  private static <T> Optional<T> parseJsonContent(
      final MinecraftServer server,
      final String jsonContent,
      final Codec<T> codec,
      final String sourcePath,
      final String logContext) {
    try {
      JsonElement jsonElement = JsonParser.parseString(jsonContent);
      RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess());

      return codec
          .parse(ops, jsonElement)
          .resultOrPartial(
              error -> log.error("Error parsing {} from {}: {}", logContext, sourcePath, error))
          .map(
              result -> {
                log.info("Successfully loaded {} from: {}", logContext, sourcePath);
                return result;
              });
    } catch (Exception e) {
      log.error("Failed to parse {} from: {}", logContext, sourcePath, e);
      return Optional.empty();
    }
  }
}
