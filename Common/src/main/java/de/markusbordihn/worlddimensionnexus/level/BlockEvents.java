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
import de.markusbordihn.worlddimensionnexus.data.portal.PortalInfoData;
import de.markusbordihn.worlddimensionnexus.network.NetworkHandler;
import de.markusbordihn.worlddimensionnexus.portal.PortalManager;
import net.minecraft.core.BlockPos;
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
    NetworkHandler.sendDelayedBlockUpdatePacket(serverLevel, serverPlayer, blockPos);

    // Check for potential portals blocks.
    if (PortalBlockManager.isRelevantPortalBlock(block, blockState)) {
      PortalInfoData portalInfo = PortalManager.getPortalFrame(serverLevel, blockPos);
      if (portalInfo != null) {

        // Remove inner portal blocks.
        for (BlockPos innerBlock : portalInfo.innerBlocks()) {
          serverLevel.removeBlock(innerBlock, true);
          NetworkHandler.sendDelayedBlockUpdatePacket(serverLevel, serverPlayer, innerBlock);
        }

        PortalManager.removePortal(portalInfo);
      }
    }
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
