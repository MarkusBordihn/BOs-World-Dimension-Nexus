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

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportTrigger;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.teleport.AutoTeleportManager;
import java.util.Collection;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;

/** Commands for managing auto-teleport rules that apply to all players. */
public class AutoTeleportCommand extends Command {

  private AutoTeleportCommand() {}

  /**
   * Registers the auto-teleport command with all its subcommands.
   *
   * @return the command builder for the auto-teleport command
   */
  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("autoteleport")
        .requires(cs -> cs.hasPermission(2))
        .then(
            Commands.literal("add")
                .then(
                    Commands.argument("trigger", StringArgumentType.string())
                        .suggests(
                            (context, builder) -> {
                              builder.suggest("always");
                              builder.suggest("daily");
                              builder.suggest("weekly");
                              builder.suggest("monthly");
                              builder.suggest("join");
                              builder.suggest("restart");
                              return builder.buildFuture();
                            })
                        .then(
                            Commands.argument("dimension", StringArgumentType.greedyString())
                                .suggests(
                                    (context, builder) -> {
                                      // Add standard Minecraft dimensions
                                      builder.suggest("minecraft:overworld");
                                      builder.suggest("minecraft:nether");
                                      builder.suggest("minecraft:the_end");

                                      // Add custom dimensions with proper validation
                                      MinecraftServer server = context.getSource().getServer();
                                      if (server != null) {
                                        Collection<String> customDimensions =
                                            DimensionManager.getDimensionNames(server);
                                        for (String dimensionName : customDimensions) {
                                          // Validate dimension name for command usage
                                          if (dimensionName != null
                                              && !dimensionName.trim().isEmpty()
                                              && dimensionName.matches("[a-z0-9_.-]+")) {
                                            builder.suggest("worlddimensionnexus:" + dimensionName);
                                          }
                                        }
                                      }
                                      return builder.buildFuture();
                                    })
                                // Execute with dimension only (use spawn point)
                                .executes(
                                    context ->
                                        addAutoTeleportWithSpawn(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "trigger"),
                                            StringArgumentType.getString(context, "dimension")))
                                .then(
                                    Commands.argument("x", DoubleArgumentType.doubleArg())
                                        .then(
                                            Commands.argument("y", DoubleArgumentType.doubleArg())
                                                .then(
                                                    Commands.argument(
                                                            "z", DoubleArgumentType.doubleArg())
                                                        // Execute with coordinates
                                                        .executes(
                                                            context ->
                                                                addAutoTeleportWithCoords(
                                                                    context.getSource(),
                                                                    StringArgumentType.getString(
                                                                        context, "trigger"),
                                                                    StringArgumentType.getString(
                                                                        context, "dimension"),
                                                                    DoubleArgumentType.getDouble(
                                                                        context, "x"),
                                                                    DoubleArgumentType.getDouble(
                                                                        context, "y"),
                                                                    DoubleArgumentType.getDouble(
                                                                        context, "z")))))))))
        .then(
            Commands.literal("remove")
                .then(
                    Commands.argument("trigger", StringArgumentType.string())
                        .suggests(
                            (context, builder) -> {
                              builder.suggest("always");
                              builder.suggest("daily");
                              builder.suggest("weekly");
                              builder.suggest("monthly");
                              builder.suggest("join");
                              builder.suggest("restart");
                              return builder.buildFuture();
                            })
                        .executes(
                            context ->
                                removeAutoTeleport(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "trigger")))))
        .then(Commands.literal("list").executes(context -> listAutoTeleports(context.getSource())))
        .then(
            Commands.literal("clear").executes(context -> clearAutoTeleports(context.getSource())));
  }

  /**
   * Adds an auto-teleport rule using the dimension's spawn point. When no coordinates are
   * specified, uses appropriate spawn coordinates based on dimension type.
   *
   * @param source the command source
   * @param triggerString the trigger string
   * @param dimension the target dimension
   * @return command result (1 for success, 0 for failure)
   */
  private static int addAutoTeleportWithSpawn(
      CommandSourceStack source, String triggerString, String dimension) {
    AutoTeleportTrigger trigger = parseTrigger(triggerString);
    if (trigger == null) {
      source.sendFailure(
          Component.literal("Invalid trigger: " + triggerString).withStyle(ChatFormatting.RED));
      return 0;
    }

    MinecraftServer server = source.getServer();
    if (server == null) {
      source.sendFailure(Component.literal("Server not available").withStyle(ChatFormatting.RED));
      return 0;
    }

    // Determine spawn coordinates based on dimension type
    double spawnX = 0.0;
    double spawnY = 100.0;
    double spawnZ = 0.0;

    try {
      if (dimension.equals("minecraft:overworld")) {
        var overworld = server.overworld();
        var spawnPos = overworld.getSharedSpawnPos();
        spawnX = spawnPos.getX();
        spawnY = spawnPos.getY();
        spawnZ = spawnPos.getZ();
      } else if (dimension.equals("minecraft:nether")) {
        spawnX = 0.0;
        spawnY = 64.0;
        spawnZ = 0.0;
      } else if (dimension.equals("minecraft:the_end")) {
        spawnX = 100.0;
        spawnY = 50.0;
        spawnZ = 0.0;
      }
      // For custom dimensions, use default safe coordinates (0, 100, 0)
    } catch (Exception e) {
      // Fall back to default coordinates if spawn detection fails
    }

    return addAutoTeleport(source, trigger, dimension, spawnX, spawnY, spawnZ, true);
  }

  /**
   * Adds an auto-teleport rule with user-specified coordinates.
   *
   * @param source the command source
   * @param triggerString the trigger string
   * @param dimension the target dimension
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   * @return command result (1 for success, 0 for failure)
   */
  private static int addAutoTeleportWithCoords(
      CommandSourceStack source,
      String triggerString,
      String dimension,
      double x,
      double y,
      double z) {
    AutoTeleportTrigger trigger = parseTrigger(triggerString);
    if (trigger == null) {
      source.sendFailure(
          Component.literal("Invalid trigger: " + triggerString).withStyle(ChatFormatting.RED));
      return 0;
    }

    return addAutoTeleport(source, trigger, dimension, x, y, z, false);
  }

  /**
   * Internal method to add an auto-teleport rule with specified parameters. Handles rule creation
   * and provides feedback to the command sender.
   *
   * @param source the command source
   * @param trigger the teleport trigger type
   * @param dimension the target dimension identifier
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   * @param isSpawn whether the coordinates represent a spawn point
   * @return command result (1 for success, 0 for failure)
   */
  private static int addAutoTeleport(
      CommandSourceStack source,
      AutoTeleportTrigger trigger,
      String dimension,
      double x,
      double y,
      double z,
      boolean isSpawn) {

    MinecraftServer server = source.getServer();
    if (server == null) {
      source.sendFailure(Component.literal("Server not available").withStyle(ChatFormatting.RED));
      return 0;
    }

    AutoTeleportManager.addGlobalAutoTeleport(server.overworld(), trigger, dimension, x, y, z);

    // Build success message with appropriate formatting
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
          Component.literal(String.format(" at %.1f, %.1f, %.1f", x, y, z))
              .withStyle(ChatFormatting.AQUA));
    }

    source.sendSuccess(() -> message, true);
    return 1;
  }

  /**
   * Removes an existing auto-teleport rule for the specified trigger.
   *
   * @param source the command source
   * @param triggerString the trigger string
   * @return command result (1 for success, 0 for failure)
   */
  private static int removeAutoTeleport(CommandSourceStack source, String triggerString) {
    AutoTeleportTrigger trigger = parseTrigger(triggerString);
    if (trigger == null) {
      source.sendFailure(
          Component.literal("Invalid trigger: " + triggerString).withStyle(ChatFormatting.RED));
      return 0;
    }

    MinecraftServer server = source.getServer();
    if (server == null) {
      source.sendFailure(Component.literal("Server not available").withStyle(ChatFormatting.RED));
      return 0;
    }

    boolean removed = AutoTeleportManager.removeGlobalAutoTeleport(server.overworld(), trigger);

    if (removed) {
      source.sendSuccess(
          () ->
              Component.literal("Removed auto-teleport rule for trigger: " + triggerString)
                  .withStyle(ChatFormatting.GREEN),
          true);
      return 1;
    } else {
      source.sendFailure(
          Component.literal("No auto-teleport rule found for trigger: " + triggerString)
              .withStyle(ChatFormatting.RED));
      return 0;
    }
  }

  /**
   * Lists all currently configured auto-teleport rules. Shows appropriate message if no rules are
   * configured.
   *
   * @param source the command source
   * @return command result (always 1)
   */
  private static int listAutoTeleports(CommandSourceStack source) {
    Map<AutoTeleportTrigger, String> rules = AutoTeleportManager.getGlobalAutoTeleportRules();

    if (rules.isEmpty()) {
      source.sendSuccess(
          () ->
              Component.literal("No auto-teleport rules configured.")
                  .withStyle(ChatFormatting.YELLOW),
          false);
      return 1;
    }

    MutableComponent message =
        Component.literal("Global Auto-teleport Rules:").withStyle(ChatFormatting.GREEN);

    for (Map.Entry<AutoTeleportTrigger, String> entry : rules.entrySet()) {
      message
          .append(Component.literal("\n• ").withStyle(ChatFormatting.GRAY))
          .append(Component.literal(entry.getKey().toString()).withStyle(ChatFormatting.WHITE))
          .append(Component.literal(" → ").withStyle(ChatFormatting.GRAY))
          .append(Component.literal(entry.getValue()).withStyle(ChatFormatting.AQUA));
    }

    source.sendSuccess(() -> message, false);
    return 1;
  }

  /**
   * Clears all configured auto-teleport rules. Removes all global auto-teleport rules, effectively
   * disabling automatic teleportation.
   *
   * @param source the command source
   * @return command result (1 for success, 0 for failure)
   */
  private static int clearAutoTeleports(CommandSourceStack source) {
    MinecraftServer server = source.getServer();
    if (server == null) {
      source.sendFailure(Component.literal("Server not available").withStyle(ChatFormatting.RED));
      return 0;
    }

    AutoTeleportManager.clearAllGlobalAutoTeleports(server.overworld());
    source.sendSuccess(
        () ->
            Component.literal("Cleared all auto-teleport rules for all players.")
                .withStyle(ChatFormatting.GREEN),
        true);
    return 1;
  }

  /**
   * Parses a trigger string into an AutoTeleportTrigger enum value. Supports case-insensitive
   * matching for better user experience.
   *
   * @param triggerString the trigger string to parse
   * @return the corresponding AutoTeleportTrigger, or null if the string is invalid
   */
  private static AutoTeleportTrigger parseTrigger(String triggerString) {
    return switch (triggerString.toLowerCase()) {
      case "always" -> AutoTeleportTrigger.ALWAYS;
      case "daily" -> AutoTeleportTrigger.ONCE_PER_DAY;
      case "weekly" -> AutoTeleportTrigger.ONCE_PER_WEEK;
      case "monthly" -> AutoTeleportTrigger.ONCE_PER_MONTH;
      case "join" -> AutoTeleportTrigger.ONCE_PER_SERVER_JOIN;
      case "restart" -> AutoTeleportTrigger.ONCE_AFTER_SERVER_RESTART;
      default -> null;
    };
  }
}
