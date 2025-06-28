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
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.data.worldgen.WorldgenConfigLoader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.WorldGenRegion;
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

public class SkyblockChunkGenerator extends ChunkGenerator {

  public static final MapCodec<SkyblockChunkGenerator> CODEC =
      MapCodec.unit(
          () -> {
            throw new UnsupportedOperationException("SkyblockChunkGenerator codec not supported");
          });

  private static final int SPAWN_Y = 64;

  public SkyblockChunkGenerator(final HolderGetter<Biome> biomeGetter) {
    super(new FixedBiomeSource(biomeGetter.getOrThrow(Biomes.PLAINS)));
  }

  @Override
  protected MapCodec<? extends ChunkGenerator> codec() {
    return CODEC;
  }

  @Override
  public void addDebugScreenInfo(
      final List<String> list, final RandomState randomState, final BlockPos blockPos) {
    list.add("Skyblock Generator");
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
    // No carving for Skyblock
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
    // No structures in Skyblock
  }

  @Override
  public void buildSurface(
      final WorldGenRegion region,
      final StructureManager structureManager,
      final RandomState randomState,
      final ChunkAccess chunk) {
    // Surface is already generated in fillFromNoise
  }

  @Override
  public void spawnOriginalMobs(final WorldGenRegion region) {
    // No original mobs in Skyblock
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

    if (chunkPos.x == 0 && chunkPos.z == 0) {
      generateSpawnIsland(chunk);
    }

    return CompletableFuture.completedFuture(chunk);
  }

  private void generateSpawnIsland(final ChunkAccess chunk) {
    Optional<WorldgenConfigLoader.WorldgenConfig> config =
        WorldgenConfigLoader.getConfig(ChunkGeneratorType.SKYBLOCK);

    int centerX = 8;
    int centerZ = 8;

    generateIslandBase(chunk, centerX, centerZ);

    config.ifPresent(
        worldgenConfig -> generateConfiguredFeatures(chunk, centerX, centerZ, worldgenConfig));
  }

  private void generateIslandBase(final ChunkAccess chunk, final int centerX, final int centerZ) {
    // Standard Skyblock island: 3x3 base with additional blocks

    // Ground layer (Y=60): Larger base
    for (int x = centerX - 3; x <= centerX + 3; x++) {
      for (int z = centerZ - 3; z <= centerZ + 3; z++) {
        if (isInBounds(x, z)) {
          // Circular island
          double distance =
              Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
          if (distance <= 3.5) {
            chunk.setBlockState(
                new BlockPos(x, SPAWN_Y - 4, z), Blocks.STONE.defaultBlockState(), false);
          }
        }
      }
    }

    // Dirt layer (Y=61-63)
    for (int y = SPAWN_Y - 3; y <= SPAWN_Y - 1; y++) {
      for (int x = centerX - 2; x <= centerX + 2; x++) {
        for (int z = centerZ - 2; z <= centerZ + 2; z++) {
          if (isInBounds(x, z)) {
            double distance =
                Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
            if (distance <= 2.5) {
              chunk.setBlockState(new BlockPos(x, y, z), Blocks.DIRT.defaultBlockState(), false);
            }
          }
        }
      }
    }

    // Grass layer (Y=64)
    for (int x = centerX - 2; x <= centerX + 2; x++) {
      for (int z = centerZ - 2; z <= centerZ + 2; z++) {
        if (isInBounds(x, z)) {
          double distance =
              Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
          if (distance <= 2.5) {
            chunk.setBlockState(
                new BlockPos(x, SPAWN_Y, z), Blocks.GRASS_BLOCK.defaultBlockState(), false);
          }
        }
      }
    }

    // Place a tree in the center
    BlockPos treePos = new BlockPos(centerX, SPAWN_Y + 1, centerZ);
    generateSimpleTree(chunk, treePos);

    // Place a chest with starter items
    BlockPos chestPos = new BlockPos(centerX + 2, SPAWN_Y + 1, centerZ);
    if (isInBounds(chestPos.getX(), chestPos.getZ())) {
      chunk.setBlockState(chestPos, Blocks.CHEST.defaultBlockState(), false);
      scheduleBlockEntityCreation(chunk, chestPos);
    }
  }

  private void generateConfiguredFeatures(
      final ChunkAccess chunk,
      final int centerX,
      final int centerZ,
      final WorldgenConfigLoader.WorldgenConfig config) {
    Map<String, String> settings = config.customSettings();
  }

  private void generateSimpleTree(final ChunkAccess chunk, final BlockPos pos) {
    for (int y = 0; y < 4; y++) {
      BlockPos trunkPos = pos.offset(0, y, 0);
      if (isInBounds(trunkPos.getX(), trunkPos.getZ())) {
        chunk.setBlockState(trunkPos, Blocks.OAK_LOG.defaultBlockState(), false);
      }
    }

    for (int x = -1; x <= 1; x++) {
      for (int z = -1; z <= 1; z++) {
        for (int y = 3; y <= 4; y++) {
          if (x == 0 && z == 0 && y == 3) continue;
          BlockPos leafPos = pos.offset(x, y, z);
          if (isInBounds(leafPos.getX(), leafPos.getZ())) {
            chunk.setBlockState(leafPos, Blocks.OAK_LEAVES.defaultBlockState(), false);
          }
        }
      }
    }
  }

  private boolean isInBounds(final int x, final int z) {
    return x >= 0 && x < 16 && z >= 0 && z < 16;
  }

  private void scheduleBlockEntityCreation(final ChunkAccess chunk, final BlockPos pos) {
    if (chunk instanceof net.minecraft.world.level.chunk.LevelChunk levelChunk) {
      createBlockEntityImmediate(levelChunk, pos);
    }
    // For proto-chunks, block entities will be handled by NetworkHandler during teleportation
  }

  private void createBlockEntityImmediate(
      final net.minecraft.world.level.chunk.LevelChunk levelChunk, final BlockPos pos) {
    var level = levelChunk.getLevel();
    var blockState = levelChunk.getBlockState(pos);

    // Get the block entity type from the block state
    if (blockState.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
      var blockEntity = entityBlock.newBlockEntity(pos, blockState);
      if (blockEntity != null) {
        levelChunk.setBlockEntity(blockEntity);

        if (blockEntity
            instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
          populateStarterChest(chestEntity);
        }

        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
          serverLevel.getChunkSource().blockChanged(pos);
        }
      }
    }
  }

  private void populateStarterChest(
      final net.minecraft.world.level.block.entity.ChestBlockEntity chestEntity) {
    chestEntity.setItem(
        0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET));
    chestEntity.setItem(
        1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ICE, 2));
    chestEntity.setItem(
        2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BONE_MEAL, 5));
    chestEntity.setItem(
        3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_SAPLING, 4));
    chestEntity.setItem(
        4, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 3));
  }

  @Override
  public int getSeaLevel() {
    return 63;
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
    if (Math.abs(x) <= 8 && Math.abs(z) <= 8) {
      return SPAWN_Y;
    }
    return level.getMinBuildHeight();
  }

  @Override
  public NoiseColumn getBaseColumn(
      final int x, final int z, final LevelHeightAccessor level, final RandomState randomState) {
    BlockState[] states = new BlockState[level.getHeight()];

    // Set blocks for the spawn island area
    if (Math.abs(x) <= 8 && Math.abs(z) <= 8) {
      double distance = Math.sqrt(x * x + z * z);
      if (distance <= 3.5) {
        int baseY = SPAWN_Y + level.getMinBuildHeight();
        if (baseY - 4 >= 0 && baseY - 4 < states.length) {
          states[baseY - 4] = Blocks.STONE.defaultBlockState();
        }
        for (int y = baseY - 3; y <= baseY - 1; y++) {
          if (y >= 0 && y < states.length && distance <= 2.5) {
            states[y] = Blocks.DIRT.defaultBlockState();
          }
        }
        if (baseY >= 0 && baseY < states.length && distance <= 2.5) {
          states[baseY] = Blocks.GRASS_BLOCK.defaultBlockState();
        }
      }
    }

    return new NoiseColumn(level.getMinBuildHeight(), states);
  }
}
