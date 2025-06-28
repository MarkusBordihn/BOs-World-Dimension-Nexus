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

/**
 * Represents an auto-teleport configuration entry. Stores all necessary information for an
 * automatic teleportation including the target dimension, coordinates, and the trigger that
 * activates the teleportation.
 */
public record AutoTeleportEntry(
    String targetDimension, double x, double y, double z, AutoTeleportTrigger trigger) {

  /** Codec for serializing and deserializing AutoTeleportEntry instances. */
  public static final Codec<AutoTeleportEntry> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.STRING
                          .fieldOf("targetDimension")
                          .forGetter(AutoTeleportEntry::targetDimension),
                      Codec.DOUBLE.fieldOf("x").forGetter(AutoTeleportEntry::x),
                      Codec.DOUBLE.fieldOf("y").forGetter(AutoTeleportEntry::y),
                      Codec.DOUBLE.fieldOf("z").forGetter(AutoTeleportEntry::z),
                      AutoTeleportTrigger.CODEC
                          .fieldOf("trigger")
                          .forGetter(AutoTeleportEntry::trigger))
                  .apply(instance, AutoTeleportEntry::new));
}
