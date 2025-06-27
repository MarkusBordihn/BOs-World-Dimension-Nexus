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

/**
 * Persistent storage for portal data.
 *
 * <p>Responsible for saving and loading portal information and target mappings to/from the world's
 * saved data. Implements a singleton pattern for global access.
 */
public class PortalDataStorage extends SavedData {

  /** Identifier used for saving the data to disk. */
  public static final String DATA_NAME = Constants.MOD_ID + "_portals";

  /** Logger for this storage class. */
  private static final PrefixLogger log = ModLogger.getPrefixLogger("[Portal Data Storage]");

  /** NBT tag names for storage. */
  private static final String PORTAL_TAG = "Portals";

  private static final String TARGETS = "Targets";

  /** Singleton instance of this storage. */
  private static PortalDataStorage instance;

  /** Lists of portals and their target mappings. */
  private final List<PortalInfoData> portals;

  private final List<PortalTargetData> targets;

  /**
   * Creates a new storage instance with the given portal and target data.
   *
   * @param portals List of portal information
   * @param targets List of portal target mappings
   */
  public PortalDataStorage(
      final List<PortalInfoData> portals, final List<PortalTargetData> targets) {
    log.info(
        "Creating new PortalDataStorage with {} portals and {} targets ...",
        portals.size(),
        targets.size());
    // Ensure the lists are mutable by creating new ArrayList instances
    this.portals = new ArrayList<>(portals);
    this.targets = new ArrayList<>(targets);
  }

  /**
   * Initializes the singleton instance with the given server level.
   *
   * @param serverLevel The server level to use for data storage
   */
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

  /**
   * Returns the singleton instance of the portal data storage.
   *
   * @return The portal data storage instance
   * @throws IllegalStateException if the storage has not been initialized
   */
  public static PortalDataStorage get() {
    if (instance == null) {
      throw new IllegalStateException("PortalDataStorage is not initialized!");
    }
    return instance;
  }

  /**
   * Gets or creates a portal data storage for the given level.
   *
   * @param level The server level to get the storage for
   * @return The portal data storage for the level
   */
  public static PortalDataStorage get(final ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
  }

  /**
   * Creates a factory for loading portal data storage.
   *
   * @return A factory that can create or load portal data storage
   */
  public static SavedData.Factory<PortalDataStorage> factory() {
    return new SavedData.Factory<>(
        () -> new PortalDataStorage(new ArrayList<>(), new ArrayList<>()),
        PortalDataStorage::load,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  }

  /**
   * Loads portal data from an NBT compound tag.
   *
   * @param compoundTag The tag to load from
   * @param provider The holder lookup provider
   * @return A new portal data storage with the loaded data
   */
  public static PortalDataStorage load(final CompoundTag compoundTag, final Provider provider) {
    List<PortalInfoData> portals =
        PortalInfoData.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(PORTAL_TAG))
            .resultOrPartial(error -> log.error("Failed to decode portal data: {}", error))
            .orElseGet(ArrayList::new);

    List<PortalTargetData> portalTargets =
        PortalTargetData.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(TARGETS))
            .resultOrPartial(error -> log.error("Failed to decode portal targets: {}", error))
            .orElseGet(ArrayList::new);

    return new PortalDataStorage(portals, portalTargets);
  }

  /**
   * Returns all stored portals.
   *
   * @return Mutable list of portals
   */
  public List<PortalInfoData> getPortals() {
    return this.portals;
  }

  /**
   * Adds a portal to the storage and marks the data as dirty.
   *
   * @param portal The portal to add
   */
  public void addPortal(final PortalInfoData portal) {
    this.portals.add(portal);
    this.setDirty();
  }

  /**
   * Removes a portal from the storage and marks the data as dirty.
   *
   * @param portal The portal to remove
   */
  public void removePortal(final PortalInfoData portal) {
    this.portals.remove(portal);
    this.setDirty();
  }

  /**
   * Returns all stored portal targets.
   *
   * @return Mutable list of portal targets
   */
  public List<PortalTargetData> getTargets() {
    return this.targets;
  }

  /**
   * Adds a portal target to the storage and marks the data as dirty.
   *
   * @param target The portal target to add
   */
  public void addTarget(final PortalTargetData target) {
    this.targets.add(target);
    this.setDirty();
  }

  /**
   * Removes a portal target from the storage and marks the data as dirty.
   *
   * @param target The portal target to remove
   */
  public void removeTarget(final PortalTargetData target) {
    this.targets.remove(target);
    this.setDirty();
  }

  /**
   * Saves the portal data to an NBT compound tag.
   *
   * @param compoundTag The tag to save to
   * @param provider The holder lookup provider
   * @return The compound tag with saved data
   */
  @Override
  public CompoundTag save(final CompoundTag compoundTag, final Provider provider) {
    PortalInfoData.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, this.portals)
        .resultOrPartial(error -> log.error("Failed to encode portal data: {}", error))
        .ifPresent(tag -> compoundTag.put(PORTAL_TAG, tag));

    PortalTargetData.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, this.targets)
        .resultOrPartial(error -> log.error("Failed to encode portal targets: {}", error))
        .ifPresent(tag -> compoundTag.put(TARGETS, tag));

    return compoundTag;
  }
}
