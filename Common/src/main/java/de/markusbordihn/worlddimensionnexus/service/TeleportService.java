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

package de.markusbordihn.worlddimensionnexus.service;

import de.markusbordihn.worlddimensionnexus.config.PortalConfig;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class TeleportService {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Teleport Service");

  private static final Map<UUID, Long> pendingTeleportTime = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> lastPendingTeleportTime = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> teleportCooldown = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> lastCooldownMessage = new ConcurrentHashMap<>();

  private TeleportService() {}

  public static void applyTeleportEffects(
      final ServerLevel serverLevel, final ServerPlayer serverPlayer) {
    // Apply confusion effect
    serverPlayer.addEffect(
        new MobEffectInstance(
            MobEffects.CONFUSION, PortalConfig.CONFUSION_DURATION, 10, true, false, false));

    // Spawn particles
    serverLevel.sendParticles(
        ParticleTypes.PORTAL,
        serverPlayer.getX(),
        serverPlayer.getY() + PortalConfig.PARTICLE_OFFSET_Y,
        serverPlayer.getZ(),
        PortalConfig.PORTAL_PARTICLE_COUNT,
        PortalConfig.PARTICLE_SPREAD_XZ,
        PortalConfig.PARTICLE_SPREAD_Y,
        PortalConfig.PARTICLE_SPREAD_XZ,
        PortalConfig.PARTICLE_SPEED);
  }

  public static void playTeleportSound(final ServerLevel serverLevel, final BlockPos position) {
    serverLevel.playSound(
        null,
        position,
        SoundEvents.PLAYER_TELEPORT,
        SoundSource.BLOCKS,
        PortalConfig.SOUND_VOLUME,
        PortalConfig.SOUND_PITCH);
  }

  public static boolean hasPlayerLeftPortal(final UUID playerUUID, final long currentTick) {
    Long lastTick = lastPendingTeleportTime.get(playerUUID);
    if (lastTick != null && currentTick - lastTick > 1) {
      pendingTeleportTime.remove(playerUUID);
      lastPendingTeleportTime.remove(playerUUID);
      return true;
    }
    lastPendingTeleportTime.put(playerUUID, currentTick);
    return false;
  }

  public static boolean startTeleportDelay(final UUID playerUUID, final long currentTick) {
    return pendingTeleportTime.putIfAbsent(playerUUID, currentTick + PortalConfig.TELEPORT_DELAY)
        == null;
  }

  public static boolean isTeleportDelayExpired(final UUID playerUUID, final long currentTick) {
    Long teleportAt = pendingTeleportTime.get(playerUUID);
    if (teleportAt != null && currentTick >= teleportAt) {
      pendingTeleportTime.remove(playerUUID);
      return true;
    }
    return false;
  }

  public static boolean isPlayerOnCooldown(
      final ServerPlayer serverPlayer, final long currentTick) {
    UUID playerUUID = serverPlayer.getUUID();
    Long nextTeleportTime = teleportCooldown.get(playerUUID);
    if (nextTeleportTime != null && currentTick < nextTeleportTime) {
      if (!lastCooldownMessage.containsKey(playerUUID)
          || lastCooldownMessage.get(playerUUID) < nextTeleportTime) {
        serverPlayer.sendSystemMessage(
            net.minecraft.network.chat.Component.literal(
                "You are on cooldown and cannot teleport yet. Please wait a moment."));
        lastCooldownMessage.put(playerUUID, nextTeleportTime);
      }
      return true;
    }
    return false;
  }

  public static void setPlayerCooldown(final UUID playerUUID, final long currentTick) {
    teleportCooldown.put(playerUUID, currentTick + PortalConfig.TELEPORT_COOLDOWN);
    lastCooldownMessage.remove(playerUUID);
  }

  public static void clearPlayerState(final UUID playerUUID) {
    pendingTeleportTime.remove(playerUUID);
    lastPendingTeleportTime.remove(playerUUID);
    teleportCooldown.remove(playerUUID);
    lastCooldownMessage.remove(playerUUID);
  }

  public static void clearAllCache() {
    pendingTeleportTime.clear();
    lastPendingTeleportTime.clear();
    teleportCooldown.clear();
    lastCooldownMessage.clear();
  }
}
