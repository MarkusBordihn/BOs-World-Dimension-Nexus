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

import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.data.worldgen.WorldgenConfig;
import de.markusbordihn.worlddimensionnexus.data.worldgen.WorldgenConfigLoader;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class ChunkGeneratorHelper {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Chunk Generator Helper");

  private ChunkGeneratorHelper() {}

  public static ChunkGenerator getDefault(
      final MinecraftServer server, final ChunkGeneratorType type) {
    Optional<ChunkGenerator> jsonChunkGenerator =
        JsonChunkGeneratorLoader.loadFromJson(server, type);
    return jsonChunkGenerator.orElseGet(
        () ->
            switch (type) {
              case FLAT -> getFlatChunkGenerator(server);
              case DEBUG -> getDebugChunkGenerator(server);
              case SKYBLOCK -> getSkyblockChunkGenerator(server);
              case FLOATING_ISLANDS -> getFloatingIslandsChunkGenerator(server);
              default -> getCustomChunkGenerator(server, type);
            });
  }

  public static ChunkGenerator getCustomChunkGenerator(
      final MinecraftServer server, final ChunkGeneratorType type) {
    Optional<ChunkGenerator> jsonChunkGenerator =
        JsonChunkGeneratorLoader.loadFromJson(server, type);
    if (jsonChunkGenerator.isPresent()) {
      log.debug(
          "Using custom chunk generator '{}' for type '{}'.",
          jsonChunkGenerator.get().getClass().getSimpleName(),
          type.getName());
      return jsonChunkGenerator.get();
    }

    Optional<WorldgenConfig> worldgenConfiguration = WorldgenConfigLoader.getConfig(type);
    if (worldgenConfiguration.isPresent()) {
      log.debug(
          "Using chunk generator '{}' for type '{}' with configuration '{}'.",
          worldgenConfiguration.get().getClass().getSimpleName(),
          type.getName(),
          worldgenConfiguration.get());
      return createChunkGeneratorFromConfig(server, worldgenConfiguration.get());
    }

    log.warn(
        "No custom chunk generator or worldgen configuration found for type '{}', falling back to flat generator.",
        type.getName());
    return getFlatChunkGenerator(server);
  }

  private static ChunkGenerator createChunkGeneratorFromConfig(
      final MinecraftServer server, final WorldgenConfig config) {
    if (config.noiseSettings().isPresent()) {
      return getNoiseChunkGenerator(server, config.type(), config.noiseSettings().get());
    }
    return getFlatChunkGenerator(server);
  }

  public static ChunkGenerator getNoiseChunkGenerator(
      final MinecraftServer server,
      final ChunkGeneratorType type,
      final ResourceLocation noiseSettingsKey) {
    HolderGetter<NoiseGeneratorSettings> noiseGetter =
        server.registryAccess().lookupOrThrow(Registries.NOISE_SETTINGS);
    HolderGetter<Biome> biomeGetter = server.registryAccess().lookupOrThrow(Registries.BIOME);

    var noiseSettings =
        noiseGetter.getOrThrow(ResourceKey.create(Registries.NOISE_SETTINGS, noiseSettingsKey));
    BiomeSource biomeSource = new FixedBiomeSource(biomeGetter.getOrThrow(Biomes.PLAINS));

    return new NoiseBasedChunkGenerator(biomeSource, noiseSettings);
  }

  public static ChunkGenerator getDebugChunkGenerator(final MinecraftServer server) {
    HolderGetter<Biome> biomeGetter = server.registryAccess().lookupOrThrow(Registries.BIOME);
    return new DebugLevelSource(biomeGetter.getOrThrow(Biomes.PLAINS));
  }

  public static ChunkGenerator getSkyblockChunkGenerator(final MinecraftServer server) {
    HolderGetter<Biome> biomeGetter = server.registryAccess().lookupOrThrow(Registries.BIOME);
    return new SkyblockChunkGenerator(biomeGetter);
  }

  public static ChunkGenerator getFloatingIslandsChunkGenerator(final MinecraftServer server) {
    HolderGetter<Biome> biomeGetter = server.registryAccess().lookupOrThrow(Registries.BIOME);
    return new FloatingIslandsChunkGenerator(biomeGetter);
  }

  public static ChunkGenerator getFlatChunkGenerator(final MinecraftServer server) {
    return getFlatChunkGenerator(
        server,
        List.of(
            new FlatLayerInfo(1, Blocks.BEDROCK),
            new FlatLayerInfo(2, Blocks.DIRT),
            new FlatLayerInfo(1, Blocks.GRASS_BLOCK)),
        Biomes.PLAINS,
        List.of(BuiltinStructureSets.STRONGHOLDS, BuiltinStructureSets.VILLAGES),
        true);
  }

  public static ChunkGenerator getFlatChunkGenerator(
      final MinecraftServer server,
      final List<FlatLayerInfo> layers,
      final ResourceKey<Biome> biomeKey,
      final List<ResourceKey<StructureSet>> structureKeys,
      final boolean lakes) {
    HolderGetter<Biome> biomeGetter = server.registryAccess().lookupOrThrow(Registries.BIOME);
    HolderGetter<StructureSet> structureGetter =
        server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
    HolderGetter<PlacedFeature> featureGetter =
        server.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
    HolderSet<StructureSet> structures =
        HolderSet.direct(
            structureKeys.stream().map(structureGetter::getOrThrow).toArray(Holder[]::new));

    FlatLevelGeneratorSettings settings =
        new FlatLevelGeneratorSettings(
            Optional.of(structures),
            biomeGetter.getOrThrow(biomeKey),
            lakes ? FlatLevelGeneratorSettings.createLakesList(featureGetter) : List.of());
    settings.getLayersInfo().addAll(layers);
    settings.updateLayers();

    return new FlatLevelSource(settings);
  }
}
