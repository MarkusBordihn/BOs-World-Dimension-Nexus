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

package de.markusbordihn.worlddimensionnexus.data.dimension;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.levelgen.ChunkGeneratorHelper;
import de.markusbordihn.worlddimensionnexus.levelgen.JsonChunkGeneratorLoader;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

public record DimensionInfoData(
    ResourceLocation resourceLocation,
    ResourceKey<DimensionType> dimensionTypeKey,
    String displayName,
    String description,
    boolean isCustom,
    ChunkGeneratorType chunkGeneratorType,
    boolean requiresHotInjectionSync,
    BlockPos spawnPoint,
    GameType gameType) {

  public static final String RESOURCE_LOCATION_TAG = "resourceLocation";
  public static final String TYPE_TAG = "type";
  public static final String DISPLAY_NAME_TAG = "displayName";
  public static final String DESCRIPTION_TAG = "description";
  public static final String IS_CUSTOM_TAG = "isCustom";
  public static final String CHUNK_GENERATOR_TYPE_TAG = "chunkGeneratorType";
  public static final String HOT_INJECTION_SYNC_TAG = "requiresHotInjectionSync";
  public static final String SPAWN_POINT_TAG = "spawnPoint";
  public static final String GAME_TYPE_TAG = "gameType";
  public static final String DEFAULT_TYPE = "minecraft:overworld";
  public static final String DEFAULT_EMPTY_STRING = "";
  public static final boolean DEFAULT_IS_CUSTOM = true;
  public static final ChunkGeneratorType DEFAULT_CHUNK_GENERATOR_TYPE = ChunkGeneratorType.FLAT;
  public static final boolean DEFAULT_HOT_INJECTION_SYNC = true;
  public static final BlockPos DEFAULT_SPAWN_POINT = new BlockPos(0, 100, 0);
  public static final GameType DEFAULT_GAME_TYPE = GameType.SURVIVAL;
  public static final ResourceKey<DimensionType> DEFAULT_DIMENSION_TYPE_KEY =
      BuiltinDimensionTypes.OVERWORLD;
  public static final Codec<DimensionInfoData> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      ResourceLocation.CODEC
                          .fieldOf(RESOURCE_LOCATION_TAG)
                          .forGetter(DimensionInfoData::resourceLocation),
                      ResourceKey.codec(Registries.DIMENSION_TYPE)
                          .optionalFieldOf(TYPE_TAG, DEFAULT_DIMENSION_TYPE_KEY)
                          .forGetter(DimensionInfoData::dimensionTypeKey),
                      Codec.STRING
                          .optionalFieldOf(DISPLAY_NAME_TAG, DEFAULT_EMPTY_STRING)
                          .forGetter(DimensionInfoData::displayName),
                      Codec.STRING
                          .optionalFieldOf(DESCRIPTION_TAG, DEFAULT_EMPTY_STRING)
                          .forGetter(DimensionInfoData::description),
                      Codec.BOOL
                          .optionalFieldOf(IS_CUSTOM_TAG, DEFAULT_IS_CUSTOM)
                          .forGetter(DimensionInfoData::isCustom),
                      ChunkGeneratorType.CODEC
                          .optionalFieldOf(CHUNK_GENERATOR_TYPE_TAG, DEFAULT_CHUNK_GENERATOR_TYPE)
                          .forGetter(DimensionInfoData::chunkGeneratorType),
                      Codec.BOOL
                          .optionalFieldOf(HOT_INJECTION_SYNC_TAG, DEFAULT_HOT_INJECTION_SYNC)
                          .forGetter(DimensionInfoData::requiresHotInjectionSync),
                      BlockPos.CODEC
                          .optionalFieldOf(SPAWN_POINT_TAG, DEFAULT_SPAWN_POINT)
                          .forGetter(DimensionInfoData::spawnPoint),
                      GameType.CODEC
                          .optionalFieldOf(GAME_TYPE_TAG, DEFAULT_GAME_TYPE)
                          .forGetter(DimensionInfoData::gameType))
                  .apply(instance, DimensionInfoData::new));
  private static final PrefixLogger log = ModLogger.getPrefixLogger("DimensionInfoData");

  public static DimensionInfoData forImport(
      final String namespace, final String path, final String type) {
    ResourceKey<DimensionType> dimensionTypeKey;
    try {
      dimensionTypeKey = createDimensionTypeKey(type);
    } catch (Exception e) {
      log.warn(
          "Failed to parse dimension type for import '{}', using default: {}",
          type,
          e.getMessage());
      dimensionTypeKey = DEFAULT_DIMENSION_TYPE_KEY;
    }

    return new DimensionInfoData(
        ResourceLocation.fromNamespaceAndPath(namespace, path),
        dimensionTypeKey,
        DEFAULT_EMPTY_STRING,
        DEFAULT_EMPTY_STRING,
        DEFAULT_IS_CUSTOM,
        DEFAULT_CHUNK_GENERATOR_TYPE,
        DEFAULT_HOT_INJECTION_SYNC,
        DEFAULT_SPAWN_POINT,
        DEFAULT_GAME_TYPE);
  }

  public static DimensionInfoData fromDimensionName(final String dimensionName) {
    return fromDimensionNameAndType(dimensionName, DEFAULT_CHUNK_GENERATOR_TYPE);
  }

  public static DimensionInfoData fromDimensionNameAndType(
      final String dimensionName, final ChunkGeneratorType chunkGeneratorType) {
    ResourceLocation resourceLocation;
    if (dimensionName.contains(":")) {
      resourceLocation = ResourceLocation.parse(dimensionName);
    } else {
      resourceLocation = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, dimensionName);
    }

    ResourceKey<DimensionType> dimensionTypeKey =
        createDimensionTypeKey(chunkGeneratorType.getDimensionType());
    return new DimensionInfoData(
        resourceLocation,
        dimensionTypeKey,
        dimensionName,
        DEFAULT_EMPTY_STRING,
        DEFAULT_IS_CUSTOM,
        chunkGeneratorType,
        DEFAULT_HOT_INJECTION_SYNC,
        DEFAULT_SPAWN_POINT,
        DEFAULT_GAME_TYPE);
  }

  private static ResourceKey<DimensionType> createDimensionTypeKey(
      final String dimensionTypeString) {
    ResourceLocation typeLocation = ResourceLocation.parse(dimensionTypeString);
    return ResourceKey.create(Registries.DIMENSION_TYPE, typeLocation);
  }

  public static DimensionInfoData fromJson(final JsonObject jsonObject) {
    ChunkGeneratorType chunkGeneratorType = DEFAULT_CHUNK_GENERATOR_TYPE;
    if (jsonObject.has(CHUNK_GENERATOR_TYPE_TAG)) {
      try {
        chunkGeneratorType =
            ChunkGeneratorType.valueOf(
                jsonObject.get(CHUNK_GENERATOR_TYPE_TAG).getAsString().toUpperCase());
      } catch (IllegalArgumentException e) {
        // Use default if invalid type
      }
    }

    BlockPos spawnPoint = DEFAULT_SPAWN_POINT;
    if (jsonObject.has(SPAWN_POINT_TAG)) {
      try {
        String[] coords = jsonObject.get(SPAWN_POINT_TAG).getAsString().split(",");
        if (coords.length == 3) {
          int x = Integer.parseInt(coords[0].trim());
          int y = Integer.parseInt(coords[1].trim());
          int z = Integer.parseInt(coords[2].trim());
          spawnPoint = new BlockPos(x, y, z);
        }
      } catch (Exception e) {
        // Use default if invalid BlockPos
      }
    }

    GameType gameType = DEFAULT_GAME_TYPE;
    if (jsonObject.has(GAME_TYPE_TAG)) {
      try {
        String gameTypeString = jsonObject.get(GAME_TYPE_TAG).getAsString();
        gameType = GameType.byName(gameTypeString, DEFAULT_GAME_TYPE);
      } catch (Exception e) {
        // Use default if invalid gameType
      }
    }

    ResourceKey<DimensionType> dimensionTypeKey = DEFAULT_DIMENSION_TYPE_KEY;
    if (jsonObject.has(TYPE_TAG)) {
      try {
        String typeString = jsonObject.get(TYPE_TAG).getAsString();
        dimensionTypeKey = createDimensionTypeKey(typeString);
      } catch (Exception e) {
        log.warn("Failed to parse dimension type from JSON: {}, using default", e.getMessage());
      }
    }

    return new DimensionInfoData(
        jsonObject.has(RESOURCE_LOCATION_TAG)
            ? ResourceLocation.parse(jsonObject.get(RESOURCE_LOCATION_TAG).getAsString())
            : ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, DEFAULT_EMPTY_STRING),
        dimensionTypeKey,
        jsonObject.has(DISPLAY_NAME_TAG)
            ? jsonObject.get(DISPLAY_NAME_TAG).getAsString()
            : DEFAULT_EMPTY_STRING,
        jsonObject.has(DESCRIPTION_TAG)
            ? jsonObject.get(DESCRIPTION_TAG).getAsString()
            : DEFAULT_EMPTY_STRING,
        jsonObject.has(IS_CUSTOM_TAG)
            ? jsonObject.get(IS_CUSTOM_TAG).getAsBoolean()
            : DEFAULT_IS_CUSTOM,
        chunkGeneratorType,
        jsonObject.has(HOT_INJECTION_SYNC_TAG)
            ? jsonObject.get(HOT_INJECTION_SYNC_TAG).getAsBoolean()
            : DEFAULT_HOT_INJECTION_SYNC,
        spawnPoint,
        gameType);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty(RESOURCE_LOCATION_TAG, resourceLocation.toString());
    json.addProperty(TYPE_TAG, dimensionTypeKey.location().toString());
    json.addProperty(DISPLAY_NAME_TAG, displayName);
    json.addProperty(DESCRIPTION_TAG, description);
    json.addProperty(IS_CUSTOM_TAG, isCustom);
    json.addProperty(CHUNK_GENERATOR_TYPE_TAG, chunkGeneratorType.name());
    json.addProperty(HOT_INJECTION_SYNC_TAG, false);
    json.addProperty(
        SPAWN_POINT_TAG, spawnPoint.getX() + "," + spawnPoint.getY() + "," + spawnPoint.getZ());
    json.addProperty(GAME_TYPE_TAG, gameType.getName());
    return json;
  }

  public ResourceLocation getResourceLocation() {
    return resourceLocation;
  }

  public ResourceLocation getTypeResourceLocation() {
    return dimensionTypeKey.location();
  }

  public ResourceKey<Level> getDimensionKey() {
    return ResourceKey.create(Registries.DIMENSION, getResourceLocation());
  }

  public DimensionInfoData withoutHotInjectionSync() {
    return new DimensionInfoData(
        resourceLocation,
        dimensionTypeKey,
        displayName,
        description,
        isCustom,
        chunkGeneratorType,
        false,
        spawnPoint,
        gameType);
  }

  public DimensionInfoData withSpawnPoint(final BlockPos newSpawnPoint) {
    return new DimensionInfoData(
        resourceLocation,
        dimensionTypeKey,
        displayName,
        description,
        isCustom,
        chunkGeneratorType,
        requiresHotInjectionSync,
        newSpawnPoint,
        gameType);
  }

  public DimensionInfoData withGameType(final GameType newGameType) {
    return new DimensionInfoData(
        resourceLocation,
        dimensionTypeKey,
        displayName,
        description,
        isCustom,
        chunkGeneratorType,
        requiresHotInjectionSync,
        spawnPoint,
        newGameType);
  }

  public ChunkGenerator getChunkGenerator(final MinecraftServer minecraftServer) {
    return ChunkGeneratorHelper.getDefault(minecraftServer, chunkGeneratorType);
  }

  public Holder<DimensionType> getDimensionTypeHolder(final MinecraftServer minecraftServer) {
    HolderGetter<DimensionType> dimensionTypeGetter =
        minecraftServer.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);

    try {
      return dimensionTypeGetter.getOrThrow(dimensionTypeKey);
    } catch (Exception e) {
      Optional<DimensionType> jsonDimensionType =
          JsonChunkGeneratorLoader.loadDimensionTypeFromJson(minecraftServer, chunkGeneratorType);

      if (jsonDimensionType.isPresent()) {
        log.warn(
            "JSON DimensionType loaded but not registered. Using overworld type for network compatibility. "
                + "Consider registering the DimensionType in the registry for: {}",
            dimensionTypeKey.location());
      }

      return dimensionTypeGetter.getOrThrow(BuiltinDimensionTypes.OVERWORLD);
    }
  }
}
