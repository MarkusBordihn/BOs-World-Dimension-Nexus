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
 * Manager class for portal targeting and teleportation. This class delegates business logic to
 * appropriate service classes.
 */
public class PortalTargetManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Portal Target Manager");

  private PortalTargetManager() {}

  /**
   * Synchronizes portal targets with the portal data storage.
   *
   * @param targetList List of portal targets to synchronize
   */
  public static void sync(final List<PortalTargetData> targetList) {
    PortalService.syncTargets(targetList);
  }

  /**
   * Automatically links portals based on matching properties.
   *
   * @param portalInfo The portal to link
   * @param dimensionPortals Portals in the same dimension
   * @param allPortals All portals across all dimensions
   */
  public static void autoLinkPortal(
      final PortalInfoData portalInfo,
      final List<PortalInfoData> dimensionPortals,
      final Iterable<PortalInfoData> allPortals) {
    PortalService.autoLinkPortal(portalInfo, dimensionPortals, allPortals);
  }

  /**
   * Gets the target for a specific portal.
   *
   * @param portalInfo The portal to get the target for
   * @return The target data or null if not found
   */
  public static PortalTargetData getTarget(final PortalInfoData portalInfo) {
    return PortalService.getTarget(portalInfo);
  }

  /**
   * Sets a portal target to another portal.
   *
   * @param portalInfo The source portal
   * @param target The target portal
   */
  public static void setTarget(final PortalInfoData portalInfo, final PortalInfoData target) {
    PortalService.setTarget(portalInfo, target);
  }

  /**
   * Sets a portal target to a specific dimension and position.
   *
   * @param portalInfo The source portal
   * @param targetDimension The target dimension
   * @param targetPosition The target position
   */
  public static void setTarget(
      final PortalInfoData portalInfo,
      final ResourceKey<Level> targetDimension,
      final BlockPos targetPosition) {
    PortalService.setTarget(portalInfo, targetDimension, targetPosition);
  }

  /**
   * Removes a portal target.
   *
   * @param portalInfo The portal to remove the target for
   */
  public static void removeTarget(final PortalInfoData portalInfo) {
    PortalService.removeTarget(portalInfo);
  }

  /**
   * Removes a portal target by UUID.
   *
   * @param portalUUID The UUID of the portal to remove
   */
  public static void removeTarget(final UUID portalUUID) {
    PortalService.removeTarget(portalUUID);
  }

  /** Clears all portal targets. */
  public static void clear() {
    PortalService.clear();
  }

  /**
   * Handles teleportation with delay when a player stands on a portal.
   *
   * @param serverLevel The server level
   * @param serverPlayer The player to teleport
   * @param portalInfo The portal information
   */
  public static void teleportPlayerWithDelay(
      final ServerLevel serverLevel,
      final ServerPlayer serverPlayer,
      final PortalInfoData portalInfo) {
    UUID playerId = serverPlayer.getUUID();
    long currentTick = serverLevel.getGameTime();

    // Validate the portal target information
    PortalTargetData portalTarget = PortalService.getTarget(portalInfo);
    if (portalTarget == null) {
      return;
    }

    // Check if the player is still standing on the portal
    if (TeleportService.hasPlayerLeftPortal(playerId, currentTick)) {
      return;
    }

    // Add teleport delay if not already pending
    if (TeleportService.startTeleportDelay(playerId, currentTick)) {
      TeleportService.applyTeleportEffects(serverLevel, serverPlayer);
      return;
    }

    // If the player is already pending teleportation, check if the delay has passed
    if (TeleportService.isTeleportDelayExpired(playerId, currentTick)) {
      teleportPlayer(serverLevel, serverPlayer, portalInfo);
    }
  }

  /**
   * Teleports a player to a portal's target.
   *
   * @param serverLevel The server level
   * @param serverPlayer The player to teleport
   * @param portalInfo The portal information
   */
  public static void teleportPlayer(
      final ServerLevel serverLevel,
      final ServerPlayer serverPlayer,
      final PortalInfoData portalInfo) {
    if (serverLevel == null || serverPlayer == null || portalInfo == null) {
      return;
    }

    // Validate the portal target information
    PortalTargetData portalTarget = PortalService.getTarget(portalInfo);
    if (portalTarget == null) {
      log.error("No portal target found for portal: {}", portalInfo);
      serverPlayer.sendSystemMessage(
          Component.literal("Error: No valid portal target found for teleportation."));
      return;
    }

    // Check if the player is on cooldown
    UUID playerUUID = serverPlayer.getUUID();
    if (TeleportService.isPlayerOnCooldown(serverPlayer, serverLevel.getGameTime())) {
      return;
    }

    // Get the target level
    ServerLevel targetLevel = serverPlayer.server.getLevel(portalTarget.dimension());
    if (targetLevel == null) {
      log.error("Target level {} not found for portal: {}", portalTarget.dimension(), portalInfo);
      serverPlayer.sendSystemMessage(
          Component.literal("Error: Target dimension not found for teleportation."));
      return;
    }

    // Set cooldown and play teleport sound
    TeleportService.setPlayerCooldown(playerUUID, serverLevel.getGameTime());
    TeleportService.playTeleportSound(serverLevel, portalInfo.origin());

    // Teleport the player
    serverPlayer.teleportTo(
        targetLevel,
        portalTarget.position().getX(),
        portalTarget.position().getY(),
        portalTarget.position().getZ(),
        serverPlayer.getYRot(),
        serverPlayer.getXRot());
  }
}
