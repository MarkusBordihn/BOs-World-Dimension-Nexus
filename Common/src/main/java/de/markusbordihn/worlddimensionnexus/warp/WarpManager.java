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

package de.markusbordihn.worlddimensionnexus.warp;

import de.markusbordihn.worlddimensionnexus.config.WarpConfig;
import de.markusbordihn.worlddimensionnexus.data.warp.WarpData;
import de.markusbordihn.worlddimensionnexus.data.warp.WarpType;
import de.markusbordihn.worlddimensionnexus.saveddata.WarpDataStorage;
import de.markusbordihn.worlddimensionnexus.teleport.TeleportManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WarpManager {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();

  public static void initialize() {
    cleanupExpiredWarps();
  }

  public static boolean createWarp(
      String warpName,
      String description,
      UUID ownerUuid,
      ResourceKey<Level> dimensionKey,
      BlockPos position,
      float yaw,
      float pitch,
      WarpType warpType) {

    if (!WarpConfig.isWarpSystemEnabled()) {
      return false;
    }

    if (!WarpConfig.isValidWarpName(warpName)) {
      return false;
    }

    if (!WarpConfig.isValidDescription(description)) {
      return false;
    }

    // Check if warp name already exists for this owner (private) or globally (public)
    if (isWarpNameTaken(warpName, ownerUuid, warpType)) {
      return false;
    }

    // Check warp limits
    if (!canCreateWarp(ownerUuid, warpType)) {
      return false;
    }

    WarpData warpData =
        new WarpData(
            warpName, description, ownerUuid, dimensionKey, position, yaw, pitch, warpType);
    WarpDataStorage.get().addWarp(warpData);

    LOGGER.info(
        "Created {} warp '{}' for player {} at {}",
        warpType.getDisplayName(),
        warpName,
        ownerUuid,
        position);
    return true;
  }

  public static boolean deleteWarp(UUID warpUuid, UUID requesterUuid, boolean isModeratorAction) {
    Optional<WarpData> warpOptional = getWarp(warpUuid);
    if (warpOptional.isEmpty()) {
      return false;
    }

    WarpData warpData = warpOptional.get();

    // Check permissions
    if (!isModeratorAction && !warpData.owner().equals(requesterUuid)) {
      return false;
    }

    // Disable the warp for the configured delay period
    long disabledUntil = System.currentTimeMillis() + WarpConfig.WARP_DELETION_DELAY;
    WarpData disabledWarp = warpData.withDisabled(disabledUntil);
    WarpDataStorage.get().updateWarp(disabledWarp);

    LOGGER.info(
        "Disabled warp '{}' owned by {} until {}",
        warpData.name(),
        warpData.owner(),
        disabledUntil);
    return true;
  }

  public static void cleanupExpiredWarps() {
    List<WarpData> allWarps = WarpDataStorage.get().getWarps();
    List<WarpData> expiredWarps =
        allWarps.stream().filter(WarpData::isExpired).collect(Collectors.toList());

    for (WarpData warpData : expiredWarps) {
      WarpDataStorage.get().removeWarp(warpData);
      LOGGER.info(
          "Permanently removed expired warp '{}' owned by {}", warpData.name(), warpData.owner());
    }
  }

  public static Optional<WarpData> getWarp(String warpName, UUID requesterUuid) {
    return WarpDataStorage.get().getWarps().stream()
        .filter(warpData -> warpData.name().equalsIgnoreCase(warpName))
        .filter(warpData -> warpData.isAccessibleBy(requesterUuid))
        .findFirst();
  }

  public static Optional<WarpData> getWarp(UUID warpUuid) {
    return WarpDataStorage.get().getWarps().stream()
        .filter(warpData -> warpData.uuid().equals(warpUuid))
        .findFirst();
  }

  public static List<WarpData> getPrivateWarpsForPlayer(UUID playerUuid) {
    return WarpDataStorage.get().getWarps().stream()
        .filter(warpData -> warpData.owner().equals(playerUuid))
        .filter(warpData -> warpData.type() == WarpType.PRIVATE)
        .filter(warpData -> warpData.enabled())
        .collect(Collectors.toList());
  }

  public static List<WarpData> getPublicWarps() {
    return WarpDataStorage.get().getWarps().stream()
        .filter(warpData -> warpData.type() == WarpType.PUBLIC)
        .filter(warpData -> warpData.enabled())
        .collect(Collectors.toList());
  }

  public static List<WarpData> getAllWarpsForPlayer(UUID playerUuid) {
    return WarpDataStorage.get().getWarps().stream()
        .filter(warpData -> warpData.owner().equals(playerUuid))
        .collect(Collectors.toList());
  }

  public static Set<WarpData> getAllWarps() {
    return Set.copyOf(WarpDataStorage.get().getWarps());
  }

  public static boolean canCreateWarp(UUID ownerUuid, WarpType warpType) {
    long existingWarps =
        WarpDataStorage.get().getWarps().stream()
            .filter(warpData -> warpData.owner().equals(ownerUuid))
            .filter(warpData -> warpData.type() == warpType)
            .filter(warpData -> warpData.enabled())
            .count();

    int maxWarps =
        switch (warpType) {
          case PRIVATE -> WarpConfig.MAX_PRIVATE_WARPS_PER_PLAYER;
          case PUBLIC -> WarpConfig.MAX_PUBLIC_WARPS_PER_PLAYER;
        };

    if (existingWarps >= maxWarps) {
      return false;
    }

    // Check global public warp limit
    if (warpType == WarpType.PUBLIC) {
      long totalPublicWarps =
          WarpDataStorage.get().getWarps().stream()
              .filter(warpData -> warpData.type() == WarpType.PUBLIC)
              .filter(warpData -> warpData.enabled())
              .count();

      return totalPublicWarps < WarpConfig.MAX_TOTAL_PUBLIC_WARPS;
    }

    return true;
  }

  private static boolean isWarpNameTaken(String warpName, UUID ownerUuid, WarpType warpType) {
    return WarpDataStorage.get().getWarps().stream()
        .filter(warpData -> warpData.enabled())
        .anyMatch(
            warpData -> {
              if (!warpData.name().equalsIgnoreCase(warpName)) {
                return false;
              }

              // For private warps, only check within the same owner
              if (warpType == WarpType.PRIVATE) {
                return warpData.owner().equals(ownerUuid);
              }

              // For public warps, check globally
              return true;
            });
  }

  public static boolean isPlayerOnCooldown(UUID playerUuid) {
    Long lastTeleport = teleportCooldowns.get(playerUuid);
    if (lastTeleport == null) {
      return false;
    }

    return System.currentTimeMillis() - lastTeleport < WarpConfig.WARP_TELEPORT_COOLDOWN;
  }

  public static void setPlayerCooldown(UUID playerUuid) {
    teleportCooldowns.put(playerUuid, System.currentTimeMillis());
  }

  public static long getRemainingCooldown(UUID playerUuid) {
    Long lastTeleport = teleportCooldowns.get(playerUuid);
    if (lastTeleport == null) {
      return 0;
    }

    long elapsed = System.currentTimeMillis() - lastTeleport;
    long cooldown = WarpConfig.WARP_TELEPORT_COOLDOWN;
    return Math.max(0, cooldown - elapsed);
  }

  public static boolean teleportPlayerToWarp(ServerPlayer serverPlayer, WarpData warpData) {
    if (!warpData.isAccessibleBy(serverPlayer.getUUID())) {
      return false;
    }

    if (isPlayerOnCooldown(serverPlayer.getUUID())) {
      return false;
    }

    // Check cross-dimension permission
    if (!WarpConfig.ALLOW_CROSS_DIMENSION_WARPS
        && !serverPlayer.level().dimension().equals(warpData.dimension())) {
      return false;
    }

    boolean teleportSuccess =
        TeleportManager.teleportPlayerToWarp(
            serverPlayer,
            warpData.dimension(),
            warpData.position(),
            warpData.yaw(),
            warpData.pitch());

    if (teleportSuccess) {
      setPlayerCooldown(serverPlayer.getUUID());
    }

    return teleportSuccess;
  }
}
