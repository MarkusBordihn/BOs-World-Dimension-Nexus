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

package de.markusbordihn.worlddimensionnexus.data.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Immutable data structure for portal target destinations.
 *
 * <p>Links a source portal to its target location (dimension and position). Used for teleportation
 * between different points in the world.
 */
public record PortalTargetData(UUID portalId, ResourceKey<Level> dimension, BlockPos position) {

  /** Codec for serializing and deserializing PortalTargetData objects. */
  public static final Codec<PortalTargetData> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf("portalId").forGetter(PortalTargetData::portalId),
                      ResourceKey.codec(Registries.DIMENSION)
                          .fieldOf("dimension")
                          .forGetter(PortalTargetData::dimension),
                      PortalInfoData.BLOCK_POS_CODEC
                          .fieldOf("position")
                          .forGetter(PortalTargetData::position))
                  .apply(instance, PortalTargetData::new));

  /**
   * Creates a new portal target in the overworld at position (0,0,0).
   *
   * @param portalId The UUID of the source portal
   */
  public PortalTargetData(UUID portalId) {
    this(
        portalId,
        ResourceKey.create(Registries.DIMENSION, Level.OVERWORLD.location()),
        BlockPos.ZERO);
  }

  /**
   * Checks if the portal target data is empty and not linked to any valid portal, dimension, or
   * position.
   *
   * @return true if the portal target data is empty, false otherwise
   */
  public boolean isEmpty() {
    return this.portalId == null
        || this.dimension == null
        || this.position == null
        || (this.dimension.location().equals(Level.OVERWORLD.location())
            && this.position.equals(BlockPos.ZERO));
  }

  @Override
  public String toString() {
    return "PortalTargetData{"
        + "portalId="
        + portalId
        + ", dimension="
        + dimension.location()
        + ", position="
        + position
        + '}';
  }
}
