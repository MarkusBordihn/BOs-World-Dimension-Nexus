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

    for (PortalInfoData portalInfo : portalList) {
      addPortal(portalInfo, false);
    }
  }

  public static boolean addPortal(final PortalInfoData portalInfo) {
    return addPortal(portalInfo, true);
  }

  private static List<BlockPos> getAllPortalBlocks(final PortalInfoData portalInfo) {
    List<BlockPos> allBlocks = new ArrayList<>();
    allBlocks.addAll(portalInfo.frameBlocks());
    allBlocks.addAll(portalInfo.cornerBlocks());
    return allBlocks;
  }

  public static boolean addPortal(final PortalInfoData portalInfo, final boolean updateStorage) {
    if (portalInfo == null) {
      return false;
    }

    log.info("Adding portal: {}", portalInfo);
    portals.add(portalInfo);
    portalsPerDimension
        .computeIfAbsent(portalInfo.dimension(), k -> new ArrayList<>())
        .add(portalInfo);

    Map<Long, List<PortalInfoData>> chunkIndex =
        portalsPerChunk.computeIfAbsent(portalInfo.dimension(), k -> new ConcurrentHashMap<>());

    for (BlockPos blockPos : getAllPortalBlocks(portalInfo)) {
      chunkIndex.computeIfAbsent(getChunkKey(blockPos), k -> new ArrayList<>()).add(portalInfo);
    }

    if (updateStorage) {
      PortalDataStorage.get().addPortal(portalInfo);
      PortalTargetManager.autoLinkPortal(
          portalInfo, getPortals(portalInfo.dimension()), getPortals());
    }
    return true;
  }

  public static void removePortal(final PortalInfoData portalInfo) {
    if (portalInfo == null || !portals.contains(portalInfo)) {
      return;
    }

    log.info("Removing portal: {}", portalInfo);

    cleanupPortalLinks(portalInfo);

    portals.remove(portalInfo);
    removeDimensionPortal(portalInfo);
    removeChunkPortal(portalInfo);

    PortalDataStorage.get().removePortal(portalInfo);

    PortalTargetManager.removeTarget(portalInfo);
  }

  private static void cleanupPortalLinks(final PortalInfoData portalToRemove) {
    UUID portalUUID = portalToRemove.uuid();

    portals.parallelStream()
        .filter(otherPortal -> !otherPortal.equals(portalToRemove))
        .forEach(
            otherPortal -> {
              PortalTargetData targetData = PortalTargetManager.getTarget(otherPortal);
              if (isTargetingPortal(targetData, portalToRemove)) {
                log.info(
                    "Removing link from portal {} to the removed portal {}",
                    otherPortal.uuid(),
                    portalUUID);
                PortalTargetManager.removeTarget(otherPortal);
              }
            });
  }

  private static void removeDimensionPortal(final PortalInfoData portalInfo) {
    List<PortalInfoData> dimensionPortals = portalsPerDimension.get(portalInfo.dimension());
    if (dimensionPortals != null) {
      dimensionPortals.remove(portalInfo);
      if (dimensionPortals.isEmpty()) {
        portalsPerDimension.remove(portalInfo.dimension());
      }
    }
  }

  private static void removeChunkPortal(final PortalInfoData portalInfo) {
    Map<Long, List<PortalInfoData>> chunkIndex = portalsPerChunk.get(portalInfo.dimension());
    if (chunkIndex != null) {
      for (BlockPos blockPos : getAllPortalBlocks(portalInfo)) {
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
  }

  public static Set<PortalInfoData> getPortals() {
    return portals;
  }

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

    Map<Long, List<PortalInfoData>> chunkMap = portalsPerChunk.get(level.dimension());
    if (chunkMap == null) {
      return null;
    }

    List<PortalInfoData> portalsInChunk = chunkMap.get(new ChunkPos(blockPos).toLong());
    if (portalsInChunk == null) {
      return null;
    }

    return portalsInChunk.stream()
        .filter(portal -> isBlockPartOfPortal(portal, blockPos))
        .findFirst()
        .orElse(null);
  }

  public static PortalInfoData getPortal(final UUID uuid) {
    if (uuid == null) {
      return null;
    }
    return portals.stream().filter(portal -> portal.uuid().equals(uuid)).findFirst().orElse(null);
  }

  private static boolean isBlockPartOfPortal(final PortalInfoData portal, final BlockPos blockPos) {
    return portal.frameBlocks().contains(blockPos)
        || portal.cornerBlocks().contains(blockPos)
        || portal.innerBlocks().contains(blockPos);
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

  public static List<PortalInfoData> getLinkedPortals(final PortalInfoData portalInfo) {
    if (portalInfo == null) {
      return new ArrayList<>();
    }

    return portals.stream()
        .filter(other -> portalInfo.isLinkedTo(other))
        .collect(java.util.stream.Collectors.toList());
  }

  private static boolean isTargetingPortal(
      final PortalTargetData targetData, final PortalInfoData portal) {
    if (targetData == null || portal == null) {
      return false;
    }

    if (!targetData.dimension().equals(portal.dimension())) {
      return false;
    }

    BlockPos targetPos = targetData.position();

    return portal.frameBlocks().contains(targetPos)
        || targetPos.equals(portal.getTeleportPosition());
  }
}
