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

package de.markusbordihn.worlddimensionnexus.level;

import de.markusbordihn.worlddimensionnexus.block.PortalBlockManager;
import de.markusbordihn.worlddimensionnexus.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockEvents {

  public static void handleBlockBreak(
      ServerLevel serverLevel,
      BlockPos blockPos,
      ServerPlayer serverPlayer,
      Block block,
      BlockState blockState) {
    // Ignore air blocks.
    if (blockState.isAir()) {
      return;
    }

    // Server-side created dimension are not always synced with the client, so we need to
    // send a block update to the client to ensure the client has the correct block state.
    serverLevel
        .getServer()
        .tell(
            new TickTask(
                serverLevel.getServer().getTickCount() + 1,
                () -> {
                  // ToDo: Send update to all players in the dimension / radius.
                  NetworkHandler.sendBlockUpdatePacket(
                      serverPlayer, blockPos, serverLevel.getBlockState(blockPos));
                }));
  }

  public static void handleBlockPlace(
      ServerLevel serverLevel,
      BlockPos blockPos,
      ServerPlayer serverPlayer,
      Block block,
      BlockState blockState) {
    PortalBlockManager.checkForPotentialPortals(
        serverLevel, blockPos, serverPlayer, block, blockState);
  }
}
