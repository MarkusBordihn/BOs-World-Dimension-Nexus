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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

public record DimensionInfoData(
    String namespace,
    String path,
    String type,
    String name,
    String description,
    boolean isCustom,
    ChunkGeneratorType chunkGeneratorType,
    boolean requiresHotInjectionSync,
    BlockPos spawnPoint) {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("DimensionInfoData");

  public static final String NAMESPACE_TAG = "namespace";
  public static final String PATH_TAG = "path";
  public static final String TYPE_TAG = "type";
  public static final String NAME_TAG = "name";
  public static final String DESCRIPTION_TAG = "description";
  public static final String IS_CUSTOM_TAG = "isCustom";
  public static final String CHUNK_GENERATOR_TYPE_TAG = "chunkGeneratorType";
  public static final String HOT_INJECTION_SYNC_TAG = "requiresHotInjectionSync";
  public static final String SPAWN_POINT_TAG = "spawnPoint";
  public static final String DEFAULT_TYPE = "minecraft:overworld";
  public static final String DEFAULT_EMPTY_STRING = "";
  public static final boolean DEFAULT_IS_CUSTOM = true;
  public static final ChunkGeneratorType DEFAULT_CHUNK_GENERATOR_TYPE = ChunkGeneratorType.FLAT;
  public static final boolean DEFAULT_HOT_INJECTION_SYNC = true;
  public static final BlockPos DEFAULT_SPAWN_POINT = new BlockPos(0, 100, 0);
  public static final Codec<DimensionInfoData> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.STRING.fieldOf(NAMESPACE_TAG).forGetter(DimensionInfoData::namespace),
                      Codec.STRING.fieldOf(PATH_TAG).forGetter(DimensionInfoData::path),
                      Codec.STRING.fieldOf(TYPE_TAG).forGetter(DimensionInfoData::type),
                      Codec.STRING
                          .optionalFieldOf(NAME_TAG, DEFAULT_EMPTY_STRING)
                          .forGetter(DimensionInfoData::name),
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
                          .forGetter(DimensionInfoData::spawnPoint))
                  .apply(instance, DimensionInfoData::new));

  public static DimensionInfoData forImport(
      final String namespace, final String path, final String type) {
    return new DimensionInfoData(
        namespace,
        path,
        type,
        DEFAULT_EMPTY_STRING,
        DEFAULT_EMPTY_STRING,
        DEFAULT_IS_CUSTOM,
        DEFAULT_CHUNK_GENERATOR_TYPE,
        DEFAULT_HOT_INJECTION_SYNC,
        DEFAULT_SPAWN_POINT);
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

    String dimensionType = getDimensionTypeForChunkGenerator(chunkGeneratorType);
    return new DimensionInfoData(
        resourceLocation.getNamespace(),
        resourceLocation.getPath(),
        dimensionType,
        dimensionName,
        DEFAULT_EMPTY_STRING,
        DEFAULT_IS_CUSTOM,
        chunkGeneratorType,
        DEFAULT_HOT_INJECTION_SYNC,
        DEFAULT_SPAWN_POINT);
  }

  private static String getDimensionTypeForChunkGenerator(
      final ChunkGeneratorType chunkGeneratorType) {
    return switch (chunkGeneratorType) {
      case LOBBY -> "world_dimension_nexus:lobby_dimension_type";
      case VOID -> "minecraft:the_end";
      case SKYBLOCK -> "minecraft:overworld";
      case CAVE -> "minecraft:overworld_caves";
      case FLOATING_ISLANDS -> "minecraft:the_end";
      default -> DEFAULT_TYPE;
    };
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

    return new DimensionInfoData(
        jsonObject.has(NAMESPACE_TAG)
            ? jsonObject.get(NAMESPACE_TAG).getAsString()
            : DEFAULT_EMPTY_STRING,
        jsonObject.has(PATH_TAG) ? jsonObject.get(PATH_TAG).getAsString() : DEFAULT_EMPTY_STRING,
        jsonObject.has(TYPE_TAG) ? jsonObject.get(TYPE_TAG).getAsString() : DEFAULT_TYPE,
        jsonObject.has(NAME_TAG) ? jsonObject.get(NAME_TAG).getAsString() : DEFAULT_EMPTY_STRING,
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
        spawnPoint);
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty(NAMESPACE_TAG, namespace);
    json.addProperty(PATH_TAG, path);
    json.addProperty(TYPE_TAG, type);
    json.addProperty(NAME_TAG, name);
    json.addProperty(DESCRIPTION_TAG, description);
    json.addProperty(IS_CUSTOM_TAG, isCustom);
    json.addProperty(CHUNK_GENERATOR_TYPE_TAG, chunkGeneratorType.name());
    json.addProperty(HOT_INJECTION_SYNC_TAG, false);
    json.addProperty(
        SPAWN_POINT_TAG, spawnPoint.getX() + "," + spawnPoint.getY() + "," + spawnPoint.getZ());
    return json;
  }

  public ResourceLocation getResourceLocation() {
    return ResourceLocation.fromNamespaceAndPath(namespace, path);
  }

  public ResourceLocation getTypeResourceLocation() {
    try {
      return ResourceLocation.parse(type);
    } catch (Exception e) {
      log.warn(
          "Failed to parse DimensionType resource location '{}', using as-is: {}",
          type,
          e.getMessage());
      if (type.contains(":")) {
        String[] parts = type.split(":", 2);
        return ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);
      } else {
        log.warn("Invalid DimensionType format '{}', falling back to overworld", type);
        return ResourceLocation.fromNamespaceAndPath("minecraft", "overworld");
      }
    }
  }

  public ResourceKey<Level> getDimensionKey() {
    return ResourceKey.create(Registries.DIMENSION, getResourceLocation());
  }

  public DimensionInfoData withoutHotInjectionSync() {
    return new DimensionInfoData(
        namespace, path, type, name, description, isCustom, chunkGeneratorType, false, spawnPoint);
  }

  public DimensionInfoData withSpawnPoint(final BlockPos newSpawnPoint) {
    return new DimensionInfoData(
        namespace,
        path,
        type,
        name,
        description,
        isCustom,
        chunkGeneratorType,
        requiresHotInjectionSync,
        newSpawnPoint);
  }

  public ChunkGenerator getChunkGenerator(final MinecraftServer minecraftServer) {
    return ChunkGeneratorHelper.getDefault(minecraftServer, chunkGeneratorType);
  }

  public Holder<DimensionType> getDimensionTypeHolder(final MinecraftServer minecraftServer) {
    HolderGetter<DimensionType> dimensionTypeGetter =
        minecraftServer.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);

    ResourceLocation typeLocation = getTypeResourceLocation();
    ResourceKey<DimensionType> typeKey =
        ResourceKey.create(Registries.DIMENSION_TYPE, typeLocation);

    try {
      return dimensionTypeGetter.getOrThrow(typeKey);
    } catch (Exception e) {
      Optional<DimensionType> jsonDimensionType =
          JsonChunkGeneratorLoader.loadDimensionTypeFromJson(minecraftServer, chunkGeneratorType);

      if (jsonDimensionType.isPresent()) {
        log.warn(
            "JSON DimensionType loaded but not registered. Using overworld type for network compatibility. "
                + "Consider registering the DimensionType in the registry for: {}",
            typeKey.location());
      }

      return dimensionTypeGetter.getOrThrow(BuiltinDimensionTypes.OVERWORLD);
    }
  }
}
