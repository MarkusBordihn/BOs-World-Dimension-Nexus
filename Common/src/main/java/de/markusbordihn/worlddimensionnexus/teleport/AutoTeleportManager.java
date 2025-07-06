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

package de.markusbordihn.worlddimensionnexus.teleport;

import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportEntry;
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportTrigger;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.saveddata.AutoTeleportDataStorage;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class AutoTeleportManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Auto Teleport Manager");
  private static final Map<UUID, Long> serverRestartTeleports = new ConcurrentHashMap<>();
  private static final Map<AutoTeleportTrigger, AutoTeleportEntry> globalRules =
      new EnumMap<>(AutoTeleportTrigger.class);

  private AutoTeleportManager() {}

  public static void handlePlayerLogin(final ServerPlayer player) {
    log.debug("Scheduling delayed auto-teleport check for player {}", player.getName().getString());
    if (player.getServer() != null) {
      player
          .getServer()
          .tell(new net.minecraft.server.TickTask(100, () -> checkGlobalAutoTeleportRules(player)));
    }
  }

  private static void checkGlobalAutoTeleportRules(final ServerPlayer player) {
    log.debug(
        "Checking {} global auto-teleport rules for player {}",
        globalRules.size(),
        player.getName().getString());

    String currentDimension = player.serverLevel().dimension().location().toString();

    for (Map.Entry<AutoTeleportTrigger, AutoTeleportEntry> entry : globalRules.entrySet()) {
      AutoTeleportTrigger trigger = entry.getKey();
      AutoTeleportEntry teleportEntry = entry.getValue();

      log.debug("Checking global trigger {} for player {}", trigger, player.getName().getString());

      if (!shouldTriggerTeleport(player, trigger)) {
        log.debug(
            "Global trigger {} should not execute for player {}",
            trigger,
            player.getName().getString());
        continue;
      }

      if (currentDimension.equals(teleportEntry.targetDimension())) {
        log.debug(
            "Player {} is already in target dimension {}, skipping teleport",
            player.getName().getString(),
            teleportEntry.targetDimension());
        continue;
      }

      log.info(
          "Executing global auto-teleport for player {} with trigger {}",
          player.getName().getString(),
          trigger);
      executeTeleport(player, teleportEntry);
      recordTriggerExecution(player, trigger);
      break;
    }
  }

  private static boolean shouldTriggerTeleport(
      final ServerPlayer player, final AutoTeleportTrigger trigger) {
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

  private static void executeTeleport(final ServerPlayer player, final AutoTeleportEntry entry) {
    if (!TeleportManager.startCountdownTeleport(player, entry.targetDimension())) {
      log.warn(
          "Failed to start countdown teleport for player {} to dimension {}",
          player.getName().getString(),
          entry.targetDimension());
    }
  }

  public static void addGlobalAutoTeleport(
      final ServerLevel level,
      final AutoTeleportTrigger trigger,
      final String targetDimension,
      final Vec3 position) {
    if (level == null) {
      log.error("Cannot add auto-teleport rule: ServerLevel is null");
      return;
    }

    if (!DimensionManager.dimensionExists(level.getServer(), targetDimension)) {
      log.warn("Cannot add auto-teleport rule: Invalid dimension {}", targetDimension);
      return;
    }

    AutoTeleportEntry entry = new AutoTeleportEntry(targetDimension, position, trigger);
    globalRules.put(trigger, entry);
    AutoTeleportDataStorage.get(level).setGlobalAutoTeleportRule(entry);

    log.info(
        "Added global auto-teleport rule: {} -> {} at {:.1f}, {:.1f}, {:.1f}",
        trigger,
        targetDimension,
        position.x,
        position.y,
        position.z);
  }

  public static boolean removeGlobalAutoTeleport(
      final ServerLevel level, final AutoTeleportTrigger trigger) {
    AutoTeleportEntry removed = globalRules.remove(trigger);
    if (removed != null && level != null) {
      AutoTeleportDataStorage.get(level).removeGlobalAutoTeleportRule(trigger);
      log.info("Removed global auto-teleport rule for trigger: {}", trigger);
      return true;
    }
    return removed != null;
  }

  public static Map<AutoTeleportTrigger, String> getGlobalAutoTeleportRules() {
    Map<AutoTeleportTrigger, String> rules = new EnumMap<>(AutoTeleportTrigger.class);
    for (Map.Entry<AutoTeleportTrigger, AutoTeleportEntry> entry : globalRules.entrySet()) {
      AutoTeleportEntry teleportEntry = entry.getValue();
      String destination =
          String.format(
              "%s at %.1f, %.1f, %.1f",
              teleportEntry.targetDimension(),
              teleportEntry.position().x,
              teleportEntry.position().y,
              teleportEntry.position().z);
      rules.put(entry.getKey(), destination);
    }
    return rules;
  }

  public static void clearAllGlobalAutoTeleports(final ServerLevel level) {
    globalRules.clear();
    if (level != null) {
      AutoTeleportDataStorage.get(level).clearAllGlobalAutoTeleportRules();
    }
    log.info("Cleared all global auto-teleport rules");
  }

  public static void loadGlobalRules(final ServerLevel level) {
    if (level == null) {
      log.warn("Cannot load global rules without server level context");
      return;
    }
    List<AutoTeleportEntry> savedRulesList =
        AutoTeleportDataStorage.get(level).getGlobalAutoTeleportRules();
    globalRules.clear();
    for (AutoTeleportEntry entry : savedRulesList) {
      globalRules.put(entry.trigger(), entry);
    }
    log.info("Loaded {} global auto-teleport rules from storage", savedRulesList.size());
  }

  private static void recordTriggerExecution(
      final ServerPlayer player, final AutoTeleportTrigger trigger) {
    UUID playerId = player.getUUID();
    ServerLevel level = player.serverLevel();

    switch (trigger) {
      case ONCE_AFTER_SERVER_RESTART ->
          serverRestartTeleports.put(playerId, System.currentTimeMillis());
      case ONCE_PER_SERVER_JOIN, ONCE_PER_DAY, ONCE_PER_WEEK, ONCE_PER_MONTH ->
          AutoTeleportDataStorage.get(level).recordTriggerExecution(playerId, trigger);
      case ALWAYS -> {}
    }
  }

  public static void clearAllCache() {
    log.info("Clearing auto-teleport manager cache for world switch...");
    serverRestartTeleports.clear();
    globalRules.clear();
  }
}
