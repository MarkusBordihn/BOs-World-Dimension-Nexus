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
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.data.worldgen.WorldgenConfigLoader;
import de.markusbordihn.worlddimensionnexus.data.worldgen.WorldgenInitializer;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.dimension.io.DimensionExporter;
import de.markusbordihn.worlddimensionnexus.dimension.io.DimensionImporter;
import de.markusbordihn.worlddimensionnexus.resources.WorldDataPackResourceManager;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionImportFileSuggestion;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionSuggestion;
import de.markusbordihn.worlddimensionnexus.teleport.TeleportManager;
import java.io.File;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
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
                                    StringArgumentType.getString(context, "name"),
                                    ChunkGeneratorType.VOID))
                        .then(
                            Commands.argument("type", StringArgumentType.word())
                                .suggests(
                                    (context, builder) -> {
                                      for (ChunkGeneratorType type : ChunkGeneratorType.values()) {
                                        builder.suggest(type.getName());
                                      }
                                      return builder.buildFuture();
                                    })
                                .executes(
                                    context ->
                                        createTypedDimension(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "name"),
                                            StringArgumentType.getString(context, "type"))))))
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
            Commands.literal("set")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("spawnpoint")
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .suggests(DimensionSuggestion.DIMENSION_NAMES)
                                .then(
                                    Commands.argument("position", BlockPosArgument.blockPos())
                                        .executes(
                                            context ->
                                                setDimensionSpawnPoint(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "name"),
                                                    BlockPosArgument.getBlockPos(
                                                        context, "position")))))))
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
                                importDimensionWithInfoData(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "file"),
                                    null,
                                    null))
                        .then(
                            Commands.argument("name", StringArgumentType.word())
                                .executes(
                                    context ->
                                        importDimensionWithInfoData(
                                            context.getSource(),
                                            StringArgumentType.getString(context, "file"),
                                            StringArgumentType.getString(context, "name"),
                                            null))
                                .then(
                                    Commands.argument("type", StringArgumentType.string())
                                        .suggests(
                                            (context, builder) -> {
                                              for (ChunkGeneratorType type :
                                                  ChunkGeneratorType.values()) {
                                                builder.suggest(type.getName());
                                              }
                                              return builder.buildFuture();
                                            })
                                        .executes(
                                            context ->
                                                importDimensionWithInfoData(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "file"),
                                                    StringArgumentType.getString(context, "name"),
                                                    StringArgumentType.getString(
                                                        context, "type")))))))
        .then(
            Commands.literal("types")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .executes(context -> listChunkGeneratorTypes(context.getSource())))
        .then(
            Commands.literal("worldgen")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_ADMINS))
                .then(
                    Commands.literal("reload")
                        .executes(context -> reloadWorldgenConfigs(context.getSource())))
                .then(
                    Commands.literal("list")
                        .executes(context -> listWorldgenConfigs(context.getSource()))));
  }

  public static int listDimensions(final CommandSourceStack context) {
    List<ResourceKey<Level>> dimensions = DimensionManager.getDimensions(context.getServer());
    if (dimensions.isEmpty()) {
      return sendFailureMessage(context, "No custom dimensions available.");
    }
    sendSuccessMessage(context, "Dimensions\n===========");
    for (ResourceKey<Level> dimension : dimensions) {
      sendSuccessMessage(context, "- " + dimension.location());
    }
    return Command.SINGLE_SUCCESS;
  }

  public static int createDimension(final CommandSourceStack context, final String dimensionName) {
    return createDimension(context, dimensionName, ChunkGeneratorType.VOID);
  }

  public static int createDimension(
      final CommandSourceStack context, final String dimensionName, final ChunkGeneratorType type) {
    DimensionInfoData dimensionInfo =
        DimensionInfoData.fromDimensionNameAndType(dimensionName, type);
    ServerLevel serverLevel = DimensionManager.addOrCreateDimension(dimensionInfo, true);
    if (serverLevel != null) {
      return sendSuccessMessage(context, "Dimension '" + dimensionName + "' created successfully!");
    }
    return sendFailureMessage(context, "Failed to create dimension '" + dimensionName + "'!");
  }

  public static int removeDimension(final CommandSourceStack source, final String name) {
    boolean removed = DimensionManager.removeDimension(name);
    if (removed) {
      return sendSuccessMessage(
          source, "Dimension '" + name + "' was removed from server (data remains).");
    }
    return sendFailureMessage(source, "Dimension '" + name + "' could not be removed.");
  }

  public static int infoDimension(final CommandSourceStack source, final String name) {
    DimensionInfoData info = DimensionManager.getDimensionInfoData(name);
    if (info == null) {
      return sendFailureMessage(source, "Dimension '" + name + "' not found.");
    }
    sendSuccessMessage(source, "Dimension info for '" + name + "':\n" + info);
    return Command.SINGLE_SUCCESS;
  }

  public static int teleportToDimension(final CommandSourceStack source, final String name)
      throws CommandSyntaxException {
    ServerPlayer player = source.getPlayerOrException();
    return teleportPlayerToDimension(source, name, player);
  }

  public static int teleportPlayerToDimension(
      final CommandSourceStack source, final String name, final ServerPlayer player) {
    if (!TeleportManager.safeTeleportToDimension(player, name)) {
      return sendFailureMessage(source, "Dimension '" + name + "' not found or teleport failed.");
    }
    sendSuccessMessage(
        source, "Teleported " + player.getName().getString() + " to '" + name + "'.");
    return Command.SINGLE_SUCCESS;
  }

  private static int exportDimension(
      final CommandSourceStack source, final ResourceKey<Level> dimension) {
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

  private static int importDimensionWithInfoData(
      final CommandSourceStack source,
      final String fileName,
      final String dimensionName,
      final String typeName) {
    MinecraftServer server = source.getServer();

    File importFile = WorldDataPackResourceManager.getDataPackFile(server, fileName);
    if (importFile == null || !importFile.exists() || !importFile.isFile()) {
      return sendFailureMessage(source, "Import file not found: " + fileName);
    }

    try {
      ChunkGeneratorType chunkGeneratorType = null;
      if (typeName != null) {
        chunkGeneratorType = ChunkGeneratorType.fromString(typeName);
      }

      boolean success =
          DimensionImporter.importDimension(server, importFile, dimensionName, chunkGeneratorType);

      if (success) {
        String finalDimensionName =
            dimensionName != null ? dimensionName : fileName.replaceAll("\\.wdn$", "");
        return sendSuccessMessage(
            source,
            "Successfully imported and registered dimension: "
                + Constants.MOD_ID
                + ":"
                + finalDimensionName);
      } else {
        return sendFailureMessage(source, "Failed to import dimension from " + fileName);
      }
    } catch (Exception e) {
      return sendFailureMessage(source, "Error during import: " + e.getMessage());
    }
  }

  public static int createTypedDimension(
      final CommandSourceStack context, final String dimensionName, final String typeName) {
    ChunkGeneratorType type = ChunkGeneratorType.fromString(typeName);
    DimensionInfoData dimensionInfo =
        DimensionInfoData.fromDimensionNameAndType(dimensionName, type);
    ServerLevel serverLevel = DimensionManager.addOrCreateDimension(dimensionInfo, true);

    if (serverLevel != null) {
      return sendSuccessMessage(
          context,
          String.format(
              "Dimension '%s' created successfully with type '%s'!",
              dimensionName, type.getName()));
    }

    return sendFailureMessage(
        context,
        String.format("Failed to create dimension '%s' with type '%s'!", dimensionName, typeName));
  }

  public static int listChunkGeneratorTypes(final CommandSourceStack context) {
    sendSuccessMessage(context, "Available Chunk Generator Types\n===============================");

    for (ChunkGeneratorType type : ChunkGeneratorType.values()) {
      String configStatus =
          WorldgenConfigLoader.getConfig(type).isPresent() ? "✓ Configured" : "✗ Default";
      sendSuccessMessage(context, String.format("- %s (%s)", type.getName(), configStatus));
    }
    return Command.SINGLE_SUCCESS;
  }

  public static int reloadWorldgenConfigs(final CommandSourceStack context) {
    try {
      WorldgenInitializer.reload(context.getServer());
      return sendSuccessMessage(context, "Worldgen configurations reloaded successfully!");
    } catch (Exception e) {
      return sendFailureMessage(context, "Failed to reload worldgen configs: " + e.getMessage());
    }
  }

  public static int listWorldgenConfigs(final CommandSourceStack context) {
    var configs = WorldgenConfigLoader.getAllConfigs();
    if (configs.isEmpty()) {
      return sendFailureMessage(context, "No worldgen configurations loaded.");
    }

    sendSuccessMessage(context, "Loaded Worldgen Configurations\n==============================");

    for (var entry : configs.entrySet()) {
      StringBuilder details = new StringBuilder();
      details.append("- ").append(entry.getKey().getName()).append(":");

      entry.getValue().noiseSettings().ifPresent(o -> details.append(" noise=").append(o));
      entry.getValue().biomeSource().ifPresent(o -> details.append(" biome=").append(o));

      if (!entry.getValue().customSettings().isEmpty()) {
        details
            .append(" custom=")
            .append(entry.getValue().customSettings().size())
            .append(" settings");
      }

      sendSuccessMessage(context, details.toString());
    }
    return Command.SINGLE_SUCCESS;
  }

  public static int setDimensionSpawnPoint(
      final CommandSourceStack source, final String dimensionName, final BlockPos spawnPoint) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfoData(dimensionName);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimensionName + "' not found.");
    }

    boolean success = DimensionManager.setDimensionSpawnPoint(dimensionName, spawnPoint);

    if (success) {
      return sendSuccessMessage(
          source,
          String.format(
              "Spawn point for dimension '%s' set to (%d, %d, %d)",
              dimensionName, spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ()));
    } else {
      return sendFailureMessage(
          source, "Failed to set spawn point for dimension '" + dimensionName + "'");
    }
  }
}
