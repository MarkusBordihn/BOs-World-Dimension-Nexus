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

import de.markusbordihn.worlddimensionnexus.data.portal.PortalInfoData;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalTargetData;
import de.markusbordihn.worlddimensionnexus.saveddata.PortalDataStorage;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;

public class PortalTargetManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Portal Target Manager");

  private static final Map<UUID, PortalTargetData> portalTargets = new ConcurrentHashMap<>();

  private static final Map<UUID, Long> pendingTeleportTime = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> lastPendingTeleportTime = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> teleportCooldown = new ConcurrentHashMap<>();
  private static final Map<UUID, Long> lastCooldownMessage = new ConcurrentHashMap<>();
  private static final int TELEPORT_DELAY = 20 * 3; // 3 seconds delay before teleport
  private static final int TELEPORT_COOLDOWN = 20 * 2; // 2 seconds cooldown
  private static final int CONFUSION_DURATION = 20 * 5; // 5 seconds of confusion effect

  private PortalTargetManager() {}

  public static void sync(List<PortalTargetData> targetList) {
    if (targetList == null || targetList.isEmpty()) {
      log.warn("Portal target list is null or empty!");
      clear();
      return;
    }

    log.info("Synchronizing {} portal targets ...", targetList.size());
    clear();

    for (PortalTargetData portalTarget : targetList) {
      if (portalTarget != null && portalTarget.portalId() != null) {
        portalTargets.put(portalTarget.portalId(), portalTarget);
      }
    }
  }

  public static void autoLinkPortal(
      PortalInfoData portalInfo,
      List<PortalInfoData> dimensionPortals,
      Iterable<PortalInfoData> allPortals) {
    if (portalInfo == null || portalInfo.uuid() == null || getTarget(portalInfo) != null) {
      return;
    }

    PortalInfoData linkedPortal = null;

    // Search for a linked portal in the same dimension
    if (dimensionPortals != null) {
      for (PortalInfoData existingPortal : dimensionPortals) {
        if (existingPortal != null
            && !existingPortal.uuid().equals(portalInfo.uuid())
            && existingPortal.edgeBlockType() == portalInfo.edgeBlockType()
            && existingPortal.color() == portalInfo.color()) {
          linkedPortal = existingPortal;
          break;
        }
      }
    }

    // Search for a linked portal in all dimensions, if not found in the same dimension
    if (linkedPortal == null && allPortals != null) {
      for (PortalInfoData existingPortal : allPortals) {
        if (existingPortal != null
            && !existingPortal.uuid().equals(portalInfo.uuid())
            && existingPortal.edgeBlockType() == portalInfo.edgeBlockType()
            && existingPortal.color() == portalInfo.color()) {
          linkedPortal = existingPortal;
          break;
        }
      }
    }

    // If a linked portal is found, set the target for both portals
    if (linkedPortal != null) {
      log.info("Auto-linking portals: {} <-> {}", portalInfo.uuid(), linkedPortal.uuid());
      setTarget(portalInfo, linkedPortal);
      setTarget(linkedPortal, portalInfo);
    }
  }

  public static PortalTargetData getTarget(PortalInfoData portalInfo) {
    if (portalInfo == null) {
      return null;
    }
    return portalTargets.get(portalInfo.uuid());
  }

  public static void setTarget(PortalInfoData portalInfo, PortalInfoData target) {
    if (portalInfo == null || target == null) {
      return;
    }
    setTarget(portalInfo, target.dimension(), target.getTeleportPosition());
  }

  public static void setTarget(
      PortalInfoData portalInfo, ResourceKey<Level> targetDimension, BlockPos targetPosition) {
    if (portalInfo == null || targetDimension == null || targetPosition == null) {
      return;
    }
    PortalTargetData portalTargetData =
        new PortalTargetData(portalInfo.uuid(), targetDimension, targetPosition);
    portalTargets.put(portalInfo.uuid(), portalTargetData);

    PortalDataStorage.get().addTarget(portalTargetData);
  }

  public static void removeTarget(PortalInfoData portalInfo) {
    if (portalInfo != null) {
      removeTarget(portalInfo.uuid());
    }
  }

  public static void removeTarget(UUID portalUUID) {
    if (portalUUID == null) {
      return;
    }
    portalTargets.remove(portalUUID);
  }

  public static void clear() {
    portalTargets.clear();
  }

  public static void teleportPlayerWithDelay(
      ServerLevel serverLevel, ServerPlayer serverPlayer, PortalInfoData portalInfo) {
    UUID playerId = serverPlayer.getUUID();
    long currentTick = serverLevel.getGameTime();

    // Validate the portal target information.
    PortalTargetData portalTarget = getTarget(portalInfo);
    if (portalTarget == null) {
      return;
    }

    // Check if the player is still standing on the portal.
    Long lastTick = pendingTeleportTime.get(playerId);
    if (lastTick != null && currentTick - lastTick > 1) {
      pendingTeleportTime.remove(playerId);
      lastPendingTeleportTime.remove(playerId);
      return;
    }
    lastPendingTeleportTime.put(playerId, currentTick);

    // Add teleport delay if not already pending.
    if (pendingTeleportTime.putIfAbsent(playerId, currentTick + TELEPORT_DELAY) == null) {
      serverPlayer.addEffect(
          new MobEffectInstance(MobEffects.CONFUSION, CONFUSION_DURATION, 10, true, false, false));
      serverLevel.sendParticles(
          ParticleTypes.PORTAL,
          serverPlayer.getX(),
          serverPlayer.getY() + 1,
          serverPlayer.getZ(),
          30,
          0.5,
          1,
          0.5,
          0.1);
      return;
    }

    // If the player is already pending teleportation, check if the delay has passed.
    long teleportAt = pendingTeleportTime.get(playerId);
    if (currentTick >= teleportAt) {
      pendingTeleportTime.remove(playerId);
      teleportPlayer(serverLevel, serverPlayer, portalInfo);
    }
  }

  public static void teleportPlayer(
      ServerLevel serverLevel, ServerPlayer serverPlayer, PortalInfoData portalInfo) {
    if (serverLevel == null || serverPlayer == null || portalInfo == null) {
      return;
    }

    // Validate the portal target information.
    PortalTargetData portalTarget = getTarget(portalInfo);
    if (portalTarget == null) {
      log.error("No portal target found for portal: {}", portalInfo);
      serverPlayer.sendSystemMessage(
          Component.literal("Error: No valid portal target found for teleportation."));
      return;
    }

    // Check if the player is on cooldown.
    UUID playerUUID = serverPlayer.getUUID();
    Long nextTeleportTime = teleportCooldown.get(playerUUID);
    if (nextTeleportTime != null && serverLevel.getGameTime() < nextTeleportTime) {
      if (!lastCooldownMessage.containsKey(playerUUID)
          || lastCooldownMessage.get(playerUUID) < nextTeleportTime) {
        serverPlayer.sendSystemMessage(
            Component.literal(
                "You are on cooldown and cannot teleport yet. Please wait a moment."));
        lastCooldownMessage.put(playerUUID, nextTeleportTime);
      }
      return;
    }

    // Verify the target dimension exists.
    ServerLevel targetLevel = serverPlayer.server.getLevel(portalTarget.dimension());
    if (targetLevel == null) {
      log.error("Target level {} not found for portal: {}", portalTarget.dimension(), portalInfo);
      serverPlayer.sendSystemMessage(
          Component.literal("Error: Target dimension not found for teleportation."));
      return;
    }

    // Update the teleport cooldown for the player.
    teleportCooldown.put(playerUUID, serverLevel.getGameTime() + TELEPORT_COOLDOWN);
    lastCooldownMessage.remove(playerUUID);

    // Play teleport sound effect.
    serverLevel.playSound(
        null, portalInfo.origin(), SoundEvents.PLAYER_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);

    // Teleport the player to the portal destination.
    serverPlayer.teleportTo(
        targetLevel,
        portalTarget.position().getX(),
        portalTarget.position().getY(),
        portalTarget.position().getZ(),
        serverPlayer.getYRot(),
        serverPlayer.getXRot());
  }
}
