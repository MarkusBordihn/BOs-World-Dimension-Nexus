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

package de.markusbordihn.worlddimensionnexus.portal;

import de.markusbordihn.worlddimensionnexus.block.PortalBlockManager;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalInfoData;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalType;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

public class PortalCreator {

  private static final int PORTAL_FRAME_HEIGHT = 5;
  private static final int PORTAL_INNER_WIDTH = 2;

  private PortalCreator() {}

  public static PortalInfoData createPortal(
      final ServerLevel serverLevel,
      final ServerPlayer serverPlayer,
      final BlockPos origin,
      final PortalType portalType,
      final DyeColor frameColor,
      final String name,
      final Direction facing) {

    Set<BlockPos> frameBlocks = generateFrameBlocks(origin, facing);
    Set<BlockPos> innerBlocks = generateInnerBlocks(origin, facing);
    Set<BlockPos> cornerBlocks = generateCornerBlocks(origin, facing);

    String portalName =
        (name == null || name.trim().isEmpty())
            ? generatePortalName(portalType, serverPlayer, frameColor)
            : name;

    PortalInfoData portalInfo =
        new PortalInfoData(
            serverLevel.dimension(),
            origin,
            frameBlocks,
            innerBlocks,
            cornerBlocks,
            serverPlayer.getUUID(),
            frameColor,
            portalType.getCornerBlock(),
            portalType,
            portalName);
    Direction.Axis portalAxis = convertFacingToAxis(facing);
    if (PortalBlockManager.createPortal(
        serverLevel, serverPlayer, portalInfo, portalAxis, facing)) {
      return portalInfo;
    }

    return null;
  }

  public static PortalInfoData createEventPortal(
      final ServerLevel serverLevel,
      final ServerPlayer serverPlayer,
      final BlockPos origin,
      final DyeColor frameColor,
      final String name,
      final BlockPos targetPosition,
      final Direction facing) {

    PortalInfoData portalInfo =
        createPortal(serverLevel, serverPlayer, origin, PortalType.EVENT, frameColor, name, facing);
    if (portalInfo == null) {
      return null;
    }

    if (targetPosition != null) {
      PortalTargetManager.setTarget(portalInfo, serverLevel.dimension(), targetPosition);
    }

    return portalInfo;
  }

  public static Direction getPlayerFacingDirection(final ServerPlayer player) {
    if (player == null) {
      return Direction.NORTH;
    }

    float normalizedYaw = normalizeYaw(player.getYRot());
    return convertYawToDirection(normalizedYaw);
  }

  private static float normalizeYaw(final float yaw) {
    float normalizedYaw = yaw;
    while (normalizedYaw < 0) normalizedYaw += 360;
    while (normalizedYaw >= 360) normalizedYaw -= 360;
    return normalizedYaw;
  }

  private static Direction convertYawToDirection(final float yaw) {
    if (yaw >= 315 || yaw < 45) {
      return Direction.SOUTH;
    } else if (yaw >= 45 && yaw < 135) {
      return Direction.WEST;
    } else if (yaw >= 135 && yaw < 225) {
      return Direction.NORTH;
    } else {
      return Direction.EAST;
    }
  }

  private static Direction.Axis convertFacingToAxis(final Direction facing) {
    return switch (facing) {
      case NORTH, SOUTH -> Direction.Axis.Z;
      case EAST, WEST -> Direction.Axis.X;
      default -> Direction.Axis.Y;
    };
  }

  private static Set<BlockPos> generateFrameBlocks(final BlockPos origin, final Direction facing) {
    Set<BlockPos> frameBlocks = new HashSet<>();
    for (int x = -1; x <= PORTAL_INNER_WIDTH; x++) {
      for (int y = 0; y <= PORTAL_FRAME_HEIGHT - 1; y++) {
        if (isFramePosition(x, y)) {
          frameBlocks.add(getRotatedPosition(origin, x, y, facing));
        }
      }
    }
    return frameBlocks;
  }

  private static boolean isFramePosition(final int x, final int y) {
    return x == -1 || x == PORTAL_INNER_WIDTH || y == 0 || y == PORTAL_FRAME_HEIGHT - 1;
  }

  private static Set<BlockPos> generateCornerBlocks(final BlockPos origin, final Direction facing) {
    Set<BlockPos> cornerBlocks = new HashSet<>();
    cornerBlocks.add(getRotatedPosition(origin, -1, 0, facing));
    cornerBlocks.add(getRotatedPosition(origin, PORTAL_INNER_WIDTH, 0, facing));
    cornerBlocks.add(getRotatedPosition(origin, -1, PORTAL_FRAME_HEIGHT - 1, facing));
    cornerBlocks.add(
        getRotatedPosition(origin, PORTAL_INNER_WIDTH, PORTAL_FRAME_HEIGHT - 1, facing));
    return cornerBlocks;
  }

  private static BlockPos getRotatedPosition(
      final BlockPos origin, final int offsetX, final int offsetY, final Direction facing) {
    return switch (facing) {
      case NORTH -> origin.offset(offsetX, offsetY, 0);
      case SOUTH -> origin.offset(-offsetX, offsetY, 0);
      case EAST -> origin.offset(0, offsetY, offsetX);
      case WEST -> origin.offset(0, offsetY, -offsetX);
      default -> origin.offset(offsetX, offsetY, 0);
    };
  }

  private static String generatePortalName(
      final PortalType portalType, final ServerPlayer player, final DyeColor frameColor) {
    return portalType.getName()
        + " Portal ("
        + frameColor.getName()
        + ") by "
        + player.getName().getString();
  }

  private static Set<BlockPos> generateInnerBlocks(final BlockPos origin, final Direction facing) {
    Set<BlockPos> innerBlocks = new HashSet<>();
    for (int x = 0; x < PORTAL_INNER_WIDTH; x++) {
      for (int y = 1; y < PORTAL_FRAME_HEIGHT - 1; y++) {
        innerBlocks.add(getRotatedPosition(origin, x, y, facing));
      }
    }
    return innerBlocks;
  }
}
