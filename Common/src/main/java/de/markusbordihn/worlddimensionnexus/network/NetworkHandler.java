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

  public static void sendDelayedBlockUpdatePacket(
      final ServerLevel serverLevel, final ServerPlayer serverPlayer, final BlockPos blockPos) {
    serverLevel
        .getServer()
        .tell(
            new TickTask(
                serverLevel.getServer().getTickCount() + 1,
                () ->
                    sendBlockUpdatePacket(
                        serverPlayer, blockPos, serverLevel.getBlockState(blockPos))));
  }

  public static void sendBlockUpdatePacket(
      final ServerPlayer serverPlayer, final BlockPos blockPos, final BlockState blockState) {
    serverPlayer.connection.send(new ClientboundBlockUpdatePacket(blockPos, blockState));
  }

  public static void sendChunkUpdateForHotInjection(
      final ServerPlayer serverPlayer,
      final ServerLevel serverLevel,
      final int chunkX,
      final int chunkZ) {
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
    BlockPos playerPos = serverPlayer.blockPosition();
    int centerChunkX = playerPos.getX() >> 4;
    int centerChunkZ = playerPos.getZ() >> 4;

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
    if (serverLevel == null || serverLevel.getServer() == null) {
      return;
    }

    final var minecraftServer = serverLevel.getServer();
    minecraftServer.execute(
        () -> {
          minecraftServer
              .getPlayerList()
              .getPlayers()
              .forEach(
                  player -> {
                    player.connection.send(
                        new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));

                    if (player.level().dimension().equals(serverLevel.dimension())) {
                      syncDimensionChunks(player, serverLevel, 3);
                    }
                  });

          minecraftServer.tell(
              new TickTask(
                  minecraftServer.getTickCount() + 5,
                  () ->
                      minecraftServer
                          .getPlayerList()
                          .getPlayers()
                          .forEach(
                              player -> {
                                if (player.level().dimension().equals(serverLevel.dimension())) {
                                  BlockPos playerPos = player.blockPosition();
                                  for (int x = -16; x <= 16; x += 8) {
                                    for (int z = -16; z <= 16; z += 8) {
                                      BlockPos updatePos = playerPos.offset(x, 0, z);
                                      sendBlockUpdatePacket(
                                          player, updatePos, serverLevel.getBlockState(updatePos));
                                    }
                                  }
                                }
                              })));
        });
  }

  public static void sendBlockEntityUpdate(
      final ServerPlayer serverPlayer, final ServerLevel serverLevel, final BlockPos blockPos) {
    var blockEntity = serverLevel.getBlockEntity(blockPos);
    if (blockEntity != null) {
      var packet = blockEntity.getUpdatePacket();
      if (packet != null) {
        serverPlayer.connection.send(packet);
      }
    }
  }

  public static void syncSkyblockSpawnChunk(
      final ServerPlayer serverPlayer, final ServerLevel serverLevel) {
    BlockPos chestPos = new BlockPos(10, 65, 8);

    serverLevel
        .getServer()
        .tell(
            new TickTask(
                serverLevel.getServer().getTickCount() + 2,
                () -> {
                  var blockEntity = serverLevel.getBlockEntity(chestPos);
                  if (blockEntity != null) {
                    sendBlockEntityUpdate(serverPlayer, serverLevel, chestPos);
                    sendBlockUpdatePacket(
                        serverPlayer, chestPos, serverLevel.getBlockState(chestPos));
                  }
                }));
  }
}
