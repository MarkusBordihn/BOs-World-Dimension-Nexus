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
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.data.spawn.SpawnRules;
import de.markusbordihn.worlddimensionnexus.data.worldgen.WorldgenConfigLoader;
import de.markusbordihn.worlddimensionnexus.data.worldgen.WorldgenInitializer;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.dimension.io.DimensionExporter;
import de.markusbordihn.worlddimensionnexus.dimension.io.DimensionImporter;
import de.markusbordihn.worlddimensionnexus.resources.WorldDataPackResourceManager;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionImportFileSuggestion;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionSuggestion;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.EntityTypeSuggestion;
import java.io.File;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
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
                                            StringArgumentType.getString(context, "type")))
                                .then(
                                    Commands.argument("gametype", StringArgumentType.word())
                                        .suggests(
                                            (context, builder) -> {
                                              builder.suggest("survival");
                                              builder.suggest("creative");
                                              builder.suggest("adventure");
                                              builder.suggest("spectator");
                                              return builder.buildFuture();
                                            })
                                        .executes(
                                            context ->
                                                createDimensionWithGameType(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "name"),
                                                    StringArgumentType.getString(context, "type"),
                                                    StringArgumentType.getString(
                                                        context, "gametype")))))))
        .then(
            Commands.literal("remove")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_ADMINS))
                .then(
                    Commands.argument("dimension", DimensionArgument.dimension())
                        .suggests(DimensionSuggestion.CUSTOM_DIMENSIONS)
                        .executes(
                            context ->
                                removeDimension(
                                    context.getSource(),
                                    DimensionArgument.getDimension(context, "dimension")
                                        .dimension()))))
        .then(
            Commands.literal("info")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument("dimension", DimensionArgument.dimension())
                        .suggests(DimensionSuggestion.CUSTOM_DIMENSIONS)
                        .executes(
                            context ->
                                infoDimension(
                                    context.getSource(),
                                    DimensionArgument.getDimension(context, "dimension")
                                        .dimension()))))
        .then(
            Commands.literal("set")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.literal("spawnpoint")
                        .then(
                            Commands.argument("dimension", DimensionArgument.dimension())
                                .suggests(DimensionSuggestion.CUSTOM_DIMENSIONS)
                                .then(
                                    Commands.argument("position", BlockPosArgument.blockPos())
                                        .executes(
                                            context ->
                                                setDimensionSpawnPoint(
                                                    context.getSource(),
                                                    DimensionArgument.getDimension(
                                                            context, "dimension")
                                                        .dimension(),
                                                    BlockPosArgument.getBlockPos(
                                                        context, "position"))))))
                .then(
                    Commands.literal("gametype")
                        .then(
                            Commands.argument("dimension", DimensionArgument.dimension())
                                .suggests(DimensionSuggestion.CUSTOM_DIMENSIONS)
                                .then(
                                    Commands.argument("gametype", StringArgumentType.word())
                                        .suggests(
                                            (context, builder) -> {
                                              builder.suggest("survival");
                                              builder.suggest("creative");
                                              builder.suggest("adventure");
                                              builder.suggest("spectator");
                                              return builder.buildFuture();
                                            })
                                        .executes(
                                            context ->
                                                setDimensionGameType(
                                                    context.getSource(),
                                                    DimensionArgument.getDimension(
                                                            context, "dimension")
                                                        .dimension(),
                                                    StringArgumentType.getString(
                                                        context, "gametype")))))))
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
                                        .dimension(),
                                    null))
                        .then(
                            Commands.argument("filename", StringArgumentType.string())
                                .executes(
                                    context ->
                                        exportDimension(
                                            context.getSource(),
                                            DimensionArgument.getDimension(context, "dimension")
                                                .dimension(),
                                            StringArgumentType.getString(context, "filename"))))))
        .then(
            Commands.literal("import")
                .requires(source -> source.hasPermission(Commands.LEVEL_OWNERS))
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
            Commands.literal("spawn")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(
                    Commands.argument("dimension", DimensionArgument.dimension())
                        .suggests(DimensionSuggestion.CUSTOM_DIMENSIONS)
                        .then(
                            Commands.literal("disableNaturalMobSpawning")
                                .executes(
                                    context ->
                                        getSpawnRuleBooleanValue(
                                            context.getSource(),
                                            DimensionArgument.getDimension(context, "dimension")
                                                .dimension(),
                                            "disableNaturalMobSpawning"))
                                .then(
                                    Commands.argument("value", StringArgumentType.word())
                                        .suggests(
                                            (context, builder) -> {
                                              builder.suggest("true");
                                              builder.suggest("false");
                                              return builder.buildFuture();
                                            })
                                        .executes(
                                            context ->
                                                setDisableNaturalMobSpawning(
                                                    context.getSource(),
                                                    DimensionArgument.getDimension(
                                                            context, "dimension")
                                                        .dimension(),
                                                    StringArgumentType.getString(
                                                        context, "value")))))
                        .then(
                            Commands.literal("disableSpawnerBlocks")
                                .executes(
                                    context ->
                                        getSpawnRuleBooleanValue(
                                            context.getSource(),
                                            DimensionArgument.getDimension(context, "dimension")
                                                .dimension(),
                                            "disableSpawnerBlocks"))
                                .then(
                                    Commands.argument("value", StringArgumentType.word())
                                        .suggests(
                                            (context, builder) -> {
                                              builder.suggest("true");
                                              builder.suggest("false");
                                              return builder.buildFuture();
                                            })
                                        .executes(
                                            context ->
                                                setDisableSpawnerBlocks(
                                                    context.getSource(),
                                                    DimensionArgument.getDimension(
                                                            context, "dimension")
                                                        .dimension(),
                                                    StringArgumentType.getString(
                                                        context, "value")))))
                        .then(
                            Commands.literal("allowedEntityTypes")
                                .then(
                                    Commands.literal("list")
                                        .executes(
                                            context ->
                                                listAllowedEntityTypes(
                                                    context.getSource(),
                                                    DimensionArgument.getDimension(
                                                            context, "dimension")
                                                        .dimension())))
                                .then(
                                    Commands.literal("add")
                                        .then(
                                            Commands.argument(
                                                    "entity", ResourceLocationArgument.id())
                                                .suggests(EntityTypeSuggestion.ALL_ENTITY_TYPES)
                                                .executes(
                                                    context ->
                                                        addAllowedEntityType(
                                                            context.getSource(),
                                                            DimensionArgument.getDimension(
                                                                    context, "dimension")
                                                                .dimension(),
                                                            ResourceLocationArgument.getId(
                                                                context, "entity")))))
                                .then(
                                    Commands.literal("remove")
                                        .then(
                                            Commands.argument(
                                                    "entity", ResourceLocationArgument.id())
                                                .suggests(EntityTypeSuggestion.ALL_ENTITY_TYPES)
                                                .executes(
                                                    context ->
                                                        removeAllowedEntityType(
                                                            context.getSource(),
                                                            DimensionArgument.getDimension(
                                                                    context, "dimension")
                                                                .dimension(),
                                                            ResourceLocationArgument.getId(
                                                                context, "entity")))))
                                .then(
                                    Commands.literal("clear")
                                        .executes(
                                            context ->
                                                clearAllowedEntityTypes(
                                                    context.getSource(),
                                                    DimensionArgument.getDimension(
                                                            context, "dimension")
                                                        .dimension()))))
                        .then(
                            Commands.literal("deniedEntityTypes")
                                .then(
                                    Commands.literal("list")
                                        .executes(
                                            context ->
                                                listDeniedEntityTypes(
                                                    context.getSource(),
                                                    DimensionArgument.getDimension(
                                                            context, "dimension")
                                                        .dimension())))
                                .then(
                                    Commands.literal("add")
                                        .then(
                                            Commands.argument(
                                                    "entity", ResourceLocationArgument.id())
                                                .suggests(EntityTypeSuggestion.ALL_ENTITY_TYPES)
                                                .executes(
                                                    context ->
                                                        addDeniedEntityType(
                                                            context.getSource(),
                                                            DimensionArgument.getDimension(
                                                                    context, "dimension")
                                                                .dimension(),
                                                            ResourceLocationArgument.getId(
                                                                context, "entity")))))
                                .then(
                                    Commands.literal("remove")
                                        .then(
                                            Commands.argument(
                                                    "entity", ResourceLocationArgument.id())
                                                .suggests(EntityTypeSuggestion.ALL_ENTITY_TYPES)
                                                .executes(
                                                    context ->
                                                        removeDeniedEntityType(
                                                            context.getSource(),
                                                            DimensionArgument.getDimension(
                                                                    context, "dimension")
                                                                .dimension(),
                                                            ResourceLocationArgument.getId(
                                                                context, "entity")))))
                                .then(
                                    Commands.literal("clear")
                                        .executes(
                                            context ->
                                                clearDeniedEntityTypes(
                                                    context.getSource(),
                                                    DimensionArgument.getDimension(
                                                            context, "dimension")
                                                        .dimension()))))))
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

  public static int removeDimension(
      final CommandSourceStack source, final ResourceKey<Level> dimension) {
    if (DimensionManager.removeDimension(dimension)) {
      return sendSuccessMessage(
          source,
          "Dimension '" + dimension.location() + "' was removed from server (data remains).");
    }
    return sendFailureMessage(
        source, "Dimension '" + dimension.location() + "' could not be removed.");
  }

  public static int infoDimension(
      final CommandSourceStack source, final ResourceKey<Level> dimension) {
    DimensionInfoData info = DimensionManager.getDimensionInfo(dimension);
    if (info == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }
    return sendSuccessMessage(
        source, "Dimension info for '" + dimension.location() + "':\n" + info);
  }

  private static int exportDimension(
      final CommandSourceStack source,
      final ResourceKey<Level> dimension,
      final String customFileName) {
    MinecraftServer server = source.getServer();
    if (dimension == null) {
      return sendFailureMessage(source, "Dimension '" + dimension + "' could not be found.");
    }

    String fileName;
    if (customFileName != null) {
      fileName = customFileName.endsWith(".wdn") ? customFileName : customFileName + ".wdn";
    } else {
      fileName =
          dimension.location().getNamespace() + "_" + dimension.location().getPath() + ".wdn";
    }

    File exportFile = new File(server.getServerDirectory().toFile(), fileName);

    if (DimensionExporter.exportDimension(server, dimension, exportFile)) {
      // Create clickable success message with file path that opens the file location
      String filePath = exportFile.getAbsolutePath();
      String parentDirectory = exportFile.getParent();

      Component filePathComponent =
          Component.literal(filePath)
              .setStyle(
                  Style.EMPTY
                      .withColor(ChatFormatting.GREEN)
                      .withUnderlined(true)
                      .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, parentDirectory))
                      .withHoverEvent(
                          new HoverEvent(
                              HoverEvent.Action.SHOW_TEXT,
                              Component.literal("Click to open file location"))));

      Component successMessage = Component.literal("Export successful: ").append(filePathComponent);
      return sendSuccessMessage(source, successMessage);
    }
    return sendFailureMessage(source, "Error exporting the dimension!");
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

  public static int createDimensionWithGameType(
      final CommandSourceStack context,
      final String dimensionName,
      final String typeName,
      final String gameTypeName) {
    ChunkGeneratorType type = ChunkGeneratorType.fromString(typeName);
    DimensionInfoData dimensionInfo =
        DimensionInfoData.fromDimensionNameAndType(dimensionName, type);

    GameType gameType = GameType.byName(gameTypeName, null);
    if (gameType == null) {
      return sendFailureMessage(
          context, "Invalid gametype. Use: survival, creative, adventure, or spectator.");
    }

    ServerLevel serverLevel =
        DimensionManager.addOrCreateDimension(dimensionInfo.withGameType(gameType), true);
    if (serverLevel != null) {
      return sendSuccessMessage(
          context,
          String.format(
              "Dimension '%s' created successfully with type '%s' and gametype '%s'!",
              dimensionName, type.getName(), gameType.getName()));
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
      final CommandSourceStack source,
      final ResourceKey<Level> dimension,
      final BlockPos spawnPoint) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    if (DimensionManager.updateDimensionInfoData(
        dimension, dimensionInfo.withSpawnPoint(spawnPoint))) {
      return sendSuccessMessage(
          source,
          String.format(
              "Spawn point for dimension '%s' set to (%d, %d, %d)",
              dimension.location(), spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ()));
    }
    return sendFailureMessage(
        source, "Failed to set spawn point for dimension '" + dimension.location() + "'");
  }

  public static int setDimensionGameType(
      final CommandSourceStack source,
      final ResourceKey<Level> dimension,
      final String gameTypeName) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    GameType gameType = GameType.byName(gameTypeName, null);
    if (gameType == null) {
      return sendFailureMessage(
          source, "Invalid gametype. Use: survival, creative, adventure, or spectator.");
    }

    if (DimensionManager.updateDimensionInfoData(dimension, dimensionInfo.withGameType(gameType))) {
      return sendSuccessMessage(
          source,
          "Set gametype for dimension '"
              + dimension.location()
              + "' to "
              + gameType.getName()
              + ".");
    }
    return sendFailureMessage(
        source, "Failed to update gametype for dimension '" + dimension.location() + "'.");
  }

  public static int setDisableNaturalMobSpawning(
      final CommandSourceStack source, final ResourceKey<Level> dimension, final String value) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    boolean disableMobSpawn = Boolean.parseBoolean(value);
    SpawnRules spawnRules =
        dimensionInfo.spawnRules().withNaturalMobSpawningDisabled(disableMobSpawn);

    if (DimensionManager.updateDimensionInfoData(
        dimension, dimensionInfo.withSpawnRules(spawnRules))) {
      return sendSuccessMessage(
          source,
          "Natural mob spawn "
              + (disableMobSpawn ? "disabled" : "enabled")
              + " for dimension '"
              + dimension.location()
              + "'.");
    }
    return sendFailureMessage(
        source,
        "Failed to update natural mob spawn setting for dimension '" + dimension.location() + "'.");
  }

  public static int setDisableSpawnerBlocks(
      final CommandSourceStack source, final ResourceKey<Level> dimension, final String value) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    boolean disableSpawner = Boolean.parseBoolean(value);
    SpawnRules spawnRules = dimensionInfo.spawnRules().withSpawnerBlocksDisabled(disableSpawner);

    if (DimensionManager.updateDimensionInfoData(
        dimension, dimensionInfo.withSpawnRules(spawnRules))) {
      return sendSuccessMessage(
          source,
          "Spawner blocks "
              + (disableSpawner ? "disabled" : "enabled")
              + " for dimension '"
              + dimension.location()
              + "'.");
    }
    return sendFailureMessage(
        source,
        "Failed to update spawner blocks setting for dimension '" + dimension.location() + "'.");
  }

  public static int getSpawnRuleBooleanValue(
      final CommandSourceStack source, final ResourceKey<Level> dimension, final String rule) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    boolean value;
    switch (rule) {
      case "disableNaturalMobSpawning":
        value = dimensionInfo.spawnRules().disableNaturalMobSpawning();
        break;
      case "disableSpawnerBlocks":
        value = dimensionInfo.spawnRules().disableSpawnerBlocks();
        break;
      default:
        return sendFailureMessage(source, "Unknown spawn rule: " + rule);
    }

    return sendSuccessMessage(source, String.format("'%s' is currently set to %s.", rule, value));
  }

  public static int listAllowedEntityTypes(
      final CommandSourceStack source, final ResourceKey<Level> dimension) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    var allowedMobs = dimensionInfo.spawnRules().allowedEntityTypes();
    if (allowedMobs.isEmpty()) {
      return sendFailureMessage(source, "No allowed mobs for this dimension.");
    }

    sendSuccessMessage(source, "Allowed mobs:\n=================");
    for (var entityType : allowedMobs) {
      sendSuccessMessage(source, "- " + entityType);
    }
    return Command.SINGLE_SUCCESS;
  }

  public static int listDeniedEntityTypes(
      final CommandSourceStack source, final ResourceKey<Level> dimension) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    var deniedMobs = dimensionInfo.spawnRules().deniedEntityTypes();
    if (deniedMobs.isEmpty()) {
      return sendFailureMessage(source, "No denied mobs for this dimension.");
    }

    sendSuccessMessage(source, "Denied mobs:\n================");
    for (var entityType : deniedMobs) {
      sendSuccessMessage(source, "- " + entityType);
    }
    return Command.SINGLE_SUCCESS;
  }

  public static int addAllowedEntityType(
      final CommandSourceStack source,
      final ResourceKey<Level> dimension,
      final ResourceLocation entity) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    SpawnRules spawnRules = dimensionInfo.spawnRules().withMobAllowedByLocation(entity);
    if (DimensionManager.updateDimensionInfoData(
        dimension, dimensionInfo.withSpawnRules(spawnRules))) {
      return sendSuccessMessage(source, "Entity '" + entity + "' added to allowed mobs.");
    }
    return sendFailureMessage(source, "Failed to add entity to allowed mobs.");
  }

  public static int removeAllowedEntityType(
      final CommandSourceStack source,
      final ResourceKey<Level> dimension,
      final ResourceLocation entity) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    SpawnRules spawnRules = dimensionInfo.spawnRules().withoutMobByLocation(entity);
    if (DimensionManager.updateDimensionInfoData(
        dimension, dimensionInfo.withSpawnRules(spawnRules))) {
      return sendSuccessMessage(source, "Entity '" + entity + "' removed from allowed mobs.");
    }
    return sendFailureMessage(source, "Failed to remove entity from allowed mobs.");
  }

  public static int addDeniedEntityType(
      final CommandSourceStack source,
      final ResourceKey<Level> dimension,
      final ResourceLocation entity) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    SpawnRules spawnRules = dimensionInfo.spawnRules().withMobDeniedByLocation(entity);
    if (DimensionManager.updateDimensionInfoData(
        dimension, dimensionInfo.withSpawnRules(spawnRules))) {
      return sendSuccessMessage(source, "Entity '" + entity + "' added to denied mobs.");
    }
    return sendFailureMessage(source, "Failed to add entity to denied mobs.");
  }

  public static int removeDeniedEntityType(
      final CommandSourceStack source,
      final ResourceKey<Level> dimension,
      final ResourceLocation entity) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    SpawnRules spawnRules = dimensionInfo.spawnRules().withoutMobByLocation(entity);
    if (DimensionManager.updateDimensionInfoData(
        dimension, dimensionInfo.withSpawnRules(spawnRules))) {
      return sendSuccessMessage(source, "Entity '" + entity + "' removed from denied mobs.");
    }
    return sendFailureMessage(source, "Failed to remove entity from denied mobs.");
  }

  public static int clearAllowedEntityTypes(
      final CommandSourceStack source, final ResourceKey<Level> dimension) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    SpawnRules spawnRules = dimensionInfo.spawnRules().clearAllowedMobs();
    if (DimensionManager.updateDimensionInfoData(
        dimension, dimensionInfo.withSpawnRules(spawnRules))) {
      return sendSuccessMessage(source, "All entities removed from allowed mobs.");
    }
    return sendFailureMessage(source, "Failed to clear allowed mobs.");
  }

  public static int clearDeniedEntityTypes(
      final CommandSourceStack source, final ResourceKey<Level> dimension) {
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfo == null) {
      return sendFailureMessage(source, "Dimension '" + dimension.location() + "' not found.");
    }

    SpawnRules spawnRules = dimensionInfo.spawnRules().clearDeniedMobs();
    if (DimensionManager.updateDimensionInfoData(
        dimension, dimensionInfo.withSpawnRules(spawnRules))) {
      return sendSuccessMessage(source, "All entities removed from denied mobs.");
    }
    return sendFailureMessage(source, "Failed to clear denied mobs.");
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

      if (DimensionImporter.importDimension(
          server, importFile, dimensionName, chunkGeneratorType)) {
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
}
