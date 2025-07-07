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
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportEntry;
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportTrigger;
import de.markusbordihn.worlddimensionnexus.data.teleport.PlayerAutoTeleportData;
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

public class AutoTeleportDataStorage extends SavedData {

  public static final String DATA_NAME = Constants.MOD_ID + "_auto_teleports";

  private static final PrefixLogger log = ModLogger.getPrefixLogger("[Auto Teleport Data Storage]");

  private static final String PLAYER_DATA_TAG = "PlayerData";
  private static final String GLOBAL_RULES_TAG = "GlobalRules";

  private static AutoTeleportDataStorage instance;

  private final List<PlayerAutoTeleportData> playerDataList;
  private final List<AutoTeleportEntry> globalRulesList;

  public AutoTeleportDataStorage(
      final List<PlayerAutoTeleportData> playerData, final List<AutoTeleportEntry> globalRules) {
    log.info(
        "Creating new Auto Teleport Data Storage with {} player entries and {} global rules.",
        playerData.size(),
        globalRules.size());
    this.playerDataList = new ArrayList<>(playerData);
    this.globalRulesList = new ArrayList<>(globalRules);
  }

  public static void init(final ServerLevel serverLevel) {
    if (serverLevel == null) {
      log.error("Cannot initialize without a valid level!");
      return;
    }
    log.info("Initializing with level: {}", serverLevel);
    instance = AutoTeleportDataStorage.get(serverLevel);
  }

  public static AutoTeleportDataStorage get() {
    if (instance == null) {
      throw new IllegalStateException("AutoTeleportDataStorage is not initialized!");
    }
    return instance;
  }

  public static AutoTeleportDataStorage get(final ServerLevel level) {
    if (instance == null) {
      instance = level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }
    return instance;
  }

  public static SavedData.Factory<AutoTeleportDataStorage> factory() {
    return new SavedData.Factory<>(
        () -> new AutoTeleportDataStorage(new ArrayList<>(), new ArrayList<>()),
        AutoTeleportDataStorage::load,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  }

  public static AutoTeleportDataStorage load(
      final CompoundTag compoundTag, final Provider provider) {
    List<PlayerAutoTeleportData> loadedPlayerData =
        PlayerAutoTeleportData.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(PLAYER_DATA_TAG))
            .resultOrPartial(
                error -> log.error("Failed to decode player auto-teleport data: {}", error))
            .orElse(new ArrayList<>());

    List<AutoTeleportEntry> loadedGlobalRules =
        AutoTeleportEntry.CODEC
            .listOf()
            .parse(NbtOps.INSTANCE, compoundTag.get(GLOBAL_RULES_TAG))
            .resultOrPartial(
                error -> log.error("Failed to decode global auto-teleport rules: {}", error))
            .orElse(new ArrayList<>());

    return new AutoTeleportDataStorage(loadedPlayerData, loadedGlobalRules);
  }

  public static void clearInstance() {
    log.info("Clearing AutoTeleportDataStorage instance");
    instance = null;
  }

  public void setAutoTeleport(final UUID playerId, final AutoTeleportEntry entry) {
    PlayerAutoTeleportData existingData =
        playerDataList.stream()
            .filter(data -> data.playerId().equals(playerId))
            .findFirst()
            .orElse(null);

    if (existingData != null) {
      PlayerAutoTeleportData newData = existingData.withTeleport(entry);
      playerDataList.remove(existingData);
      playerDataList.add(newData);
    } else {
      PlayerAutoTeleportData newData = PlayerAutoTeleportData.empty(playerId).withTeleport(entry);
      playerDataList.add(newData);
    }
    this.setDirty();
  }

  public void removeAutoTeleport(final UUID playerId, final AutoTeleportTrigger trigger) {
    playerDataList.removeIf(
        data -> {
          if (data.playerId().equals(playerId)) {
            PlayerAutoTeleportData newData = data.withoutTeleport(trigger);
            if (!newData.hasAnyTeleports()) {
              return true;
            }

            playerDataList.add(newData);
            return true;
          }
          return false;
        });
    this.setDirty();
  }

  public void removeAllAutoTeleports(final UUID playerId) {
    playerDataList.removeIf(data -> data.playerId().equals(playerId));
    this.setDirty();
  }

  public void updateLastExecution(
      final UUID playerId, final AutoTeleportTrigger trigger, final long timestamp) {
    playerDataList.stream()
        .filter(data -> data.playerId().equals(playerId))
        .findFirst()
        .ifPresent(
            data -> {
              PlayerAutoTeleportData newData = data.withExecution(trigger, timestamp);
              playerDataList.remove(data);
              playerDataList.add(newData);
            });
    this.setDirty();
  }

  public long getLastExecution(final UUID playerId, final AutoTeleportTrigger trigger) {
    return playerDataList.stream()
        .filter(data -> data.playerId().equals(playerId))
        .findFirst()
        .map(data -> data.getLastExecution(trigger))
        .orElse(0L);
  }

  public boolean hasAutoTeleport(final UUID playerId, final AutoTeleportTrigger trigger) {
    return playerDataList.stream()
        .filter(data -> data.playerId().equals(playerId))
        .findFirst()
        .map(data -> data.autoTeleports().containsKey(trigger))
        .orElse(false);
  }

  public void setGlobalAutoTeleportRule(final AutoTeleportEntry entry) {
    globalRulesList.removeIf(rule -> rule.trigger().equals(entry.trigger()));
    globalRulesList.add(entry);
    this.setDirty();
  }

  public void clearAllGlobalAutoTeleportRules() {
    globalRulesList.clear();
    this.setDirty();
  }

  public void removeGlobalAutoTeleportRule(AutoTeleportTrigger trigger) {
    globalRulesList.removeIf(rule -> rule.trigger().equals(trigger));
    this.setDirty();
  }

  public List<AutoTeleportEntry> getGlobalAutoTeleportRules() {
    return new ArrayList<>(globalRulesList);
  }

  public boolean hasPlayerTriggered(UUID playerId, AutoTeleportTrigger trigger) {
    return playerDataList.stream()
        .anyMatch(
            data -> data.playerId().equals(playerId) && data.autoTeleports().containsKey(trigger));
  }

  public void recordTriggerExecution(final UUID playerId, final AutoTeleportTrigger trigger) {
    updateLastExecution(playerId, trigger, System.currentTimeMillis());
  }

  public boolean hasPlayerTriggeredToday(final UUID playerId, final AutoTeleportTrigger trigger) {
    return isWithinTimeFrame(getLastExecution(playerId, trigger), 24 * 60 * 60 * 1000L);
  }

  public boolean hasPlayerTriggeredThisWeek(
      final UUID playerId, final AutoTeleportTrigger trigger) {
    return isWithinTimeFrame(getLastExecution(playerId, trigger), 7 * 24 * 60 * 60 * 1000L);
  }

  public boolean hasPlayerTriggeredThisMonth(
      final UUID playerId, final AutoTeleportTrigger trigger) {
    return isWithinTimeFrame(getLastExecution(playerId, trigger), 30L * 24 * 60 * 60 * 1000L);
  }

  private boolean isWithinTimeFrame(long lastExecution, long timeFrameMillis) {
    if (lastExecution == 0L) {
      return false;
    }
    long currentTime = System.currentTimeMillis();
    return (currentTime - lastExecution) < timeFrameMillis;
  }

  public void clear() {
    playerDataList.clear();
    globalRulesList.clear();
    log.info("Cleared all auto-teleport data");
    this.setDirty();
  }

  @Override
  public CompoundTag save(final CompoundTag compoundTag, final Provider provider) {
    PlayerAutoTeleportData.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, playerDataList)
        .resultOrPartial(
            error -> log.error("Failed to encode player auto-teleport data: {}", error))
        .ifPresent(tag -> compoundTag.put(PLAYER_DATA_TAG, tag));

    AutoTeleportEntry.CODEC
        .listOf()
        .encodeStart(NbtOps.INSTANCE, globalRulesList)
        .resultOrPartial(
            error -> log.error("Failed to encode global auto-teleport rules: {}", error))
        .ifPresent(tag -> compoundTag.put(GLOBAL_RULES_TAG, tag));

    return compoundTag;
  }
}
