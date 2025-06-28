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
import de.markusbordihn.worlddimensionnexus.service.PortalService;
import de.markusbordihn.worlddimensionnexus.service.TeleportService;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Manager for portal targeting and teleportation operations. Serves as a facade that delegates
 * portal operations to appropriate service classes.
 */
public class PortalTargetManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Portal Target Manager");

  private PortalTargetManager() {}

  public static void sync(final List<PortalTargetData> targetList) {
    PortalService.syncTargets(targetList);
  }

  public static void autoLinkPortal(
      final PortalInfoData portalInfo,
      final List<PortalInfoData> dimensionPortals,
      final Iterable<PortalInfoData> allPortals) {
    PortalService.autoLinkPortal(portalInfo, dimensionPortals, allPortals);
  }

  public static PortalTargetData getTarget(final PortalInfoData portalInfo) {
    return PortalService.getTarget(portalInfo);
  }

  public static void setTarget(final PortalInfoData portalInfo, final PortalInfoData target) {
    PortalService.setTarget(portalInfo, target);
  }

  public static void setTarget(
      final PortalInfoData portalInfo,
      final ResourceKey<Level> targetDimension,
      final BlockPos targetPosition) {
    PortalService.setTarget(portalInfo, targetDimension, targetPosition);
  }

  public static void removeTarget(final PortalInfoData portalInfo) {
    PortalService.removeTarget(portalInfo);
  }

  public static void removeTarget(final UUID portalUUID) {
    PortalService.removeTarget(portalUUID);
  }

  public static void clear() {
    PortalService.clear();
  }

  /** Handles delayed teleportation when a player stands on a portal. */
  public static void teleportPlayerWithDelay(
      final ServerLevel serverLevel,
      final ServerPlayer serverPlayer,
      final PortalInfoData portalInfo) {
    UUID playerId = serverPlayer.getUUID();
    long currentTick = serverLevel.getGameTime();

    PortalTargetData portalTarget = PortalService.getTarget(portalInfo);
    if (portalTarget == null) {
      return;
    }

    if (TeleportService.hasPlayerLeftPortal(playerId, currentTick)) {
      return;
    }

    if (TeleportService.startTeleportDelay(playerId, currentTick)) {
      TeleportService.applyTeleportEffects(serverLevel, serverPlayer);
      return;
    }

    if (TeleportService.isTeleportDelayExpired(playerId, currentTick)) {
      teleportPlayer(serverLevel, serverPlayer, portalInfo);
    }
  }

  /** Immediately teleports a player to a portal's target destination. */
  public static void teleportPlayer(
      final ServerLevel serverLevel,
      final ServerPlayer serverPlayer,
      final PortalInfoData portalInfo) {
    if (serverLevel == null || serverPlayer == null || portalInfo == null) {
      return;
    }

    PortalTargetData portalTarget = PortalService.getTarget(portalInfo);
    if (portalTarget == null) {
      log.error("No portal target found for portal: {}", portalInfo);
      serverPlayer.sendSystemMessage(
          Component.literal("Error: No valid portal target found for teleportation."));
      return;
    }

    UUID playerUUID = serverPlayer.getUUID();
    if (TeleportService.isPlayerOnCooldown(serverPlayer, serverLevel.getGameTime())) {
      return;
    }

    ServerLevel targetLevel = serverPlayer.server.getLevel(portalTarget.dimension());
    if (targetLevel == null) {
      log.error("Target level {} not found for portal: {}", portalTarget.dimension(), portalInfo);
      serverPlayer.sendSystemMessage(
          Component.literal("Error: Target dimension not found for teleportation."));
      return;
    }

    TeleportService.setPlayerCooldown(playerUUID, serverLevel.getGameTime());
    TeleportService.playTeleportSound(serverLevel, portalInfo.origin());

    serverPlayer.teleportTo(
        targetLevel,
        portalTarget.position().getX(),
        portalTarget.position().getY(),
        portalTarget.position().getZ(),
        serverPlayer.getYRot(),
        serverPlayer.getXRot());
  }
}
