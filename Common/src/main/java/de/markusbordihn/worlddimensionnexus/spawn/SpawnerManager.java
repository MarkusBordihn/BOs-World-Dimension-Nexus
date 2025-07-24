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

package de.markusbordihn.worlddimensionnexus.spawn;

import de.markusbordihn.worlddimensionnexus.Constants;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpawnerManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  private static final int SPAWNER_SEARCH_RADIUS = 6;
  private static final int SPAWNER_REMOVAL_DELAY_TICKS = 20;
  private static final Set<BlockPos> scheduledForRemoval = ConcurrentHashMap.newKeySet();

  private SpawnerManager() {}

  public static boolean isNearSpawnerBlock(BlockPos spawnPosition, ServerLevel serverLevel) {
    LevelChunk chunk = serverLevel.getChunkAt(spawnPosition);

    for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
      if (blockEntity instanceof SpawnerBlockEntity) {
        BlockPos spawnerPosition = blockEntity.getBlockPos();
        double distanceSquared = spawnPosition.distSqr(spawnerPosition);

        // Check if within spawner range (using squared distance for performance)
        if (distanceSquared <= SPAWNER_SEARCH_RADIUS * SPAWNER_SEARCH_RADIUS) {
          log.debug("Found spawner at {} near spawn position {}", spawnerPosition, spawnPosition);
          return true;
        }
      }
    }

    return false;
  }

  public static void scheduleSpawnerRemoval(BlockPos spawnPosition, ServerLevel serverLevel) {
    LevelChunk chunk = serverLevel.getChunkAt(spawnPosition);

    List<BlockPos> spawnersToSchedule =
        chunk.getBlockEntities().values().stream()
            .filter(blockEntity -> blockEntity instanceof SpawnerBlockEntity)
            .map(BlockEntity::getBlockPos)
            .filter(
                spawnerPosition -> {
                  double distanceSquared = spawnPosition.distSqr(spawnerPosition);
                  return distanceSquared <= SPAWNER_SEARCH_RADIUS * SPAWNER_SEARCH_RADIUS;
                })
            .filter(spawnerPosition -> !scheduledForRemoval.contains(spawnerPosition))
            .toList();

    // Remove the spawners
    for (BlockPos spawnerPosition : spawnersToSchedule) {
      scheduledForRemoval.add(spawnerPosition);

      // Use Minecraft's server scheduler instead of raw threading
      serverLevel
          .getServer()
          .tell(
              new net.minecraft.server.TickTask(
                  serverLevel.getServer().getTickCount() + SPAWNER_REMOVAL_DELAY_TICKS,
                  () -> removeSpawnerIfStillExists(spawnerPosition, serverLevel)));

      log.debug(
          "Scheduled spawner at {} for removal in {} ticks",
          spawnerPosition,
          SPAWNER_REMOVAL_DELAY_TICKS);
    }
  }

  private static void removeSpawnerIfStillExists(
      BlockPos spawnerPosition, ServerLevel serverLevel) {
    if (serverLevel.getBlockEntity(spawnerPosition) instanceof SpawnerBlockEntity) {
      serverLevel.setBlock(spawnerPosition, Blocks.AIR.defaultBlockState(), 3);
      log.info("Removed spawner block at {} (spawner blocks disabled)", spawnerPosition);
    }
    scheduledForRemoval.remove(spawnerPosition);
  }

  public static boolean shouldBlockSpawnerSpawn(
      BlockPos spawnPosition, ServerLevel serverLevel, boolean spawnersDisabled) {
    if (!spawnersDisabled) {
      return false;
    }

    boolean isNearSpawner = isNearSpawnerBlock(spawnPosition, serverLevel);
    if (isNearSpawner) {
      // Schedule spawner removal with delay
      scheduleSpawnerRemoval(spawnPosition, serverLevel);
    }

    return isNearSpawner;
  }

  public static boolean handleSpawnerSpawn(
      BlockPos spawnPosition, ServerLevel serverLevel, boolean spawnersDisabled) {
    // Early return if spawners are not disabled - no need to check for spawners
    if (!spawnersDisabled) {
      return true;
    }

    // Only check for nearby spawners if they are disabled
    boolean isFromSpawner = isNearSpawnerBlock(spawnPosition, serverLevel);
    if (isFromSpawner) {
      log.debug("Blocking spawner spawn at {} (spawner blocks disabled)", spawnPosition);

      // Remove the spawner block that caused this spawn
      scheduleSpawnerRemoval(spawnPosition, serverLevel);
      return false;
    }

    return true;
  }
}
