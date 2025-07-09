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
import de.markusbordihn.worlddimensionnexus.block.PortalBlockManager;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalInfoData;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalTargetData;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalType;
import de.markusbordihn.worlddimensionnexus.portal.PortalCreator;
import de.markusbordihn.worlddimensionnexus.portal.PortalManager;
import de.markusbordihn.worlddimensionnexus.portal.PortalTargetManager;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.FrameColorSuggestion;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

public class EventPortalCommand extends Command {

  private static final String ERROR_NOT_EVENT_PORTAL = "Portal is not an event portal!";
  private static final String ARG_FRAME_COLOR = "frame_color";
  private static final String ARG_BLOCK_POS = "block_pos";
  private static final String ARG_TARGET_POS = "target_pos";
  private static final String ARG_PORTAL_UUID = "portal_uuid";
  private static final String ARG_PORTAL_POS = "portal_pos";
  private static final String ARG_DIMENSION = "dimension";

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("event_portal")
        .requires(commandSourceStack -> commandSourceStack.hasPermission(2))
        .then(
            Commands.literal("create")
                .then(
                    Commands.argument(ARG_FRAME_COLOR, StringArgumentType.word())
                        .suggests(FrameColorSuggestion.FRAME_COLOR)
                        .then(
                            Commands.argument(ARG_BLOCK_POS, BlockPosArgument.blockPos())
                                .then(
                                    Commands.argument(ARG_TARGET_POS, BlockPosArgument.blockPos())
                                        .executes(
                                            context ->
                                                createEventPortal(
                                                    context.getSource(),
                                                    StringArgumentType.getString(
                                                        context, ARG_FRAME_COLOR),
                                                    BlockPosArgument.getBlockPos(
                                                        context, ARG_BLOCK_POS),
                                                    BlockPosArgument.getBlockPos(
                                                        context, ARG_TARGET_POS),
                                                    null))
                                        .then(
                                            Commands.argument(
                                                    "name", StringArgumentType.greedyString())
                                                .executes(
                                                    context ->
                                                        createEventPortal(
                                                            context.getSource(),
                                                            StringArgumentType.getString(
                                                                context, ARG_FRAME_COLOR),
                                                            BlockPosArgument.getBlockPos(
                                                                context, ARG_BLOCK_POS),
                                                            BlockPosArgument.getBlockPos(
                                                                context, ARG_TARGET_POS),
                                                            StringArgumentType.getString(
                                                                context, "name"))))))))
        .then(
            Commands.literal("set")
                .then(
                    Commands.literal("target")
                        .then(
                            Commands.argument(ARG_PORTAL_UUID, UuidArgument.uuid())
                                .then(
                                    Commands.argument(ARG_TARGET_POS, BlockPosArgument.blockPos())
                                        .executes(
                                            context ->
                                                setEventPortalTargetByUUID(
                                                    context.getSource(),
                                                    UuidArgument.getUuid(context, ARG_PORTAL_UUID),
                                                    BlockPosArgument.getBlockPos(
                                                        context, ARG_TARGET_POS)))))
                        .then(
                            Commands.argument(ARG_PORTAL_POS, BlockPosArgument.blockPos())
                                .then(
                                    Commands.argument(ARG_TARGET_POS, BlockPosArgument.blockPos())
                                        .executes(
                                            context ->
                                                setEventPortalTargetByPosition(
                                                    context.getSource(),
                                                    BlockPosArgument.getBlockPos(
                                                        context, ARG_PORTAL_POS),
                                                    BlockPosArgument.getBlockPos(
                                                        context, ARG_TARGET_POS)))))))
        .then(Commands.literal("list").executes(context -> listEventPortals(context.getSource())))
        .then(
            Commands.literal("remove")
                .then(
                    Commands.argument(ARG_PORTAL_UUID, UuidArgument.uuid())
                        .executes(
                            context ->
                                removeEventPortalByUUID(
                                    context.getSource(),
                                    UuidArgument.getUuid(context, ARG_PORTAL_UUID))))
                .then(
                    Commands.argument(ARG_PORTAL_POS, BlockPosArgument.blockPos())
                        .executes(
                            context ->
                                removeEventPortalByPosition(
                                    context.getSource(),
                                    BlockPosArgument.getBlockPos(context, ARG_PORTAL_POS)))))
        .then(
            Commands.literal("destroy")
                .then(
                    Commands.argument(ARG_PORTAL_UUID, UuidArgument.uuid())
                        .executes(
                            context ->
                                destroyEventPortalByUUID(
                                    context.getSource(),
                                    UuidArgument.getUuid(context, ARG_PORTAL_UUID))))
                .then(
                    Commands.argument(ARG_PORTAL_POS, BlockPosArgument.blockPos())
                        .executes(
                            context ->
                                destroyEventPortalByPosition(
                                    context.getSource(),
                                    BlockPosArgument.getBlockPos(context, ARG_PORTAL_POS)))))
        .then(
            Commands.literal("info")
                .then(
                    Commands.argument(ARG_PORTAL_UUID, UuidArgument.uuid())
                        .executes(
                            context ->
                                infoEventPortalByUUID(
                                    context.getSource(),
                                    UuidArgument.getUuid(context, ARG_PORTAL_UUID))))
                .then(
                    Commands.argument(ARG_PORTAL_POS, BlockPosArgument.blockPos())
                        .executes(
                            context ->
                                infoEventPortalByPosition(
                                    context.getSource(),
                                    BlockPosArgument.getBlockPos(context, ARG_PORTAL_POS)))));
  }

  private static int createEventPortal(
      final CommandSourceStack source,
      final String frameColorName,
      final BlockPos blockPos,
      final BlockPos targetPos,
      final String name)
      throws CommandSyntaxException {

    ServerLevel serverLevel = source.getLevel();
    ServerPlayer serverPlayer = source.getPlayerOrException();

    DyeColor frameColor = DyeColor.byName(frameColorName, null);
    if (frameColor == null) {
      return sendFailureMessage(source, "Invalid frame color: " + frameColorName);
    }

    if (!PortalType.EVENT.isEnabled()) {
      return sendFailureMessage(source, "Event portals are disabled on this server!");
    }

    String portalName =
        (name == null || name.trim().isEmpty())
            ? "Event Portal (" + frameColor.getName() + ")"
            : name;

    Direction playerFacing = PortalCreator.getPlayerFacingDirection(serverPlayer);

    PortalCreator.createEventPortal(
        serverLevel, serverPlayer, blockPos, frameColor, portalName, targetPos, playerFacing);

    return sendSuccessMessage(source, "Event portal created successfully!", ChatFormatting.GREEN);
  }

  private static int setEventPortalTargetByUUID(
      final CommandSourceStack source, final UUID portalUUID, final BlockPos targetPos) {

    ServerLevel serverLevel = source.getLevel();

    PortalInfoData portalInfo = PortalManager.getPortal(portalUUID);
    if (portalInfo == null) {
      return sendFailureMessage(source, "Portal with UUID " + portalUUID + " not found!");
    }

    if (portalInfo.portalType() != PortalType.EVENT) {
      return sendFailureMessage(source, ERROR_NOT_EVENT_PORTAL);
    }

    PortalTargetManager.setTarget(portalInfo, serverLevel.dimension(), targetPos);

    return sendSuccessMessage(
        source, "Event portal target set successfully!", ChatFormatting.GREEN);
  }

  private static int setEventPortalTargetByPosition(
      final CommandSourceStack source, final BlockPos portalPos, final BlockPos targetPos) {

    ServerLevel serverLevel = source.getLevel();

    PortalInfoData portalInfo = PortalManager.getPortal(serverLevel, portalPos);
    if (portalInfo == null) {
      return sendFailureMessage(source, "No portal found at position " + portalPos);
    }

    if (portalInfo.portalType() != PortalType.EVENT) {
      return sendFailureMessage(source, ERROR_NOT_EVENT_PORTAL);
    }

    PortalTargetManager.setTarget(portalInfo, serverLevel.dimension(), targetPos);

    return sendSuccessMessage(
        source, "Event portal target set successfully!", ChatFormatting.GREEN);
  }

  private static int listEventPortals(final CommandSourceStack source) {
    ServerLevel serverLevel = source.getLevel();

    List<PortalInfoData> eventPortals =
        PortalManager.getPortals(serverLevel.dimension()).stream()
            .filter(portal -> portal.portalType() == PortalType.EVENT)
            .toList();

    if (eventPortals.isEmpty()) {
      source.sendSuccess(
          () ->
              Component.literal("No event portals found in this dimension.")
                  .withStyle(ChatFormatting.YELLOW),
          false);
      return 0;
    }

    source.sendSuccess(
        () ->
            Component.literal("Event Portals in " + serverLevel.dimension().location() + ":")
                .withStyle(ChatFormatting.AQUA),
        false);

    for (PortalInfoData portal : eventPortals) {
      PortalTargetData target = PortalTargetManager.getTarget(portal);
      String targetInfo =
          target != null ? " -> " + target.position().toShortString() : " (no target)";

      source.sendSuccess(
          () ->
              Component.literal(
                      "- "
                          + portal.getDisplayName()
                          + " at "
                          + portal.origin().toShortString()
                          + targetInfo)
                  .withStyle(ChatFormatting.WHITE),
          false);
    }

    return eventPortals.size();
  }

  private static int removeEventPortalByUUID(
      final CommandSourceStack source, final UUID portalUUID) {
    PortalInfoData portalInfo = PortalManager.getPortal(portalUUID);
    if (portalInfo == null) {
      return sendFailureMessage(source, "Portal with UUID " + portalUUID + " not found!");
    }

    if (portalInfo.portalType() != PortalType.EVENT) {
      return sendFailureMessage(source, ERROR_NOT_EVENT_PORTAL);
    }

    ServerLevel serverLevel = source.getServer().getLevel(portalInfo.dimension());
    if (serverLevel != null) {
      PortalBlockManager.destroyPortal(serverLevel, source.getPlayer(), portalInfo);
    }

    return sendSuccessMessage(source, "Event portal removed successfully!", ChatFormatting.GREEN);
  }

  private static int removeEventPortalByPosition(
      final CommandSourceStack source, final BlockPos portalPos) {
    ServerLevel serverLevel = source.getLevel();

    PortalInfoData portalInfo = PortalManager.getPortal(serverLevel, portalPos);
    if (portalInfo == null) {
      return sendFailureMessage(source, "No portal found at position " + portalPos);
    }

    if (portalInfo.portalType() != PortalType.EVENT) {
      return sendFailureMessage(source, ERROR_NOT_EVENT_PORTAL);
    }

    PortalBlockManager.destroyPortal(serverLevel, source.getPlayer(), portalInfo);

    return sendSuccessMessage(source, "Event portal removed successfully!", ChatFormatting.GREEN);
  }

  private static int destroyEventPortalByUUID(
      final CommandSourceStack source, final UUID portalUUID) {
    PortalInfoData portalInfo = PortalManager.getPortal(portalUUID);
    if (portalInfo == null) {
      return sendFailureMessage(source, "Portal with UUID " + portalUUID + " not found!");
    }

    if (portalInfo.portalType() != PortalType.EVENT) {
      return sendFailureMessage(source, ERROR_NOT_EVENT_PORTAL);
    }

    ServerLevel serverLevel = source.getServer().getLevel(portalInfo.dimension());
    if (serverLevel != null) {
      PortalBlockManager.destroyPortal(serverLevel, source.getPlayer(), portalInfo);
    }

    return sendSuccessMessage(source, "Event portal destroyed successfully!", ChatFormatting.GREEN);
  }

  private static int destroyEventPortalByPosition(
      final CommandSourceStack source, final BlockPos portalPos) {
    ServerLevel serverLevel = source.getLevel();

    PortalInfoData portalInfo = PortalManager.getPortal(serverLevel, portalPos);
    if (portalInfo == null) {
      return sendFailureMessage(source, "No portal found at position " + portalPos);
    }

    if (portalInfo.portalType() != PortalType.EVENT) {
      return sendFailureMessage(source, ERROR_NOT_EVENT_PORTAL);
    }

    PortalBlockManager.destroyPortal(serverLevel, source.getPlayer(), portalInfo);

    return sendSuccessMessage(source, "Event portal destroyed successfully!", ChatFormatting.GREEN);
  }

  private static int infoEventPortalByUUID(final CommandSourceStack source, final UUID portalUUID) {
    PortalInfoData portalInfo = PortalManager.getPortal(portalUUID);
    if (portalInfo == null) {
      return sendFailureMessage(source, "Portal with UUID " + portalUUID + " not found!");
    }

    if (portalInfo.portalType() != PortalType.EVENT) {
      return sendFailureMessage(source, ERROR_NOT_EVENT_PORTAL);
    }

    PortalTargetData target = PortalTargetManager.getTarget(portalInfo);
    String targetInfo =
        target != null
            ? target.position().toShortString() + " in " + target.dimension().location()
            : "No target set";

    Component infoMessage =
        Component.literal("Event Portal Info:")
            .withStyle(ChatFormatting.AQUA)
            .append("\n")
            .append(Component.literal("UUID: " + portalInfo.uuid()).withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(
                Component.literal("Name: " + portalInfo.getDisplayName())
                    .withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(
                Component.literal("Position: " + portalInfo.origin().toShortString())
                    .withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(Component.literal("Target: " + targetInfo).withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(
                Component.literal("Dimension: " + portalInfo.dimension().location())
                    .withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(
                Component.literal("Frame Color: " + portalInfo.color().getName())
                    .withStyle(ChatFormatting.WHITE));

    source.sendSuccess(() -> infoMessage, false);
    return 1;
  }

  private static int infoEventPortalByPosition(
      final CommandSourceStack source, final BlockPos portalPos) {
    ServerLevel serverLevel = source.getLevel();

    PortalInfoData portalInfo = PortalManager.getPortal(serverLevel, portalPos);
    if (portalInfo == null) {
      return sendFailureMessage(source, "No portal found at position " + portalPos);
    }

    if (portalInfo.portalType() != PortalType.EVENT) {
      return sendFailureMessage(source, ERROR_NOT_EVENT_PORTAL);
    }

    PortalTargetData target = PortalTargetManager.getTarget(portalInfo);
    String targetInfo =
        target != null
            ? target.position().toShortString() + " in " + target.dimension().location()
            : "No target set";

    Component infoMessage =
        Component.literal("Event Portal Info:")
            .withStyle(ChatFormatting.AQUA)
            .append("\n")
            .append(Component.literal("UUID: " + portalInfo.uuid()).withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(
                Component.literal("Name: " + portalInfo.getDisplayName())
                    .withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(
                Component.literal("Position: " + portalInfo.origin().toShortString())
                    .withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(Component.literal("Target: " + targetInfo).withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(
                Component.literal("Dimension: " + portalInfo.dimension().location())
                    .withStyle(ChatFormatting.WHITE))
            .append("\n")
            .append(
                Component.literal("Frame Color: " + portalInfo.color().getName())
                    .withStyle(ChatFormatting.WHITE));

    source.sendSuccess(() -> infoMessage, false);
    return 1;
  }
}
