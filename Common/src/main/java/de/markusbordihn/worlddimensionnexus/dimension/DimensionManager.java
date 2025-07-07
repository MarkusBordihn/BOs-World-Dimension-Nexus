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

package de.markusbordihn.worlddimensionnexus.dimension;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.data.worldgen.WorldgenInitializer;
import de.markusbordihn.worlddimensionnexus.network.NetworkHandler;
import de.markusbordihn.worlddimensionnexus.saveddata.DimensionDataStorage;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.ServerLevelData;

public class DimensionManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Dimension Manager");
  private static final Set<DimensionInfoData> dimensions = ConcurrentHashMap.newKeySet();
  private static MinecraftServer minecraftServer;

  private DimensionManager() {}

  public static void sync(
      final MinecraftServer minecraftServer, final List<DimensionInfoData> dimensionList) {
    if (minecraftServer == null) {
      log.error("Minecraft server is null, cannot synchronize dimensions.");
      return;
    }
    DimensionManager.minecraftServer = minecraftServer;

    WorldgenInitializer.initialize(minecraftServer);

    if (dimensionList == null || dimensionList.isEmpty()) {
      log.warn(
          "No dimensions to synchronize: list is {}.", (dimensionList == null ? "null" : "empty"));
      return;
    }

    log.info("Synchronizing {} dimensions ...", dimensionList.size());
    clear();

    List<DimensionInfoData> dimensionsToUpdate = new ArrayList<>();
    for (DimensionInfoData dimensionInfo : dimensionList) {
      ServerLevel level = addOrCreateDimension(dimensionInfo, false);
      if (level != null) {
        DimensionInfoData updated = markDimensionAsServerStartLoaded(dimensionInfo);
        if (updated != null) {
          dimensionsToUpdate.add(updated);
        }
      }
    }

    if (!dimensionsToUpdate.isEmpty()) {
      updateDimensionStorage(dimensionsToUpdate, dimensionList);
    }
  }

  private static DimensionInfoData markDimensionAsServerStartLoaded(
      final DimensionInfoData dimensionInfo) {
    if (dimensionInfo.requiresHotInjectionSync()) {
      DimensionInfoData updatedInfo = dimensionInfo.withoutHotInjectionSync();
      dimensions.remove(dimensionInfo);
      dimensions.add(updatedInfo);
      return updatedInfo;
    }
    return null;
  }

  private static void updateDimensionStorage(
      final List<DimensionInfoData> updatedDimensions,
      final List<DimensionInfoData> originalDimensions) {
    DimensionDataStorage storage = DimensionDataStorage.get();
    List<DimensionInfoData> originalDimensionsCopy = new ArrayList<>(originalDimensions);
    for (DimensionInfoData original : originalDimensionsCopy) {
      if (original.requiresHotInjectionSync()) {
        storage.removeDimension(original);
      }
    }

    for (DimensionInfoData updated : updatedDimensions) {
      storage.addDimension(updated);
    }
  }

  public static ServerLevel addOrCreateDimension(final String dimensionName) {
    return addOrCreateDimension(DimensionInfoData.fromDimensionName(dimensionName), true);
  }

  public static ServerLevel addOrCreateDimension(
      final DimensionInfoData dimensionInfo, final boolean updateStorage) {
    if (dimensionInfo == null) {
      log.warn("DimensionInfoData is null, skipping ...");
      return null;
    }

    if (dimensions.contains(dimensionInfo)) {
      log.info("Dimension {} already exists, skipping ...", dimensionInfo);
      return getServerLevel(dimensionInfo.getDimensionKey());
    }

    return createNewDimension(dimensionInfo, updateStorage);
  }

  private static ServerLevel createNewDimension(
      final DimensionInfoData dimensionInfo, final boolean updateStorage) {
    ChunkGenerator chunkGenerator = dimensionInfo.getChunkGenerator(minecraftServer);
    LevelStem levelStem =
        new LevelStem(dimensionInfo.getDimensionTypeHolder(minecraftServer), chunkGenerator);

    ServerLevel newLevel = buildServerLevel(dimensionInfo, levelStem);
    registerDimension(dimensionInfo, newLevel, updateStorage);

    log.info("Created and loaded new dimension: {}", dimensionInfo.getDimensionKey().location());
    return newLevel;
  }

  public static ServerLevel createNewDimensionWithJsonSupport(
      final DimensionInfoData dimensionInfo, final boolean updateStorage) {

    Optional<LevelStem> jsonLevelStem = tryLoadLevelStemFromJson(dimensionInfo);
    LevelStem levelStem;
    if (jsonLevelStem.isPresent()) {
      levelStem = jsonLevelStem.get();
      log.info(
          "Using complete LevelStem from JSON for dimension: {}",
          dimensionInfo.getDimensionKey().location());
    } else {
      ChunkGenerator chunkGenerator = dimensionInfo.getChunkGenerator(minecraftServer);
      levelStem =
          new LevelStem(dimensionInfo.getDimensionTypeHolder(minecraftServer), chunkGenerator);
    }

    ServerLevel newLevel = buildServerLevel(dimensionInfo, levelStem);
    registerDimension(dimensionInfo, newLevel, updateStorage);

    log.info("Created and loaded new dimension: {}", dimensionInfo.getDimensionKey().location());
    return newLevel;
  }

  private static Optional<LevelStem> tryLoadLevelStemFromJson(
      final DimensionInfoData dimensionInfo) {
    try {
      String dimensionJsonPath =
          String.format(
              "data/%s/dimension/%s_dimension.json",
              Constants.MOD_ID, dimensionInfo.chunkGeneratorType().getName());
      Path resourcePath = tryGetResourcePath(dimensionJsonPath);
      if (resourcePath != null && Files.exists(resourcePath)) {
        return Optional.of(loadLevelStem(resourcePath, minecraftServer.registryAccess()));
      }

    } catch (Exception e) {
      log.debug(
          "Could not load LevelStem from JSON for {}: {}",
          dimensionInfo.chunkGeneratorType().getName(),
          e.getMessage());
    }

    return Optional.empty();
  }

  private static Path tryGetResourcePath(final String resourcePath) {
    try {
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  private static ServerLevel buildServerLevel(
      final DimensionInfoData dimensionInfo, final LevelStem levelStem) {
    ServerLevel overworld = minecraftServer.overworld();
    return new ServerLevel(
        minecraftServer,
        minecraftServer.executor,
        minecraftServer.storageSource,
        (ServerLevelData) overworld.getLevelData(),
        dimensionInfo.getDimensionKey(),
        levelStem,
        overworld.getChunkSource().chunkMap.progressListener,
        false,
        overworld.getSeed(),
        List.of(),
        overworld.isDebug(),
        null);
  }

  private static void registerDimension(
      final DimensionInfoData dimensionInfo, final ServerLevel newLevel, boolean updateStorage) {
    minecraftServer.levels.put(dimensionInfo.getDimensionKey(), newLevel);
    dimensions.add(dimensionInfo);

    if (dimensionInfo.requiresHotInjectionSync()) {
      NetworkHandler.syncDimensionToClients(newLevel);
    }

    if (updateStorage) {
      DimensionDataStorage.get().addDimension(dimensionInfo);
    }
  }

  public static boolean removeDimension(final String name) {
    return removeDimension(getDimensionInfoData(name));
  }

  public static boolean removeDimension(final DimensionInfoData dimensionInfoData) {
    if (dimensionInfoData == null) {
      log.warn("DimensionInfoData is null, skipping ...");
      return false;
    }

    if (dimensions.remove(dimensionInfoData)) {
      ResourceKey<Level> levelKey = dimensionInfoData.getDimensionKey();
      ServerLevel serverLevel = getServerLevel(levelKey);
      if (serverLevel != null) {
        minecraftServer.levels.remove(levelKey);
        DimensionDataStorage.get().removeDimension(dimensionInfoData);

        log.info("Removed dimension: {}", levelKey.location());
        return true;
      } else {
        log.warn("Dimension {} does not exist, skipping removal ...", levelKey.location());
      }
    } else {
      log.warn("Dimension {} not found in dimensions set.", dimensionInfoData);
    }
    return false;
  }

  public static DimensionInfoData getDimensionInfoData(final String name) {
    if (!name.contains(":")) {
      return getDimensionInfoDataByModName(name);
    }

    try {
      ResourceLocation resourceLocation = ResourceLocation.parse(name);
      return getDimensionInfoData(resourceLocation);
    } catch (Exception e) {
      log.warn("Invalid resource location format: {}", name);
      return null;
    }
  }

  public static DimensionInfoData getDimensionInfoDataByModName(final String simpleName) {
    for (DimensionInfoData dimensionInfo : dimensions) {
      ResourceLocation dimensionLocation = dimensionInfo.getDimensionKey().location();
      if (dimensionLocation.getNamespace().equals(Constants.MOD_ID)
          && dimensionLocation.getPath().equals(simpleName)) {
        return dimensionInfo;
      }
    }
    return null;
  }

  public static DimensionInfoData getDimensionInfoData(final ResourceLocation resourceLocation) {
    for (DimensionInfoData dimensionInfo : dimensions) {
      if (dimensionInfo.getDimensionKey().location().equals(resourceLocation)) {
        return dimensionInfo;
      }
    }
    return null;
  }

  public static ServerLevel getDimensionServerLevel(final String name) {
    DimensionInfoData dimensionInfo = getDimensionInfoData(name);
    if (dimensionInfo != null) {
      return getServerLevel(dimensionInfo.getDimensionKey());
    }
    log.warn("Dimension {} not found, returning null.", name);
    return null;
  }

  public static List<ResourceKey<Level>> getDimensions(final MinecraftServer server) {
    return server.levelKeys().stream()
        .filter(levelKey -> !levelKey.equals(Level.OVERWORLD))
        .toList();
  }

  public static DimensionInfoData getDimensionInfo(final ResourceKey<Level> levelKey) {
    for (DimensionInfoData dimensionInfo : dimensions) {
      if (dimensionInfo.getDimensionKey().equals(levelKey)) {
        return dimensionInfo;
      }
    }
    return null;
  }

  public static Collection<String> getDimensionNames() {
    return dimensions.stream()
        .map(dimensionInfo -> dimensionInfo.getDimensionKey().location())
        .filter(location -> location.getNamespace().equals(Constants.MOD_ID))
        .map(ResourceLocation::getPath)
        .toList();
  }

  public static DimensionType loadDimensionType(
      final Path path, final RegistryAccess registryAccess) throws IllegalArgumentException {
    try {
      String json = Files.readString(path);
      JsonElement element = JsonParser.parseString(json);
      RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
      var result = DimensionType.DIRECT_CODEC.parse(ops, element);
      if (result.error().isPresent()) {
        throw new IllegalArgumentException(
            "Error parsing DimensionType: " + result.error().get().message());
      }
      return result
          .result()
          .orElseThrow(() -> new IllegalArgumentException("Failed to parse DimensionType"));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to load DimensionType from path: " + path, e);
    }
  }

  public static LevelStem loadLevelStem(final Path path, final RegistryAccess registryAccess)
      throws IllegalArgumentException {
    try {
      String json = Files.readString(path);
      JsonElement element = JsonParser.parseString(json);
      RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
      var result = LevelStem.CODEC.parse(ops, element);
      if (result.error().isPresent()) {
        throw new IllegalArgumentException(
            "Error parsing LevelStem: " + result.error().get().message());
      }
      return result
          .result()
          .orElseThrow(() -> new IllegalArgumentException("Failed to parse LevelStem"));
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to load LevelStem from path: " + path, e);
    }
  }

  private static ServerLevel getServerLevel(final ResourceKey<Level> levelKey) {
    return minecraftServer == null ? null : minecraftServer.getLevel(levelKey);
  }

  public static void clear() {
    log.debug("Clearing all dimensions ...");
    dimensions.clear();
  }

  public static boolean dimensionExists(final MinecraftServer server, final String dimension) {
    if (server == null || dimension == null) {
      return false;
    }

    if (getDimensionInfoData(dimension) != null) {
      return true;
    }

    try {
      ResourceLocation dimensionResourceLocation = ResourceLocation.parse(dimension);
      ServerLevel level =
          server.getLevel(
              ResourceKey.create(
                  net.minecraft.core.registries.Registries.DIMENSION, dimensionResourceLocation));
      return level != null;
    } catch (Exception e) {
      log.debug("Failed to parse dimension resource location: {}", dimension);
      return false;
    }
  }

  public static boolean setDimensionSpawnPoint(
      final String dimensionName, final BlockPos spawnPoint) {
    DimensionInfoData dimensionInfo = getDimensionInfoData(dimensionName);
    if (dimensionInfo == null) {
      return false;
    }

    DimensionInfoData updatedInfo = dimensionInfo.withSpawnPoint(spawnPoint);
    dimensions.remove(dimensionInfo);
    dimensions.add(updatedInfo);

    // Update storage
    DimensionDataStorage.get().addDimension(updatedInfo);

    return true;
  }

  public static void clearAllCache() {
    log.info("Clearing dimension manager cache for world switch...");
    dimensions.clear();
    minecraftServer = null;
  }
}
