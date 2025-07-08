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

package de.markusbordihn.worlddimensionnexus.teleport;

import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.data.teleport.TeleportLocation;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.gamemode.GameModeHistory;
import de.markusbordihn.worlddimensionnexus.network.NetworkHandler;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TeleportManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Teleport Manager");
  private static final Map<UUID, CountdownTeleport> countdownTeleports = new ConcurrentHashMap<>();
  private static final double MOVEMENT_THRESHOLD = 0.5;

  private TeleportManager() {}

  public static void processCountdownTeleports() {
    if (countdownTeleports.isEmpty()) {
      return;
    }

    for (Map.Entry<UUID, CountdownTeleport> entry : countdownTeleports.entrySet()) {
      UUID playerId = entry.getKey();
      CountdownTeleport countdown = entry.getValue();

      if (countdown.hasPlayerMoved()) {
        countdownTeleports.remove(playerId);
        sendMessage(countdown.player, "Teleport cancelled: You moved!", ChatFormatting.RED);
        continue;
      }

      countdown.remainingSeconds--;
      if (countdown.remainingSeconds <= 0) {
        countdownTeleports.remove(playerId);
        executeCountdownTeleport(countdown);
      } else {
        sendMessage(
            countdown.player,
            String.format(
                "Teleport to %s in %d seconds...",
                countdown.targetDimension, countdown.remainingSeconds),
            ChatFormatting.YELLOW);
      }
    }
  }

  public static boolean startCountdownTeleport(
      final ServerPlayer player,
      final ResourceKey<Level> dimensionKey,
      final int countdownSeconds,
      final boolean enableMovementDetection) {
    UUID playerId = player.getUUID();

    if (countdownTeleports.containsKey(playerId)) {
      log.debug(
          "Player {} already has a countdown teleport in progress", player.getName().getString());
      return false;
    }

    ServerLevel targetLevel = player.server.getLevel(dimensionKey);
    if (targetLevel == null) {
      log.warn("Dimension {} does not exist for countdown teleport", dimensionKey.location());
      sendMessage(player, "Target dimension is not available.", ChatFormatting.RED);
      return false;
    }

    // If countdown is 0 or negative, teleport immediately
    if (countdownSeconds <= 0) {
      return safeTeleportToDimension(player, dimensionKey);
    }

    CountdownTeleport countdown =
        new CountdownTeleport(player, dimensionKey, countdownSeconds, enableMovementDetection);
    countdownTeleports.put(playerId, countdown);

    String movementWarning = enableMovementDetection ? "Please stand still!" : "";
    sendMessage(
        player,
        String.format(
            "Teleport to %s in %d seconds. %s",
            dimensionKey.location(), countdownSeconds, movementWarning),
        ChatFormatting.YELLOW);

    return true;
  }

  private static void executeCountdownTeleport(final CountdownTeleport countdown) {
    if (safeTeleportToDimension(countdown.player, countdown.targetDimensionKey)) {
      sendMessage(
          countdown.player,
          String.format("Successfully teleported to %s!", countdown.targetDimensionKey.location()),
          ChatFormatting.GREEN);
    } else {
      sendMessage(
          countdown.player,
          "Failed to teleport to dimension. Please try again later.",
          ChatFormatting.RED);
    }
  }

  private static void sendMessage(
      final ServerPlayer player, final String message, final ChatFormatting color) {
    Component component = Component.literal(message).withStyle(color);
    player.sendSystemMessage(component);
  }

  public static boolean safeTeleportToDimension(
      final ServerPlayer serverPlayer, final ResourceKey<Level> dimensionKey) {
    ServerLevel targetLevel = serverPlayer.server.getLevel(dimensionKey);
    if (targetLevel == null) {
      return false;
    }

    recordCurrentLocation(serverPlayer);
    BlockPos teleportPos = getSafeTeleportLocation(serverPlayer, targetLevel, dimensionKey);
    executePlayerTeleport(serverPlayer, targetLevel, teleportPos);
    handlePostTeleportActions(targetLevel, dimensionKey);
    handleGameTypeChange(serverPlayer, targetLevel);

    return true;
  }

  public static boolean teleportBack(final ServerPlayer serverPlayer) {
    TeleportLocation lastLocation = TeleportHistory.popLastLocation(serverPlayer.getUUID());
    if (lastLocation == null) {
      return false;
    }

    ServerLevel targetLevel = serverPlayer.server.getLevel(lastLocation.dimension());
    if (targetLevel == null) {
      return false;
    }

    executePlayerTeleport(serverPlayer, targetLevel, lastLocation);
    GameModeHistory.restoreGameTypeFromHistory(serverPlayer, lastLocation.dimension());
    return true;
  }

  public static boolean teleportToDimensionWithoutHistory(
      final ServerPlayer serverPlayer, final ResourceKey<Level> dimensionKey) {
    recordCurrentLocation(serverPlayer);

    ServerLevel targetLevel = serverPlayer.server.getLevel(dimensionKey);
    if (targetLevel == null) {
      return false;
    }

    BlockPos teleportPos = getSafeTeleportLocation(serverPlayer, targetLevel, dimensionKey);
    executePlayerTeleport(serverPlayer, targetLevel, teleportPos);
    handlePostTeleportActions(targetLevel, dimensionKey);
    handleGameTypeChange(serverPlayer, targetLevel);
    return true;
  }

  private static void executePlayerTeleport(
      final ServerPlayer serverPlayer, final ServerLevel targetLevel, final BlockPos position) {
    serverPlayer.teleportTo(
        targetLevel,
        position.getX() + 0.5,
        position.getY(),
        position.getZ() + 0.5,
        serverPlayer.getYRot(),
        serverPlayer.getXRot());
  }

  private static void executePlayerTeleport(
      final ServerPlayer serverPlayer,
      final ServerLevel targetLevel,
      final TeleportLocation location) {
    serverPlayer.teleportTo(
        targetLevel,
        location.position().getX() + 0.5,
        location.position().getY(),
        location.position().getZ() + 0.5,
        location.yRot(),
        location.xRot());
  }

  private static void handlePostTeleportActions(
      final ServerLevel targetLevel, final ResourceKey<Level> dimensionKey) {
    DimensionInfoData dimensionInfo =
        DimensionManager.getDimensionInfoData(dimensionKey.location());

    if (dimensionInfo != null
        && dimensionInfo.chunkGeneratorType() == ChunkGeneratorType.SKYBLOCK) {
      NetworkHandler.syncSkyblockSpawnChunk(null, targetLevel);
    }
  }

  private static BlockPos getSafeTeleportLocation(
      final ServerPlayer serverPlayer,
      final ServerLevel targetLevel,
      final ResourceKey<Level> dimensionKey) {
    DimensionInfoData dimensionInfo =
        DimensionManager.getDimensionInfoData(dimensionKey.location());
    return getSafeTeleportLocationInternal(serverPlayer, targetLevel, dimensionInfo);
  }

  private static BlockPos getSafeTeleportLocationInternal(
      final ServerPlayer serverPlayer,
      final ServerLevel targetLevel,
      final DimensionInfoData dimensionInfo) {
    // First check if dimension has a custom spawn point set
    if (dimensionInfo != null && dimensionInfo.spawnPoint() != null) {
      BlockPos customSpawn = dimensionInfo.spawnPoint();
      if (isSafeLocation(targetLevel, customSpawn)) {
        return customSpawn;
      } else {
        // If custom spawn is not safe, try to find a safe location near it
        return findSafeLocationNear(targetLevel, customSpawn);
      }
    }

    if (dimensionInfo != null
        && dimensionInfo.chunkGeneratorType() == ChunkGeneratorType.SKYBLOCK) {
      return getSkyblockSpawnLocation();
    }

    // Try world spawn
    BlockPos worldSpawn = targetLevel.getSharedSpawnPos();
    if (isSafeLocation(targetLevel, worldSpawn)) {
      return worldSpawn;
    }

    // Last resort: find safe location near origin
    return findSafeLocationNear(targetLevel, new BlockPos(0, 64, 0));
  }

  private static BlockPos getSkyblockSpawnLocation() {
    return new BlockPos(8, 65, 8);
  }

  private static boolean isSafeLocation(final ServerLevel level, final BlockPos position) {
    BlockState groundState = level.getBlockState(position.below());
    if (groundState.isAir()) {
      return false;
    }

    BlockState spawnState = level.getBlockState(position);
    BlockState aboveState = level.getBlockState(position.above());

    return spawnState.isAir() && aboveState.isAir();
  }

  private static BlockPos findSafeLocationNear(final ServerLevel level, final BlockPos center) {
    if (isSafeLocation(level, center)) {
      return center;
    }

    BlockPos safePosition = searchForSafeLocation(level, center);
    return safePosition != null ? safePosition : createSafePlatform(level, center);
  }

  private static BlockPos searchForSafeLocation(final ServerLevel level, final BlockPos center) {
    for (int radius = 1; radius <= 16; radius++) {
      BlockPos foundPosition = searchAtRadius(level, center, radius);
      if (foundPosition != null) {
        return foundPosition;
      }
    }
    return null;
  }

  private static BlockPos searchAtRadius(
      final ServerLevel level, final BlockPos center, final int radius) {
    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {
        if (Math.abs(x) != radius && Math.abs(z) != radius) {
          continue;
        }

        BlockPos checkPosition = center.offset(x, 0, z);
        BlockPos safePosition = findSafeYLevel(level, checkPosition);
        if (safePosition != null) {
          return safePosition;
        }
      }
    }
    return null;
  }

  private static BlockPos findSafeYLevel(final ServerLevel level, final BlockPos basePosition) {
    for (int yOffset = -5; yOffset <= 10; yOffset++) {
      BlockPos testPosition = basePosition.offset(0, yOffset, 0);
      if (isValidYLevel(level, testPosition) && isSafeLocation(level, testPosition)) {
        return testPosition;
      }
    }
    return null;
  }

  private static boolean isValidYLevel(final ServerLevel level, final BlockPos position) {
    return position.getY() >= level.getMinBuildHeight()
        && position.getY() <= level.getMaxBuildHeight() - 2;
  }

  private static BlockPos createSafePlatform(final ServerLevel level, final BlockPos center) {
    BlockPos safePosition =
        new BlockPos(center.getX(), Math.max(64, level.getMinBuildHeight() + 10), center.getZ());

    placePlatformBlocks(level, safePosition);
    clearSpawnArea(level, safePosition);

    return safePosition;
  }

  private static void placePlatformBlocks(final ServerLevel level, final BlockPos spawnPosition) {
    BlockPos platform = spawnPosition.below();
    BlockState stoneState = Blocks.STONE.defaultBlockState();

    level.setBlock(platform, stoneState, 3);
    level.setBlock(platform.north(), stoneState, 3);
    level.setBlock(platform.south(), stoneState, 3);
    level.setBlock(platform.east(), stoneState, 3);
    level.setBlock(platform.west(), stoneState, 3);
  }

  private static void clearSpawnArea(final ServerLevel level, final BlockPos spawnPosition) {
    BlockState airState = Blocks.AIR.defaultBlockState();

    level.setBlock(spawnPosition, airState, 3);
    level.setBlock(spawnPosition.above(), airState, 3);
  }

  private static void recordCurrentLocation(final ServerPlayer serverPlayer) {
    GameType currentGameType = serverPlayer.gameMode.getGameModeForPlayer();

    TeleportHistory.recordLocation(
        serverPlayer.getUUID(),
        serverPlayer.level().dimension(),
        serverPlayer.blockPosition(),
        serverPlayer.getYRot(),
        serverPlayer.getXRot(),
        currentGameType);
  }

  private static void handleGameTypeChange(
      final ServerPlayer serverPlayer, final ServerLevel targetLevel) {
    DimensionInfoData dimensionInfo =
        DimensionManager.getDimensionInfoData(targetLevel.dimension().location());
    if (dimensionInfo != null) {
      GameModeHistory.applyGameTypeForPlayer(serverPlayer, dimensionInfo.gameType());
    }
  }

  private static class CountdownTeleport {
    final String targetDimension;
    final ResourceKey<Level> targetDimensionKey;
    final Vec3 startPosition;
    final ServerPlayer player;
    final boolean enableMovementDetection;
    int remainingSeconds;

    CountdownTeleport(
        final ServerPlayer player,
        final ResourceKey<Level> targetDimensionKey,
        final int countdownSeconds,
        final boolean enableMovementDetection) {
      this.player = player;
      this.targetDimension = targetDimensionKey.location().toString();
      this.targetDimensionKey = targetDimensionKey;
      this.startPosition = player.position();
      this.remainingSeconds = countdownSeconds;
      this.enableMovementDetection = enableMovementDetection;
    }

    boolean hasPlayerMoved() {
      return enableMovementDetection
          && player.position().distanceTo(startPosition) > MOVEMENT_THRESHOLD;
    }
  }
}
