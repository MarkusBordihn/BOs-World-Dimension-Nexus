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

package de.markusbordihn.worlddimensionnexus.data.color;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

public class WoolColor {

  private static final Map<Block, DyeColor> WOOL_BLOCK_COLOR_MAP = new HashMap<>();

  static {
    WOOL_BLOCK_COLOR_MAP.put(Blocks.WHITE_WOOL, DyeColor.WHITE);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.ORANGE_WOOL, DyeColor.ORANGE);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.MAGENTA_WOOL, DyeColor.MAGENTA);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.LIGHT_BLUE_WOOL, DyeColor.LIGHT_BLUE);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.YELLOW_WOOL, DyeColor.YELLOW);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.LIME_WOOL, DyeColor.LIME);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.PINK_WOOL, DyeColor.PINK);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.GRAY_WOOL, DyeColor.GRAY);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.LIGHT_GRAY_WOOL, DyeColor.LIGHT_GRAY);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.CYAN_WOOL, DyeColor.CYAN);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.PURPLE_WOOL, DyeColor.PURPLE);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.BLUE_WOOL, DyeColor.BLUE);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.BROWN_WOOL, DyeColor.BROWN);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.GREEN_WOOL, DyeColor.GREEN);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.RED_WOOL, DyeColor.RED);
    WOOL_BLOCK_COLOR_MAP.put(Blocks.BLACK_WOOL, DyeColor.BLACK);
  }

  public static Optional<DyeColor> get(BlockState blockState) {
    Block block = blockState.getBlock();
    if (block instanceof WoolCarpetBlock) {
      return Optional.empty();
    }
    return Optional.ofNullable(WOOL_BLOCK_COLOR_MAP.get(block));
  }
}
