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

package de.markusbordihn.worlddimensionnexus.data.chunk;

import com.mojang.serialization.Codec;

public enum ChunkGeneratorType {
  CUSTOM("custom", "minecraft:overworld"),
  FLAT("flat", "minecraft:overworld"),
  NOISE("noise", "minecraft:overworld"),
  DEBUG("debug", "minecraft:overworld"),
  VOID("void", "minecraft:the_end"),
  LOBBY("lobby", "world_dimension_nexus:lobby_dimension_type"),
  SKYBLOCK("skyblock", "minecraft:overworld"),
  CAVE("cave", "minecraft:overworld_caves"),
  FLOATING_ISLANDS("floating_islands", "minecraft:the_end"),
  AMPLIFIED("amplified", "minecraft:overworld"),
  WATER("water", "world_dimension_nexus:water_dimension_type");

  public static final Codec<ChunkGeneratorType> CODEC =
      Codec.STRING.xmap(ChunkGeneratorType::fromString, ChunkGeneratorType::getName);
  private final String name;
  private final String dimensionType;

  ChunkGeneratorType(final String name, final String dimensionType) {
    this.name = name;
    this.dimensionType = dimensionType;
  }

  public static ChunkGeneratorType fromString(final String name) {
    for (ChunkGeneratorType type : values()) {
      if (type.name.equalsIgnoreCase(name)) {
        return type;
      }
    }
    return CUSTOM; // Fallback
  }

  public String getName() {
    return name;
  }

  public String getDimensionType() {
    return dimensionType;
  }
}
