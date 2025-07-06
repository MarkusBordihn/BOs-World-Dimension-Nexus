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

package de.markusbordihn.worlddimensionnexus.teleport;

import de.markusbordihn.worlddimensionnexus.config.TeleportConfig;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public class TeleportCooldownManager {

  private static final ConcurrentHashMap<UUID, Long> backTeleportCooldowns =
      new ConcurrentHashMap<>();

  public static boolean canTeleportBack(final ServerPlayer player) {
    if (!TeleportConfig.isBackTeleportCooldownEnabled()) {
      return true;
    }

    UUID playerId = player.getUUID();
    Long lastTeleport = backTeleportCooldowns.get(playerId);

    if (lastTeleport == null) {
      return true;
    }

    long currentTime = System.currentTimeMillis();
    long cooldownTime = TeleportConfig.getBackTeleportCooldown() * 1000L;

    return (currentTime - lastTeleport) >= cooldownTime;
  }

  public static void recordBackTeleport(final ServerPlayer player) {
    if (TeleportConfig.isBackTeleportCooldownEnabled()) {
      backTeleportCooldowns.put(player.getUUID(), System.currentTimeMillis());
    }
  }

  public static int getRemainingCooldown(final ServerPlayer player) {
    if (!TeleportConfig.isBackTeleportCooldownEnabled()) {
      return 0;
    }

    UUID playerId = player.getUUID();
    Long lastTeleport = backTeleportCooldowns.get(playerId);

    if (lastTeleport == null) {
      return 0;
    }

    long currentTime = System.currentTimeMillis();
    long cooldownTime = TeleportConfig.getBackTeleportCooldown() * 1000L;
    long timePassed = currentTime - lastTeleport;

    if (timePassed >= cooldownTime) {
      return 0;
    }

    return (int) ((cooldownTime - timePassed) / 1000L);
  }

  public static void clearCooldown(final ServerPlayer player) {
    backTeleportCooldowns.remove(player.getUUID());
  }

  public static void clearAllCooldowns() {
    backTeleportCooldowns.clear();
  }
}
