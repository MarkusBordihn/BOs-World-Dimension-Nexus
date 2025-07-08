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

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionSuggestion;
import de.markusbordihn.worlddimensionnexus.teleport.TeleportCooldownManager;
import de.markusbordihn.worlddimensionnexus.teleport.TeleportHistory;
import de.markusbordihn.worlddimensionnexus.teleport.TeleportManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class TeleportCommand extends Command {

  private static final String PLAYER_ARGUMENT = "player";
  private static final String TELEPORTED_MESSAGE = "Teleported ";

  private TeleportCommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("teleport")
        .then(
            Commands.literal("dimension")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument("name", ResourceLocationArgument.id())
                        .suggests(DimensionSuggestion.DIMENSION_NAMES)
                        .executes(TeleportCommand::teleportToDimension)
                        .then(
                            Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                                .executes(TeleportCommand::teleportPlayerToDimension))))
        .then(
            Commands.literal("back")
                .executes(TeleportCommand::teleportBack)
                .then(
                    Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                        .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                        .executes(TeleportCommand::teleportPlayerBack)))
        .then(
            Commands.literal("overworld")
                .executes(TeleportCommand::teleportToOverworld)
                .then(
                    Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                        .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                        .executes(TeleportCommand::teleportPlayerToOverworld)))
        .then(
            Commands.literal("history")
                .executes(TeleportCommand::showTeleportHistory)
                .then(
                    Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                        .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                        .executes(TeleportCommand::showPlayerTeleportHistory)));
  }

  private static int teleportToDimension(final CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ResourceLocation dimensionLocation = ResourceLocationArgument.getId(context, "name");
    ServerPlayer player = context.getSource().getPlayerOrException();

    ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
    if (TeleportManager.safeTeleportToDimension(player, dimensionKey)) {
      return sendSuccessMessage(
          context.getSource(),
          Component.literal("Teleported to dimension ")
              .withStyle(ChatFormatting.GREEN)
              .append(
                  Component.literal(dimensionLocation.toString())
                      .withStyle(ChatFormatting.YELLOW)));
    }
    return sendFailureMessage(
        context.getSource(),
        Component.literal("Failed to teleport to dimension ")
            .withStyle(ChatFormatting.RED)
            .append(
                Component.literal(dimensionLocation.toString()).withStyle(ChatFormatting.YELLOW))
            .append(
                Component.literal(". Dimension may not exist or be loaded.")
                    .withStyle(ChatFormatting.GRAY)));
  }

  private static int teleportPlayerToDimension(final CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ResourceLocation dimensionLocation = ResourceLocationArgument.getId(context, "name");
    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, PLAYER_ARGUMENT);

    ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionLocation);
    if (TeleportManager.safeTeleportToDimension(targetPlayer, dimensionKey)) {
      return sendSuccessMessage(
          context.getSource(),
          Component.literal(TELEPORTED_MESSAGE)
              .withStyle(ChatFormatting.GREEN)
              .append(
                  Component.literal(targetPlayer.getName().getString())
                      .withStyle(ChatFormatting.YELLOW))
              .append(Component.literal(" to dimension ").withStyle(ChatFormatting.GREEN))
              .append(
                  Component.literal(dimensionLocation.toString()).withStyle(ChatFormatting.AQUA)));
    }
    return sendFailureMessage(
        context.getSource(),
        Component.literal("Failed to teleport ")
            .withStyle(ChatFormatting.RED)
            .append(
                Component.literal(targetPlayer.getName().getString())
                    .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" to dimension ").withStyle(ChatFormatting.RED))
            .append(
                Component.literal(dimensionLocation.toString()).withStyle(ChatFormatting.YELLOW)));
  }

  private static int teleportBack(final CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();

    // Check cooldown for non-moderator players
    if (!context.getSource().hasPermission(Commands.LEVEL_MODERATORS)
        && !TeleportCooldownManager.canTeleportBack(player)) {
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

    if (TeleportManager.teleportBack(player)) {
      // Record cooldown only for non-moderator players
      if (!context.getSource().hasPermission(Commands.LEVEL_MODERATORS)) {
        TeleportCooldownManager.recordBackTeleport(player);
      }

      return sendSuccessMessage(
          context.getSource(),
          Component.literal("Teleported back to previous location")
              .withStyle(ChatFormatting.GREEN));
    }

    return sendFailureMessage(
        context.getSource(),
        Component.literal("No previous location found to teleport back to")
            .withStyle(ChatFormatting.RED));
  }

  private static int teleportPlayerBack(final CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

    if (TeleportManager.teleportBack(targetPlayer)) {
      return sendSuccessMessage(
          context.getSource(),
          Component.literal(TELEPORTED_MESSAGE)
              .withStyle(ChatFormatting.GREEN)
              .append(
                  Component.literal(targetPlayer.getName().getString())
                      .withStyle(ChatFormatting.YELLOW))
              .append(
                  Component.literal(" back to previous location").withStyle(ChatFormatting.GREEN)));
    }

    return sendFailureMessage(
        context.getSource(),
        Component.literal("No previous location found for ")
            .withStyle(ChatFormatting.RED)
            .append(
                Component.literal(targetPlayer.getName().getString())
                    .withStyle(ChatFormatting.YELLOW)));
  }

  private static int teleportToOverworld(final CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();

    if (TeleportManager.teleportToDimensionWithoutHistory(player, Level.OVERWORLD)) {
      return sendSuccessMessage(
          context.getSource(),
          Component.literal("Teleported to the Overworld").withStyle(ChatFormatting.GREEN));
    }

    return sendFailureMessage(
        context.getSource(),
        Component.literal("Failed to teleport to the Overworld").withStyle(ChatFormatting.RED));
  }

  private static int teleportPlayerToOverworld(final CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");

    if (TeleportManager.teleportToDimensionWithoutHistory(targetPlayer, Level.OVERWORLD)) {
      return sendSuccessMessage(
          context.getSource(),
          Component.literal(TELEPORTED_MESSAGE)
              .withStyle(ChatFormatting.GREEN)
              .append(
                  Component.literal(targetPlayer.getName().getString())
                      .withStyle(ChatFormatting.YELLOW))
              .append(Component.literal(" to the Overworld").withStyle(ChatFormatting.AQUA)));
    }

    return sendFailureMessage(
        context.getSource(),
        Component.literal("Failed to teleport ")
            .withStyle(ChatFormatting.RED)
            .append(
                Component.literal(targetPlayer.getName().getString())
                    .withStyle(ChatFormatting.YELLOW))
            .append(Component.literal(" to the Overworld").withStyle(ChatFormatting.RED)));
  }

  private static int showTeleportHistory(final CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer player = context.getSource().getPlayerOrException();
    String historyText = TeleportHistory.getFormattedPlayerHistory(player.getUUID());

    Component historyComponent = Component.literal(historyText).withStyle(ChatFormatting.AQUA);
    return sendSuccessMessage(context.getSource(), historyComponent);
  }

  private static int showPlayerTeleportHistory(final CommandContext<CommandSourceStack> context)
      throws CommandSyntaxException {
    ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
    String historyText = TeleportHistory.getFormattedPlayerHistory(targetPlayer.getUUID());

    Component historyComponent =
        Component.literal(targetPlayer.getName().getString() + "'s ")
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal(historyText).withStyle(ChatFormatting.AQUA));

    return sendSuccessMessage(context.getSource(), historyComponent);
  }
}
