package de.markusbordihn.worlddimensionnexus.dimension;

import de.markusbordihn.worlddimensionnexus.Constants;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.storage.ServerLevelData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DimensionManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  public static ServerLevel createFlatDimension(MinecraftServer server, String dimensionName) {
    ResourceKey<Level> levelKey =
        ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, dimensionName));
    ResourceKey<DimensionType> dimensionTypeKey = BuiltinDimensionTypes.OVERWORLD;

    // Define the level generator settings and level stem for the flat dimension.
    FlatLevelGeneratorSettings generatorSettings =
        createFlatGeneratorSettings(
            server.registryAccess().lookupOrThrow(Registries.BIOME),
            server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET),
            server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE));
    LevelStem levelStem =
        new LevelStem(
            server
                .registryAccess()
                .registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(dimensionTypeKey),
            new FlatLevelSource(generatorSettings));

    // Create server level with the overworld's properties
    ServerLevel overworld = server.overworld();
    ServerLevel newLevel =
        new ServerLevel(
            server,
            server.executor,
            server.storageSource,
            (ServerLevelData) overworld.getLevelData(),
            levelKey,
            levelStem,
            overworld.getChunkSource().chunkMap.progressListener,
            false,
            overworld.getSeed(),
            List.of(),
            overworld.isDebug(),
            null);

    // Add the new dimension to the server
    server.levels.put(levelKey, newLevel);
    log.info("Created and loaded new dimension: {}", levelKey.location());
    return newLevel;
  }

  private static FlatLevelGeneratorSettings createFlatGeneratorSettings(
      HolderGetter<Biome> biomeGetter,
      HolderGetter<StructureSet> structureGetter,
      HolderGetter<PlacedFeature> featureGetter) {

    HolderSet<StructureSet> structures =
        HolderSet.direct(
            structureGetter.getOrThrow(BuiltinStructureSets.STRONGHOLDS),
            structureGetter.getOrThrow(BuiltinStructureSets.VILLAGES));

    FlatLevelGeneratorSettings settings =
        new FlatLevelGeneratorSettings(
            Optional.of(structures),
            biomeGetter.getOrThrow(Biomes.PLAINS),
            FlatLevelGeneratorSettings.createLakesList(featureGetter));
    settings
        .getLayersInfo()
        .addAll(
            List.of(
                new FlatLayerInfo(1, Blocks.BEDROCK),
                new FlatLayerInfo(2, Blocks.DIRT),
                new FlatLayerInfo(1, Blocks.GRASS_BLOCK)));
    settings.updateLayers();

    return settings;
  }
}
