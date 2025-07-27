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
import de.markusbordihn.worlddimensionnexus.config.WarpConfig;
import de.markusbordihn.worlddimensionnexus.data.warp.WarpData;
import de.markusbordihn.worlddimensionnexus.data.warp.WarpType;
import de.markusbordihn.worlddimensionnexus.server.commands.suggestions.WarpSuggestion;
import de.markusbordihn.worlddimensionnexus.warp.WarpManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WarpCommand extends Command {

  private static final String WARP_NOT_FOUND = "Warp not found: ";
  private static final String ARG_WARP_NAME = "warp_name";
  private static final String ARG_DESCRIPTION = "description";
  private static final String ARG_PLAYER = "player";
  private static final String ARG_WARP_UUID = "warp_uuid";

  private WarpCommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("warp")
        .then(
            Commands.literal("create")
                .then(
                    Commands.literal("private")
                        .then(
                            Commands.argument(ARG_WARP_NAME, StringArgumentType.word())
                                .executes(
                                    context -> {
                                      try {
                                        return createWarp(
                                            context.getSource(),
                                            StringArgumentType.getString(context, ARG_WARP_NAME),
                                            "",
                                            WarpType.PRIVATE,
                                            context.getSource().getPlayerOrException());
                                      } catch (CommandSyntaxException e) {
                                        return sendFailureMessage(
                                            context.getSource(),
                                            "This command requires a player context.");
                                      }
                                    })
                                .then(
                                    Commands.argument(
                                            ARG_DESCRIPTION, StringArgumentType.greedyString())
                                        .executes(
                                            context -> {
                                              try {
                                                return createWarp(
                                                    context.getSource(),
                                                    StringArgumentType.getString(
                                                        context, ARG_WARP_NAME),
                                                    StringArgumentType.getString(
                                                        context, ARG_DESCRIPTION),
                                                    WarpType.PRIVATE,
                                                    context.getSource().getPlayerOrException());
                                              } catch (CommandSyntaxException e) {
                                                return sendFailureMessage(
                                                    context.getSource(),
                                                    "This command requires a player context.");
                                              }
                                            }))))
                .then(
                    Commands.literal("public")
                        .then(
                            Commands.argument(ARG_WARP_NAME, StringArgumentType.word())
                                .executes(
                                    context -> {
                                      try {
                                        return createWarp(
                                            context.getSource(),
                                            StringArgumentType.getString(context, ARG_WARP_NAME),
                                            "",
                                            WarpType.PUBLIC,
                                            context.getSource().getPlayerOrException());
                                      } catch (CommandSyntaxException e) {
                                        return sendFailureMessage(
                                            context.getSource(),
                                            "This command requires a player context.");
                                      }
                                    })
                                .then(
                                    Commands.argument(
                                            ARG_DESCRIPTION, StringArgumentType.greedyString())
                                        .executes(
                                            context -> {
                                              try {
                                                return createWarp(
                                                    context.getSource(),
                                                    StringArgumentType.getString(
                                                        context, ARG_WARP_NAME),
                                                    StringArgumentType.getString(
                                                        context, ARG_DESCRIPTION),
                                                    WarpType.PUBLIC,
                                                    context.getSource().getPlayerOrException());
                                              } catch (CommandSyntaxException e) {
                                                return sendFailureMessage(
                                                    context.getSource(),
                                                    "This command requires a player context.");
                                              }
                                            })))))
        .then(
            Commands.literal("delete")
                .then(
                    Commands.argument(ARG_WARP_NAME, StringArgumentType.word())
                        .suggests(WarpSuggestion.PLAYER_WARPS)
                        .executes(
                            context -> {
                              try {
                                return deleteWarp(
                                    context.getSource(),
                                    StringArgumentType.getString(context, ARG_WARP_NAME),
                                    context.getSource().getPlayerOrException(),
                                    false);
                              } catch (CommandSyntaxException e) {
                                return sendFailureMessage(
                                    context.getSource(), "This command requires a player context.");
                              }
                            })))
        .then(
            Commands.literal("list")
                .executes(
                    context -> {
                      try {
                        return listOwnWarps(
                            context.getSource(), context.getSource().getPlayerOrException());
                      } catch (CommandSyntaxException e) {
                        return sendFailureMessage(
                            context.getSource(), "This command requires a player context.");
                      }
                    }))
        .then(
            Commands.literal("private")
                .executes(
                    context -> {
                      try {
                        return listPrivateWarps(
                            context.getSource(), context.getSource().getPlayerOrException());
                      } catch (CommandSyntaxException e) {
                        return sendFailureMessage(
                            context.getSource(), "This command requires a player context.");
                      }
                    })
                .then(
                    Commands.argument(ARG_WARP_NAME, StringArgumentType.word())
                        .suggests(WarpSuggestion.PRIVATE_WARPS)
                        .executes(
                            context -> {
                              try {
                                return teleportToPrivateWarp(
                                    context.getSource(),
                                    StringArgumentType.getString(context, ARG_WARP_NAME),
                                    context.getSource().getPlayerOrException());
                              } catch (CommandSyntaxException e) {
                                return sendFailureMessage(
                                    context.getSource(), "This command requires a player context.");
                              }
                            })))
        .then(
            Commands.literal("public")
                .executes(context -> listPublicWarps(context.getSource()))
                .then(
                    Commands.argument(ARG_WARP_NAME, StringArgumentType.word())
                        .suggests(WarpSuggestion.PUBLIC_WARPS)
                        .executes(
                            context -> {
                              try {
                                return teleportToPublicWarp(
                                    context.getSource(),
                                    StringArgumentType.getString(context, ARG_WARP_NAME),
                                    context.getSource().getPlayerOrException());
                              } catch (CommandSyntaxException e) {
                                return sendFailureMessage(
                                    context.getSource(), "This command requires a player context.");
                              }
                            })))
        .then(
            Commands.literal("info")
                .then(
                    Commands.argument(ARG_WARP_NAME, StringArgumentType.word())
                        .suggests(WarpSuggestion.ACCESSIBLE_WARPS)
                        .executes(
                            context -> {
                              try {
                                return showWarpInfo(
                                    context.getSource(),
                                    StringArgumentType.getString(context, ARG_WARP_NAME),
                                    context.getSource().getPlayerOrException());
                              } catch (CommandSyntaxException e) {
                                return sendFailureMessage(
                                    context.getSource(), "This command requires a player context.");
                              }
                            })))
        .then(
            Commands.literal("teleport")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.argument(ARG_PLAYER, EntityArgument.player())
                        .then(
                            Commands.argument(ARG_WARP_NAME, StringArgumentType.word())
                                .suggests(WarpSuggestion.ALL_WARPS)
                                .executes(
                                    context ->
                                        teleportPlayerToWarp(
                                            context.getSource(),
                                            StringArgumentType.getString(context, ARG_WARP_NAME),
                                            EntityArgument.getPlayer(context, ARG_PLAYER))))))
        .then(
            Commands.literal("admin")
                .requires(cs -> cs.hasPermission(Commands.LEVEL_MODERATORS))
                .then(
                    Commands.literal("list")
                        .then(
                            Commands.argument(ARG_PLAYER, EntityArgument.player())
                                .executes(
                                    context ->
                                        listPlayerWarpsAdmin(
                                            context.getSource(),
                                            EntityArgument.getPlayer(context, ARG_PLAYER)))))
                .then(
                    Commands.literal("delete")
                        .then(
                            Commands.argument(ARG_WARP_UUID, UuidArgument.uuid())
                                .executes(
                                    context ->
                                        deleteWarpByUuid(
                                            context.getSource(),
                                            UuidArgument.getUuid(context, ARG_WARP_UUID)))))
                .then(
                    Commands.literal("cleanup")
                        .executes(context -> cleanupExpiredWarps(context.getSource()))));
  }

  private static int createWarp(
      final CommandSourceStack source,
      final String warpName,
      final String description,
      final WarpType warpType,
      final ServerPlayer serverPlayer) {

    if (!WarpConfig.isWarpSystemEnabled()) {
      return sendFailureMessage(source, "Warp system is disabled on this server.");
    }

    if (!WarpConfig.isValidWarpName(warpName)) {
      if (WarpConfig.isNameReserved(warpName)) {
        return sendFailureMessage(
            source, "Warp name '" + warpName + "' is reserved and cannot be used.");
      }
      return sendFailureMessage(
          source,
          "Invalid warp name. Must be 3-16 characters, alphanumeric with underscore/hyphen only.");
    }

    if (!WarpConfig.isValidDescription(description)) {
      return sendFailureMessage(
          source,
          "Description too long. Maximum "
              + WarpConfig.MAX_WARP_DESCRIPTION_LENGTH
              + " characters.");
    }

    if (warpType == WarpType.PUBLIC
        && WarpConfig.REQUIRE_PERMISSION_FOR_PUBLIC_WARPS
        && !source.hasPermission(Commands.LEVEL_MODERATORS)) {
      return sendFailureMessage(source, "You don't have permission to create public warps.");
    }

    if (!WarpManager.canCreateWarp(serverPlayer.getUUID(), warpType)) {
      int maxWarps =
          warpType == WarpType.PRIVATE
              ? WarpConfig.MAX_PRIVATE_WARPS_PER_PLAYER
              : WarpConfig.MAX_PUBLIC_WARPS_PER_PLAYER;
      return sendFailureMessage(
          source,
          "You have reached the maximum number of "
              + warpType.getName()
              + " warps ("
              + maxWarps
              + ").");
    }

    boolean success =
        WarpManager.createWarp(
            warpName,
            description,
            serverPlayer.getUUID(),
            serverPlayer.level().dimension(),
            serverPlayer.blockPosition(),
            serverPlayer.getYRot(),
            serverPlayer.getXRot(),
            warpType);

    if (!success) {
      return sendFailureMessage(source, "Failed to create warp. Name might already be taken.");
    }

    String message = warpType.getDisplayName() + " warp '" + warpName + "' created successfully!";
    return sendSuccessMessage(source, message, ChatFormatting.GREEN);
  }

  private static int deleteWarp(
      final CommandSourceStack source,
      final String warpName,
      final ServerPlayer serverPlayer,
      final boolean isModeratorAction) {

    Optional<WarpData> warpOptional = WarpManager.getWarp(warpName, serverPlayer.getUUID());
    if (warpOptional.isEmpty()) {
      return sendFailureMessage(source, WARP_NOT_FOUND + warpName);
    }

    WarpData warpData = warpOptional.get();
    if (!isModeratorAction && !warpData.owner().equals(serverPlayer.getUUID())) {
      return sendFailureMessage(source, "You can only delete your own warps.");
    }

    boolean success =
        WarpManager.deleteWarp(warpData.uuid(), serverPlayer.getUUID(), isModeratorAction);
    if (!success) {
      return sendFailureMessage(source, "Failed to delete warp.");
    }

    long delayMinutes = WarpConfig.WARP_DELETION_DELAY / (60 * 1000);
    String message =
        "Warp '"
            + warpName
            + "' has been disabled and will be permanently deleted in "
            + delayMinutes
            + " minutes.";
    return sendSuccessMessage(source, message, ChatFormatting.YELLOW);
  }

  private static int deleteWarpByUuid(final CommandSourceStack source, final UUID warpUuid) {
    Optional<WarpData> warpOptional = WarpManager.getWarp(warpUuid);
    if (warpOptional.isEmpty()) {
      return sendFailureMessage(source, "Warp not found with UUID: " + warpUuid);
    }

    WarpData warpData = warpOptional.get();
    boolean success =
        WarpManager.deleteWarp(
            warpUuid, UUID.randomUUID(), true); // Using random UUID for moderator action

    if (!success) {
      return sendFailureMessage(source, "Failed to delete warp.");
    }

    long delayMinutes = WarpConfig.WARP_DELETION_DELAY / (60 * 1000);
    String message =
        "Warp '"
            + warpData.name()
            + "' has been disabled and will be permanently deleted in "
            + delayMinutes
            + " minutes.";
    return sendSuccessMessage(source, message, ChatFormatting.YELLOW);
  }

  private static int teleportToPrivateWarp(
      final CommandSourceStack source, final String warpName, final ServerPlayer serverPlayer) {

    if (!WarpConfig.isWarpSystemEnabled()) {
      return sendFailureMessage(source, "Warp system is disabled on this server.");
    }

    if (WarpManager.isPlayerOnCooldown(serverPlayer.getUUID())) {
      long remainingMs = WarpManager.getRemainingCooldown(serverPlayer.getUUID());
      long remainingSecs = remainingMs / 1000;
      return sendFailureMessage(
          source, "Teleport cooldown active. Wait " + remainingSecs + " more seconds.");
    }

    // Get private warp specifically
    Optional<WarpData> warpOptional =
        WarpManager.getPrivateWarpsForPlayer(serverPlayer.getUUID()).stream()
            .filter(warpData -> warpData.name().equalsIgnoreCase(warpName))
            .findFirst();

    if (warpOptional.isEmpty()) {
      return sendFailureMessage(source, "Private warp not found: " + warpName);
    }

    WarpData warpData = warpOptional.get();
    boolean success = WarpManager.teleportPlayerToWarp(serverPlayer, warpData);

    if (!success) {
      return sendFailureMessage(source, "Failed to teleport to private warp '" + warpName + "'.");
    }

    return sendSuccessMessage(
        source, "Teleported to private warp '" + warpName + "'.", ChatFormatting.GREEN);
  }

  private static int teleportToPublicWarp(
      final CommandSourceStack source, final String warpName, final ServerPlayer serverPlayer) {

    if (!WarpConfig.isWarpSystemEnabled()) {
      return sendFailureMessage(source, "Warp system is disabled on this server.");
    }

    if (WarpManager.isPlayerOnCooldown(serverPlayer.getUUID())) {
      long remainingMs = WarpManager.getRemainingCooldown(serverPlayer.getUUID());
      long remainingSecs = remainingMs / 1000;
      return sendFailureMessage(
          source, "Teleport cooldown active. Wait " + remainingSecs + " more seconds.");
    }

    // Get public warp specifically
    Optional<WarpData> warpOptional =
        WarpManager.getPublicWarps().stream()
            .filter(warpData -> warpData.name().equalsIgnoreCase(warpName))
            .findFirst();

    if (warpOptional.isEmpty()) {
      return sendFailureMessage(source, "Public warp not found: " + warpName);
    }

    WarpData warpData = warpOptional.get();
    boolean success = WarpManager.teleportPlayerToWarp(serverPlayer, warpData);

    if (!success) {
      return sendFailureMessage(source, "Failed to teleport to public warp '" + warpName + "'.");
    }

    return sendSuccessMessage(
        source, "Teleported to public warp '" + warpName + "'.", ChatFormatting.GREEN);
  }

  private static int teleportPlayerToWarp(
      final CommandSourceStack source, final String warpName, final ServerPlayer targetPlayer) {

    if (!WarpConfig.isWarpSystemEnabled()) {
      return sendFailureMessage(source, "Warp system is disabled on this server.");
    }

    if (WarpManager.isPlayerOnCooldown(targetPlayer.getUUID())) {
      long remainingMs = WarpManager.getRemainingCooldown(targetPlayer.getUUID());
      long remainingSecs = remainingMs / 1000;
      return sendFailureMessage(
          source, "Player is on teleport cooldown. Wait " + remainingSecs + " more seconds.");
    }

    Optional<WarpData> warpOptional = WarpManager.getWarp(warpName, targetPlayer.getUUID());
    if (warpOptional.isEmpty()) {
      return sendFailureMessage(source, WARP_NOT_FOUND + warpName);
    }

    WarpData warpData = warpOptional.get();
    boolean success = WarpManager.teleportPlayerToWarp(targetPlayer, warpData);

    if (!success) {
      return sendFailureMessage(source, "Failed to teleport player to warp '" + warpName + "'.");
    }

    String message =
        targetPlayer.equals(source.getEntity())
            ? "Teleported to warp '" + warpName + "'."
            : "Teleported " + targetPlayer.getName().getString() + " to warp '" + warpName + "'.";

    return sendSuccessMessage(source, message, ChatFormatting.GREEN);
  }

  private static int listOwnWarps(
      final CommandSourceStack source, final ServerPlayer serverPlayer) {
    List<WarpData> privateWarps = WarpManager.getPrivateWarpsForPlayer(serverPlayer.getUUID());
    List<WarpData> ownedPublicWarps =
        WarpManager.getPublicWarps().stream()
            .filter(warpData -> warpData.owner().equals(serverPlayer.getUUID()))
            .toList();

    if (privateWarps.isEmpty() && ownedPublicWarps.isEmpty()) {
      return sendFailureMessage(source, "You have no warps.");
    }

    source.sendSuccess(
        () -> Component.literal("=== Your Warps ===").withStyle(ChatFormatting.GOLD), false);

    if (!privateWarps.isEmpty()) {
      source.sendSuccess(
          () -> Component.literal("Private Warps:").withStyle(ChatFormatting.AQUA), false);
      for (WarpData warpData : privateWarps) {
        displayWarpListEntry(source, warpData);
      }
    }

    if (!ownedPublicWarps.isEmpty()) {
      source.sendSuccess(
          () -> Component.literal("Public Warps:").withStyle(ChatFormatting.GREEN), false);
      for (WarpData warpData : ownedPublicWarps) {
        displayWarpListEntry(source, warpData);
      }
    }

    return SINGLE_SUCCESS;
  }

  private static int listPrivateWarps(
      final CommandSourceStack source, final ServerPlayer serverPlayer) {
    List<WarpData> privateWarps = WarpManager.getPrivateWarpsForPlayer(serverPlayer.getUUID());
    if (privateWarps.isEmpty()) {
      return sendFailureMessage(source, "You have no private warps.");
    }

    source.sendSuccess(
        () -> Component.literal("=== Your Private Warps ===").withStyle(ChatFormatting.AQUA),
        false);

    for (WarpData warpData : privateWarps) {
      displayWarpListEntry(source, warpData);
    }

    return SINGLE_SUCCESS;
  }

  private static int listPublicWarps(final CommandSourceStack source) {
    List<WarpData> publicWarps = WarpManager.getPublicWarps();
    if (publicWarps.isEmpty()) {
      return sendFailureMessage(source, "No public warps available.");
    }

    source.sendSuccess(
        () -> Component.literal("=== Public Warps ===").withStyle(ChatFormatting.GREEN), false);

    for (WarpData warpData : publicWarps) {
      displayWarpListEntry(source, warpData);
    }

    return SINGLE_SUCCESS;
  }

  private static void displayWarpListEntry(
      final CommandSourceStack source, final WarpData warpData) {
    Component warpInfo =
        Component.literal("• ")
            .withStyle(ChatFormatting.GRAY)
            .append(Component.literal(warpData.name()).withStyle(ChatFormatting.WHITE));

    if (!warpData.description().isEmpty()) {
      warpInfo =
          warpInfo
              .copy()
              .append(
                  Component.literal(" - " + warpData.description()).withStyle(ChatFormatting.GRAY));
    }

    final Component finalWarpInfo = warpInfo;
    source.sendSuccess(() -> finalWarpInfo, false);
  }

  private static int listPlayerWarpsAdmin(
      final CommandSourceStack source, final ServerPlayer targetPlayer) {
    List<WarpData> playerWarps = WarpManager.getAllWarpsForPlayer(targetPlayer.getUUID());
    if (playerWarps.isEmpty()) {
      return sendFailureMessage(
          source, "Player " + targetPlayer.getName().getString() + " has no warps.");
    }

    source.sendSuccess(
        () ->
            Component.literal("=== Warps for " + targetPlayer.getName().getString() + " ===")
                .withStyle(ChatFormatting.GOLD),
        false);

    for (WarpData warpData : playerWarps) {
      ChatFormatting statusColor = warpData.enabled() ? ChatFormatting.GREEN : ChatFormatting.RED;
      String status = warpData.enabled() ? "enabled" : "disabled";

      Component warpInfo =
          Component.literal("• ")
              .withStyle(ChatFormatting.GRAY)
              .append(Component.literal(warpData.name()).withStyle(ChatFormatting.AQUA))
              .append(
                  Component.literal(" (" + warpData.type().getDisplayName() + ")")
                      .withStyle(ChatFormatting.YELLOW))
              .append(Component.literal(" [" + status + "]").withStyle(statusColor))
              .append(
                  Component.literal(" UUID: " + warpData.uuid().toString().substring(0, 8) + "...")
                      .withStyle(ChatFormatting.DARK_GRAY));

      source.sendSuccess(() -> warpInfo, false);
    }

    return SINGLE_SUCCESS;
  }

  private static int showWarpInfo(
      final CommandSourceStack source, final String warpName, final ServerPlayer serverPlayer) {
    Optional<WarpData> warpOptional = WarpManager.getWarp(warpName, serverPlayer.getUUID());
    if (warpOptional.isEmpty()) {
      return sendFailureMessage(source, WARP_NOT_FOUND + warpName);
    }

    WarpData warpData = warpOptional.get();

    source.sendSuccess(
        () -> Component.literal("=== Warp Information ===").withStyle(ChatFormatting.GOLD), false);

    source.sendSuccess(
        () ->
            Component.literal("Name: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(warpData.name()).withStyle(ChatFormatting.AQUA)),
        false);

    source.sendSuccess(
        () ->
            Component.literal("Type: ")
                .withStyle(ChatFormatting.GRAY)
                .append(
                    Component.literal(warpData.type().getDisplayName())
                        .withStyle(ChatFormatting.YELLOW)),
        false);

    if (!warpData.description().isEmpty()) {
      source.sendSuccess(
          () ->
              Component.literal("Description: ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(
                      Component.literal(warpData.description()).withStyle(ChatFormatting.WHITE)),
          false);
    }

    source.sendSuccess(
        () ->
            Component.literal("Dimension: ")
                .withStyle(ChatFormatting.GRAY)
                .append(
                    Component.literal(warpData.dimension().location().toString())
                        .withStyle(ChatFormatting.GREEN)),
        false);

    source.sendSuccess(
        () ->
            Component.literal("Position: ")
                .withStyle(ChatFormatting.GRAY)
                .append(
                    Component.literal(warpData.position().toShortString())
                        .withStyle(ChatFormatting.WHITE)),
        false);

    return SINGLE_SUCCESS;
  }

  private static int cleanupExpiredWarps(final CommandSourceStack source) {
    WarpManager.cleanupExpiredWarps();
    return sendSuccessMessage(source, "Expired warps have been cleaned up.", ChatFormatting.GREEN);
  }
}
