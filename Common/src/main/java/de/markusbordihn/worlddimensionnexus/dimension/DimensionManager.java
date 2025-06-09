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
import de.markusbordihn.worlddimensionnexus.saveddata.DimensionDataStorage;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

  public static void sync(MinecraftServer minecraftServer, List<DimensionInfoData> dimensionList) {

    // Validate and store server instance before synchronizing dimensions.
    if (minecraftServer == null) {
      log.error("Minecraft server is null, cannot synchronize dimensions.");
      return;
    }
    DimensionManager.minecraftServer = minecraftServer;

    // Validate the dimension list.
    if (dimensionList == null || dimensionList.isEmpty()) {
      log.warn(
          "No dimensions to synchronize: list is {}.", (dimensionList == null ? "null" : "empty"));
      return;
    }

    // Synchronize dimensions by clearing existing ones and creating new ones.
    log.info("Synchronizing {} dimensions ...", dimensionList.size());
    clear();
    for (DimensionInfoData dimensionInfo : dimensionList) {
      addOrCreateDimension(dimensionInfo, false);
    }
  }

  public static ServerLevel addOrCreateDimension(String dimensionName) {
    return addOrCreateDimension(new DimensionInfoData(dimensionName), true);
  }

  public static ServerLevel addOrCreateDimension(
      DimensionInfoData dimensionInfo, boolean updateStorage) {
    if (dimensionInfo == null) {
      log.warn("DimensionInfoData is null, skipping ...");
      return null;
    }

    // Check if the dimension already exists and return it if it does.
    if (dimensions.contains(dimensionInfo)) {
      log.info("Dimension {} already exists, skipping ...", dimensionInfo);
      return getServerLevel(dimensionInfo.name());
    }

    // Create the dimension if it does not exist.
    ChunkGenerator chunkGenerator = dimensionInfo.getChunkGenerator(minecraftServer);
    LevelStem levelStem =
        new LevelStem(dimensionInfo.getDimensionTypeHolder(minecraftServer), chunkGenerator);

    // Create server level with the overworld's properties
    ServerLevel overworld = minecraftServer.overworld();
    ServerLevel newLevel =
        new ServerLevel(
            minecraftServer,
            minecraftServer.executor,
            minecraftServer.storageSource,
            (ServerLevelData) overworld.getLevelData(),
            dimensionInfo.name(),
            levelStem,
            overworld.getChunkSource().chunkMap.progressListener,
            false,
            overworld.getSeed(),
            List.of(),
            overworld.isDebug(),
            null);

    // Add the new dimension to the server
    minecraftServer.levels.put(dimensionInfo.name(), newLevel);
    dimensions.add(dimensionInfo);

    // Add the dimension to the storage if required.
    if (updateStorage) {
      DimensionDataStorage.get().addDimension(dimensionInfo);
    }

    log.info("Created and loaded new dimension: {}", dimensionInfo.name().location());
    return newLevel;
  }

  public static boolean removeDimension(String name) {
    return removeDimension(getDimensionInfoData(name));
  }

  public static boolean removeDimension(DimensionInfoData dimensionInfoData) {
    if (dimensionInfoData == null) {
      log.warn("DimensionInfoData is null, skipping ...");
      return false;
    }

    // Check if the dimension exists and remove it.
    if (dimensions.remove(dimensionInfoData)) {
      ResourceKey<Level> levelKey = dimensionInfoData.name();
      ServerLevel serverLevel = getServerLevel(levelKey);
      if (serverLevel != null) {
        minecraftServer.levels.remove(levelKey);
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

  public static DimensionInfoData getDimensionInfoData(String name) {
    for (DimensionInfoData dimensionInfo : dimensions) {
      ResourceLocation dimensionLocation = dimensionInfo.name().location();
      if (dimensionLocation.getNamespace().equals(Constants.MOD_ID)
          && dimensionLocation.getPath().equals(name)) {
        return dimensionInfo;
      }
    }
    return null;
  }

  public static ServerLevel getDimensionServerLevel(String name) {
    DimensionInfoData dimensionInfo = getDimensionInfoData(name);
    if (dimensionInfo != null) {
      return getServerLevel(dimensionInfo.name());
    }
    log.warn("Dimension {} not found, returning null.", name);
    return null;
  }

  public static List<ResourceKey<Level>> getDimensions(MinecraftServer server) {
    return server.levelKeys().stream()
        .filter(levelKey -> !levelKey.equals(Level.OVERWORLD))
        .toList();
  }

  public static Collection<String> getDimensionNames(MinecraftServer server) {
    return dimensions.stream()
        .map(dimensionInfo -> dimensionInfo.name().location())
        .filter(location -> location.getNamespace().equals(Constants.MOD_ID))
        .map(ResourceLocation::getPath)
        .toList();
  }

  public static DimensionType loadDimensionType(Path path, RegistryAccess registryAccess)
      throws Exception {
    String json = Files.readString(path);
    JsonElement element = JsonParser.parseString(json);
    RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
    var result = DimensionType.DIRECT_CODEC.parse(ops, element);
    if (result.result().isEmpty()) {
      throw new IllegalArgumentException(
          "Fehler beim Parsen von DimensionType: " + result.error().get().message());
    }
    return result.result().get();
  }

  public static LevelStem loadLevelStem(Path path, RegistryAccess registryAccess) throws Exception {
    String json = Files.readString(path);
    JsonElement element = JsonParser.parseString(json);
    RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
    var result = LevelStem.CODEC.parse(ops, element);
    if (result.result().isEmpty()) {
      throw new IllegalArgumentException(
          "Fehler beim Parsen von LevelStem: " + result.error().get().message());
    }
    return result.result().get();
  }

  private static ServerLevel getServerLevel(ResourceKey<Level> levelKey) {
    return minecraftServer == null ? null : minecraftServer.getLevel(levelKey);
  }

  public static void clear() {
    log.debug("Clearing all dimensions ...");
    dimensions.clear();
  }
}
