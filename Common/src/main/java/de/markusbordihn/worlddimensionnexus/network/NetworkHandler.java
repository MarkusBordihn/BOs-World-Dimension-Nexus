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

package de.markusbordihn.worlddimensionnexus.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public class NetworkHandler {

  // Constants for better readability and maintainability
  private static final int DELAYED_UPDATE_TICKS = 1;
  private static final int DIMENSION_SYNC_DELAY_TICKS = 5;
  private static final int SKYBLOCK_SPAWN_DELAY_TICKS = 2;
  private static final int DEFAULT_SYNC_RADIUS = 3;
  private static final int BLOCK_UPDATE_RANGE = 16;
  private static final int BLOCK_UPDATE_STEP = 8;
  private static final int LEVEL_EVENT_SOUND_ID = 1032;

  // Skyblock spawn chest coordinates
  private static final BlockPos SKYBLOCK_CHEST_POSITION = new BlockPos(10, 65, 8);

  public static void sendDelayedBlockUpdatePacket(
      final ServerLevel serverLevel, final ServerPlayer serverPlayer, final BlockPos blockPos) {
    if (!isValidForNetworkOperation(serverLevel, serverPlayer, blockPos)) {
      return;
    }

    serverLevel
        .getServer()
        .tell(
            new TickTask(
                serverLevel.getServer().getTickCount() + DELAYED_UPDATE_TICKS,
                () ->
                    sendBlockUpdatePacket(
                        serverPlayer, blockPos, serverLevel.getBlockState(blockPos))));
  }

  public static void sendBlockUpdatePacket(
      final ServerPlayer serverPlayer, final BlockPos blockPos, final BlockState blockState) {
    if (!isValidForNetworkOperation(serverPlayer, blockPos, blockState)) {
      return;
    }
    serverPlayer.connection.send(new ClientboundBlockUpdatePacket(blockPos, blockState));
  }

  public static void sendChunkUpdateForHotInjection(
      final ServerPlayer serverPlayer,
      final ServerLevel serverLevel,
      final int chunkX,
      final int chunkZ) {
    if (!isValidForNetworkOperation(serverLevel, serverPlayer)) {
      return;
    }

    serverLevel
        .getServer()
        .execute(
            () -> {
              var chunkAccess = serverLevel.getChunk(chunkX, chunkZ);
              if (chunkAccess instanceof LevelChunk levelChunk) {
                serverPlayer.connection.send(
                    new ClientboundLevelChunkWithLightPacket(
                        levelChunk, serverLevel.getLightEngine(), null, null));
              }
            });
  }

  public static void syncDimensionChunks(
      final ServerPlayer serverPlayer, final ServerLevel serverLevel, final int radius) {
    if (!isValidForNetworkOperation(serverLevel, serverPlayer)) {
      return;
    }

    BlockPos playerPosition = serverPlayer.blockPosition();
    int centerChunkX = playerPosition.getX() >> 4;
    int centerChunkZ = playerPosition.getZ() >> 4;

    serverLevel
        .getServer()
        .execute(
            () -> {
              for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                  sendChunkUpdateForHotInjection(
                      serverPlayer, serverLevel, centerChunkX + x, centerChunkZ + z);
                }
              }
            });
  }

  public static void syncDimensionToClients(final ServerLevel serverLevel) {
    if (!isValidForNetworkOperation(serverLevel)) {
      return;
    }

    final var minecraftServer = serverLevel.getServer();
    minecraftServer.execute(
        () -> {
          sendLevelEventToAllPlayers(minecraftServer);
          syncChunksForPlayersInDimension(serverLevel);
          scheduleDelayedBlockUpdates(serverLevel);
        });
  }

  public static void sendBlockEntityUpdate(
      final ServerPlayer serverPlayer, final ServerLevel serverLevel, final BlockPos blockPos) {
    if (!isValidForNetworkOperation(serverLevel, serverPlayer, blockPos)) {
      return;
    }

    var blockEntity = serverLevel.getBlockEntity(blockPos);
    if (blockEntity != null) {
      var updatePacket = blockEntity.getUpdatePacket();
      if (updatePacket != null) {
        serverPlayer.connection.send(updatePacket);
      }
    }
  }

  public static void syncSkyblockSpawnChunk(
      final ServerPlayer serverPlayer, final ServerLevel serverLevel) {
    if (!isValidForNetworkOperation(serverLevel, serverPlayer)) {
      return;
    }

    serverLevel
        .getServer()
        .tell(
            new TickTask(
                serverLevel.getServer().getTickCount() + SKYBLOCK_SPAWN_DELAY_TICKS,
                () -> syncSkyblockChestAtPosition(serverPlayer, serverLevel)));
  }

  // Private helper methods for better code organization and reusability

  private static boolean isValidForNetworkOperation(final Object... objects) {
    for (Object obj : objects) {
      if (obj == null) {
        return false;
      }
    }
    return true;
  }

  private static void sendLevelEventToAllPlayers(
      final net.minecraft.server.MinecraftServer server) {
    server
        .getPlayerList()
        .getPlayers()
        .forEach(
            player ->
                player.connection.send(
                    new ClientboundLevelEventPacket(
                        LEVEL_EVENT_SOUND_ID, BlockPos.ZERO, 0, false)));
  }

  private static void syncChunksForPlayersInDimension(final ServerLevel serverLevel) {
    serverLevel
        .getServer()
        .getPlayerList()
        .getPlayers()
        .forEach(
            player -> {
              if (isPlayerInSameDimension(player, serverLevel)) {
                syncDimensionChunks(player, serverLevel, DEFAULT_SYNC_RADIUS);
              }
            });
  }

  private static void scheduleDelayedBlockUpdates(final ServerLevel serverLevel) {
    final var minecraftServer = serverLevel.getServer();
    minecraftServer.tell(
        new TickTask(
            minecraftServer.getTickCount() + DIMENSION_SYNC_DELAY_TICKS,
            () -> updateBlocksAroundPlayersInDimension(serverLevel)));
  }

  private static void updateBlocksAroundPlayersInDimension(final ServerLevel serverLevel) {
    serverLevel
        .getServer()
        .getPlayerList()
        .getPlayers()
        .forEach(
            player -> {
              if (isPlayerInSameDimension(player, serverLevel)) {
                updateBlocksAroundPlayer(player, serverLevel);
              }
            });
  }

  private static void updateBlocksAroundPlayer(
      final ServerPlayer player, final ServerLevel serverLevel) {
    BlockPos playerPosition = player.blockPosition();
    for (int x = -BLOCK_UPDATE_RANGE; x <= BLOCK_UPDATE_RANGE; x += BLOCK_UPDATE_STEP) {
      for (int z = -BLOCK_UPDATE_RANGE; z <= BLOCK_UPDATE_RANGE; z += BLOCK_UPDATE_STEP) {
        BlockPos updatePosition = playerPosition.offset(x, 0, z);
        sendBlockUpdatePacket(player, updatePosition, serverLevel.getBlockState(updatePosition));
      }
    }
  }

  private static boolean isPlayerInSameDimension(
      final ServerPlayer player, final ServerLevel serverLevel) {
    return player.level().dimension().equals(serverLevel.dimension());
  }

  private static void syncSkyblockChestAtPosition(
      final ServerPlayer serverPlayer, final ServerLevel serverLevel) {
    var blockEntity = serverLevel.getBlockEntity(SKYBLOCK_CHEST_POSITION);
    if (blockEntity != null) {
      sendBlockEntityUpdate(serverPlayer, serverLevel, SKYBLOCK_CHEST_POSITION);
      sendBlockUpdatePacket(
          serverPlayer,
          SKYBLOCK_CHEST_POSITION,
          serverLevel.getBlockState(SKYBLOCK_CHEST_POSITION));
    }
  }
}
