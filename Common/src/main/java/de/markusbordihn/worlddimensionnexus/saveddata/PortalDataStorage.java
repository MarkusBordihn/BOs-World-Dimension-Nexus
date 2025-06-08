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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PortalDataStorage extends SavedData {

  public static final String DATA_NAME = "world_dimension_nexus_portals";
  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  private static final String LOG_PREFIX = "[PortalDataStorage]";
  private static final String PORTAL_TAG = "Portals";
  private static final String TARGETS = "Targets";

  private static PortalDataStorage instance;
  private final List<PortalInfoData> portals;
  private final List<PortalTargetData> targets;

  public PortalDataStorage(
      final List<PortalInfoData> portals, final List<PortalTargetData> targets) {
    this.portals = portals;
    this.targets = targets;
  }

  public static void init(final ServerLevel level) {
    if (instance == null) {
      if (level == null) {
        log.error("{} Cannot initialize without a valid level!", LOG_PREFIX);
        return;
      }
      log.info("{} Initializing with level: {}", LOG_PREFIX, level);
      instance = PortalDataStorage.get(level);
    }
  }

  public static PortalDataStorage get() {
    if (instance == null) {
      throw new IllegalStateException("PortalDataStorage is not initialized!");
    }
    return instance;
  }

  public static SavedData.Factory<PortalDataStorage> factory() {
    return new SavedData.Factory<>(
        () -> new PortalDataStorage(new ArrayList<>(), new ArrayList<>()),
        PortalDataStorage::load,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  }

  public static PortalDataStorage get(final ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
  }

  public static PortalDataStorage load(final CompoundTag compoundTag, final Provider provider) {
    // Decode portal data
    List<PortalInfoData> portals =
        PortalInfoData.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(PORTAL_TAG))
            .resultOrPartial(
                error -> log.error("{} Failed to decode portal data: {}", LOG_PREFIX, error))
            .orElseGet(ArrayList::new);

    // Decode portal targets
    List<PortalTargetData> portalTargets =
        PortalTargetData.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(TARGETS))
            .resultOrPartial(
                error -> log.error("{} Failed to decode portal targets: {}", LOG_PREFIX, error))
            .orElseGet(ArrayList::new);

    return new PortalDataStorage(new ArrayList<>(portals), new ArrayList<>(portalTargets));
  }

  public List<PortalInfoData> getPortals() {
    return this.portals;
  }

  public void addPortal(final PortalInfoData portal) {
    this.portals.add(portal);
    setDirty();
  }

  public void removePortal(final PortalInfoData portal) {
    this.portals.remove(portal);
    setDirty();
  }

  public List<PortalTargetData> getTargets() {
    return this.targets;
  }

  public void addTarget(final PortalTargetData target) {
    this.targets.add(target);
    setDirty();
  }

  public void removeTarget(final PortalTargetData target) {
    this.targets.remove(target);
    setDirty();
  }

  @Override
  public CompoundTag save(final CompoundTag compoundTag, final Provider provider) {
    // Encode portal data
    PortalInfoData.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, this.portals)
        .resultOrPartial(
            error -> log.error("{} Failed to encode portal data: {}", LOG_PREFIX, error))
        .ifPresent(tag -> compoundTag.put(PORTAL_TAG, tag));

    // Encode portal targets
    PortalTargetData.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, this.targets)
        .resultOrPartial(
            error -> log.error("{} Failed to encode portal targets: {}", LOG_PREFIX, error))
        .ifPresent(tag -> compoundTag.put(TARGETS, tag));

    return compoundTag;
  }
}
