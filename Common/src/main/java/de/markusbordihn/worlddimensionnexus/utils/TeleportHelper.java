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

package de.markusbordihn.worlddimensionnexus.utils;

import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.data.teleport.TeleportHistory;
import de.markusbordihn.worlddimensionnexus.data.teleport.TeleportLocation;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class TeleportHelper {

  public static boolean safeTeleportToDimension(
      final ServerPlayer player, final String dimensionName) {
    ServerLevel targetLevel = DimensionManager.getDimensionServerLevel(dimensionName);
    if (targetLevel == null) {
      return false;
    }

    TeleportHistory.recordLocation(
        player.getUUID(),
        player.level().dimension(),
        player.blockPosition(),
        player.getYRot(),
        player.getXRot());

    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfoData(dimensionName);
    BlockPos teleportPos = getSafeTeleportLocation(player, targetLevel, dimensionName);

    player.teleportTo(
        targetLevel,
        teleportPos.getX() + 0.5,
        teleportPos.getY(),
        teleportPos.getZ() + 0.5,
        player.getYRot(),
        player.getXRot());

    if (dimensionInfo != null
        && dimensionInfo.chunkGeneratorType() == ChunkGeneratorType.SKYBLOCK) {
      NetworkHandler.syncSkyblockSpawnChunk(player, targetLevel);
    }

    return true;
  }

  public static boolean teleportBack(final ServerPlayer player) {
    TeleportLocation lastLocation = TeleportHistory.popLastLocation(player.getUUID());
    if (lastLocation == null) {
      return false;
    }

    ServerLevel targetLevel = player.server.getLevel(lastLocation.dimension());
    if (targetLevel == null) {
      return false;
    }

    player.teleportTo(
        targetLevel,
        lastLocation.position().getX() + 0.5,
        lastLocation.position().getY(),
        lastLocation.position().getZ() + 0.5,
        lastLocation.yRot(),
        lastLocation.xRot());

    return true;
  }

  private static BlockPos getSafeTeleportLocation(
      final ServerPlayer player, final ServerLevel targetLevel, final String dimensionName) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfoData(dimensionName);

    if (dimensionInfo != null
        && dimensionInfo.chunkGeneratorType() == ChunkGeneratorType.SKYBLOCK) {
      return getSkyblockSpawnLocation();
    }

    BlockPos playerSpawn = getPlayerSpawnInDimension(player, targetLevel);
    if (playerSpawn != null) {
      return playerSpawn;
    }

    BlockPos worldSpawn = targetLevel.getSharedSpawnPos();
    if (isSafeLocation(targetLevel, worldSpawn)) {
      return worldSpawn;
    }

    return findSafeLocationNear(targetLevel, new BlockPos(0, 64, 0));
  }

  private static BlockPos getSkyblockSpawnLocation() {
    return new BlockPos(8, 65, 8);
  }

  private static BlockPos getPlayerSpawnInDimension(
      final ServerPlayer player, final ServerLevel level) {
    return null;
  }

  private static boolean isSafeLocation(final ServerLevel level, final BlockPos pos) {
    BlockState groundState = level.getBlockState(pos.below());
    if (groundState.isAir()) {
      return false;
    }

    BlockState spawnState = level.getBlockState(pos);
    BlockState aboveState = level.getBlockState(pos.above());

    return spawnState.isAir() && aboveState.isAir();
  }

  private static BlockPos findSafeLocationNear(final ServerLevel level, final BlockPos center) {
    if (isSafeLocation(level, center)) {
      return center;
    }

    BlockPos safePos = searchForSafeLocation(level, center);
    return safePos != null ? safePos : createSafePlatform(level, center);
  }

  private static BlockPos searchForSafeLocation(final ServerLevel level, final BlockPos center) {
    for (int radius = 1; radius <= 16; radius++) {
      BlockPos found = searchAtRadius(level, center, radius);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private static BlockPos searchAtRadius(
      final ServerLevel level, final BlockPos center, int radius) {
    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {
        if (Math.abs(x) != radius && Math.abs(z) != radius) {
          continue;
        }

        BlockPos checkPos = center.offset(x, 0, z);
        BlockPos safePos = findSafeYLevel(level, checkPos);
        if (safePos != null) {
          return safePos;
        }
      }
    }
    return null;
  }

  private static BlockPos findSafeYLevel(final ServerLevel level, final BlockPos basePos) {
    for (int yOffset = -5; yOffset <= 10; yOffset++) {
      BlockPos testPos = basePos.offset(0, yOffset, 0);
      if (isValidYLevel(level, testPos) && isSafeLocation(level, testPos)) {
        return testPos;
      }
    }
    return null;
  }

  private static boolean isValidYLevel(final ServerLevel level, final BlockPos pos) {
    return pos.getY() >= level.getMinBuildHeight() && pos.getY() <= level.getMaxBuildHeight() - 2;
  }

  private static BlockPos createSafePlatform(final ServerLevel level, final BlockPos center) {
    BlockPos safePos =
        new BlockPos(center.getX(), Math.max(64, level.getMinBuildHeight() + 10), center.getZ());

    placePlatformBlocks(level, safePos);
    clearSpawnArea(level, safePos);

    return safePos;
  }

  private static void placePlatformBlocks(final ServerLevel level, final BlockPos safePos) {
    BlockPos platform = safePos.below();
    level.setBlock(platform, Blocks.STONE.defaultBlockState(), 3);
    level.setBlock(platform.north(), Blocks.STONE.defaultBlockState(), 3);
    level.setBlock(platform.south(), Blocks.STONE.defaultBlockState(), 3);
    level.setBlock(platform.east(), Blocks.STONE.defaultBlockState(), 3);
    level.setBlock(platform.west(), Blocks.STONE.defaultBlockState(), 3);
  }

  private static void clearSpawnArea(final ServerLevel level, final BlockPos safePos) {
    level.setBlock(safePos, Blocks.AIR.defaultBlockState(), 3);
    level.setBlock(safePos.above(), Blocks.AIR.defaultBlockState(), 3);
  }
}
