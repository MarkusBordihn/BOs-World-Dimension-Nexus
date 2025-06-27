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
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.dimension.io.DimensionExporter;
import de.markusbordihn.worlddimensionnexus.dimension.io.DimensionImporter;
import de.markusbordihn.worlddimensionnexus.resources.WorldDataPackResourceManager;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionImportFileSuggestion;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionSuggestion;
import java.io.File;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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
                        .suggests(DimensionSuggestion.DIMENSION_NAMES)
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
                        .suggests(DimensionSuggestion.DIMENSION_NAMES)
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
                        .suggests(DimensionSuggestion.DIMENSION_NAMES)
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
            Commands.literal("export")
                .requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
                .then(
                    Commands.argument("dimension", DimensionArgument.dimension())
                        .suggests(DimensionSuggestion.ALL_DIMENSIONS)
                        .executes(
                            context ->
                                exportDimension(
                                    context.getSource(),
                                    DimensionArgument.getDimension(context, "dimension")
                                        .dimension()))))
        .then(
            Commands.literal("import")
                .then(
                    Commands.argument("file", StringArgumentType.string())
                        .suggests(DimensionImportFileSuggestion::suggestImportFiles)
                        .executes(
                            context ->
                                importDimension(
                                    context.getSource(),
                                    null,
                                    null,
                                    StringArgumentType.getString(context, "file")))
                        .then(
                            Commands.argument("dimension", ResourceLocationArgument.id())
                                .executes(
                                    context ->
                                        importDimension(
                                            context.getSource(),
                                            ResourceLocationArgument.getId(context, "dimension"),
                                            null,
                                            StringArgumentType.getString(context, "file")))
                                .then(
                                    Commands.argument(
                                            "dimension_type", ResourceLocationArgument.id())
                                        .executes(
                                            context ->
                                                importDimension(
                                                    context.getSource(),
                                                    ResourceLocationArgument.getId(
                                                        context, "dimension"),
                                                    ResourceLocationArgument.getId(
                                                        context, "dimension_type"),
                                                    StringArgumentType.getString(
                                                        context, "file")))))))
        .then(
            Commands.literal("autoteleport")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_ADMINS))
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .suggests(DimensionSuggestion.DIMENSION_NAMES)
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

  private static int exportDimension(CommandSourceStack source, ResourceKey<Level> dimension) {
    MinecraftServer server = source.getServer();
    if (dimension == null) {
      return sendFailureMessage(source, "Dimension '" + dimension + "' could not be found.");
    }

    String fileName =
        dimension.location().getNamespace() + "_" + dimension.location().getPath() + ".wdn";
    File exportFile = new File(server.getServerDirectory().toFile(), fileName);

    if (DimensionExporter.exportDimension(server, dimension, exportFile)) {
      return sendSuccessMessage(source, "Export successful: " + exportFile.getAbsolutePath());
    } else {
      return sendFailureMessage(source, "Error exporting the dimension!");
    }
  }

  private static int importDimension(
      CommandSourceStack source,
      ResourceLocation dimensionId,
      ResourceLocation dimensionTypeId,
      String fileName) {
    MinecraftServer server = source.getServer();
    File importFile = WorldDataPackResourceManager.getDataPackFile(server, fileName);
    if (importFile == null || !importFile.exists() || !importFile.isFile()) {
      return sendFailureMessage(source, "Import file not found: " + fileName);
    }

    try {
      boolean success =
          DimensionImporter.importDimension(server, importFile, dimensionId, dimensionTypeId);
      if (success) {
        return sendSuccessMessage(source, "Imported dimension from " + fileName);
      } else {
        return sendFailureMessage(source, "Import failed for " + fileName);
      }
    } catch (Exception e) {
      return sendFailureMessage(source, "Error importing: " + e);
    }
  }

  public static int clearAutoTeleport(CommandSourceStack source) throws CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    // AutoTeleportManager.clearAutoTeleport(player.getUUID());
    return sendSuccessMessage(source, "Auto-teleport on join cleared.");
  }
}
