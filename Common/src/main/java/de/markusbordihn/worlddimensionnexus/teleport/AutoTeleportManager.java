package de.markusbordihn.worlddimensionnexus.teleport;

import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportEntry;
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportTrigger;
import de.markusbordihn.worlddimensionnexus.saveddata.AutoTeleportDataStorage;
import de.markusbordihn.worlddimensionnexus.utils.TeleportHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Manages auto-teleport rules that apply globally to all players but are tracked individually. */
public class AutoTeleportManager {

  private static final Logger LOGGER = LogManager.getLogger(Constants.LOG_NAME);
  private static final ConcurrentHashMap<UUID, Long> serverRestartTeleports =
      new ConcurrentHashMap<>();

  // Global auto-teleport rules that apply to all players
  private static final Map<AutoTeleportTrigger, AutoTeleportEntry> globalRules =
      new ConcurrentHashMap<>();

  public static void handlePlayerLogin(ServerPlayer player) {
    UUID playerId = player.getUUID();
    String playerName = player.getName().getString();

    LOGGER.debug("Checking auto-teleport conditions for player {} ({})", playerName, playerId);

    checkGlobalAutoTeleportRules(player);
  }

  public static void handlePlayerLogout(ServerPlayer player) {
    LOGGER.debug("Player {} logged out", player.getName().getString());
  }

  /** Adds a global auto-teleport rule that applies to all players. */
  public static void addGlobalAutoTeleport(
      AutoTeleportTrigger trigger, String targetDimension, double x, double y, double z) {
    addGlobalAutoTeleport(null, trigger, targetDimension, x, y, z);
  }

  /** Adds a global auto-teleport rule that applies to all players with server level context. */
  public static void addGlobalAutoTeleport(
      ServerLevel level,
      AutoTeleportTrigger trigger,
      String targetDimension,
      double x,
      double y,
      double z) {
    AutoTeleportEntry entry = new AutoTeleportEntry(targetDimension, x, y, z, trigger);
    globalRules.put(trigger, entry);

    // Save persistent rules to storage
    if (isPersistentTrigger(trigger) && level != null) {
      AutoTeleportDataStorage.get().setGlobalAutoTeleportRule(trigger, entry);
    }

    LOGGER.info(
        "Added global auto-teleport rule: {} -> {} at {}, {}, {}",
        trigger,
        targetDimension,
        x,
        y,
        z);
  }

  /** Removes a global auto-teleport rule. */
  public static boolean removeGlobalAutoTeleport(AutoTeleportTrigger trigger) {
    return removeGlobalAutoTeleport(null, trigger);
  }

  /** Removes a global auto-teleport rule with server level context. */
  public static boolean removeGlobalAutoTeleport(ServerLevel level, AutoTeleportTrigger trigger) {
    AutoTeleportEntry removed = globalRules.remove(trigger);

    if (removed != null) {
      // Remove from persistent storage
      if (isPersistentTrigger(trigger) && level != null) {
        AutoTeleportDataStorage.get(level).removeGlobalAutoTeleportRule(trigger);
      }

      LOGGER.info("Removed global auto-teleport rule for trigger: {}", trigger);
      return true;
    }

    return false;
  }

  /** Gets all global auto-teleport rules. */
  public static Map<AutoTeleportTrigger, String> getGlobalAutoTeleportRules() {
    Map<AutoTeleportTrigger, String> rules = new HashMap<>();
    for (Map.Entry<AutoTeleportTrigger, AutoTeleportEntry> entry : globalRules.entrySet()) {
      AutoTeleportEntry teleportEntry = entry.getValue();
      String destination =
          String.format(
              "%s at %.1f, %.1f, %.1f",
              teleportEntry.targetDimension(),
              teleportEntry.x(),
              teleportEntry.y(),
              teleportEntry.z());
      rules.put(entry.getKey(), destination);
    }
    return rules;
  }

  /** Clears all global auto-teleport rules. */
  public static void clearAllGlobalAutoTeleports() {
    clearAllGlobalAutoTeleports(null);
  }

  /** Clears all global auto-teleport rules with server level context. */
  public static void clearAllGlobalAutoTeleports(ServerLevel level) {
    globalRules.clear();
    if (level != null) {
      AutoTeleportDataStorage.get(level).clearAllGlobalAutoTeleportRules();
    }
    LOGGER.info("Cleared all global auto-teleport rules");
  }

  /** Loads global auto-teleport rules from storage. */
  public static void loadGlobalRules(ServerLevel level) {
    if (level == null) {
      LOGGER.warn("Cannot load global rules without server level context");
      return;
    }
    Map<AutoTeleportTrigger, AutoTeleportEntry> savedRules =
        AutoTeleportDataStorage.get(level).getGlobalAutoTeleportRules();
    globalRules.putAll(savedRules);
    LOGGER.info("Loaded {} global auto-teleport rules from storage", savedRules.size());
  }

  private static void checkGlobalAutoTeleportRules(ServerPlayer player) {
    for (Map.Entry<AutoTeleportTrigger, AutoTeleportEntry> entry : globalRules.entrySet()) {
      AutoTeleportTrigger trigger = entry.getKey();
      AutoTeleportEntry teleportEntry = entry.getValue();

      if (shouldTriggerTeleport(player, trigger)) {
        executeTeleport(player, teleportEntry);
        recordTriggerExecution(player, trigger);
      }
    }
  }

  private static boolean shouldTriggerTeleport(ServerPlayer player, AutoTeleportTrigger trigger) {
    UUID playerId = player.getUUID();
    ServerLevel level = player.serverLevel();

    return switch (trigger) {
      case ALWAYS -> true;
      case ONCE_AFTER_SERVER_RESTART -> !serverRestartTeleports.containsKey(playerId);
      case ONCE_PER_SERVER_JOIN ->
          !AutoTeleportDataStorage.get(level).hasPlayerTriggered(playerId, trigger);
      case ONCE_PER_DAY ->
          !AutoTeleportDataStorage.get(level).hasPlayerTriggeredToday(playerId, trigger);
      case ONCE_PER_WEEK ->
          !AutoTeleportDataStorage.get(level).hasPlayerTriggeredThisWeek(playerId, trigger);
      case ONCE_PER_MONTH ->
          !AutoTeleportDataStorage.get(level).hasPlayerTriggeredThisMonth(playerId, trigger);
    };
  }

  private static void executeTeleport(ServerPlayer player, AutoTeleportEntry entry) {
    boolean success = TeleportHelper.safeTeleportToDimension(player, entry.targetDimension());

    if (success) {
      LOGGER.info(
          "Auto-teleported player {} to {} at {}, {}, {}",
          player.getName().getString(),
          entry.targetDimension(),
          entry.x(),
          entry.y(),
          entry.z());
    } else {
      LOGGER.warn(
          "Failed to auto-teleport player {} to {}",
          player.getName().getString(),
          entry.targetDimension());
    }
  }

  private static void recordTriggerExecution(ServerPlayer player, AutoTeleportTrigger trigger) {
    UUID playerId = player.getUUID();
    ServerLevel level = player.serverLevel();

    switch (trigger) {
      case ONCE_AFTER_SERVER_RESTART ->
          serverRestartTeleports.put(playerId, System.currentTimeMillis());
      case ONCE_PER_SERVER_JOIN, ONCE_PER_DAY, ONCE_PER_WEEK, ONCE_PER_MONTH ->
          AutoTeleportDataStorage.get(level).recordTriggerExecution(playerId, trigger);
        // ALWAYS doesn't need recording
    }
  }

  private static boolean isPersistentTrigger(AutoTeleportTrigger trigger) {
    return trigger != AutoTeleportTrigger.ALWAYS
        && trigger != AutoTeleportTrigger.ONCE_AFTER_SERVER_RESTART;
  }

  /** Clears all auto-teleport state data for cache cleanup. */
  public static void clearAllState() {
    serverRestartTeleports.clear();
    globalRules.clear();
  }

  /**
   * Registers an auto-teleport for a specific player.
   *
   * @param player the player to register the auto-teleport for
   * @param trigger the trigger type
   * @param targetDimension the target dimension
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   */
  public static void registerAutoTeleport(
      final ServerPlayer player,
      final AutoTeleportTrigger trigger,
      final String targetDimension,
      final double x,
      final double y,
      final double z) {
    AutoTeleportEntry entry = new AutoTeleportEntry(targetDimension, x, y, z, trigger);
    AutoTeleportDataStorage.get(player.serverLevel()).setAutoTeleport(player.getUUID(), entry);
    LOGGER.info(
        "Registered auto-teleport for player {}: {} -> {} at {}, {}, {}",
        player.getName().getString(),
        trigger,
        targetDimension,
        x,
        y,
        z);
  }

  /**
   * Removes an auto-teleport for a specific player and trigger.
   *
   * @param player the player to remove the auto-teleport for
   * @param trigger the trigger type to remove
   * @return true if the auto-teleport was removed, false if it didn't exist
   */
  public static boolean removeAutoTeleport(
      final ServerPlayer player, final AutoTeleportTrigger trigger) {
    UUID playerId = player.getUUID();
    ServerLevel level = player.serverLevel();
    boolean hadEntry = AutoTeleportDataStorage.get(level).hasAutoTeleport(playerId, trigger);
    if (hadEntry) {
      AutoTeleportDataStorage.get(level).removeAutoTeleport(playerId, trigger);
      LOGGER.info("Removed auto-teleport for player {}: {}", player.getName().getString(), trigger);
    }
    return hadEntry;
  }

  /**
   * Removes all auto-teleports for a specific player.
   *
   * @param player the player to remove all auto-teleports for
   */
  public static void removeAllAutoTeleports(final ServerPlayer player) {
    AutoTeleportDataStorage.get(player.serverLevel()).removeAllAutoTeleports(player.getUUID());
    LOGGER.info("Removed all auto-teleports for player {}", player.getName().getString());
  }
}
