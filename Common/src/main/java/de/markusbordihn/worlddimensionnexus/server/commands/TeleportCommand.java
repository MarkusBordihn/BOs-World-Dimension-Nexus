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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionSuggestion;
import de.markusbordihn.worlddimensionnexus.teleport.TeleportCooldownManager;
import de.markusbordihn.worlddimensionnexus.utils.TeleportHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TeleportCommand extends Command {

  private TeleportCommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("teleport")
        .then(
            Commands.literal("dimension")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .suggests(DimensionSuggestion.DIMENSION_NAMES)
                        .executes(TeleportCommand::teleportToDimension)
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes(TeleportCommand::teleportPlayerToDimension))))
        .then(
            Commands.literal("back")
                .executes(TeleportCommand::teleportBack)
                .then(
                    Commands.argument("player", EntityArgument.player())
                        .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                        .executes(TeleportCommand::teleportPlayerBack)));
  }

  private static int teleportToDimension(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    String dimensionName = StringArgumentType.getString(context, "name");
    ServerPlayer player = context.getSource().getPlayerOrException();

    boolean success = TeleportHelper.safeTeleportToDimension(player, dimensionName);

    if (success) {
      return sendSuccessMessage(
          context.getSource(),
          Component.literal("Teleported to dimension ")
              .withStyle(ChatFormatting.GREEN)
              .append(Component.literal(dimensionName).withStyle(ChatFormatting.YELLOW)));
    } else {
      return sendFailureMessage(
          context.getSource(),
          Component.literal("Failed to teleport to dimension ")
              .withStyle(ChatFormatting.RED)
              .append(Component.literal(dimensionName).withStyle(ChatFormatting.YELLOW))
              .append(
                  Component.literal(". Dimension may not exist or be loaded.")
                      .withStyle(ChatFormatting.GRAY)));
    }
  }

  private static int teleportPlayerToDimension(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    String dimensionName = StringArgumentType.getString(context, "name");
    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

    boolean success = TeleportHelper.safeTeleportToDimension(targetPlayer, dimensionName);

    if (success) {
      return sendSuccessMessage(
          context.getSource(),
          Component.literal("Teleported ")
              .withStyle(ChatFormatting.GREEN)
              .append(
                  Component.literal(targetPlayer.getName().getString())
                      .withStyle(ChatFormatting.YELLOW))
              .append(Component.literal(" to dimension ").withStyle(ChatFormatting.GREEN))
              .append(Component.literal(dimensionName).withStyle(ChatFormatting.AQUA)));
    } else {
      return sendFailureMessage(
          context.getSource(),
          Component.literal("Failed to teleport ")
              .withStyle(ChatFormatting.RED)
              .append(
                  Component.literal(targetPlayer.getName().getString())
                      .withStyle(ChatFormatting.YELLOW))
              .append(Component.literal(" to dimension ").withStyle(ChatFormatting.RED))
              .append(Component.literal(dimensionName).withStyle(ChatFormatting.YELLOW)));
    }
  }

  private static int teleportBack(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();

    // Check cooldown for non-moderator players
    if (!context.getSource().hasPermission(Commands.LEVEL_MODERATORS)) {
      if (!TeleportCooldownManager.canTeleportBack(player)) {
        int remainingSeconds = TeleportCooldownManager.getRemainingCooldown(player);
        return sendFailureMessage(
            context.getSource(),
            Component.literal("You must wait ")
                .withStyle(ChatFormatting.RED)
                .append(
                    Component.literal(String.valueOf(remainingSeconds))
                        .withStyle(ChatFormatting.YELLOW))
                .append(
                    Component.literal(" more seconds before using teleport back again")
                        .withStyle(ChatFormatting.RED)));
      }
    }

    boolean success = TeleportHelper.teleportBack(player);

    if (success) {
      // Record cooldown only for non-moderator players
      if (!context.getSource().hasPermission(Commands.LEVEL_MODERATORS)) {
        TeleportCooldownManager.recordBackTeleport(player);
      }

      return sendSuccessMessage(
          context.getSource(),
          Component.literal("Teleported back to previous location")
              .withStyle(ChatFormatting.GREEN));
    } else {
      return sendFailureMessage(
          context.getSource(),
          Component.literal("No previous location found to teleport back to")
              .withStyle(ChatFormatting.RED));
    }
  }

  private static int teleportPlayerBack(CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

    boolean success = TeleportHelper.teleportBack(targetPlayer);

    if (success) {
      return sendSuccessMessage(
          context.getSource(),
          Component.literal("Teleported ")
              .withStyle(ChatFormatting.GREEN)
              .append(
                  Component.literal(targetPlayer.getName().getString())
                      .withStyle(ChatFormatting.YELLOW))
              .append(
                  Component.literal(" back to previous location").withStyle(ChatFormatting.GREEN)));
    } else {
      return sendFailureMessage(
          context.getSource(),
          Component.literal("No previous location found for ")
              .withStyle(ChatFormatting.RED)
              .append(
                  Component.literal(targetPlayer.getName().getString())
                      .withStyle(ChatFormatting.YELLOW)));
    }
  }
}
