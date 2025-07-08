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

package de.markusbordihn.worlddimensionnexus.server.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportTrigger;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.AutoTeleportTriggerSuggestion;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionSuggestion;
import de.markusbordihn.worlddimensionnexus.teleport.AutoTeleportManager;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

public class AutoTeleportCommand extends Command {

  private AutoTeleportCommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("autoteleport")
        .requires(cs -> cs.hasPermission(2))
        .then(
            Commands.literal("add")
                .then(
                    Commands.argument("trigger", StringArgumentType.string())
                        .suggests(AutoTeleportTriggerSuggestion.AUTO_TELEPORT_TRIGGERS)
                        .then(
                            Commands.argument("dimension", StringArgumentType.greedyString())
                                .suggests(DimensionSuggestion.ALL_DIMENSIONS)
                                .executes(
                                    context ->
                                        addAutoTeleportWithSpawn(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "trigger"),
                                            StringArgumentType.getString(context, "dimension")))
                                .then(
                                    Commands.argument("position", BlockPosArgument.blockPos())
                                        .executes(
                                            context ->
                                                addAutoTeleportWithCoords(
                                                    context.getSource(),
                                                    StringArgumentType.getString(
                                                        context, "trigger"),
                                                    StringArgumentType.getString(
                                                        context, "dimension"),
                                                    BlockPosArgument.getBlockPos(
                                                        context, "position")))))))
        .then(
            Commands.literal("remove")
                .then(
                    Commands.argument("trigger", StringArgumentType.string())
                        .suggests(
                            (context, builder) ->
                                AutoTeleportTriggerSuggestion.suggestExistingTriggers(builder))
                        .executes(
                            context ->
                                removeAutoTeleport(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "trigger")))))
        .then(
            Commands.literal("set")
                .then(
                    Commands.literal("position")
                        .then(
                            Commands.argument("trigger", StringArgumentType.string())
                                .suggests(
                                    (context, builder) ->
                                        AutoTeleportTriggerSuggestion.suggestExistingTriggers(
                                            builder))
                                .then(
                                    Commands.argument("position", BlockPosArgument.blockPos())
                                        .executes(
                                            context ->
                                                setAutoTeleportPosition(
                                                    context.getSource(),
                                                    StringArgumentType.getString(
                                                        context, "trigger"),
                                                    BlockPosArgument.getBlockPos(
                                                        context, "position"))))))
                .then(
                    Commands.literal("countdown")
                        .then(
                            Commands.argument("trigger", StringArgumentType.string())
                                .suggests(
                                    (context, builder) ->
                                        AutoTeleportTriggerSuggestion.suggestExistingTriggers(
                                            builder))
                                .then(
                                    Commands.argument("seconds", IntegerArgumentType.integer(0, 60))
                                        .executes(
                                            context ->
                                                setAutoTeleportCountdown(
                                                    context.getSource(),
                                                    StringArgumentType.getString(
                                                        context, "trigger"),
                                                    IntegerArgumentType.getInteger(
                                                        context, "seconds"))))))
                .then(
                    Commands.literal("movement_detection")
                        .then(
                            Commands.argument("trigger", StringArgumentType.string())
                                .suggests(
                                    (context, builder) ->
                                        AutoTeleportTriggerSuggestion.suggestExistingTriggers(
                                            builder))
                                .then(
                                    Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(
                                            context ->
                                                setAutoTeleportMovementDetection(
                                                    context.getSource(),
                                                    StringArgumentType.getString(
                                                        context, "trigger"),
                                                    BoolArgumentType.getBool(
                                                        context, "enabled")))))))
        .then(Commands.literal("list").executes(context -> listAutoTeleports(context.getSource())))
        .then(
            Commands.literal("clear").executes(context -> clearAutoTeleports(context.getSource())));
  }

  private static int addAutoTeleportWithSpawn(
      final CommandSourceStack source, final String triggerString, final String dimension) {
    AutoTeleportTrigger trigger =
        AutoTeleportTriggerSuggestion.parseTriggerFromString(triggerString);
    if (trigger == null) {
      return sendFailureMessage(source, "Invalid trigger: " + triggerString);
    }
    MinecraftServer server = source.getServer();

    // Determine spawn coordinates based on dimension type
    Vec3 spawnPosition = new Vec3(0.0, 100.0, 0.0); // Default position
    try {
      switch (dimension) {
        case "minecraft:overworld" -> {
          var overworld = server.overworld();
          var spawnPos = overworld.getSharedSpawnPos();
          spawnPosition = new Vec3(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        }
        case "minecraft:nether" -> spawnPosition = new Vec3(0.0, 64.0, 0.0);
        case "minecraft:the_end" -> spawnPosition = new Vec3(100.0, 50.0, 0.0);
      }
    } catch (Exception e) {
      // Fall back to default coordinates if spawn detection fails
    }

    return addAutoTeleport(source, trigger, dimension, spawnPosition, true);
  }

  private static int addAutoTeleportWithCoords(
      final CommandSourceStack source,
      final String triggerString,
      final String dimension,
      final BlockPos position) {
    AutoTeleportTrigger trigger =
        AutoTeleportTriggerSuggestion.parseTriggerFromString(triggerString);
    if (trigger == null) {
      return sendFailureMessage(source, "Invalid trigger: " + triggerString);
    }

    Vec3 targetPosition = new Vec3(position.getX(), position.getY(), position.getZ());
    return addAutoTeleport(source, trigger, dimension, targetPosition, false);
  }

  private static int addAutoTeleport(
      final CommandSourceStack source,
      final AutoTeleportTrigger trigger,
      final String dimension,
      final Vec3 position,
      final boolean isSpawn) {
    MinecraftServer server = source.getServer();
    if (!DimensionManager.dimensionExists(server, dimension)) {
      return sendFailureMessage(source, "Dimension '" + dimension + "' does not exist!");
    }

    AutoTeleportManager.addAutoTeleport(server, trigger, dimension, position);
    MutableComponent message =
        Component.literal("Added auto-teleport rule for all players:")
            .withStyle(ChatFormatting.GREEN)
            .append(Component.literal("\nTrigger: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(trigger.toString()).withStyle(ChatFormatting.WHITE))
            .append(Component.literal("\nDestination: ").withStyle(ChatFormatting.GRAY))
            .append(Component.literal(dimension).withStyle(ChatFormatting.WHITE));

    if (isSpawn) {
      message.append(Component.literal(" (spawn point)").withStyle(ChatFormatting.YELLOW));
    } else {
      message.append(
          Component.literal(
                  String.format(" at %.1f, %.1f, %.1f", position.x, position.y, position.z))
              .withStyle(ChatFormatting.AQUA));
    }

    // Add information about trigger-specific settings
    switch (trigger) {
      case ON_DEATH ->
          message.append(
              Component.literal("\nSettings: No countdown, movement detection disabled")
                  .withStyle(ChatFormatting.GRAY));
      case ALWAYS ->
          message.append(
              Component.literal("\nSettings: 3 second countdown, movement detection enabled")
                  .withStyle(ChatFormatting.GRAY));
      default ->
          message.append(
              Component.literal("\nSettings: 5 second countdown, movement detection enabled")
                  .withStyle(ChatFormatting.GRAY));
    }

    return sendSuccessMessage(source, message);
  }

  private static int removeAutoTeleport(
      final CommandSourceStack source, final String triggerString) {
    AutoTeleportTrigger trigger =
        AutoTeleportTriggerSuggestion.parseTriggerFromString(triggerString);
    if (trigger == null) {
      return sendFailureMessage(source, "Invalid trigger: " + triggerString);
    }

    if (AutoTeleportManager.removeAutoTeleport(trigger)) {
      return sendSuccessMessage(source, "Removed auto-teleport rule for trigger: " + triggerString);
    }
    return sendFailureMessage(source, "No auto-teleport rule found for trigger: " + triggerString);
  }

  private static int setAutoTeleportPosition(
      final CommandSourceStack source, final String triggerString, final BlockPos position) {
    AutoTeleportTrigger trigger =
        AutoTeleportTriggerSuggestion.parseTriggerFromString(triggerString);
    if (trigger == null) {
      return sendFailureMessage(source, "Invalid trigger: " + triggerString);
    }

    Vec3 targetPosition = new Vec3(position.getX(), position.getY(), position.getZ());
    if (AutoTeleportManager.setAutoTeleportPosition(trigger, targetPosition)) {
      return sendSuccessMessage(
          source, "Updated position for auto-teleport rule: " + triggerString);
    }
    return sendFailureMessage(
        source, "Failed to update position for auto-teleport rule: " + triggerString);
  }

  private static int setAutoTeleportCountdown(
      final CommandSourceStack source, final String triggerString, final int seconds) {
    AutoTeleportTrigger trigger =
        AutoTeleportTriggerSuggestion.parseTriggerFromString(triggerString);
    if (trigger == null) {
      return sendFailureMessage(source, "Invalid trigger: " + triggerString);
    }

    if (AutoTeleportManager.setAutoTeleportCountdown(trigger, seconds)) {
      return sendSuccessMessage(
          source, "Updated countdown for auto-teleport rule: " + triggerString);
    }
    return sendFailureMessage(
        source, "Failed to update countdown for auto-teleport rule: " + triggerString);
  }

  private static int setAutoTeleportMovementDetection(
      final CommandSourceStack source, final String triggerString, final boolean enabled) {
    AutoTeleportTrigger trigger =
        AutoTeleportTriggerSuggestion.parseTriggerFromString(triggerString);
    if (trigger == null) {
      return sendFailureMessage(source, "Invalid trigger: " + triggerString);
    }

    if (AutoTeleportManager.setAutoTeleportMovementDetection(trigger, enabled)) {
      return sendSuccessMessage(
          source, "Updated movement detection for auto-teleport rule: " + triggerString);
    }
    return sendFailureMessage(
        source, "Failed to update movement detection for auto-teleport rule: " + triggerString);
  }

  private static int listAutoTeleports(final CommandSourceStack source) {
    Map<AutoTeleportTrigger, String> rules = AutoTeleportManager.getAutoTeleportRules();

    if (rules.isEmpty()) {
      return sendSuccessMessage(
          source, "No auto-teleport rules configured.", ChatFormatting.YELLOW);
    }

    MutableComponent message = createAutoTeleportListHeader();
    appendAutoTeleportRulesToMessage(message, rules);

    source.sendSuccess(() -> message, false);
    return SINGLE_SUCCESS;
  }

  private static MutableComponent createAutoTeleportListHeader() {
    return Component.literal("Global Auto-teleport Rules:").withStyle(ChatFormatting.GREEN);
  }

  private static void appendAutoTeleportRulesToMessage(
      MutableComponent message, Map<AutoTeleportTrigger, String> rules) {
    for (Map.Entry<AutoTeleportTrigger, String> entry : rules.entrySet()) {
      AutoTeleportTrigger trigger = entry.getKey();
      String destination = entry.getValue();

      message
          .append(Component.literal("\n• ").withStyle(ChatFormatting.GRAY))
          .append(Component.literal(trigger.getSerializedName()).withStyle(ChatFormatting.WHITE))
          .append(Component.literal(" → ").withStyle(ChatFormatting.GRAY))
          .append(Component.literal(destination).withStyle(ChatFormatting.AQUA));
    }
  }

  private static int clearAutoTeleports(final CommandSourceStack source) {
    AutoTeleportManager.clearAllAutoTeleports();
    return sendSuccessMessage(source, "Cleared all auto-teleport rules for all players.");
  }
}
