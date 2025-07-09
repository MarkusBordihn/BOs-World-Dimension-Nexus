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
import de.markusbordihn.worlddimensionnexus.data.teleport.TeleportLocation;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class TeleportHistoryDataStorage extends SavedData {

  public static final String DATA_NAME = Constants.MOD_ID + "_teleport_history";
  private static final PrefixLogger log =
      ModLogger.getPrefixLogger("[Teleport History Data Storage]");
  private static final String PLAYER_HISTORIES_TAG = "PlayerHistories";

  private static TeleportHistoryDataStorage instance = null;
  private final Map<UUID, List<TeleportLocation>> playerHistories;

  public TeleportHistoryDataStorage(final Map<UUID, List<TeleportLocation>> playerHistories) {
    log.info(
        "Creating new Teleport History Data Storage with {} player histories.",
        playerHistories.size());
    this.playerHistories = new HashMap<>(playerHistories);
  }

  public static void init(final ServerLevel serverLevel) {
    if (serverLevel == null) {
      log.error("Cannot initialize without a valid level!");
      return;
    }
    log.info("Initializing with level: {}", serverLevel);
    instance = TeleportHistoryDataStorage.get(serverLevel);
  }

  public static TeleportHistoryDataStorage get() {
    if (instance == null) {
      throw new IllegalStateException("TeleportHistoryDataStorage is not initialized!");
    }
    return instance;
  }

  public static TeleportHistoryDataStorage get(final ServerLevel level) {
    if (instance == null) {
      instance = level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }
    return instance;
  }

  public static SavedData.Factory<TeleportHistoryDataStorage> factory() {
    return new SavedData.Factory<>(
        () -> new TeleportHistoryDataStorage(new HashMap<>()),
        TeleportHistoryDataStorage::load,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  }

  public static TeleportHistoryDataStorage load(
      final CompoundTag compoundTag, final Provider provider) {
    Map<UUID, List<TeleportLocation>> playerHistories = new HashMap<>();

    if (compoundTag.contains(PLAYER_HISTORIES_TAG)) {
      CompoundTag historiesTag = compoundTag.getCompound(PLAYER_HISTORIES_TAG);

      for (String playerIdString : historiesTag.getAllKeys()) {
        try {
          UUID playerId = UUID.fromString(playerIdString);
          ListTag historyListTag = historiesTag.getList(playerIdString, 10);

          List<TeleportLocation> history = new ArrayList<>();
          for (int i = 0; i < historyListTag.size(); i++) {
            CompoundTag locationTag = historyListTag.getCompound(i);

            TeleportLocation.CODEC
                .parse(NbtOps.INSTANCE, locationTag)
                .resultOrPartial(
                    error -> log.error("Failed to decode teleport location: {}", error))
                .ifPresent(history::add);
          }

          if (!history.isEmpty()) {
            playerHistories.put(playerId, history);
          }
        } catch (Exception e) {
          log.error(
              "Failed to load teleport history for player {}: {}", playerIdString, e.getMessage());
        }
      }
    }

    return new TeleportHistoryDataStorage(playerHistories);
  }

  public static void clearInstance() {
    log.info("Clearing TeleportHistoryDataStorage instance");
    instance = null;
  }

  public void savePlayerHistory(final UUID playerId, final List<TeleportLocation> history) {
    if (history.isEmpty()) {
      this.playerHistories.remove(playerId);
    } else {
      this.playerHistories.put(playerId, new ArrayList<>(history));
    }
    this.setDirty();
  }

  public List<TeleportLocation> getPlayerHistory(final UUID playerId) {
    return new ArrayList<>(this.playerHistories.getOrDefault(playerId, new ArrayList<>()));
  }

  public void clearPlayerHistory(final UUID playerId) {
    if (this.playerHistories.remove(playerId) != null) {
      this.setDirty();
      log.info("Cleared teleport history for player: {}", playerId);
    }
  }

  public Map<UUID, List<TeleportLocation>> getAllPlayerHistories() {
    return new HashMap<>(this.playerHistories);
  }

  @Override
  public CompoundTag save(final CompoundTag compoundTag, final Provider provider) {
    CompoundTag historiesTag = new CompoundTag();

    for (Map.Entry<UUID, List<TeleportLocation>> entry : this.playerHistories.entrySet()) {
      String playerIdString = entry.getKey().toString();
      List<TeleportLocation> history = entry.getValue();

      ListTag historyListTag = new ListTag();
      for (TeleportLocation location : history) {
        TeleportLocation.CODEC
            .encodeStart(NbtOps.INSTANCE, location)
            .resultOrPartial(error -> log.error("Failed to encode teleport location: {}", error))
            .ifPresent(historyListTag::add);
      }

      if (!historyListTag.isEmpty()) {
        historiesTag.put(playerIdString, historyListTag);
      }
    }

    compoundTag.put(PLAYER_HISTORIES_TAG, historiesTag);
    return compoundTag;
  }
}
