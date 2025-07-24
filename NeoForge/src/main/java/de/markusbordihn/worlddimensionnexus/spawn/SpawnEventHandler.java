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

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck.Result;

@EventBusSubscriber
public class SpawnEventHandler {

  private SpawnEventHandler() {}

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
    if (event.isCanceled()) {
      return;
    }
    if (event.getEntity() instanceof Mob mob
        && event.getLevel() instanceof ServerLevel serverLevel
        && !SpawnEvents.handleEntityJoinLevelEvent(mob, serverLevel)) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onMobSpawn(MobSpawnEvent.SpawnPlacementCheck event) {
    if (event.getResult() == Result.FAIL) {
      return;
    }

    if (event.getLevel() instanceof ServerLevel serverLevel
        && !SpawnEvents.handleMobSpawnEvent(event.getEntityType(), serverLevel, event.getPos())) {
      event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
    if (event.isCanceled()) {
      return;
    }

    if (event.getEntity() instanceof Mob mob
        && event.getLevel() instanceof ServerLevel serverLevel
        && !SpawnEvents.handleFinalizeSpawnEvent(mob, serverLevel)) {
      event.setCanceled(true);
    }
  }
}
