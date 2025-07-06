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
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
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

public class DimensionDataStorage extends SavedData {

  public static final String DATA_NAME = Constants.MOD_ID + "_dimensions";
  private static final PrefixLogger log = ModLogger.getPrefixLogger("[Dimension Data Storage]");
  private static final String DIMENSION_TAG = "Dimensions";

  private static DimensionDataStorage instance = null;
  private final List<DimensionInfoData> dimensionList;

  public DimensionDataStorage(final List<DimensionInfoData> dimensionList) {
    log.info("Creating new Dimension Data Storage with {} dimensions.", dimensionList.size());
    this.dimensionList = dimensionList;
  }

  public static void init(final ServerLevel serverLevel) {
    if (instance == null) {
      if (serverLevel == null) {
        log.error("Cannot initialize without a valid level!");
        return;
      }
      log.info("Initializing with level: {}", serverLevel);
      instance = DimensionDataStorage.get(serverLevel);
    }
  }

  public static DimensionDataStorage get() {
    if (instance == null) {
      throw new IllegalStateException("DimensionDataStorage is not initialized!");
    }
    return instance;
  }

  public static DimensionDataStorage get(final ServerLevel level) {
    if (instance == null) {
      instance = level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }
    return instance;
  }

  public static SavedData.Factory<DimensionDataStorage> factory() {
    return new SavedData.Factory<>(
        () -> new DimensionDataStorage(new ArrayList<>()),
        DimensionDataStorage::load,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  }

  public static DimensionDataStorage load(final CompoundTag compoundTag, final Provider provider) {
    List<DimensionInfoData> dimensions =
        DimensionInfoData.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(DIMENSION_TAG))
            .resultOrPartial(error -> log.error("Failed to decode dimension data: {}", error))
            .orElseGet(ArrayList::new);

    return new DimensionDataStorage(new ArrayList<>(dimensions));
  }

  public void addDimension(final DimensionInfoData dimensionInfoData) {
    this.dimensionList.add(dimensionInfoData);
    this.setDirty();
  }

  public boolean removeDimension(final DimensionInfoData dimensionInfoData) {
    boolean removed = this.dimensionList.remove(dimensionInfoData);
    if (removed) {
      this.setDirty();
      log.info("Removed dimension from storage: {}", dimensionInfoData.name().location());
    } else {
      log.warn("Dimension not found in storage: {}", dimensionInfoData.name().location());
    }
    return removed;
  }

  public boolean removeDimensionByName(final String name) {
    DimensionInfoData toRemove = null;
    for (DimensionInfoData dimension : this.dimensionList) {
      if (dimension.name().location().getNamespace().equals(Constants.MOD_ID)
          && dimension.name().location().getPath().equals(name)) {
        toRemove = dimension;
        break;
      }
    }

    if (toRemove != null) {
      return removeDimension(toRemove);
    }
    return false;
  }

  public List<DimensionInfoData> getDimensions() {
    return this.dimensionList;
  }

  @Override
  public CompoundTag save(final CompoundTag compoundTag, final Provider provider) {
    DimensionInfoData.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, this.dimensionList)
        .resultOrPartial(error -> log.error("Failed to encode dimension data: {}", error))
        .ifPresent(dimensionsTag -> compoundTag.put(DIMENSION_TAG, dimensionsTag));

    return compoundTag;
  }
}
