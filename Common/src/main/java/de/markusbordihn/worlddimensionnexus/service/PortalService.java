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
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Portal Service for managing portals and their targeting information. Provides functionality to
 * create, manage, and maintain portal links between different dimensions. Handles automatic linking
 * based on matching properties and manages portal target storage.
 */
public class PortalService {

  /** Logger instance for this service. */
  private static final PrefixLogger log = ModLogger.getPrefixLogger("Portal Service");

  /**
   * Thread-safe map that stores portal target information. Maps from portal UUID to target
   * information (dimension and position).
   */
  private static final Map<UUID, PortalTargetData> portalTargets = new ConcurrentHashMap<>();

  /** Private constructor to prevent instantiation. */
  private PortalService() {
    // Private constructor to prevent instantiation
  }

  /**
   * Synchronize the portal targets with the saved data.
   *
   * @param targetList List of portal target data to synchronize
   */
  public static void syncTargets(final List<PortalTargetData> targetList) {
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

  /**
   * Automatically links portals with matching properties (edge block type and color).
   *
   * @param portalInfo The portal to link
   * @param dimensionPortals Portals in the same dimension
   * @param allPortals Portals across all dimensions
   */
  public static void autoLinkPortal(
      final PortalInfoData portalInfo,
      final List<PortalInfoData> dimensionPortals,
      final Iterable<PortalInfoData> allPortals) {
    // Skip processing if portal is invalid or already has a target
    if (portalInfo == null || portalInfo.uuid() == null || getTarget(portalInfo) != null) {
      return;
    }

    PortalInfoData linkedPortal = null;

    // First try to find a matching portal in the same dimension
    if (dimensionPortals != null) {
      linkedPortal = findMatchingPortal(portalInfo, dimensionPortals);
    }

    // If no match in same dimension, search across all dimensions
    if (linkedPortal == null && allPortals != null) {
      linkedPortal = findMatchingPortal(portalInfo, allPortals);
    }

    // Link the portals bidirectionally if a match was found
    if (linkedPortal != null) {
      log.info("Auto-linking portals: {} <-> {}", portalInfo.uuid(), linkedPortal.uuid());
      setTarget(portalInfo, linkedPortal);
      setTarget(linkedPortal, portalInfo);
    }
  }

  /**
   * Helper method to find a matching portal with the same properties.
   *
   * @param portalInfo The reference portal
   * @param potentialMatches Collection of portals to search
   * @return The first matching portal or null if none found
   */
  private static PortalInfoData findMatchingPortal(
      final PortalInfoData portalInfo, final Iterable<PortalInfoData> potentialMatches) {
    for (PortalInfoData existingPortal : potentialMatches) {
      if (existingPortal != null
          && !existingPortal.uuid().equals(portalInfo.uuid())
          && existingPortal.edgeBlockType() == portalInfo.edgeBlockType()
          && existingPortal.color() == portalInfo.color()) {
        return existingPortal;
      }
    }
    return null;
  }

  /**
   * Get the target for a portal.
   *
   * @param portalInfo The portal to get the target for
   * @return The portal target data, or null if not found
   */
  public static PortalTargetData getTarget(final PortalInfoData portalInfo) {
    if (portalInfo == null) {
      return null;
    }
    return portalTargets.get(portalInfo.uuid());
  }

  /**
   * Set a portal's target to another portal.
   *
   * @param portalInfo The source portal
   * @param target The target portal
   */
  public static void setTarget(final PortalInfoData portalInfo, final PortalInfoData target) {
    if (portalInfo == null || target == null) {
      return;
    }
    setTarget(portalInfo, target.dimension(), target.getTeleportPosition());
  }

  /**
   * Set a portal's target to a specific dimension and position.
   *
   * @param portalInfo The source portal
   * @param targetDimension The target dimension
   * @param targetPosition The target position
   */
  public static void setTarget(
      final PortalInfoData portalInfo,
      final ResourceKey<Level> targetDimension,
      final BlockPos targetPosition) {
    if (portalInfo == null || targetDimension == null || targetPosition == null) {
      return;
    }
    PortalTargetData portalTargetData =
        new PortalTargetData(portalInfo.uuid(), targetDimension, targetPosition);
    portalTargets.put(portalInfo.uuid(), portalTargetData);

    PortalDataStorage.get().addTarget(portalTargetData);
  }

  /**
   * Remove a portal's target.
   *
   * @param portalInfo The portal to remove the target for
   */
  public static void removeTarget(final PortalInfoData portalInfo) {
    if (portalInfo != null) {
      removeTarget(portalInfo.uuid());
    }
  }

  /**
   * Remove a portal's target by UUID.
   *
   * @param portalUUID The UUID of the portal to remove the target for
   */
  public static void removeTarget(final UUID portalUUID) {
    if (portalUUID == null) {
      return;
    }
    portalTargets.remove(portalUUID);

    // Also remove from persistent storage
    PortalDataStorage storage = PortalDataStorage.get();
    if (storage != null) {
      storage.getTargets().removeIf(target -> target.portalId().equals(portalUUID));
    }
  }

  /** Clear all portal targets. */
  public static void clear() {
    portalTargets.clear();
  }
}
