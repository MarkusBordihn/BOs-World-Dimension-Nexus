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

import de.markusbordihn.worlddimensionnexus.Constants;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class AutoTeleportManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Auto Teleport Manager");
  private static final Map<UUID, Long> serverRestartTeleports = new ConcurrentHashMap<>();
  private static final Map<AutoTeleportTrigger, AutoTeleportEntry> globalRules =
      new EnumMap<>(AutoTeleportTrigger.class);
  private static final Map<UUID, String> pendingDeathTeleports = new ConcurrentHashMap<>();

  private AutoTeleportManager() {}

  public static void handlePlayerLogin(final ServerPlayer player) {
    log.debug("Scheduling delayed auto-teleport check for player {}", player.getName().getString());
    if (player.getServer() != null) {
      player
          .getServer()
          .tell(new net.minecraft.server.TickTask(100, () -> checkGlobalAutoTeleportRules(player)));
    }
  }

  public static void handlePlayerDeath(final ServerPlayer player) {
    log.debug(
        "Processing death-triggered auto-teleport for player: {}", player.getName().getString());
    processPlayerDeath(player);
  }

  public static void markPlayerForDeathTeleport(final ServerPlayer player) {
    log.debug("Marking player {} for death teleport", player.getName().getString());

    AutoTeleportEntry deathTeleportRule = globalRules.get(AutoTeleportTrigger.ON_DEATH);
    if (deathTeleportRule == null) {
      log.debug("No death auto-teleport rule configured");
      return;
    }

    String currentDimensionId = getCurrentDimensionId(player);
    if (!canExecuteTrigger(
        player, AutoTeleportTrigger.ON_DEATH, currentDimensionId, deathTeleportRule)) {
      return;
    }

    // Mark player for teleport after respawn
    pendingDeathTeleports.put(player.getUUID(), deathTeleportRule.targetDimension());
    log.debug(
        "Player {} marked for death teleport to {}",
        player.getName().getString(),
        deathTeleportRule.targetDimension());
  }

  public static void handlePlayerRespawn(final ServerPlayer player) {
    UUID playerId = player.getUUID();
    String targetDimension = pendingDeathTeleports.remove(playerId);

    if (targetDimension == null) {
      log.debug("No pending death teleport for player {}", player.getName().getString());
      return;
    }

    log.debug(
        "Executing pending death teleport for player {} to {}",
        player.getName().getString(),
        targetDimension);

    AutoTeleportEntry deathTeleportRule = globalRules.get(AutoTeleportTrigger.ON_DEATH);
    if (deathTeleportRule != null) {
      if (player.getServer() != null) {
        player
            .getServer()
            .tell(
                new net.minecraft.server.TickTask(
                    10,
                    () -> {
                      executeTeleport(player, deathTeleportRule);
                      recordTriggerExecution(player, AutoTeleportTrigger.ON_DEATH);
                    }));
      }
    }
  }

  private static void checkGlobalAutoTeleportRules(final ServerPlayer player) {
    log.debug(
        "Evaluating {} global auto-teleport rules for player: {}",
        globalRules.size(),
        player.getName().getString());

    String currentDimensionId = getCurrentDimensionId(player);

    for (Map.Entry<AutoTeleportTrigger, AutoTeleportEntry> ruleEntry : globalRules.entrySet()) {
      AutoTeleportTrigger triggerType = ruleEntry.getKey();
      AutoTeleportEntry teleportRule = ruleEntry.getValue();

      if (shouldSkipTriggerInGlobalCheck(triggerType)) {
        continue;
      }

      log.debug(
          "Evaluating trigger '{}' for player: {}", triggerType, player.getName().getString());

      if (!canExecuteTrigger(player, triggerType, currentDimensionId, teleportRule)) {
        continue;
      }

      executeTeleportRule(player, triggerType, teleportRule);
      return; // Exit after first successful teleport to avoid multiple teleports
    }
  }

  private static void processPlayerDeath(final ServerPlayer player) {
    log.debug("Processing death trigger for player: {}", player.getName().getString());

    AutoTeleportEntry deathTeleportRule = globalRules.get(AutoTeleportTrigger.ON_DEATH);
    if (deathTeleportRule == null) {
      log.debug("No death auto-teleport rule configured");
      return;
    }

    String currentDimensionId = getCurrentDimensionId(player);

    if (!canExecuteTrigger(
        player, AutoTeleportTrigger.ON_DEATH, currentDimensionId, deathTeleportRule)) {
      return;
    }

    executeTeleportRule(player, AutoTeleportTrigger.ON_DEATH, deathTeleportRule);
  }

  private static String getCurrentDimensionId(final ServerPlayer player) {
    return player.serverLevel().dimension().location().toString();
  }

  private static boolean canExecuteTrigger(
      final ServerPlayer player,
      final AutoTeleportTrigger triggerType,
      final String currentDimensionId,
      final AutoTeleportEntry teleportRule) {

    if (!isTriggerConditionSatisfied(player, triggerType)) {
      log.debug(
          "Trigger '{}' conditions not satisfied for player: {}",
          triggerType,
          player.getName().getString());
      return false;
    }

    return !isPlayerAlreadyInTargetDimension(
        currentDimensionId, teleportRule.targetDimension(), player);
  }

  private static boolean shouldSkipTriggerInGlobalCheck(final AutoTeleportTrigger triggerType) {
    // ON_DEATH has dedicated event handling and should not be processed in global checks
    return triggerType == AutoTeleportTrigger.ON_DEATH;
  }

  private static boolean isPlayerAlreadyInTargetDimension(
      final String currentDimensionId, final String targetDimensionId, final ServerPlayer player) {
    if (currentDimensionId.equals(targetDimensionId)) {
      log.debug(
          "Player '{}' already in target dimension '{}', skipping teleport",
          player.getName().getString(),
          targetDimensionId);
      return true;
    }
    return false;
  }

  private static void executeTeleportRule(
      final ServerPlayer player,
      final AutoTeleportTrigger triggerType,
      final AutoTeleportEntry teleportRule) {
    log.debug(
        "Executing auto-teleport rule: player '{}', trigger '{}', destination '{}'",
        player.getName().getString(),
        triggerType,
        teleportRule.targetDimension());

    executeTeleport(player, teleportRule);
    recordTriggerExecution(player, triggerType);
  }

  private static boolean isTriggerConditionSatisfied(
      final ServerPlayer player, final AutoTeleportTrigger trigger) {
    UUID playerId = player.getUUID();

    return switch (trigger) {
      case ALWAYS -> true;
      case ON_DEATH -> true; // Death trigger always executes when player dies
      case ONCE_AFTER_SERVER_RESTART -> !serverRestartTeleports.containsKey(playerId);
      case ONCE_PER_SERVER_JOIN ->
          !AutoTeleportDataStorage.get().hasPlayerTriggered(playerId, trigger);
      case ONCE_PER_DAY ->
          !AutoTeleportDataStorage.get().hasPlayerTriggeredToday(playerId, trigger);
      case ONCE_PER_WEEK ->
          !AutoTeleportDataStorage.get().hasPlayerTriggeredThisWeek(playerId, trigger);
      case ONCE_PER_MONTH ->
          !AutoTeleportDataStorage.get().hasPlayerTriggeredThisMonth(playerId, trigger);
    };
  }

  private static void executeTeleport(final ServerPlayer player, final AutoTeleportEntry entry) {
    ResourceKey<Level> dimensionKey = createDimensionKey(entry.targetDimension());
    if (!TeleportManager.startCountdownTeleport(
        player, dimensionKey, entry.countdownSeconds(), !entry.skipMovementDetection())) {
      log.warn(
          "Failed to start countdown teleport for player {} to dimension {}",
          player.getName().getString(),
          entry.targetDimension());
    }
  }

  private static ResourceKey<Level> createDimensionKey(final String dimensionName) {
    if (dimensionName.contains(":")) {
      return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(dimensionName));
    }
    return ResourceKey.create(
        Registries.DIMENSION,
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, dimensionName));
  }

  public static void addAutoTeleport(
      final MinecraftServer server,
      final AutoTeleportTrigger trigger,
      final String targetDimension,
      final Vec3 position) {
    if (!DimensionManager.dimensionExists(server, targetDimension)) {
      log.warn("Cannot add auto-teleport rule: Invalid dimension {}", targetDimension);
      return;
    }

    AutoTeleportEntry entry = new AutoTeleportEntry(targetDimension, position, trigger);
    globalRules.put(trigger, entry);
    AutoTeleportDataStorage.get().setAutoTeleportRule(entry);

    log.debug(
        "Added auto-teleport rule: {} -> {} at {:.1f}, {:.1f}, {:.1f}",
        trigger,
        targetDimension,
        position.x,
        position.y,
        position.z);
  }

  public static boolean removeAutoTeleport(final AutoTeleportTrigger trigger) {
    AutoTeleportEntry removed = globalRules.remove(trigger);
    if (removed != null) {
      AutoTeleportDataStorage.get().removeAutoTeleportRule(trigger);
      log.debug("Removed auto-teleport rule for trigger: {}", trigger);
      return true;
    }
    return false;
  }

  public static Map<AutoTeleportTrigger, String> getAutoTeleportRules() {
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

  public static void clearAllAutoTeleports() {
    globalRules.clear();
    AutoTeleportDataStorage.get().clearAllAutoTeleportRules();
    log.info("Cleared all auto-teleport rules");
  }

  public static void loadRules(final ServerLevel level) {
    if (level == null) {
      log.warn("Cannot load rules without server level context");
      return;
    }
    List<AutoTeleportEntry> savedRulesList = AutoTeleportDataStorage.get().getAutoTeleportRules();
    globalRules.clear();
    for (AutoTeleportEntry entry : savedRulesList) {
      globalRules.put(entry.trigger(), entry);
    }
    log.debug("Loaded {} auto-teleport rules from storage", savedRulesList.size());
  }

  private static void recordTriggerExecution(
      final ServerPlayer player, final AutoTeleportTrigger trigger) {
    UUID playerId = player.getUUID();

    switch (trigger) {
      case ONCE_AFTER_SERVER_RESTART ->
          serverRestartTeleports.put(playerId, System.currentTimeMillis());
      case ONCE_PER_SERVER_JOIN, ONCE_PER_DAY, ONCE_PER_WEEK, ONCE_PER_MONTH ->
          AutoTeleportDataStorage.get().recordTriggerExecution(playerId, trigger);
      case ALWAYS, ON_DEATH -> {
        // Intentionally empty: ALWAYS and ON_DEATH triggers do not require execution tracking
        // ALWAYS triggers can fire repeatedly, ON_DEATH triggers fire on every death event
      }
    }
  }

  public static void clearAllCache() {
    log.info("Clearing auto-teleport manager cache for world switch...");
    serverRestartTeleports.clear();
    globalRules.clear();
    pendingDeathTeleports.clear();
  }

  public static boolean setAutoTeleportPosition(
      final AutoTeleportTrigger trigger, final Vec3 position) {
    AutoTeleportEntry existingRule = globalRules.get(trigger);
    if (existingRule == null) {
      log.warn("No auto-teleport rule found for trigger: {}", trigger);
      return false;
    }

    AutoTeleportEntry updatedRule = existingRule.withPosition(position);
    globalRules.put(trigger, updatedRule);
    AutoTeleportDataStorage.get().setAutoTeleportRule(updatedRule);

    log.debug(
        "Updated position for auto-teleport rule: trigger '{}', new position: {}",
        trigger,
        position);
    return true;
  }

  public static boolean setAutoTeleportCountdown(
      final AutoTeleportTrigger trigger, final int countdownSeconds) {
    AutoTeleportEntry existingRule = globalRules.get(trigger);
    if (existingRule == null) {
      log.warn("No auto-teleport rule found for trigger: {}", trigger);
      return false;
    }

    AutoTeleportEntry updatedRule = existingRule.withCountdown(countdownSeconds);
    globalRules.put(trigger, updatedRule);
    AutoTeleportDataStorage.get().setAutoTeleportRule(updatedRule);

    log.debug(
        "Updated countdown for auto-teleport rule: trigger '{}', new countdown: {} seconds",
        trigger,
        countdownSeconds);
    return true;
  }

  public static boolean setAutoTeleportMovementDetection(
      final AutoTeleportTrigger trigger, final boolean enabled) {
    AutoTeleportEntry existingRule = globalRules.get(trigger);
    if (existingRule == null) {
      log.warn("No auto-teleport rule found for trigger: {}", trigger);
      return false;
    }

    AutoTeleportEntry updatedRule = existingRule.withMovementDetection(!enabled);
    globalRules.put(trigger, updatedRule);
    AutoTeleportDataStorage.get().setAutoTeleportRule(updatedRule);

    log.debug(
        "Updated movement detection for auto-teleport rule: trigger '{}', movement detection enabled: {}",
        trigger,
        enabled);
    return true;
  }
}
