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
import de.markusbordihn.worlddimensionnexus.data.warp.WarpData;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class WarpDataStorage extends SavedData {

  public static final String DATA_NAME = Constants.MOD_ID + "_warps";

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Warp Data Storage");

  private static final String WARPS_TAG = "Warps";

  private static WarpDataStorage instance;

  private final List<WarpData> warpList;

  public WarpDataStorage(final List<WarpData> warps) {
    log.info("Creating new WarpDataStorage with {} warps ...", warps.size());
    this.warpList = new ArrayList<>(warps);
  }

  public static void init(final ServerLevel serverLevel) {
    if (serverLevel == null) {
      log.error("Cannot initialize without a valid level!");
      return;
    }
    log.info("Initializing with level: {}", serverLevel);
    instance = WarpDataStorage.get(serverLevel);
  }

  public static WarpDataStorage get() {
    if (instance == null) {
      throw new IllegalStateException("WarpDataStorage is not initialized!");
    }
    return instance;
  }

  public static WarpDataStorage get(final ServerLevel level) {
    if (instance == null) {
      instance = level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }
    return instance;
  }

  public static SavedData.Factory<WarpDataStorage> factory() {
    return new SavedData.Factory<>(
        () -> new WarpDataStorage(new ArrayList<>()),
        WarpDataStorage::load,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  }

  public static WarpDataStorage load(final CompoundTag compoundTag, final Provider provider) {
    List<WarpData> loadedWarps =
        WarpData.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(WARPS_TAG))
            .resultOrPartial(error -> log.error("Failed to decode warp data: {}", error))
            .orElse(new ArrayList<>());

    return new WarpDataStorage(loadedWarps);
  }

  public static void clearInstance() {
    log.info("Clearing WarpDataStorage instance");
    instance = null;
  }

  public void addWarp(final WarpData warp) {
    if (warp == null) {
      log.warn("Cannot add null warp.");
      return;
    }
    warpList.add(warp);
    log.info("Added warp: {} ({})", warp.name(), warp.uuid());
    this.setDirty();
  }

  public void removeWarp(final WarpData warp) {
    if (warp == null) {
      log.warn("Cannot remove null warp.");
      return;
    }
    if (warpList.remove(warp)) {
      log.info("Removed warp: {} ({})", warp.name(), warp.uuid());
      this.setDirty();
    }
  }

  public void removeWarp(final UUID warpId) {
    if (warpId == null) {
      log.warn("Cannot remove warp with null ID.");
      return;
    }
    if (warpList.removeIf(warp -> warpId.equals(warp.uuid()))) {
      log.info("Removed warp with ID: {}", warpId);
      this.setDirty();
    }
  }

  public void updateWarp(final WarpData updatedWarp) {
    if (updatedWarp == null) {
      log.warn("Cannot update with null warp.");
      return;
    }
    for (int i = 0; i < warpList.size(); i++) {
      if (warpList.get(i).uuid().equals(updatedWarp.uuid())) {
        warpList.set(i, updatedWarp);
        log.info("Updated warp: {} ({})", updatedWarp.name(), updatedWarp.uuid());
        this.setDirty();
        return;
      }
    }
    log.warn("Warp not found for update: {} ({})", updatedWarp.name(), updatedWarp.uuid());
  }

  public List<WarpData> getWarps() {
    return new ArrayList<>(warpList);
  }

  public void clear() {
    warpList.clear();
    log.info("Cleared all warp data");
    this.setDirty();
  }

  @Override
  public CompoundTag save(final CompoundTag compoundTag, final Provider provider) {
    WarpData.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, warpList)
        .resultOrPartial(error -> log.error("Failed to encode warp data: {}", error))
        .ifPresent(tag -> compoundTag.put(WARPS_TAG, tag));

    return compoundTag;
  }
}
