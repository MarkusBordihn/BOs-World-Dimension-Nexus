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
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.data.spawn.SpawnRules;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpawnEvents {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private SpawnEvents() {}

  public static boolean handleEntityJoinLevelEvent(Mob mob, ServerLevel serverLevel) {
    SpawnRules spawnRules = getSpawnRulesForDimension(serverLevel);
    if (spawnRules == null) {
      return true;
    }

    // Handle spawner blocks if they are disabled (optimized check)
    if (!SpawnerManager.handleSpawnerSpawn(
        mob.blockPosition(), serverLevel, spawnRules.disableSpawnerBlocks())) {
      return false;
    }

    return SpawnManager.isNaturalEntitySpawnAllowed(spawnRules, EntityType.getKey(mob.getType()));
  }

  public static boolean handleMobSpawnEvent(
      EntityType<?> entityType, ServerLevel serverLevel, BlockPos spawnPos) {
    SpawnRules spawnRules = getSpawnRulesForDimension(serverLevel);
    if (spawnRules == null) {
      return true;
    }

    // Block spawner spawns early (before mob is created) but don't remove spawners yet
    if (SpawnerManager.shouldBlockSpawnerSpawn(
        spawnPos, serverLevel, spawnRules.disableSpawnerBlocks())) {
      log.debug(
          "Blocking early spawner spawn for entity type {} at {}",
          EntityType.getKey(entityType),
          spawnPos);
      return false;
    }

    return SpawnManager.isNaturalEntitySpawnAllowed(spawnRules, EntityType.getKey(entityType));
  }

  public static boolean handleFinalizeSpawnEvent(Mob mob, ServerLevel serverLevel) {
    return handleFinalizeSpawnEvent(mob, serverLevel, null);
  }

  public static boolean handleFinalizeSpawnEvent(
      Mob mob, ServerLevel serverLevel, Boolean isFromSpawner) {
    SpawnRules spawnRules = getSpawnRulesForDimension(serverLevel);
    if (spawnRules == null) {
      return true;
    }

    // Handle spawner blocks with removal (for mobs that somehow got through early check)
    if (!SpawnerManager.handleSpawnerSpawn(
        mob.blockPosition(), serverLevel, spawnRules.disableSpawnerBlocks())) {
      return false;
    }

    return SpawnManager.shouldAllowEntitySpawn(
        spawnRules, EntityType.getKey(mob.getType()), isFromSpawner);
  }

  private static SpawnRules getSpawnRulesForDimension(ServerLevel serverLevel) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(serverLevel.dimension());
    if (dimensionInfo == null) {
      log.debug("No dimension info found for {}", serverLevel.dimension());
      return null;
    }

    SpawnRules spawnRules = dimensionInfo.spawnRules();
    if (spawnRules == null) {
      log.debug("No spawn rules found for dimension {}", serverLevel.dimension());
    }

    return spawnRules;
  }
}
