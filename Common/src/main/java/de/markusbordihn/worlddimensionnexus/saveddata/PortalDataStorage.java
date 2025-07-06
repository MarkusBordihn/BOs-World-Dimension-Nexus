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

package de.markusbordihn.worlddimensionnexus.saveddata;

import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalInfoData;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalTargetData;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class PortalDataStorage extends SavedData {

  public static final String DATA_NAME = Constants.MOD_ID + "_portals";

  private static final PrefixLogger log = ModLogger.getPrefixLogger("[Portal Data Storage]");

  private static final String PORTAL_TAG = "Portals";
  private static final String TARGETS_TAG = "Targets";

  private static PortalDataStorage instance;

  private final List<PortalInfoData> portalList;
  private final List<PortalTargetData> targetList;

  public PortalDataStorage(
      final List<PortalInfoData> portals, final List<PortalTargetData> targets) {
    log.info(
        "Creating new PortalDataStorage with {} portals and {} targets ...",
        portals.size(),
        targets.size());
    // Ensure the lists are mutable by creating new ArrayList instances
    this.portalList = new ArrayList<>(portals);
    this.targetList = new ArrayList<>(targets);
  }

  public static void init(final ServerLevel serverLevel) {
    if (instance == null) {
      if (serverLevel == null) {
        log.error("Cannot initialize without a valid level!");
        return;
      }
      log.info("Initializing with level: {}", serverLevel);
      instance = PortalDataStorage.get(serverLevel);
    }
  }

  public static PortalDataStorage get() {
    if (instance == null) {
      throw new IllegalStateException("PortalDataStorage is not initialized!");
    }
    return instance;
  }

  public static PortalDataStorage get(final ServerLevel level) {
    if (instance == null) {
      instance = level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }
    return instance;
  }

  public static SavedData.Factory<PortalDataStorage> factory() {
    return new SavedData.Factory<>(
        () -> new PortalDataStorage(new ArrayList<>(), new ArrayList<>()),
        PortalDataStorage::load,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  }

  public static PortalDataStorage load(final CompoundTag compoundTag, final Provider provider) {
    List<PortalInfoData> loadedPortals =
        PortalInfoData.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(PORTAL_TAG))
            .resultOrPartial(error -> log.error("Failed to decode portal data: {}", error))
            .orElse(new ArrayList<>());

    List<PortalTargetData> loadedTargets =
        PortalTargetData.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(TARGETS_TAG))
            .resultOrPartial(error -> log.error("Failed to decode target data: {}", error))
            .orElse(new ArrayList<>());

    return new PortalDataStorage(loadedPortals, loadedTargets);
  }

  @Override
  public CompoundTag save(final CompoundTag compoundTag, final Provider provider) {
    PortalInfoData.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, portalList)
        .resultOrPartial(error -> log.error("Failed to encode portal data: {}", error))
        .ifPresent(tag -> compoundTag.put(PORTAL_TAG, tag));

    PortalTargetData.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, targetList)
        .resultOrPartial(error -> log.error("Failed to encode target data: {}", error))
        .ifPresent(tag -> compoundTag.put(TARGETS_TAG, tag));

    return compoundTag;
  }

  public void addPortal(final PortalInfoData portal) {
    if (portal == null) {
      log.warn("Cannot add null portal.");
      return;
    }
    portalList.add(portal);
    log.info("Added portal: {}", portal.uuid());
    this.setDirty();
  }

  public void removePortal(final PortalInfoData portal) {
    if (portal == null) {
      log.warn("Cannot remove null portal.");
      return;
    }
    if (portalList.remove(portal)) {
      log.info("Removed portal: {}", portal.uuid());
      this.setDirty();
    }
  }

  public void addTarget(final PortalTargetData target) {
    if (target == null) {
      log.warn("Cannot add null target.");
      return;
    }
    targetList.add(target);
    log.info("Added target: {}", target.portalId());
    this.setDirty();
  }

  public void removeTarget(final PortalTargetData target) {
    if (target == null) {
      log.warn("Cannot remove null target.");
      return;
    }
    if (targetList.remove(target)) {
      log.info("Removed target: {}", target.portalId());
      this.setDirty();
    }
  }

  public List<PortalInfoData> getPortals() {
    return new ArrayList<>(portalList);
  }

  public List<PortalTargetData> getTargets() {
    return new ArrayList<>(targetList);
  }

  public void clear() {
    portalList.clear();
    targetList.clear();
    log.info("Cleared all portal and target data");
    this.setDirty();
  }
}
