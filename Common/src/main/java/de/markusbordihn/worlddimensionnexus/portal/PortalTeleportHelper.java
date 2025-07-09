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

package de.markusbordihn.worlddimensionnexus.portal;

import de.markusbordihn.worlddimensionnexus.config.PortalConfig;
import de.markusbordihn.worlddimensionnexus.teleport.TeleportManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

public class PortalTeleportHelper {

  private static final Map<UUID, Long> pendingPortalTeleports = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> lastPortalInteraction = new ConcurrentHashMap<>();

  private PortalTeleportHelper() {}

  public static boolean executeTeleport(
      final ServerPlayer player,
      final ResourceKey<Level> targetDimension,
      final BlockPos targetPosition) {
    return TeleportManager.teleportPlayer(player, targetDimension, targetPosition);
  }

  public static void applyPortalEffects(final ServerLevel level, final ServerPlayer player) {
    player.addEffect(
        new MobEffectInstance(
            MobEffects.CONFUSION, PortalConfig.CONFUSION_DURATION, 10, true, false, false));

    level.sendParticles(
        ParticleTypes.PORTAL,
        player.getX(),
        player.getY() + PortalConfig.PARTICLE_OFFSET_Y,
        player.getZ(),
        PortalConfig.PORTAL_PARTICLE_COUNT,
        PortalConfig.PARTICLE_SPREAD_XZ,
        PortalConfig.PARTICLE_SPREAD_Y,
        PortalConfig.PARTICLE_SPREAD_XZ,
        PortalConfig.PARTICLE_SPEED);
  }

  public static void playPortalSound(final ServerLevel level, final BlockPos position) {
    level.playSound(
        null,
        position,
        SoundEvents.PLAYER_TELEPORT,
        SoundSource.BLOCKS,
        PortalConfig.SOUND_VOLUME,
        PortalConfig.SOUND_PITCH);
  }

  public static boolean hasPlayerLeftPortal(final UUID playerId, final long currentTick) {
    Long lastTick = lastPortalInteraction.get(playerId);
    if (lastTick != null && currentTick - lastTick > 1) {
      pendingPortalTeleports.remove(playerId);
      lastPortalInteraction.remove(playerId);
      return true;
    }
    lastPortalInteraction.put(playerId, currentTick);
    return false;
  }

  public static boolean startPortalTeleportDelay(final UUID playerId, final long currentTick) {
    return pendingPortalTeleports.putIfAbsent(playerId, currentTick + PortalConfig.TELEPORT_DELAY)
        == null;
  }

  public static boolean isPortalTeleportReady(final UUID playerId, final long currentTick) {
    Long teleportAt = pendingPortalTeleports.get(playerId);
    if (teleportAt != null && currentTick >= teleportAt) {
      pendingPortalTeleports.remove(playerId);
      return true;
    }
    return false;
  }

  public static void clearPlayerPortalState(final UUID playerId) {
    pendingPortalTeleports.remove(playerId);
    lastPortalInteraction.remove(playerId);
  }

  public static void clearAllCache() {
    pendingPortalTeleports.clear();
    lastPortalInteraction.clear();
  }
}
