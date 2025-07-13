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

import com.mojang.serialization.MapCodec;
import de.markusbordihn.worlddimensionnexus.config.FloatingIslandsChunkGeneratorConfig;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

public class FloatingIslandsChunkGenerator extends ChunkGenerator {

  public static final MapCodec<FloatingIslandsChunkGenerator> CODEC =
      MapCodec.unit(
          () -> {
            throw new UnsupportedOperationException(
                "FloatingIslandsChunkGenerator codec not supported");
          });

  private final PerlinSimplexNoise islandNoise;
  private final PerlinSimplexNoise heightNoise;
  private final PerlinSimplexNoise detailNoise;
  private final RandomSource vegetationRandom;

  public FloatingIslandsChunkGenerator(final HolderGetter<Biome> biomeGetter) {
    super(new FixedBiomeSource(biomeGetter.getOrThrow(Biomes.END_HIGHLANDS)));

    this.islandNoise =
        new PerlinSimplexNoise(
            RandomSource.create(FloatingIslandsChunkGeneratorConfig.ISLAND_NOISE_SEED),
            List.of(-4, -3, -2, -1, 0));
    this.heightNoise =
        new PerlinSimplexNoise(
            RandomSource.create(FloatingIslandsChunkGeneratorConfig.HEIGHT_NOISE_SEED),
            List.of(-2, -1, 0, 1));
    this.detailNoise =
        new PerlinSimplexNoise(
            RandomSource.create(FloatingIslandsChunkGeneratorConfig.DETAIL_NOISE_SEED),
            List.of(-1, 0, 1, 2));
    this.vegetationRandom = RandomSource.create(12345L);
  }

  @Override
  protected MapCodec<? extends ChunkGenerator> codec() {
    return CODEC;
  }

  @Override
  public void addDebugScreenInfo(
      final List<String> list, final RandomState randomState, final BlockPos blockPos) {
    list.add("Floating Islands Generator");
    list.add("Islands: " + getIslandDensityAt(blockPos.getX(), blockPos.getZ()));
  }

  @Override
  public void applyCarvers(
      final WorldGenRegion region,
      final long seed,
      final RandomState randomState,
      final BiomeManager biomeManager,
      final StructureManager structureManager,
      final ChunkAccess chunk,
      final GenerationStep.Carving carving) {
    // No carving applied to floating islands to maintain their natural shape
  }

  @Override
  public CompletableFuture<ChunkAccess> createBiomes(
      final RandomState randomState,
      final Blender blender,
      final StructureManager structureManager,
      final ChunkAccess chunk) {
    return CompletableFuture.completedFuture(chunk);
  }

  @Override
  public void createStructures(
      final RegistryAccess registryAccess,
      final ChunkGeneratorStructureState structureState,
      final StructureManager structureManager,
      final ChunkAccess chunk,
      final StructureTemplateManager templateManager) {
    // No structures generated on floating islands initially
  }

  @Override
  public void buildSurface(
      final WorldGenRegion region,
      final StructureManager structureManager,
      final RandomState randomState,
      final ChunkAccess chunk) {
    // Surface building is handled in fillFromNoise method
  }

  @Override
  public void spawnOriginalMobs(final WorldGenRegion region) {
    // No original mob spawning on floating islands
  }

  @Override
  public int getGenDepth() {
    return 384;
  }

  @Override
  public CompletableFuture<ChunkAccess> fillFromNoise(
      final Blender blender,
      final RandomState randomState,
      final StructureManager structureManager,
      final ChunkAccess chunk) {

    ChunkPos chunkPos = chunk.getPos();

    for (int localX = 0; localX < 16; localX++) {
      for (int localZ = 0; localZ < 16; localZ++) {
        int worldX = chunkPos.getMinBlockX() + localX;
        int worldZ = chunkPos.getMinBlockZ() + localZ;

        generateColumnAt(chunk, localX, localZ, worldX, worldZ);
      }
    }

    return CompletableFuture.completedFuture(chunk);
  }

  private double getIslandDensityAt(final int x, final int z) {
    double noiseScale = FloatingIslandsChunkGeneratorConfig.NOISE_SCALE;

    double density = 0.0;
    density += islandNoise.getValue(x * noiseScale, z * noiseScale, false) * 0.5;
    density += islandNoise.getValue(x * noiseScale * 2, z * noiseScale * 2, false) * 0.3;
    density += islandNoise.getValue(x * noiseScale * 4, z * noiseScale * 4, false) * 0.2;

    int islandSpacing = FloatingIslandsChunkGeneratorConfig.ISLAND_SPACING;
    double clusterNoise =
        Math.sin(x * Math.PI / islandSpacing) * Math.sin(z * Math.PI / islandSpacing);
    density += clusterNoise * 0.4;

    return Mth.clamp(density, -1.0, 1.0);
  }

  private int getIslandHeightAt(final int x, final int z) {
    double noiseScale = FloatingIslandsChunkGeneratorConfig.NOISE_SCALE;
    double heightVariation =
        this.heightNoise.getValue(x * noiseScale * 0.5, z * noiseScale * 0.5, false);

    int minHeight = FloatingIslandsChunkGeneratorConfig.MIN_ISLAND_HEIGHT;
    int maxHeight = FloatingIslandsChunkGeneratorConfig.MAX_ISLAND_HEIGHT;

    int height = (int) (minHeight + (heightVariation + 1.0) * 0.5 * (maxHeight - minHeight));
    return Mth.clamp(height, minHeight, maxHeight);
  }

  private int getIslandThicknessAt(final int x, final int z) {
    int islandSpacing = FloatingIslandsChunkGeneratorConfig.ISLAND_SPACING;
    double radiusMultiplier = FloatingIslandsChunkGeneratorConfig.ISLAND_RADIUS_MULTIPLIER;

    double distance =
        Math.sqrt(
            (x % islandSpacing - islandSpacing / 2.0) * (x % islandSpacing - islandSpacing / 2.0)
                + (z % islandSpacing - islandSpacing / 2.0)
                    * (z % islandSpacing - islandSpacing / 2.0));

    double maxRadius = islandSpacing * radiusMultiplier;
    double thickness =
        Math.max(
            0,
            (maxRadius - distance)
                / maxRadius
                * FloatingIslandsChunkGeneratorConfig.DEFAULT_ISLAND_THICKNESS);

    return (int) Math.max(FloatingIslandsChunkGeneratorConfig.MIN_ISLAND_THICKNESS, thickness);
  }

  private void generateColumnAt(
      final ChunkAccess chunk,
      final int localX,
      final int localZ,
      final int worldX,
      final int worldZ) {
    double islandDensity = getIslandDensityAt(worldX, worldZ);

    if (islandDensity > FloatingIslandsChunkGeneratorConfig.ISLAND_DENSITY) {
      int islandHeight = getIslandHeightAt(worldX, worldZ);
      generateIslandColumn(
          chunk,
          localX,
          localZ,
          worldX,
          worldZ,
          islandHeight,
          getIslandThicknessAt(worldX, worldZ));
    }
  }

  private void generateIslandColumn(
      final ChunkAccess chunk,
      final int localX,
      final int localZ,
      final int worldX,
      final int worldZ,
      final int centerHeight,
      final int thickness) {

    double noiseScale = FloatingIslandsChunkGeneratorConfig.NOISE_SCALE;
    double surfaceVariation =
        this.detailNoise.getValue(worldX * noiseScale * 8, worldZ * noiseScale * 8, false);
    int heightVariation =
        (int) (surfaceVariation * FloatingIslandsChunkGeneratorConfig.HEIGHT_VARIATION_RANGE);

    int topY = centerHeight + heightVariation;
    int bottomY = Math.max(centerHeight - thickness, getMinY());

    for (int y = bottomY; y <= topY; y++) {
      BlockState blockState = getBlockStateForIslandLayer(y, bottomY, topY);
      chunk.setBlockState(new BlockPos(localX, y, localZ), blockState, false);
    }

    if (vegetationRandom.nextFloat() < FloatingIslandsChunkGeneratorConfig.VEGETATION_CHANCE) {
      addVegetation(chunk, localX, localZ, topY + 1);
    }
  }

  private BlockState getBlockStateForIslandLayer(final int y, final int bottomY, final int topY) {
    if (y == topY) {
      return Blocks.GRASS_BLOCK.defaultBlockState();
    } else if (y > topY - 3) {
      return Blocks.DIRT.defaultBlockState();
    } else if (y > bottomY + 2) {
      return Blocks.STONE.defaultBlockState();
    } else {
      return Blocks.END_STONE.defaultBlockState();
    }
  }

  private void addVegetation(
      final ChunkAccess chunk, final int localX, final int localZ, final int y) {
    if (y < getMinY() || y > getMinY() + getGenDepth()) return;

    double grassChance = FloatingIslandsChunkGeneratorConfig.GRASS_CHANCE;
    double treeChance = FloatingIslandsChunkGeneratorConfig.TREE_CHANCE;

    if (vegetationRandom.nextFloat() < grassChance) {
      chunk.setBlockState(
          new BlockPos(localX, y, localZ), Blocks.GRASS_BLOCK.defaultBlockState(), false);
    } else if (vegetationRandom.nextFloat() < treeChance) {
      chunk.setBlockState(
          new BlockPos(localX, y, localZ), Blocks.OAK_SAPLING.defaultBlockState(), false);
    } else {
      chunk.setBlockState(
          new BlockPos(localX, y, localZ), Blocks.DANDELION.defaultBlockState(), false);
    }
  }

  @Override
  public int getSeaLevel() {
    return 0;
  }

  @Override
  public int getMinY() {
    return -64;
  }

  @Override
  public int getBaseHeight(
      final int x,
      final int z,
      final Heightmap.Types types,
      final LevelHeightAccessor level,
      final RandomState randomState) {

    double islandDensity = getIslandDensityAt(x, z);
    if (islandDensity > FloatingIslandsChunkGeneratorConfig.ISLAND_DENSITY) {
      return getIslandHeightAt(x, z);
    }
    return level.getMinBuildHeight();
  }

  @Override
  public NoiseColumn getBaseColumn(
      final int x, final int z, final LevelHeightAccessor level, final RandomState randomState) {

    BlockState[] states = new BlockState[level.getHeight()];

    double islandDensity = getIslandDensityAt(x, z);
    if (islandDensity > FloatingIslandsChunkGeneratorConfig.ISLAND_DENSITY) {
      int islandHeight = getIslandHeightAt(x, z);
      int thickness = getIslandThicknessAt(x, z);

      int bottomY = Math.max(islandHeight - thickness, level.getMinBuildHeight());

      for (int y = bottomY; y <= islandHeight; y++) {
        int arrayIndex = y - level.getMinBuildHeight();
        if (arrayIndex >= 0 && arrayIndex < states.length) {
          states[arrayIndex] = getBlockStateForIslandLayer(y, bottomY, islandHeight);
        }
      }
    }

    return new NoiseColumn(level.getMinBuildHeight(), states);
  }
}
