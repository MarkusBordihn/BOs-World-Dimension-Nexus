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
import de.markusbordihn.worlddimensionnexus.Constants;
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

public class AutoTeleportCommand extends Command {

  private AutoTeleportCommand() {}

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

                                      MinecraftServer server = context.getSource().getServer();
                                      if (server != null) {
                                        Collection<String> customDimensions =
                                            DimensionManager.getDimensionNames();
                                        for (String dimensionName : customDimensions) {
                                          if (dimensionName != null
                                              && !dimensionName.trim().isEmpty()
                                              && dimensionName.matches("[a-z0-9_.-]+")) {
                                            builder.suggest(Constants.MOD_ID + ":" + dimensionName);
                                          }
                                        }
                                      }
                                      return builder.buildFuture();
                                    })
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

  private static int addAutoTeleportWithSpawn(
      final CommandSourceStack source, final String triggerString, final String dimension) {
    AutoTeleportTrigger trigger = parseTrigger(triggerString);
    if (trigger == null) {
      return sendFailureMessage(source, "Invalid trigger: " + triggerString);
    }
    MinecraftServer server = source.getServer();

    // Determine spawn coordinates based on dimension type
    double spawnX = 0.0;
    double spawnY = 100.0;
    double spawnZ = 0.0;
    try {
      switch (dimension) {
        case "minecraft:overworld" -> {
          var overworld = server.overworld();
          var spawnPos = overworld.getSharedSpawnPos();
          spawnX = spawnPos.getX();
          spawnY = spawnPos.getY();
          spawnZ = spawnPos.getZ();
        }
        case "minecraft:nether" -> {
          spawnX = 0.0;
          spawnY = 64.0;
          spawnZ = 0.0;
        }
        case "minecraft:the_end" -> {
          spawnX = 100.0;
          spawnY = 50.0;
          spawnZ = 0.0;
        }
      }
    } catch (Exception e) {
      // Fall back to default coordinates if spawn detection fails
    }

    return addAutoTeleport(source, trigger, dimension, spawnX, spawnY, spawnZ, true);
  }

  private static int addAutoTeleportWithCoords(
      final CommandSourceStack source,
      final String triggerString,
      final String dimension,
      final double x,
      final double y,
      final double z) {
    AutoTeleportTrigger trigger = parseTrigger(triggerString);
    if (trigger == null) {
      return sendFailureMessage(source, "Invalid trigger: " + triggerString);
    }

    return addAutoTeleport(source, trigger, dimension, x, y, z, false);
  }

  private static int addAutoTeleport(
      final CommandSourceStack source,
      final AutoTeleportTrigger trigger,
      final String dimension,
      final double x,
      final double y,
      final double z,
      final boolean isSpawn) {
    MinecraftServer server = source.getServer();
    if (!DimensionManager.dimensionExists(server, dimension)) {
      return sendFailureMessage(source, "Dimension '" + dimension + "' does not exist!");
    }

    AutoTeleportManager.addGlobalAutoTeleport(server.overworld(), trigger, dimension, x, y, z);
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

    return sendSuccessMessage(source, message);
  }

  private static int removeAutoTeleport(
      final CommandSourceStack source, final String triggerString) {
    AutoTeleportTrigger trigger = parseTrigger(triggerString);
    if (trigger == null) {
      return sendFailureMessage(source, "Invalid trigger: " + triggerString);
    }
    MinecraftServer server = source.getServer();

    // Attempt to remove the auto-teleport rule
    boolean removed = AutoTeleportManager.removeGlobalAutoTeleport(server.overworld(), trigger);
    if (removed) {
      return sendSuccessMessage(source, "Removed auto-teleport rule for trigger: " + triggerString);
    } else {
      return sendFailureMessage(
          source, "No auto-teleport rule found for trigger: " + triggerString);
    }
  }

  private static int listAutoTeleports(final CommandSourceStack source) {
    Map<AutoTeleportTrigger, String> rules = AutoTeleportManager.getGlobalAutoTeleportRules();

    if (rules.isEmpty()) {
      return sendSuccessMessage(
          source, "No auto-teleport rules configured.", ChatFormatting.YELLOW);
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
    return SINGLE_SUCCESS;
  }

  private static int clearAutoTeleports(final CommandSourceStack source) {
    MinecraftServer server = source.getServer();
    AutoTeleportManager.clearAllGlobalAutoTeleports(server.overworld());
    return sendSuccessMessage(source, "Cleared all auto-teleport rules for all players.");
  }

  private static AutoTeleportTrigger parseTrigger(final String triggerString) {
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
