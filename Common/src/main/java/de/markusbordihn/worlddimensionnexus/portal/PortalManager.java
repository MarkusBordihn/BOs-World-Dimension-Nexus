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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class PortalManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Portal Manager");

  private static final Map<ResourceKey<Level>, List<PortalInfoData>> portalsPerDimension =
      new ConcurrentHashMap<>();
  private static final Map<ResourceKey<Level>, Map<Long, List<PortalInfoData>>> portalsPerChunk =
      new ConcurrentHashMap<>();
  private static final Set<PortalInfoData> portals = ConcurrentHashMap.newKeySet();

  private PortalManager() {}

  public static void sync(final List<PortalInfoData> portalList) {
    if (portalList == null || portalList.isEmpty()) {
      log.warn("No portals to synchronize: list is {}.", (portalList == null ? "null" : "empty"));
      return;
    }

    log.info("Synchronizing {} portals ...", portalList.size());
    clear();

    // Add portals, but without updating the persistent storage.
    for (PortalInfoData portalInfo : portalList) {
      addPortal(portalInfo, false);
    }
  }

  public static void addPortal(final PortalInfoData portalInfo) {
    addPortal(portalInfo, true);
  }

  public static void addPortal(final PortalInfoData portalInfo, final boolean updateStorage) {
    if (portalInfo == null) {
      return;
    }

    // Add portal to the global portal set and dimension-specific list for fast access.
    log.info("Adding portal: {}", portalInfo);
    portals.add(portalInfo);
    portalsPerDimension
        .computeIfAbsent(portalInfo.dimension(), k -> new java.util.ArrayList<>())
        .add(portalInfo);

    // Add portal to chunk index for fast access and easier block checks.
    Map<Long, List<PortalInfoData>> chunkIndex =
        portalsPerChunk.computeIfAbsent(portalInfo.dimension(), k -> new ConcurrentHashMap<>());
    List<BlockPos> allBlocks = new ArrayList<>();
    allBlocks.addAll(portalInfo.frameBlocks());
    allBlocks.addAll(portalInfo.cornerBlocks());
    for (BlockPos blockPos : allBlocks) {
      chunkIndex.computeIfAbsent(getChunkKey(blockPos), k -> new ArrayList<>()).add(portalInfo);
    }

    // Add portal to the persistent storage.
    if (updateStorage) {
      PortalDataStorage.get().addPortal(portalInfo);

      // Automatically link the portal if it has a target.
      PortalTargetManager.autoLinkPortal(
          portalInfo, getPortals(portalInfo.dimension()), getPortals());
    }
  }

  public static void removePortal(final PortalInfoData portalInfo) {
    if (portalInfo == null || !portals.contains(portalInfo)) {
      return;
    }

    log.info("Removing portal: {}", portalInfo);

    // Find any portals linked to this one and remove their targets
    UUID portalUUID = portalInfo.uuid();
    for (PortalInfoData otherPortal : portals) {
      // Skip the portal being removed
      if (otherPortal.equals(portalInfo)) {
        continue;
      }

      // Check if this other portal links to the portal being removed
      PortalTargetData targetData = PortalTargetManager.getTarget(otherPortal);
      if (targetData != null) {
        // If this portal links to the one being removed, remove the link
        if (isTargetingPortal(targetData, portalInfo)) {
          log.info(
              "Removing link from portal {} to the removed portal {}",
              otherPortal.uuid(),
              portalUUID);
          PortalTargetManager.removeTarget(otherPortal);
        }
      }
    }

    // Remove portal from the global portal set and dimension-specific list.
    portals.remove(portalInfo);
    List<PortalInfoData> dimensionPortals = portalsPerDimension.get(portalInfo.dimension());
    if (dimensionPortals != null) {
      dimensionPortals.remove(portalInfo);
      if (dimensionPortals.isEmpty()) {
        portalsPerDimension.remove(portalInfo.dimension());
      }
    }

    // Remove portal from chunk index.
    Map<Long, List<PortalInfoData>> chunkIndex = portalsPerChunk.get(portalInfo.dimension());
    if (chunkIndex != null) {
      List<BlockPos> allBlocks = new ArrayList<>();
      allBlocks.addAll(portalInfo.frameBlocks());
      allBlocks.addAll(portalInfo.cornerBlocks());

      for (BlockPos blockPos : allBlocks) {
        long chunkKey = getChunkKey(blockPos);
        List<PortalInfoData> chunkPortals = chunkIndex.get(chunkKey);
        if (chunkPortals != null) {
          chunkPortals.remove(portalInfo);
          if (chunkPortals.isEmpty()) {
            chunkIndex.remove(chunkKey);
          }
        }
      }

      if (chunkIndex.isEmpty()) {
        portalsPerChunk.remove(portalInfo.dimension());
      }
    }

    // Remove portal from the persistent storage.
    PortalDataStorage.get().removePortal(portalInfo);

    // Remove portal from the target manager.
    PortalTargetManager.removeTarget(portalInfo);
  }

  public static Set<PortalInfoData> getPortals() {
    return portals;
  }

  /**
   * Gets all portals in the specified dimension.
   *
   * @param dimension The dimension key
   * @return List of portals in that dimension (empty list if none found)
   */
  public static List<PortalInfoData> getPortals(final ResourceKey<Level> dimension) {
    if (dimension == null) {
      return new ArrayList<>();
    }
    return portalsPerDimension.getOrDefault(dimension, new ArrayList<>());
  }

  public static PortalInfoData getPortal(final Level level, final BlockPos blockPos) {
    if (level == null || blockPos == null) {
      return null;
    }

    // Check if the dimension has any portals registered.
    Map<Long, List<PortalInfoData>> chunkMap = portalsPerChunk.get(level.dimension());
    if (chunkMap == null) {
      return null;
    }

    // Check if the block position is within a chunk that has registered portals.
    List<PortalInfoData> portalsInChunk = chunkMap.get(new ChunkPos(blockPos).toLong());
    if (portalsInChunk == null) {
      return null;
    }

    // Check if the block position is part of any portal frame or corner blocks.
    for (PortalInfoData portal : portalsInChunk) {
      if (portal.frameBlocks().contains(blockPos)
          || portal.cornerBlocks().contains(blockPos)
          || portal.innerBlocks().contains(blockPos)) {
        return portal;
      }
    }
    return null;
  }

  public static void clear() {
    log.debug("Clearing all portals ...");
    portals.clear();
    portalsPerDimension.clear();
    portalsPerChunk.clear();
  }

  private static long getChunkKey(final BlockPos blockPos) {
    if (blockPos == null) {
      return 0L;
    }
    return new ChunkPos(blockPos).toLong();
  }

  /**
   * Checks if a portal target is pointing to the given portal.
   *
   * @param targetData The target data to check
   * @param portal The portal to check if being targeted
   * @return true if the target points to the given portal
   */
  private static boolean isTargetingPortal(
      final PortalTargetData targetData, final PortalInfoData portal) {
    if (targetData == null || portal == null) {
      return false;
    }

    // Check if the target's dimension matches the portal's dimension
    if (!targetData.dimension().equals(portal.dimension())) {
      return false;
    }

    // Check if the target's position is within the portal's area
    BlockPos targetPos = targetData.position();

    // Check frame blocks
    for (BlockPos frameBlock : portal.frameBlocks()) {
      if (frameBlock.getX() == targetPos.getX()
          && frameBlock.getY() == targetPos.getY()
          && frameBlock.getZ() == targetPos.getZ()) {
        return true;
      }
    }

    // Also check the portal's teleport position specifically
    BlockPos portalPos = portal.getTeleportPosition();
    return portalPos.getX() == targetPos.getX()
        && portalPos.getY() == targetPos.getY()
        && portalPos.getZ() == targetPos.getZ();
  }
}
