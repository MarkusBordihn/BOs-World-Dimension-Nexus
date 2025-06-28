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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

public class NetworkHandler {

  public static void sendDelayedBlockUpdatePacket(
      final ServerLevel serverLevel, final ServerPlayer serverPlayer, final BlockPos blockPos) {
    serverLevel
        .getServer()
        .tell(
            new net.minecraft.server.TickTask(
                serverLevel.getServer().getTickCount() + 1,
                () ->
                    sendBlockUpdatePacket(
                        serverPlayer, blockPos, serverLevel.getBlockState(blockPos))));
  }

  public static void sendBlockUpdatePacket(
      final ServerPlayer serverPlayer, final BlockPos blockPos, final BlockState blockState) {
    serverPlayer.connection.send(new ClientboundBlockUpdatePacket(blockPos, blockState));
  }

  /**
   * Sends block entity data for positions that need immediate synchronization. Used for dynamically
   * generated chunks with block entities like chests.
   */
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

  /**
   * Synchronizes Skyblock spawn chunk block entities for a player. This is called only when
   * teleporting to ensure block entities are visible.
   */
  public static void syncSkyblockSpawnChunk(
      final ServerPlayer serverPlayer, final ServerLevel serverLevel) {
    BlockPos chestPos = new BlockPos(10, 65, 8);

    serverLevel
        .getServer()
        .tell(
            new net.minecraft.server.TickTask(
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
