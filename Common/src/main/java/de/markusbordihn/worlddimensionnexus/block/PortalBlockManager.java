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

package de.markusbordihn.worlddimensionnexus.block;

import de.markusbordihn.worlddimensionnexus.data.color.ColoredGlassPane;
import de.markusbordihn.worlddimensionnexus.data.color.WoolColor;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalInfoData;
import de.markusbordihn.worlddimensionnexus.network.NetworkHandler;
import de.markusbordihn.worlddimensionnexus.portal.PortalManager;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;

public class PortalBlockManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Portal Block Manager");

  private static final Block CORNER_BLOCK_MATERIAL = Blocks.DIAMOND_BLOCK;
  private static final int PORTAL_INNER_WIDTH = 2;
  private static final int PORTAL_INNER_HEIGHT = 3;

  public static void checkForPotentialPortals(
      final ServerLevel serverLevel,
      final BlockPos blockPos,
      final ServerPlayer serverPlayer,
      final Block block,
      final BlockState blockState) {
    // Ignore non-corner blocks or wool blocks without color.
    boolean isCorner = block == CORNER_BLOCK_MATERIAL;
    Optional<DyeColor> woolColorOpt = WoolColor.get(blockState);
    if (!isCorner && woolColorOpt.isEmpty()) {
      return;
    }

    // Check for potential portals from the corner block or wool block.
    if (isCorner) {
      for (Direction.Axis axis : Direction.Axis.values()) {
        checkPotentialPortalFromCorner(serverLevel, blockPos, axis, serverPlayer);
      }
    } else {
      woolColorOpt.ifPresent(
          color -> {
            for (Direction.Axis axis : Direction.Axis.values()) {
              for (int x = -PORTAL_INNER_WIDTH - 1; x <= PORTAL_INNER_WIDTH + 1; x++) {
                for (int y = -PORTAL_INNER_HEIGHT - 1; y <= PORTAL_INNER_HEIGHT + 1; y++) {
                  for (int z = -PORTAL_INNER_WIDTH - 1; z <= PORTAL_INNER_WIDTH + 1; z++) {
                    BlockPos potentialCornerPos = blockPos.offset(x, y, z);
                    if (serverLevel.getBlockState(potentialCornerPos).getBlock()
                        == CORNER_BLOCK_MATERIAL) {
                      checkPotentialPortalFromCorner(
                          serverLevel, potentialCornerPos, axis, serverPlayer);
                    }
                  }
                }
              }
            }
          });
    }
  }

  public static boolean isRelevantPortalBlock(final Block block, final BlockState blockState) {
    return isRelevantPortalFrameBlock(block, blockState)
        || isRelevantInnerPortalBlock(block, blockState);
  }

  public static boolean isRelevantPortalFrameBlock(final Block block, final BlockState blockState) {
    return block == CORNER_BLOCK_MATERIAL || WoolColor.get(blockState).isPresent();
  }

  public static boolean isRelevantInnerPortalBlock(final Block block, final BlockState blockState) {
    return block instanceof IronBarsBlock;
  }

  public static void createPortal(
      final ServerLevel serverLevel,
      final ServerPlayer serverPlayer,
      final PortalInfoData portalInfo,
      final Direction.Axis portalAxis,
      final Direction currentHorizontal) {
    if (serverLevel == null
        || portalInfo == null
        || portalAxis == null
        || currentHorizontal == null) {
      log.error("Unable to create portal, missing parameter!");
      return;
    }

    // Play portal spawn sound at the origin position.
    serverLevel.playSound(
        null, portalInfo.origin(), SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);

    // Glass pane extending Iron Bars Blocks, for this reason we are checking for them
    // instead of stained-glass panes or glass panes.
    Block glassBlock = ColoredGlassPane.get(portalInfo.color());
    BlockState glassState = glassBlock.defaultBlockState();
    if (glassBlock instanceof IronBarsBlock ironBarsBlock) {
      if (portalAxis == Direction.Axis.Y) {
        glassState =
            ironBarsBlock
                .defaultBlockState()
                .setValue(CrossCollisionBlock.NORTH, true)
                .setValue(CrossCollisionBlock.SOUTH, true)
                .setValue(CrossCollisionBlock.EAST, true)
                .setValue(CrossCollisionBlock.WEST, true);
      } else {
        Direction.Axis connectAxis = currentHorizontal.getAxis();
        glassState =
            ironBarsBlock
                .defaultBlockState()
                .setValue(CrossCollisionBlock.NORTH, connectAxis == Direction.Axis.Z)
                .setValue(CrossCollisionBlock.SOUTH, connectAxis == Direction.Axis.Z)
                .setValue(CrossCollisionBlock.EAST, connectAxis == Direction.Axis.X)
                .setValue(CrossCollisionBlock.WEST, connectAxis == Direction.Axis.X);
      }
    }

    // Set the inner area blocks to glass panes and send block update packets.
    for (BlockPos pos : portalInfo.innerBlocks()) {
      serverLevel.setBlock(pos, glassState, 3);
      NetworkHandler.sendBlockUpdatePacket(serverPlayer, pos, glassState);
    }

    // Send a message to the player who created the portal.
    if (serverPlayer != null) {
      serverPlayer.sendSystemMessage(
          Component.literal("Portal (" + portalInfo.color().getName() + ") created!"));
    }

    // Register the portal in the PortalManager.
    PortalManager.addPortal(portalInfo);
  }

  public static void destroyPortal(
      final ServerLevel level, final ServerPlayer player, final PortalInfoData portalInfo) {
    if (level == null || portalInfo == null) {
      return;
    }

    // Play portal destruction sound at the origin position.
    level.playSound(
        null, portalInfo.origin(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.7F, 1.2F);
    level.playSound(
        null, portalInfo.origin(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.5F, 1.25F);

    // Remove the inner portal blocks and send block update packets.
    for (BlockPos innerBlock : portalInfo.innerBlocks()) {
      level.removeBlock(innerBlock, true);
      if (player != null) {
        NetworkHandler.sendDelayedBlockUpdatePacket(level, player, innerBlock);
      }
    }

    // Send a message to the player who destroyed the portal.
    if (player != null) {
      player.sendSystemMessage(Component.literal("Portal destroyed!"));
    }

    PortalManager.removePortal(portalInfo);
  }

  private static void checkPotentialPortalFromCorner(
      final ServerLevel serverLevel,
      final BlockPos blockPos,
      final Direction.Axis portalAxis,
      final ServerPlayer serverPlayer) {
    Direction verticalDirection;
    Direction horizontalDirection;
    int verticalFrameLength;
    int horizontalFrameLength;

    // Determine the vertical and horizontal directions based on the portal axis.
    switch (portalAxis) {
      case Direction.Axis.X -> {
        verticalDirection = Direction.UP;
        horizontalDirection = Direction.SOUTH;
        verticalFrameLength = PORTAL_INNER_HEIGHT;
        horizontalFrameLength = PORTAL_INNER_WIDTH;
      }
      case Direction.Axis.Y -> {
        verticalDirection = Direction.EAST;
        horizontalDirection = Direction.SOUTH;
        verticalFrameLength = PORTAL_INNER_WIDTH;
        horizontalFrameLength = PORTAL_INNER_WIDTH;
      }
      default -> {
        verticalDirection = Direction.UP;
        horizontalDirection = Direction.EAST;
        verticalFrameLength = PORTAL_INNER_HEIGHT;
        horizontalFrameLength = PORTAL_INNER_WIDTH;
      }
    }

    // Check for all combinations of vertical and horizontal flips.
    for (int verticalFlip = 0; verticalFlip < 2; verticalFlip++) {
      for (int horizontalFlip = 0; horizontalFlip < 2; horizontalFlip++) {
        Direction currentVertical =
            (verticalFlip == 0) ? verticalDirection : verticalDirection.getOpposite();
        Direction currentHorizontal =
            (horizontalFlip == 0) ? horizontalDirection : horizontalDirection.getOpposite();

        if (currentVertical.getAxis() == currentHorizontal.getAxis()) {
          continue;
        }
        BlockPos secondCorner = blockPos.relative(currentVertical, verticalFrameLength + 1);
        BlockPos thirdCorner = blockPos.relative(currentHorizontal, horizontalFrameLength + 1);
        BlockPos fourthCorner = secondCorner.relative(currentHorizontal, horizontalFrameLength + 1);

        // Check if the corners are valid corner blocks.
        boolean isValidFrame =
            serverLevel.getBlockState(blockPos).getBlock() == CORNER_BLOCK_MATERIAL
                && serverLevel.getBlockState(secondCorner).getBlock() == CORNER_BLOCK_MATERIAL
                && serverLevel.getBlockState(thirdCorner).getBlock() == CORNER_BLOCK_MATERIAL
                && serverLevel.getBlockState(fourthCorner).getBlock() == CORNER_BLOCK_MATERIAL;
        if (!isValidFrame) {
          continue;
        }

        // Check if the frame is complete and has the same color.
        Optional<DyeColor> optionalFrameColor =
            checkFrameAndGetColor(
                serverLevel,
                blockPos,
                currentVertical,
                currentHorizontal,
                verticalFrameLength,
                horizontalFrameLength);
        if (optionalFrameColor.isEmpty()) {
          continue;
        }

        // Check if the inner area is clear.
        List<BlockPos> innerAreaPositions =
            getInnerBlockPositions(
                blockPos,
                currentVertical,
                currentHorizontal,
                verticalFrameLength,
                horizontalFrameLength);
        if (innerAreaPositions.isEmpty()) {
          continue;
        }

        // Check if the inner area is clear of blocks.
        boolean isInteriorClear =
            innerAreaPositions.stream()
                .allMatch(
                    pos -> serverLevel.isEmptyBlock(pos) || serverLevel.getBlockState(pos).isAir());
        if (!isInteriorClear) {
          continue;
        }

        // Create portal information to create the portal.
        PortalInfoData portalInfo =
            new PortalInfoData(
                serverLevel.dimension(),
                blockPos.immutable(),
                new HashSet<>(
                    getFrameBlockPositions(
                        blockPos,
                        currentVertical,
                        currentHorizontal,
                        verticalFrameLength,
                        horizontalFrameLength)),
                new HashSet<>(innerAreaPositions),
                Set.of(blockPos, secondCorner, thirdCorner, fourthCorner),
                serverPlayer != null ? serverPlayer.getUUID() : UUID.randomUUID(),
                optionalFrameColor.get(),
                serverLevel.getBlockState(blockPos).getBlock());

        // Create the portal with the portal information.
        createPortal(serverLevel, serverPlayer, portalInfo, portalAxis, currentHorizontal);
        return;
      }
    }
  }

  private static Optional<DyeColor> checkFrameAndGetColor(
      final ServerLevel serverLevel,
      final BlockPos blockPos,
      final Direction verticalDirection,
      final Direction horizontalDirection,
      final int verticalFrameLength,
      final int horizontalFrameLength) {
    DyeColor foundColor = null;

    // Check all four sides of the frame.
    for (int i = 0; i < verticalFrameLength; i++) {
      if (!checkWoolBlock(
          serverLevel,
          blockPos.relative(verticalDirection, i + 1).relative(horizontalDirection, 0),
          foundColor)) {
        return Optional.empty();
      }
      foundColor =
          updateFoundColor(
              serverLevel,
              blockPos.relative(verticalDirection, i + 1).relative(horizontalDirection, 0),
              foundColor);
    }
    for (int i = 0; i < verticalFrameLength; i++) {
      if (!checkWoolBlock(
          serverLevel,
          blockPos
              .relative(verticalDirection, i + 1)
              .relative(horizontalDirection, horizontalFrameLength + 1),
          foundColor)) {
        return Optional.empty();
      }
    }
    for (int i = 0; i < horizontalFrameLength; i++) {
      if (!checkWoolBlock(
          serverLevel,
          blockPos.relative(verticalDirection, 0).relative(horizontalDirection, i + 1),
          foundColor)) {
        return Optional.empty();
      }
    }
    for (int i = 0; i < horizontalFrameLength; i++) {
      if (!checkWoolBlock(
          serverLevel,
          blockPos
              .relative(verticalDirection, verticalFrameLength + 1)
              .relative(horizontalDirection, i + 1),
          foundColor)) {
        return Optional.empty();
      }
    }
    return Optional.ofNullable(foundColor);
  }

  private static boolean checkWoolBlock(
      final ServerLevel level, final BlockPos blockPos, final DyeColor foundColor) {
    Optional<DyeColor> color = WoolColor.get(level.getBlockState(blockPos));
    return color.isPresent() && (foundColor == null || foundColor.equals(color.get()));
  }

  private static DyeColor updateFoundColor(
      final ServerLevel level, final BlockPos blockPos, final DyeColor foundColor) {
    return foundColor == null
        ? WoolColor.get(level.getBlockState(blockPos)).orElse(null)
        : foundColor;
  }

  private static List<BlockPos> getInnerBlockPositions(
      final BlockPos cornerPosition,
      final Direction verticalDirection,
      final Direction horizontalDirection,
      final int verticalLength,
      final int horizontalLength) {
    List<BlockPos> innerBlockPositions = new ArrayList<>();
    BlockPos firstInnerPosition =
        cornerPosition.relative(verticalDirection).relative(horizontalDirection);

    // Check if the inner area is valid.
    for (int verticalOffset = 0; verticalOffset < verticalLength; verticalOffset++) {
      for (int horizontalOffset = 0; horizontalOffset < horizontalLength; horizontalOffset++) {
        BlockPos currentPosition =
            firstInnerPosition
                .relative(verticalDirection, verticalOffset)
                .relative(horizontalDirection, horizontalOffset);
        innerBlockPositions.add(currentPosition);
      }
    }

    return innerBlockPositions;
  }

  private static List<BlockPos> getFrameBlockPositions(
      final BlockPos corner,
      final Direction vertical,
      final Direction horizontal,
      final int verticalLength,
      final int horizontalLength) {
    List<BlockPos> frameBlocks = new ArrayList<>();

    // Vertical Sides
    for (int i = 0; i <= verticalLength + 1; i++) {
      frameBlocks.add(corner.relative(vertical, i));
      frameBlocks.add(corner.relative(horizontal, horizontalLength + 1).relative(vertical, i));
    }

    // Horizontal Sides
    for (int i = 1; i <= horizontalLength; i++) {
      frameBlocks.add(corner.relative(horizontal, i));
      frameBlocks.add(corner.relative(vertical, verticalLength + 1).relative(horizontal, i));
    }

    return frameBlocks;
  }
}
