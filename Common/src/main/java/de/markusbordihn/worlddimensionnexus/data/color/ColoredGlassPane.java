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

import java.util.EnumMap;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class ColoredGlassPane {

  private static final EnumMap<DyeColor, Block> STAINED_GLASS_MAP = new EnumMap<>(DyeColor.class);

  static {
    STAINED_GLASS_MAP.put(DyeColor.WHITE, Blocks.WHITE_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.ORANGE, Blocks.ORANGE_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.MAGENTA, Blocks.MAGENTA_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.YELLOW, Blocks.YELLOW_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.LIME, Blocks.LIME_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.PINK, Blocks.PINK_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.GRAY, Blocks.GRAY_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.CYAN, Blocks.CYAN_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.PURPLE, Blocks.PURPLE_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.BLUE, Blocks.BLUE_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.BROWN, Blocks.BROWN_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.GREEN, Blocks.GREEN_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.RED, Blocks.RED_STAINED_GLASS_PANE);
    STAINED_GLASS_MAP.put(DyeColor.BLACK, Blocks.BLACK_STAINED_GLASS_PANE);
  }

  public static Block get(DyeColor color) {
    return STAINED_GLASS_MAP.getOrDefault(color, Blocks.GLASS_PANE);
  }
}
