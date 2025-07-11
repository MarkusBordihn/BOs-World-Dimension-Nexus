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

package de.markusbordihn.worlddimensionnexus.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;

@EventBusSubscriber
public class BlockEventHandler {

  private BlockEventHandler() {}

  @SubscribeEvent
  public static void onBlockPlace(final EntityPlaceEvent event) {
    if (event.isCanceled()) {
      return;
    }

    if (!(event.getLevel() instanceof ServerLevel serverLevel)
        || !(event.getEntity() instanceof ServerPlayer serverPlayer)) {
      return;
    }

    BlockEvents.handleBlockPlace(
        serverLevel,
        event.getPos(),
        serverPlayer,
        event.getPlacedBlock().getBlock(),
        event.getPlacedBlock());
  }

  @SubscribeEvent
  public static void onBlockBreak(final BlockEvent.BreakEvent event) {
    if (event.isCanceled()) {
      return;
    }

    if (!(event.getLevel() instanceof ServerLevel serverLevel)
        || !(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
      return;
    }

    boolean allowed =
        BlockEvents.handleBlockBreak(
            serverLevel,
            event.getPos(),
            serverPlayer,
            event.getState().getBlock(),
            event.getState());

    if (!allowed) {
      event.setCanceled(true);
    }
  }
}
