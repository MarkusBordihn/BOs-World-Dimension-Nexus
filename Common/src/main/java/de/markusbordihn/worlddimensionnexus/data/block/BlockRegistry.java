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
    // Initialize the block name mappings
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

  private BlockRegistry() {
    // Utility class - prevent instantiation
  }

  /**
   * Converts a block name string to a Block instance.
   *
   * @param blockName the minecraft block name (e.g., "minecraft:diamond_block")
   * @return the corresponding Block instance, or diamond block as fallback
   */
  public static Block getBlockFromName(String blockName) {
    if (blockName == null || blockName.trim().isEmpty()) {
      return Blocks.DIAMOND_BLOCK;
    }

    String normalizedName = blockName.toLowerCase().trim();
    Block block = BLOCK_NAME_MAP.get(normalizedName);

    if (block != null) {
      return block;
    }

    // Try without the minecraft: prefix if it was provided
    if (normalizedName.startsWith("minecraft:")) {
      String simpleName = normalizedName.substring("minecraft:".length());
      block = BLOCK_NAME_MAP.get("minecraft:" + simpleName);
      if (block != null) {
        return block;
      }
    } else {
      // Try with minecraft: prefix if it wasn't provided
      block = BLOCK_NAME_MAP.get("minecraft:" + normalizedName);
      if (block != null) {
        return block;
      }
    }

    // Return diamond block as safe fallback
    return Blocks.DIAMOND_BLOCK;
  }

  /**
   * Checks if a block name is supported by this registry.
   *
   * @param blockName the block name to check
   * @return true if the block name is supported, false otherwise
   */
  public static boolean isBlockNameSupported(String blockName) {
    if (blockName == null || blockName.trim().isEmpty()) {
      return false;
    }

    String normalizedName = blockName.toLowerCase().trim();
    if (BLOCK_NAME_MAP.containsKey(normalizedName)) {
      return true;
    }

    // Check with/without minecraft: prefix
    if (normalizedName.startsWith("minecraft:")) {
      String simpleName = normalizedName.substring("minecraft:".length());
      return BLOCK_NAME_MAP.containsKey("minecraft:" + simpleName);
    } else {
      return BLOCK_NAME_MAP.containsKey("minecraft:" + normalizedName);
    }
  }

  /**
   * Gets all supported block names.
   *
   * @return an array of all supported block names
   */
  public static String[] getSupportedBlockNames() {
    return BLOCK_NAME_MAP.keySet().toArray(new String[0]);
  }
}
