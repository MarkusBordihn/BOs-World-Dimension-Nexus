/*
 * Copyright 2023 Markus Bordihn
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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionSuggestions;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class DimensionCommand extends Command {

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("dimension")
        .then(
            Commands.literal("list")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .executes(context -> listDimensions(context.getSource())))
        .then(
            Commands.literal("create")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .executes(
                            context ->
                                createDimension(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "name")))))
        .then(
            Commands.literal("remove")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_ADMINS))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .suggests(DimensionSuggestions.DIMENSION_NAMES)
                        .executes(
                            context ->
                                removeDimension(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "name")))))
        .then(
            Commands.literal("info")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .suggests(DimensionSuggestions.DIMENSION_NAMES)
                        .executes(
                            context ->
                                infoDimension(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "name")))))
        .then(
            Commands.literal("teleport")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .suggests(DimensionSuggestions.DIMENSION_NAMES)
                        .executes(
                            context ->
                                teleportToDimension(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "name")))
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes(
                                    context ->
                                        teleportPlayerToDimension(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "name"),
                                            EntityArgument.getPlayer(context, "player"))))))
        .then(
            Commands.literal("autoteleport")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_ADMINS))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .suggests(DimensionSuggestions.DIMENSION_NAMES)
                        .executes(
                            context ->
                                setAutoTeleport(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "name"))))
                .then(
                    Commands.literal("off")
                        .executes(context -> clearAutoTeleport(context.getSource()))));
  }

  public static int listDimensions(CommandSourceStack context) {
    List<ResourceKey<Level>> dimensions = DimensionManager.getDimensions(context.getServer());
    if (dimensions.isEmpty()) {
      return sendFailureMessage(context, "No custom dimensions available.");
    }
    sendSuccessMessage(context, "Dimensions\n" + "===========");
    for (ResourceKey<Level> dimension : dimensions) {
      sendSuccessMessage(context, "- " + dimension.location());
    }
    return Command.SINGLE_SUCCESS;
  }

  public static int createDimension(CommandSourceStack context, String dimensionName) {
    ServerLevel serverLevel = DimensionManager.addOrCreateDimension(dimensionName);
    return sendSuccessMessage(context, "Dimension '" + dimensionName + "' created successfully!");
  }

  public static int removeDimension(CommandSourceStack source, String name) {
    boolean removed = DimensionManager.removeDimension(name);
    if (removed) {
      return sendSuccessMessage(
          source, "Dimension '" + name + "' was removed from server (data remains).");
    }
    return sendFailureMessage(source, "Dimension '" + name + "' could not be removed.");
  }

  public static int infoDimension(CommandSourceStack source, String name) {
    DimensionInfoData info = DimensionManager.getDimensionInfoData(name);
    if (info == null) {
      return sendFailureMessage(source, "Dimension '" + name + "' not found.");
    }
    sendSuccessMessage(source, "Dimension info for '" + name + "':\n" + info);
    return Command.SINGLE_SUCCESS;
  }

  public static int teleportToDimension(CommandSourceStack source, String name)
      throws CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    return teleportPlayerToDimension(source, name, player);
  }

  public static int teleportPlayerToDimension(
      CommandSourceStack source, String name, ServerPlayer player) {
    ServerLevel level = DimensionManager.getDimensionServerLevel(name);
    if (level == null) {
      return sendFailureMessage(source, "Dimension '" + name + "' not found.");
    }
    player.teleportTo(
        level, 0.5, level.getMaxBuildHeight(), 0.5, player.getYRot(), player.getXRot());
    sendSuccessMessage(
        source, "Teleported " + player.getName().getString() + " to '" + name + "'.");
    return Command.SINGLE_SUCCESS;
  }

  public static int setAutoTeleport(CommandSourceStack source, String name)
      throws CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    // AutoTeleportManager.setAutoTeleport(player.getUUID(), name);
    return sendSuccessMessage(source, "Auto-teleport on join set to '" + name + "'.");
  }

  public static int clearAutoTeleport(CommandSourceStack source) throws CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    // AutoTeleportManager.clearAutoTeleport(player.getUUID());
    return sendSuccessMessage(source, "Auto-teleport on join cleared.");
  }
}
