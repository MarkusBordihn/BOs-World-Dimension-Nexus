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

import de.markusbordihn.worlddimensionnexus.data.spawn.SpawnRules;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import net.minecraft.resources.ResourceLocation;

public class SpawnManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("[Spawn Manager]");

  private SpawnManager() {}

  public static boolean isNaturalEntitySpawnAllowed(
      SpawnRules spawnRules, ResourceLocation entityType) {
    // If no spawn rules are provided, allow all spawns.
    if (spawnRules == null) {
      log.debug("No spawn rules provided, allowing natural spawn for entity: {}", entityType);
      return true;
    }

    // If natural mob spawning is globally disabled, only check allow list
    if (spawnRules.disableNaturalMobSpawning()) {
      boolean isAllowed = spawnRules.allowedEntityTypes().contains(entityType);
      log.debug(
          "Natural mob spawning globally disabled. Entity {} is {}",
          entityType,
          isAllowed ? "allowed" : "denied");
      return isAllowed;
    }

    // Check deny list first - if entity is explicitly denied, block it
    if (spawnRules.deniedEntityTypes().contains(entityType)) {
      log.debug("Entity {} is in deny list, blocking natural spawn", entityType);
      return false;
    }

    // Check allow list only if it's not empty
    if (!spawnRules.allowedEntityTypes().isEmpty()
        && !spawnRules.allowedEntityTypes().contains(entityType)) {
      log.debug("Entity {} is not in allow list, blocking natural spawn", entityType);
      return false;
    }

    log.debug("Entity {} is allowed to spawn naturally", entityType);
    return true;
  }

  public static boolean areSpawnerBlocksDisabled(SpawnRules spawnRules) {
    if (spawnRules == null) {
      return false;
    }

    boolean disabled = spawnRules.disableSpawnerBlocks();
    log.debug("Spawner blocks are {}", disabled ? "disabled" : "enabled");
    return disabled;
  }

  public static boolean shouldAllowEntitySpawn(
      SpawnRules spawnRules, ResourceLocation entityType, Boolean isFromSpawner) {
    if (spawnRules == null) {
      log.debug("No spawn rules provided, allowing spawn for entity: {}", entityType);
      return true;
    }

    // Check spawner-specific rules first if this is from a spawner
    if (isFromSpawner != null && isFromSpawner && areSpawnerBlocksDisabled(spawnRules)) {
      log.debug("Spawner spawn blocked for entity {} (spawner blocks disabled)", entityType);
      return false;
    }

    // Then check natural spawning rules
    return isNaturalEntitySpawnAllowed(spawnRules, entityType);
  }
}
