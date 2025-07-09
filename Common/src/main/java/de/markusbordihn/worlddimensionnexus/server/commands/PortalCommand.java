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
import de.markusbordihn.worlddimensionnexus.data.portal.PortalInfoData;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalTargetData;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalType;
import de.markusbordihn.worlddimensionnexus.portal.PortalCreator;
import de.markusbordihn.worlddimensionnexus.portal.PortalManager;
import de.markusbordihn.worlddimensionnexus.portal.PortalTargetManager;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.DimensionSuggestion;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.FrameColorSuggestion;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.PortalTypeSuggestion;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

public class PortalCommand extends Command {

  private static final String PORTAL_NOT_FOUND = "Portal not found: ";
  private static final String ARG_DIMENSION = "dimension";
  private static final String ARG_POSITION = "position";
  private static final String ARG_PORTAL_TYPE = "portal_type";
  private static final String ARG_FRAME_COLOR = "frame_color";
  private static final String ARG_BLOCK_POS = "block_pos";

  private PortalCommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("portal")
        .then(
            Commands.literal("list")
                .executes(context -> listPortals(context.getSource()))
                .then(
                    Commands.argument(ARG_DIMENSION, ResourceLocationArgument.id())
                        .suggests(DimensionSuggestion.DIMENSION_NAMES)
                        .executes(
                            context ->
                                listPortalsInDimension(
                                    context.getSource(),
                                    ResourceLocationArgument.getId(context, ARG_DIMENSION)))))
        .then(
            Commands.literal("info")
                .then(
                    Commands.argument(ARG_POSITION, BlockPosArgument.blockPos())
                        .executes(
                            context ->
                                showPortalInfo(
                                    context.getSource(),
                                    BlockPosArgument.getBlockPos(context, ARG_POSITION)))))
        .then(
            Commands.literal("create")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument(ARG_PORTAL_TYPE, StringArgumentType.word())
                        .suggests(PortalTypeSuggestion.PORTAL_TYPE)
                        .then(
                            Commands.argument(ARG_FRAME_COLOR, StringArgumentType.word())
                                .suggests(FrameColorSuggestion.FRAME_COLOR)
                                .executes(
                                    context ->
                                        createTypedPortal(
                                            context.getSource(),
                                            StringArgumentType.getString(context, ARG_PORTAL_TYPE),
                                            StringArgumentType.getString(context, ARG_FRAME_COLOR),
                                            null,
                                            null))
                                .then(
                                    Commands.argument(ARG_BLOCK_POS, BlockPosArgument.blockPos())
                                        .executes(
                                            context ->
                                                createTypedPortal(
                                                    context.getSource(),
                                                    StringArgumentType.getString(
                                                        context, ARG_PORTAL_TYPE),
                                                    StringArgumentType.getString(
                                                        context, ARG_FRAME_COLOR),
                                                    BlockPosArgument.getBlockPos(
                                                        context, ARG_BLOCK_POS),
                                                    null))
                                        .then(
                                            Commands.argument(
                                                    "name", StringArgumentType.greedyString())
                                                .executes(
                                                    context ->
                                                        createTypedPortal(
                                                            context.getSource(),
                                                            StringArgumentType.getString(
                                                                context, ARG_PORTAL_TYPE),
                                                            StringArgumentType.getString(
                                                                context, ARG_FRAME_COLOR),
                                                            BlockPosArgument.getBlockPos(
                                                                context, ARG_BLOCK_POS),
                                                            StringArgumentType.getString(
                                                                context, "name")))))
                                .then(
                                    Commands.argument(ARG_POSITION, BlockPosArgument.blockPos())
                                        .executes(
                                            context ->
                                                createPortalAtPosition(
                                                    context.getSource(),
                                                    BlockPosArgument.getBlockPos(
                                                        context, ARG_POSITION)))
                                        .then(
                                            Commands.argument(
                                                    ARG_DIMENSION, ResourceLocationArgument.id())
                                                .suggests(DimensionSuggestion.DIMENSION_NAMES)
                                                .executes(
                                                    context ->
                                                        createPortalAtPositionInDimension(
                                                            context.getSource(),
                                                            BlockPosArgument.getBlockPos(
                                                                context, ARG_POSITION),
                                                            ResourceLocationArgument.getId(
                                                                context, ARG_DIMENSION))))))))
        .then(
            Commands.literal("link")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument("portal_uuid", UuidArgument.uuid())
                        .then(
                            Commands.argument("target_uuid", UuidArgument.uuid())
                                .executes(
                                    context ->
                                        linkPortals(
                                            context.getSource(),
                                            UuidArgument.getUuid(context, "portal_uuid"),
                                            UuidArgument.getUuid(context, "target_uuid"))))))
        .then(
            Commands.literal("unlink")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument(ARG_POSITION, BlockPosArgument.blockPos())
                        .executes(
                            context ->
                                unlinkPortal(
                                    context.getSource(),
                                    BlockPosArgument.getBlockPos(context, ARG_POSITION)))))
        .then(
            Commands.literal("remove")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument(ARG_POSITION, BlockPosArgument.blockPos())
                        .executes(
                            context ->
                                removePortal(
                                    context.getSource(),
                                    BlockPosArgument.getBlockPos(context, ARG_POSITION)))))
        .then(
            Commands.literal("teleport")
                .then(
                    Commands.argument(ARG_POSITION, BlockPosArgument.blockPos())
                        .executes(
                            context -> {
                              try {
                                return teleportPlayerToPortal(
                                    context.getSource(),
                                    BlockPosArgument.getBlockPos(context, ARG_POSITION),
                                    context.getSource().getPlayerOrException());
                              } catch (CommandSyntaxException e) {
                                return sendFailureMessage(
                                    context.getSource(), "This command requires a player context.");
                              }
                            })
                        .then(
                            Commands.argument("player", EntityArgument.player())
                                .executes(
                                    context ->
                                        teleportPlayerToPortal(
                                            context.getSource(),
                                            BlockPosArgument.getBlockPos(context, ARG_POSITION),
                                            EntityArgument.getPlayer(context, "player"))))));
  }

  private static int createTypedPortal(
      final CommandSourceStack source,
      final String portalTypeName,
      final String frameColorName,
      final BlockPos blockPos,
      final String name)
      throws CommandSyntaxException {

    ServerLevel serverLevel = source.getLevel();
    ServerPlayer serverPlayer = source.getPlayerOrException();

    PortalType portalType;
    try {
      portalType = PortalType.valueOf(portalTypeName.toUpperCase());
    } catch (IllegalArgumentException e) {
      return sendFailureMessage(source, "Invalid portal type: " + portalTypeName);
    }

    if (!portalType.isEnabled()) {
      return sendFailureMessage(
          source, portalType.getName() + " portals are disabled on this server!");
    }

    if (!portalType.isPlayerCreatable() && !source.hasPermission(2)) {
      return sendFailureMessage(
          source, "Only moderators can create " + portalType.getName() + " portals!");
    }

    DyeColor frameColor = DyeColor.byName(frameColorName, null);
    if (frameColor == null) {
      return sendFailureMessage(source, "Invalid frame color: " + frameColorName);
    }

    BlockPos portalPosition = (blockPos == null) ? serverPlayer.blockPosition() : blockPos;
    Direction playerFacing = PortalCreator.getPlayerFacingDirection(serverPlayer);

    if (PortalCreator.createPortal(
            serverLevel, serverPlayer, portalPosition, portalType, frameColor, name, playerFacing)
        == null) {
      return sendFailureMessage(
          source, "Failed to create " + portalType.getName() + " portal at " + portalPosition);
    }
    return sendSuccessMessage(
        source, portalType.getName() + " portal created successfully!", ChatFormatting.GREEN);
  }

  private static int listPortals(final CommandSourceStack source) {
    Set<PortalInfoData> allPortals = PortalManager.getPortals();
    if (allPortals.isEmpty()) {
      return sendFailureMessage(source, "No portals found in any dimension.");
    }

    source.sendSuccess(
        () -> Component.literal("=== Portal List ===").withStyle(ChatFormatting.GOLD), false);
    source.sendSuccess(
        () ->
            Component.literal("Total portals: " + allPortals.size())
                .withStyle(ChatFormatting.YELLOW),
        false);

    for (PortalInfoData portal : allPortals) {
      Component portalInfo =
          Component.literal("• ")
              .withStyle(ChatFormatting.GRAY)
              .append(
                  Component.literal(portal.uuid().toString().substring(0, 8) + "...")
                      .withStyle(ChatFormatting.AQUA))
              .append(Component.literal(" in ").withStyle(ChatFormatting.GRAY))
              .append(
                  Component.literal(portal.dimension().location().toString())
                      .withStyle(ChatFormatting.GREEN))
              .append(Component.literal(" at ").withStyle(ChatFormatting.GRAY))
              .append(
                  Component.literal(portal.origin().toShortString())
                      .withStyle(ChatFormatting.WHITE));

      source.sendSuccess(() -> portalInfo, false);
    }

    return SINGLE_SUCCESS;
  }

  private static int listPortalsInDimension(
      final CommandSourceStack source, final ResourceLocation dimensionId) {
    ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
    List<PortalInfoData> portals = PortalManager.getPortals(dimension);
    if (portals.isEmpty()) {
      return sendFailureMessage(source, "No portals found in dimension: " + dimensionId);
    }

    source.sendSuccess(
        () ->
            Component.literal("=== Portals in " + dimensionId + " ===")
                .withStyle(ChatFormatting.GOLD),
        false);
    source.sendSuccess(
        () ->
            Component.literal("Found " + portals.size() + " portal(s)")
                .withStyle(ChatFormatting.YELLOW),
        false);

    for (PortalInfoData portal : portals) {
      Component portalInfo =
          Component.literal("• ")
              .withStyle(ChatFormatting.GRAY)
              .append(
                  Component.literal(portal.uuid().toString().substring(0, 8) + "...")
                      .withStyle(ChatFormatting.AQUA))
              .append(Component.literal(" at ").withStyle(ChatFormatting.GRAY))
              .append(
                  Component.literal(portal.origin().toShortString())
                      .withStyle(ChatFormatting.WHITE));

      PortalTargetData target = PortalTargetManager.getTarget(portal);
      if (target != null) {
        Component targetInfo =
            portalInfo
                .copy()
                .append(Component.literal(" → ").withStyle(ChatFormatting.DARK_GRAY))
                .append(
                    Component.literal(target.dimension().location().toString())
                        .withStyle(ChatFormatting.GREEN));
        source.sendSuccess(() -> targetInfo, false);
      } else {
        Component unlinkedInfo =
            portalInfo
                .copy()
                .append(Component.literal(" (unlinked)").withStyle(ChatFormatting.RED));
        source.sendSuccess(() -> unlinkedInfo, false);
      }
    }

    return SINGLE_SUCCESS;
  }

  private static int showPortalInfo(final CommandSourceStack source, final BlockPos position) {
    ServerLevel level = source.getLevel();
    PortalInfoData portal = PortalManager.getPortal(level, position);
    if (portal == null) {
      return sendFailureMessage(source, PORTAL_NOT_FOUND + position.toShortString());
    }

    source.sendSuccess(
        () -> Component.literal("=== Portal Information ===").withStyle(ChatFormatting.GOLD),
        false);
    source.sendSuccess(
        () ->
            Component.literal("UUID: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(portal.uuid().toString()).withStyle(ChatFormatting.AQUA)),
        false);
    source.sendSuccess(
        () ->
            Component.literal("Type: ")
                .withStyle(ChatFormatting.GRAY)
                .append(
                    Component.literal(portal.portalType().getName())
                        .withStyle(ChatFormatting.YELLOW)),
        false);
    source.sendSuccess(
        () ->
            Component.literal("Name: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(portal.getDisplayName()).withStyle(ChatFormatting.WHITE)),
        false);
    source.sendSuccess(
        () ->
            Component.literal("Dimension: ")
                .withStyle(ChatFormatting.GRAY)
                .append(
                    Component.literal(portal.dimension().location().toString())
                        .withStyle(ChatFormatting.GREEN)),
        false);
    source.sendSuccess(
        () ->
            Component.literal("Origin: ")
                .withStyle(ChatFormatting.GRAY)
                .append(
                    Component.literal(portal.origin().toShortString())
                        .withStyle(ChatFormatting.WHITE)),
        false);

    PortalTargetData target = PortalTargetManager.getTarget(portal);
    if (target != null) {
      source.sendSuccess(
          () ->
              Component.literal("Target: ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(
                      Component.literal(target.dimension().location().toString())
                          .withStyle(ChatFormatting.GREEN))
                  .append(Component.literal(" at ").withStyle(ChatFormatting.GRAY))
                  .append(
                      Component.literal(target.position().toShortString())
                          .withStyle(ChatFormatting.WHITE)),
          false);
    } else {
      source.sendSuccess(
          () ->
              Component.literal("Target: ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(Component.literal("Not linked").withStyle(ChatFormatting.RED)),
          false);
    }

    return SINGLE_SUCCESS;
  }

  private static int createPortalAtPosition(
      final CommandSourceStack source, final BlockPos position) {
    return sendSuccessMessage(
        source,
        "Portal creation at "
            + position.toShortString()
            + " initiated. Use portal scanner or build portal structure manually.");
  }

  private static int createPortalAtPositionInDimension(
      final CommandSourceStack source,
      final BlockPos position,
      final ResourceLocation dimensionId) {
    return sendSuccessMessage(
        source,
        "Portal creation at "
            + position.toShortString()
            + " in dimension "
            + dimensionId
            + " initiated. Use portal scanner or build portal structure manually.");
  }

  private static int linkPortals(
      final CommandSourceStack source, final UUID portalUuid, final UUID targetUuid) {
    PortalInfoData sourcePortal = PortalManager.getPortal(portalUuid);
    PortalInfoData targetPortal = PortalManager.getPortal(targetUuid);

    if (sourcePortal == null) {
      return sendFailureMessage(source, "Source portal not found: " + portalUuid);
    }
    if (targetPortal == null) {
      return sendFailureMessage(source, "Target portal not found: " + targetUuid);
    }

    PortalTargetManager.setTarget(sourcePortal, targetPortal.dimension(), targetPortal.origin());
    PortalTargetManager.setTarget(targetPortal, sourcePortal.dimension(), sourcePortal.origin());

    return sendSuccessMessage(
        source,
        "Successfully linked portals "
            + portalUuid.toString().substring(0, 8)
            + "... ↔ "
            + targetUuid.toString().substring(0, 8)
            + "...");
  }

  private static int unlinkPortal(final CommandSourceStack source, final BlockPos position) {
    ServerLevel level = source.getLevel();
    PortalInfoData portal = PortalManager.getPortal(level, position);

    if (portal == null) {
      return sendFailureMessage(source, PORTAL_NOT_FOUND + position.toShortString());
    }

    PortalTargetManager.removeTarget(portal);
    return sendSuccessMessage(
        source, "Successfully unlinked portal at " + position.toShortString());
  }

  private static int removePortal(final CommandSourceStack source, final BlockPos position) {
    ServerLevel level = source.getLevel();
    PortalInfoData portal = PortalManager.getPortal(level, position);

    if (portal == null) {
      return sendFailureMessage(source, PORTAL_NOT_FOUND + position.toShortString());
    }

    PortalManager.removePortal(portal);
    return sendSuccessMessage(
        source,
        "Successfully removed portal at "
            + position.toShortString()
            + " from "
            + portal.dimension().location());
  }

  private static int teleportPlayerToPortal(
      final CommandSourceStack source, final BlockPos position, final ServerPlayer targetPlayer) {
    ServerLevel level = source.getLevel();
    PortalInfoData portal = PortalManager.getPortal(level, position);

    if (portal == null) {
      return sendFailureMessage(source, PORTAL_NOT_FOUND + position.toShortString());
    }

    targetPlayer.teleportTo(
        level,
        portal.origin().getX() + 0.5,
        portal.origin().getY() + 1.0,
        portal.origin().getZ() + 0.5,
        targetPlayer.getYRot(),
        targetPlayer.getXRot());

    String message =
        targetPlayer.equals(source.getEntity())
            ? "Teleported to portal in " + portal.dimension().location()
            : "Teleported "
                + targetPlayer.getName().getString()
                + " to portal in "
                + portal.dimension().location();

    return sendSuccessMessage(source, message);
  }
}
