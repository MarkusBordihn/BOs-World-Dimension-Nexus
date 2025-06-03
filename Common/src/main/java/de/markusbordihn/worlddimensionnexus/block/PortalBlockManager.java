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

import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.color.ColoredGlassPane;
import de.markusbordihn.worlddimensionnexus.data.color.WoolColor;
import de.markusbordihn.worlddimensionnexus.network.NetworkHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PortalBlockManager {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private static final Block CORNER_BLOCK_MATERIAL = Blocks.DIAMOND_BLOCK;
  private static final int PORTAL_INNER_WIDTH = 2;
  private static final int PORTAL_INNER_HEIGHT = 3;

  public static void checkForPotentialPortals(
      ServerLevel serverLevel,
      BlockPos blockPos,
      ServerPlayer serverPlayer,
      Block block,
      BlockState blockState) {
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

  private static void checkPotentialPortalFromCorner(
      ServerLevel serverLevel,
      BlockPos blockPos,
      Direction.Axis portalAxis,
      ServerPlayer serverPlayer) {
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

        // We have a valid portal frame with a color and clear inner area.
        DyeColor frameColor = optionalFrameColor.get();
        if (serverPlayer != null) {
          serverPlayer.sendSystemMessage(
              Component.literal("Portal (" + frameColor.getName() + ") detected!"));
        }
        serverLevel.playSound(
            null, blockPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);

        // Glass pane extending Iron Bars Blocks, for this reason we are checking for them
        // instead of stained-glass panes or glass panes.
        Block glassBlock = ColoredGlassPane.get(frameColor);
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
        for (BlockPos pos : innerAreaPositions) {
          serverLevel.setBlock(pos, glassState, 3);
          NetworkHandler.sendBlockUpdatePacket(serverPlayer, pos, glassState);
        }

        // Inform the portal manager about the new portal.
        log.info(
            "Registering portal at {} with color {}, inner blocks: {}, axis: {}",
            blockPos,
            frameColor,
            innerAreaPositions.size(),
            portalAxis);
        return;
      }
    }
  }

  private static Optional<DyeColor> checkFrameAndGetColor(
      ServerLevel serverLevel,
      BlockPos blockPos,
      Direction dir1,
      Direction dir2,
      int innerDim1,
      int innerDim2) {
    DyeColor foundColor = null;

    // Check all four sides of the frame.
    for (int i = 0; i < innerDim1; i++) {
      if (!checkWoolBlock(
          serverLevel, blockPos.relative(dir1, i + 1).relative(dir2, 0), foundColor)) {
        return Optional.empty();
      }
      foundColor =
          updateFoundColor(
              serverLevel, blockPos.relative(dir1, i + 1).relative(dir2, 0), foundColor);
    }
    for (int i = 0; i < innerDim1; i++) {
      if (!checkWoolBlock(
          serverLevel, blockPos.relative(dir1, i + 1).relative(dir2, innerDim2 + 1), foundColor)) {
        return Optional.empty();
      }
    }
    for (int i = 0; i < innerDim2; i++) {
      if (!checkWoolBlock(
          serverLevel, blockPos.relative(dir1, 0).relative(dir2, i + 1), foundColor)) {
        return Optional.empty();
      }
    }
    for (int i = 0; i < innerDim2; i++) {
      if (!checkWoolBlock(
          serverLevel, blockPos.relative(dir1, innerDim1 + 1).relative(dir2, i + 1), foundColor)) {
        return Optional.empty();
      }
    }
    return Optional.ofNullable(foundColor);
  }

  private static boolean checkWoolBlock(ServerLevel level, BlockPos pos, DyeColor foundColor) {
    Optional<DyeColor> color = WoolColor.get(level.getBlockState(pos));
    return color.isPresent() && (foundColor == null || foundColor.equals(color.get()));
  }

  private static DyeColor updateFoundColor(ServerLevel level, BlockPos pos, DyeColor foundColor) {
    return foundColor == null ? WoolColor.get(level.getBlockState(pos)).orElse(null) : foundColor;
  }

  private static List<BlockPos> getInnerBlockPositions(
      BlockPos cornerPosition,
      Direction verticalDirection,
      Direction horizontalDirection,
      int verticalLength,
      int horizontalLength) {
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
}
