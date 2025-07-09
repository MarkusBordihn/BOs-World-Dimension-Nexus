package de.markusbordihn.worlddimensionnexus.player;

import de.markusbordihn.worlddimensionnexus.block.PortalBlockManager;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalInfoData;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.portal.PortalManager;
import de.markusbordihn.worlddimensionnexus.portal.PortalTargetManager;
import de.markusbordihn.worlddimensionnexus.teleport.AutoTeleportManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PlayerEvents {

  public static void handlePlayerLoginEvent(final ServerPlayer serverPlayer) {
    AutoTeleportManager.handlePlayerLogin(serverPlayer);
  }

  public static void handlePlayerLogoutEvent(final ServerPlayer serverPlayer) {
    // Unused, but can be implemented if needed in the future.
  }

  public static void handlePlayerPostTickEvent(final ServerPlayer serverPlayer) {
    BlockState blockState = serverPlayer.getInBlockState();
    if (blockState.isAir()) {
      return;
    }

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

    ServerLevel serverLevel = serverPlayer.serverLevel();
    if (PortalBlockManager.isRelevantInnerPortalBlock(block)) {
      PortalInfoData portalInfo = PortalManager.getPortal(serverLevel, serverPlayer.getOnPos());
      if (portalInfo != null) {
        PortalTargetManager.teleportPlayerWithDelay(serverLevel, serverPlayer, portalInfo);
      }
    }
  }

  public static void handlePlayerChangeDimensionEvent(
      final ServerPlayer serverPlayer,
      final ResourceKey<Level> fromLevel,
      final ResourceKey<Level> toLevel) {

    // Check if the target dimension requires hot injection sync
    DimensionInfoData dimensionInfo = DimensionManager.getDimensionInfoData(toLevel.location());
    if (dimensionInfo != null && dimensionInfo.requiresHotInjectionSync()) {
      Component warningMessage =
          Component.literal(
"""
"Warning: The dimension is not fully loaded yet.
The dimension and all entities will only be available as expected after a server restart.
The current state may be used for preliminary testing, but does not accurately reflect the final dimension.""")
              .withStyle(ChatFormatting.RED);
      serverPlayer.sendSystemMessage(warningMessage);
    }
  }

  public static void handlePlayerDeathEvent(final ServerPlayer serverPlayer) {
    // Mark player for death auto-teleport instead of executing immediately
    AutoTeleportManager.markPlayerForDeathTeleport(serverPlayer);
  }

  public static void handlePlayerRespawnEvent(final ServerPlayer serverPlayer) {
    // Execute death auto-teleport after respawn if marked
    AutoTeleportManager.handlePlayerRespawn(serverPlayer);
  }
}
