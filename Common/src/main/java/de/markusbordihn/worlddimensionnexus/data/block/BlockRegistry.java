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

package de.markusbordihn.worlddimensionnexus.data.block;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockRegistry {

  private static final Map<String, Block> BLOCK_NAME_MAP = new HashMap<>();

  static {
    BLOCK_NAME_MAP.put("minecraft:diamond_block", Blocks.DIAMOND_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:emerald_block", Blocks.EMERALD_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:netherite_block", Blocks.NETHERITE_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:beacon", Blocks.BEACON);
    BLOCK_NAME_MAP.put("minecraft:gold_block", Blocks.GOLD_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:iron_block", Blocks.IRON_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:redstone_block", Blocks.REDSTONE_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:lapis_block", Blocks.LAPIS_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:copper_block", Blocks.COPPER_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:obsidian", Blocks.OBSIDIAN);
    BLOCK_NAME_MAP.put("minecraft:crying_obsidian", Blocks.CRYING_OBSIDIAN);
    BLOCK_NAME_MAP.put("minecraft:coal_block", Blocks.COAL_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:quartz_block", Blocks.QUARTZ_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:purpur_block", Blocks.PURPUR_BLOCK);
    BLOCK_NAME_MAP.put("minecraft:end_stone", Blocks.END_STONE);
    BLOCK_NAME_MAP.put("minecraft:nether_bricks", Blocks.NETHER_BRICKS);
    BLOCK_NAME_MAP.put("minecraft:blackstone", Blocks.BLACKSTONE);
    BLOCK_NAME_MAP.put("minecraft:deepslate", Blocks.DEEPSLATE);
  }

  private BlockRegistry() {}

  private static String normalizeBlockName(String blockName) {
    if (blockName == null || blockName.trim().isEmpty()) {
      return null;
    }

    String normalized = blockName.toLowerCase().trim();
    return normalized.startsWith("minecraft:") ? normalized : "minecraft:" + normalized;
  }

  public static Block getBlockFromName(String blockName) {
    String normalizedName = normalizeBlockName(blockName);
    return normalizedName != null
        ? BLOCK_NAME_MAP.getOrDefault(normalizedName, Blocks.DIAMOND_BLOCK)
        : Blocks.DIAMOND_BLOCK;
  }
}
