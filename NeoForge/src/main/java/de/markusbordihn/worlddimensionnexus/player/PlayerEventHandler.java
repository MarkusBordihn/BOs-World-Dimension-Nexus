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

package de.markusbordihn.worlddimensionnexus.player;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class PlayerEventHandler {

  @SubscribeEvent
  public static void onPlayerLogin(final PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      PlayerEvents.handlePlayerLoginEvent(serverPlayer);
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      PlayerEvents.handlePlayerLogoutEvent(serverPlayer);
    }
  }

  @SubscribeEvent
  public static void onPlayerTick(final PlayerTickEvent.Post event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      PlayerEvents.handlePlayerPostTickEvent(serverPlayer);
    }
  }

  @SubscribeEvent
  public static void onPlayerChangeDimension(final PlayerEvent.PlayerChangedDimensionEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      PlayerEvents.handlePlayerChangeDimensionEvent(serverPlayer, event.getFrom(), event.getTo());
    }
  }

  @SubscribeEvent
  public static void onPlayerDeath(final LivingDeathEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      PlayerEvents.handlePlayerDeathEvent(serverPlayer);
    }
  }

  @SubscribeEvent
  public static void onPlayerRespawn(final PlayerEvent.PlayerRespawnEvent event) {
    if (event.getEntity() instanceof ServerPlayer serverPlayer) {
      PlayerEvents.handlePlayerRespawnEvent(serverPlayer);
    }
  }
}
