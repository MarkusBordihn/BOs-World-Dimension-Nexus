package de.markusbordihn.worlddimensionnexus.player;

import de.markusbordihn.worlddimensionnexus.block.PortalBlockManager;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalInfoData;
import de.markusbordihn.worlddimensionnexus.portal.PortalManager;
import de.markusbordihn.worlddimensionnexus.portal.PortalTargetManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PlayerEvents {

  public static void handlePlayerPostTickEvent(ServerPlayer serverPlayer) {
    // Check block state for performance reasons.
    BlockState blockState = serverPlayer.getInBlockState();
    if (blockState.isAir()) {
      return;
    }

    // Check block itself for performance reasons.
    Block block = blockState.getBlock();
    if (block == Blocks.WATER
        || block == Blocks.LAVA
        || block == Blocks.FIRE
        || block == Blocks.SCAFFOLDING
        || block == Blocks.BAMBOO
        || block == Blocks.BAMBOO_SAPLING
        || block == Blocks.BAMBOO_BLOCK) {
      return;
    }

    // Last check make sure the block is a relevant inner portal block.
    ServerLevel serverLevel = serverPlayer.serverLevel();
    if (PortalBlockManager.isRelevantInnerPortalBlock(block, blockState)) {
      PortalInfoData portalInfo = PortalManager.getPortal(serverLevel, serverPlayer.getOnPos());
      if (portalInfo != null) {
        PortalTargetManager.teleportPlayer(serverLevel, serverPlayer, portalInfo);
      }
    }
  }
}
