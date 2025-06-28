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

import com.mojang.serialization.Codec;
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportEntry;
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportTrigger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persistent storage for auto-teleport data. Responsible for saving and loading auto-teleport
 * configurations and execution timestamps to/from the world's saved data. Implements a singleton
 * pattern for global access and uses CODEC-based serialization for data consistency.
 */
public class AutoTeleportDataStorage extends SavedData {

  /** Identifier used for saving the data to disk. */
  public static final String DATA_NAME = Constants.MOD_ID + "_auto_teleports";

  /** Logger for this storage class. */
  private static final PrefixLogger log = ModLogger.getPrefixLogger("[Auto Teleport Data Storage]");

  /** NBT tag names for storage. */
  private static final String AUTO_TELEPORTS_TAG = "AutoTeleports";

  private static final String LAST_EXECUTIONS_TAG = "LastExecutions";

  /**
   * Codec for serializing player auto-teleport entries. Maps UUID to a map of triggers and their
   * corresponding entries.
   */
  private static final Codec<Map<UUID, Map<AutoTeleportTrigger, AutoTeleportEntry>>>
      AUTO_TELEPORTS_CODEC =
          Codec.unboundedMap(
              UUIDUtil.CODEC,
              Codec.unboundedMap(AutoTeleportTrigger.CODEC, AutoTeleportEntry.CODEC));

  /**
   * Codec for serializing player execution timestamps. Maps UUID to a map of triggers and their
   * last execution times.
   */
  private static final Codec<Map<UUID, Map<AutoTeleportTrigger, Long>>> LAST_EXECUTIONS_CODEC =
      Codec.unboundedMap(UUIDUtil.CODEC, Codec.unboundedMap(AutoTeleportTrigger.CODEC, Codec.LONG));

  /** Singleton instance of this storage. */
  private static AutoTeleportDataStorage instance;

  /** Maps of auto-teleport entries and last execution timestamps. */
  private final ConcurrentHashMap<UUID, ConcurrentHashMap<AutoTeleportTrigger, AutoTeleportEntry>>
      autoTeleports;

  private final ConcurrentHashMap<UUID, ConcurrentHashMap<AutoTeleportTrigger, Long>>
      lastExecutions;

  /**
   * Creates a new storage instance with the given auto-teleport data.
   *
   * @param autoTeleports map of auto-teleport configurations
   * @param lastExecutions map of last execution timestamps
   */
  public AutoTeleportDataStorage(
      final Map<UUID, Map<AutoTeleportTrigger, AutoTeleportEntry>> autoTeleports,
      final Map<UUID, Map<AutoTeleportTrigger, Long>> lastExecutions) {
    int totalEntries = autoTeleports.values().stream().mapToInt(Map::size).sum();
    log.info(
        "Creating new Auto Teleport Data Storage with {} entries for {} players.",
        totalEntries,
        autoTeleports.size());
    this.autoTeleports = new ConcurrentHashMap<>();
    autoTeleports.forEach(
        (uuid, triggerMap) -> this.autoTeleports.put(uuid, new ConcurrentHashMap<>(triggerMap)));

    this.lastExecutions = new ConcurrentHashMap<>();
    lastExecutions.forEach(
        (uuid, triggerMap) -> this.lastExecutions.put(uuid, new ConcurrentHashMap<>(triggerMap)));
  }

  /**
   * Initializes the singleton instance with the given server level.
   *
   * @param serverLevel the server level to use for data storage
   */
  public static void init(final ServerLevel serverLevel) {
    if (instance == null) {
      if (serverLevel == null) {
        log.error("Cannot initialize without a valid level!");
        return;
      }
      log.info("Initializing with level: {}", serverLevel);
      instance = AutoTeleportDataStorage.get(serverLevel);
    }
  }

  /**
   * Returns the singleton instance of the auto-teleport data storage.
   *
   * @return the auto-teleport data storage instance
   * @throws IllegalStateException if the storage has not been initialized
   */
  public static AutoTeleportDataStorage get() {
    if (instance == null) {
      throw new IllegalStateException("AutoTeleportDataStorage is not initialized!");
    }
    return instance;
  }

  /**
   * Gets or creates an auto-teleport data storage for the given level.
   *
   * @param level the server level to get the storage for
   * @return the auto-teleport data storage for the level
   */
  public static AutoTeleportDataStorage get(final ServerLevel level) {
    if (instance == null) {
      instance = level.getDataStorage().computeIfAbsent(factory(), DATA_NAME);
    }
    return instance;
  }

  /**
   * Creates a factory for loading auto-teleport data storage.
   *
   * @return a factory that can create or load auto-teleport data storage
   */
  public static SavedData.Factory<AutoTeleportDataStorage> factory() {
    return new SavedData.Factory<>(
        () -> new AutoTeleportDataStorage(new ConcurrentHashMap<>(), new ConcurrentHashMap<>()),
        AutoTeleportDataStorage::load,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
  }

  /**
   * Loads auto-teleport data from an NBT compound tag using CODEC-based deserialization.
   *
   * @param compoundTag the tag to load from
   * @param provider the holder lookup provider
   * @return a new auto-teleport data storage with the loaded data
   */
  public static AutoTeleportDataStorage load(
      final CompoundTag compoundTag, final Provider provider) {
    // Load auto-teleport entries using CODEC
    Map<UUID, Map<AutoTeleportTrigger, AutoTeleportEntry>> autoTeleportsMap =
        AUTO_TELEPORTS_CODEC
            .parse(NbtOps.INSTANCE, compoundTag.get(AUTO_TELEPORTS_TAG))
            .resultOrPartial(error -> log.error("Failed to decode auto-teleport data: {}", error))
            .orElseGet(HashMap::new);

    // Load last execution timestamps using CODEC
    Map<UUID, Map<AutoTeleportTrigger, Long>> lastExecutionsMap =
        LAST_EXECUTIONS_CODEC
            .parse(NbtOps.INSTANCE, compoundTag.get(LAST_EXECUTIONS_TAG))
            .resultOrPartial(error -> log.error("Failed to decode last executions: {}", error))
            .orElseGet(HashMap::new);

    return new AutoTeleportDataStorage(autoTeleportsMap, lastExecutionsMap);
  }

  /**
   * Saves the auto-teleport data to an NBT compound tag using CODEC-based serialization.
   *
   * @param compoundTag the tag to save to
   * @param provider the holder lookup provider
   * @return the compound tag with saved data
   */
  @Override
  public CompoundTag save(final CompoundTag compoundTag, final Provider provider) {
    // Convert ConcurrentHashMap to EnumMap for better performance with enum keys
    Map<UUID, Map<AutoTeleportTrigger, AutoTeleportEntry>> autoTeleportsMap = new HashMap<>();
    autoTeleports.forEach(
        (uuid, triggerMap) -> autoTeleportsMap.put(uuid, new EnumMap<>(triggerMap)));

    Map<UUID, Map<AutoTeleportTrigger, Long>> lastExecutionsMap = new HashMap<>();
    lastExecutions.forEach(
        (uuid, triggerMap) -> lastExecutionsMap.put(uuid, new EnumMap<>(triggerMap)));

    // Save auto-teleport entries using CODEC
    AUTO_TELEPORTS_CODEC
        .encodeStart(NbtOps.INSTANCE, autoTeleportsMap)
        .resultOrPartial(error -> log.error("Failed to encode auto-teleport data: {}", error))
        .ifPresent(tag -> compoundTag.put(AUTO_TELEPORTS_TAG, tag));

    // Save last execution timestamps using CODEC
    LAST_EXECUTIONS_CODEC
        .encodeStart(NbtOps.INSTANCE, lastExecutionsMap)
        .resultOrPartial(error -> log.error("Failed to encode last executions: {}", error))
        .ifPresent(tag -> compoundTag.put(LAST_EXECUTIONS_TAG, tag));

    return compoundTag;
  }

  /**
   * Sets an auto-teleport entry for a player and marks the data as dirty.
   *
   * @param playerId the player's UUID
   * @param entry the auto-teleport entry to set
   */
  public void setAutoTeleport(final UUID playerId, final AutoTeleportEntry entry) {
    autoTeleports
        .computeIfAbsent(playerId, uuid -> new ConcurrentHashMap<>())
        .put(entry.trigger(), entry);
    this.setDirty();
  }

  /**
   * Removes an auto-teleport entry for a player and trigger, and marks the data as dirty.
   *
   * @param playerId the player's UUID
   * @param trigger the teleport trigger
   */
  public void removeAutoTeleport(final UUID playerId, final AutoTeleportTrigger trigger) {
    ConcurrentHashMap<AutoTeleportTrigger, AutoTeleportEntry> playerTeleports =
        autoTeleports.get(playerId);
    if (playerTeleports != null) {
      playerTeleports.remove(trigger);
      if (playerTeleports.isEmpty()) {
        autoTeleports.remove(playerId);
      }
    }

    ConcurrentHashMap<AutoTeleportTrigger, Long> playerExecutions = lastExecutions.get(playerId);
    if (playerExecutions != null) {
      playerExecutions.remove(trigger);
      if (playerExecutions.isEmpty()) {
        lastExecutions.remove(playerId);
      }
    }
    this.setDirty();
  }

  /**
   * Removes all auto-teleport entries for a player and marks the data as dirty.
   *
   * @param playerId the player's UUID
   */
  public void removeAllAutoTeleports(final UUID playerId) {
    boolean removed = autoTeleports.remove(playerId) != null;
    removed |= lastExecutions.remove(playerId) != null;
    if (removed) {
      log.info("Removed all auto-teleport entries for player: {}", playerId);
      this.setDirty();
    }
  }

  /**
   * Updates the last execution timestamp for a player and trigger, and marks the data as dirty.
   *
   * @param playerId the player's UUID
   * @param trigger the teleport trigger
   * @param timestamp the execution timestamp
   */
  public void updateLastExecution(
      final UUID playerId, final AutoTeleportTrigger trigger, final long timestamp) {
    lastExecutions
        .computeIfAbsent(playerId, uuid -> new ConcurrentHashMap<>())
        .put(trigger, timestamp);
    this.setDirty();
  }

  /**
   * Gets the last execution timestamp for a player and trigger.
   *
   * @param playerId the player's UUID
   * @param trigger the teleport trigger
   * @return the last execution timestamp, or 0 if not found
   */
  public long getLastExecution(final UUID playerId, final AutoTeleportTrigger trigger) {
    ConcurrentHashMap<AutoTeleportTrigger, Long> playerExecutions = lastExecutions.get(playerId);
    return playerExecutions != null ? playerExecutions.getOrDefault(trigger, 0L) : 0L;
  }

  /**
   * Checks if a player has an auto-teleport entry for a specific trigger.
   *
   * @param playerId the player's UUID
   * @param trigger the teleport trigger
   * @return true if the player has an auto-teleport entry for the trigger
   */
  public boolean hasAutoTeleport(final UUID playerId, final AutoTeleportTrigger trigger) {
    ConcurrentHashMap<AutoTeleportTrigger, AutoTeleportEntry> playerTeleports =
        autoTeleports.get(playerId);
    return playerTeleports != null && playerTeleports.containsKey(trigger);
  }

  /**
   * Sets a global auto-teleport rule that applies to all players.
   *
   * @param trigger the trigger type
   * @param entry the auto-teleport entry
   */
  public void setGlobalAutoTeleportRule(
      final AutoTeleportTrigger trigger, final AutoTeleportEntry entry) {
    // Use a special UUID for global rules
    UUID globalRuleId = new UUID(0L, trigger.ordinal());
    autoTeleports
        .computeIfAbsent(globalRuleId, uuid -> new ConcurrentHashMap<>())
        .put(trigger, entry);
    this.setDirty();
  }

  /**
   * Removes a global auto-teleport rule.
   *
   * @param trigger the trigger type to remove
   */
  public void removeGlobalAutoTeleportRule(final AutoTeleportTrigger trigger) {
    UUID globalRuleId = new UUID(0L, trigger.ordinal());
    ConcurrentHashMap<AutoTeleportTrigger, AutoTeleportEntry> globalRules =
        autoTeleports.get(globalRuleId);
    if (globalRules != null) {
      globalRules.remove(trigger);
      if (globalRules.isEmpty()) {
        autoTeleports.remove(globalRuleId);
      }
    }
    this.setDirty();
  }

  /**
   * Gets all global auto-teleport rules.
   *
   * @return map of global auto-teleport rules
   */
  public Map<AutoTeleportTrigger, AutoTeleportEntry> getGlobalAutoTeleportRules() {
    Map<AutoTeleportTrigger, AutoTeleportEntry> globalRules =
        new EnumMap<>(AutoTeleportTrigger.class);
    for (AutoTeleportTrigger trigger : AutoTeleportTrigger.values()) {
      UUID globalRuleId = new UUID(0L, trigger.ordinal());
      ConcurrentHashMap<AutoTeleportTrigger, AutoTeleportEntry> rules =
          autoTeleports.get(globalRuleId);
      if (rules != null && rules.containsKey(trigger)) {
        globalRules.put(trigger, rules.get(trigger));
      }
    }
    return globalRules;
  }

  /** Clears all global auto-teleport rules. */
  public void clearAllGlobalAutoTeleportRules() {
    for (AutoTeleportTrigger trigger : AutoTeleportTrigger.values()) {
      UUID globalRuleId = new UUID(0L, trigger.ordinal());
      autoTeleports.remove(globalRuleId);
    }
    this.setDirty();
  }

  /**
   * Checks if a player has triggered a specific auto-teleport.
   *
   * @param playerId the player's UUID
   * @param trigger the trigger type
   * @return true if the player has triggered this auto-teleport
   */
  public boolean hasPlayerTriggered(final UUID playerId, final AutoTeleportTrigger trigger) {
    return getLastExecution(playerId, trigger) > 0L;
  }

  /**
   * Checks if a player has triggered an auto-teleport today.
   *
   * @param playerId the player's UUID
   * @param trigger the trigger type
   * @return true if the player has triggered this auto-teleport today
   */
  public boolean hasPlayerTriggeredToday(final UUID playerId, final AutoTeleportTrigger trigger) {
    long lastExecution = getLastExecution(playerId, trigger);
    if (lastExecution == 0L) {
      return false;
    }

    long currentTime = System.currentTimeMillis();
    long oneDayInMillis = 24 * 60 * 60 * 1000L;
    return (currentTime - lastExecution) < oneDayInMillis;
  }

  /**
   * Checks if a player has triggered an auto-teleport this week.
   *
   * @param playerId the player's UUID
   * @param trigger the trigger type
   * @return true if the player has triggered this auto-teleport this week
   */
  public boolean hasPlayerTriggeredThisWeek(
      final UUID playerId, final AutoTeleportTrigger trigger) {
    long lastExecution = getLastExecution(playerId, trigger);
    if (lastExecution == 0L) {
      return false;
    }

    long currentTime = System.currentTimeMillis();
    long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L;
    return (currentTime - lastExecution) < oneWeekInMillis;
  }

  /**
   * Checks if a player has triggered an auto-teleport this month.
   *
   * @param playerId the player's UUID
   * @param trigger the trigger type
   * @return true if the player has triggered this auto-teleport this month
   */
  public boolean hasPlayerTriggeredThisMonth(
      final UUID playerId, final AutoTeleportTrigger trigger) {
    long lastExecution = getLastExecution(playerId, trigger);
    if (lastExecution == 0L) {
      return false;
    }

    long currentTime = System.currentTimeMillis();
    long oneMonthInMillis = 30L * 24 * 60 * 60 * 1000L; // Approximate month
    return (currentTime - lastExecution) < oneMonthInMillis;
  }

  /**
   * Records the execution of a trigger for a player.
   *
   * @param playerId the player's UUID
   * @param trigger the trigger type
   */
  public void recordTriggerExecution(final UUID playerId, final AutoTeleportTrigger trigger) {
    updateLastExecution(playerId, trigger, System.currentTimeMillis());
  }

  /** Clears all auto-teleport data and marks the data as dirty. */
  public void clear() {
    autoTeleports.clear();
    lastExecutions.clear();
    log.info("Cleared all auto-teleport data");
    this.setDirty();
  }
}
