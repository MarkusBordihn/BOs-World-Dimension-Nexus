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

package de.markusbordihn.worlddimensionnexus.data.teleport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public record TeleportLocation(
    ResourceKey<Level> dimension,
    BlockPos position,
    float yRot,
    float xRot,
    long timestamp,
    GameType gameType) {

  public static final Codec<TeleportLocation> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      ResourceKey.codec(Registries.DIMENSION)
                          .fieldOf("dimension")
                          .forGetter(TeleportLocation::dimension),
                      BlockPos.CODEC.fieldOf("position").forGetter(TeleportLocation::position),
                      Codec.FLOAT.fieldOf("yRot").forGetter(TeleportLocation::yRot),
                      Codec.FLOAT.fieldOf("xRot").forGetter(TeleportLocation::xRot),
                      Codec.LONG.fieldOf("timestamp").forGetter(TeleportLocation::timestamp),
                      GameType.CODEC
                          .optionalFieldOf("gameType", GameType.SURVIVAL)
                          .forGetter(TeleportLocation::gameType))
                  .apply(instance, TeleportLocation::new));

  public TeleportLocation(
      final ResourceKey<Level> dimension,
      final BlockPos blockPos,
      final float yRot,
      final float xRot,
      final GameType gameType) {
    this(dimension, blockPos, yRot, xRot, System.currentTimeMillis(), gameType);
  }

  public TeleportLocation(
      final ResourceKey<Level> dimension,
      final BlockPos blockPos,
      final float yRot,
      final float xRot) {
    this(dimension, blockPos, yRot, xRot, System.currentTimeMillis(), GameType.SURVIVAL);
  }
}
