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
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Immutable data structure representing a portal in the world. Contains all necessary information
 * to identify, locate, and manage a portal, including its position, dimensions, appearance, and
 * metadata.
 */
public record PortalInfoData(
    UUID uuid,
    ResourceKey<Level> dimension,
    BlockPos origin,
    Set<BlockPos> frameBlocks,
    Set<BlockPos> innerBlocks,
    Set<BlockPos> cornerBlocks,
    UUID creator,
    DyeColor color,
    Block edgeBlockType,
    long lastUsed) {

  /** Codec for serializing and deserializing BlockPos objects. */
  public static final Codec<BlockPos> BLOCK_POS_CODEC =
      Codec.INT_STREAM.comapFlatMap(
          stream -> {
            int[] array = stream.toArray();
            return array.length == 3
                ? DataResult.success(new BlockPos(array[0], array[1], array[2]))
                : DataResult.error(() -> "Expected 3 ints for BlockPos");
          },
          pos -> Arrays.stream(new int[] {pos.getX(), pos.getY(), pos.getZ()}));

  /** Codec for dimension resource keys. */
  public static final Codec<ResourceKey<Level>> LEVEL_KEY_CODEC =
      net.minecraft.resources.ResourceKey.codec(Registries.DIMENSION);

  /** Codec for serializing and deserializing Sets of BlockPos objects. */
  public static final Codec<Set<BlockPos>> BLOCK_POS_SET_CODEC =
      BLOCK_POS_CODEC.listOf().xmap(HashSet::new, ArrayList::new);

  /** Main codec for serializing and deserializing PortalInfoData objects. */
  public static final Codec<PortalInfoData> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf("uuid").forGetter(PortalInfoData::uuid),
                      LEVEL_KEY_CODEC.fieldOf("dimension").forGetter(PortalInfoData::dimension),
                      BLOCK_POS_CODEC.fieldOf("origin").forGetter(PortalInfoData::origin),
                      BLOCK_POS_SET_CODEC
                          .fieldOf("frameBlocks")
                          .forGetter(PortalInfoData::frameBlocks),
                      BLOCK_POS_SET_CODEC
                          .fieldOf("innerBlocks")
                          .forGetter(PortalInfoData::innerBlocks),
                      BLOCK_POS_SET_CODEC
                          .fieldOf("cornerBlocks")
                          .forGetter(PortalInfoData::cornerBlocks),
                      UUIDUtil.CODEC.fieldOf("creator").forGetter(PortalInfoData::creator),
                      DyeColor.CODEC.fieldOf("color").forGetter(PortalInfoData::color),
                      BuiltInRegistries.BLOCK
                          .byNameCodec()
                          .fieldOf("edgeBlock")
                          .forGetter(PortalInfoData::edgeBlockType),
                      Codec.LONG
                          .optionalFieldOf("lastUsed", 0L)
                          .forGetter(PortalInfoData::lastUsed))
                  .apply(instance, PortalInfoData::new));

  /**
   * Creates a new portal with auto-generated UUID and current timestamp.
   *
   * @param dimension The dimension where the portal exists
   * @param origin The origin position of the portal
   * @param frameBlocks Set of positions for the frame blocks
   * @param innerBlocks Set of positions for the inner blocks
   * @param cornerBlocks Set of positions for the corner blocks
   * @param creator UUID of the player who created the portal
   * @param color Color of the portal
   * @param edgeBlockType Block type used for the portal frame
   */
  public PortalInfoData(
      final ResourceKey<Level> dimension,
      final BlockPos origin,
      final Set<BlockPos> frameBlocks,
      final Set<BlockPos> innerBlocks,
      final Set<BlockPos> cornerBlocks,
      final UUID creator,
      final DyeColor color,
      final Block edgeBlockType) {
    this(
        UUID.randomUUID(),
        dimension,
        origin,
        frameBlocks,
        innerBlocks,
        cornerBlocks,
        creator,
        color,
        edgeBlockType,
        System.currentTimeMillis());
  }

  /**
   * Creates a copy of this portal with an updated last used timestamp.
   *
   * @return New PortalInfoData instance with current timestamp
   */
  public PortalInfoData withUpdatedLastUsed() {
    return new PortalInfoData(
        this.uuid,
        this.dimension,
        this.origin,
        this.frameBlocks,
        this.innerBlocks,
        this.cornerBlocks,
        this.creator,
        this.color,
        this.edgeBlockType,
        System.currentTimeMillis());
  }

  /**
   * Checks if the portal contains the given position within its inner blocks.
   *
   * @param pos Position to check
   * @return true if the position is inside the portal
   */
  public boolean contains(final BlockPos pos) {
    return innerBlocks.contains(pos);
  }

  /**
   * Determines if this portal can be linked to another portal based on matching properties.
   *
   * @param other The other portal to check for compatibility
   * @return true if the portals can be linked
   */
  public boolean isLinkedTo(final PortalInfoData other) {
    return !this.equals(other)
        && this.color == other.color
        && this.edgeBlockType == other.edgeBlockType;
  }

  /**
   * Calculates the teleport position for entities using this portal. Uses the center of inner
   * blocks or falls back to origin if needed.
   *
   * @return The position where entities should teleport to
   */
  public BlockPos getTeleportPosition() {

    // Use inner blocks to calculate the teleport position.
    if (this.innerBlocks != null && !this.innerBlocks.isEmpty()) {
      BlockPos middle =
          this.innerBlocks.stream()
              .reduce(
                  BlockPos.ZERO,
                  (a, b) ->
                      new BlockPos(a.getX() + b.getX(), a.getY() + b.getY(), a.getZ() + b.getZ()));
      return new BlockPos(
          middle.getX() / this.innerBlocks.size(),
          middle.getY() / this.innerBlocks.size(),
          middle.getZ() / this.innerBlocks.size());
    }

    return this.origin != null ? this.origin.above() : BlockPos.ZERO;
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof PortalInfoData other && this.uuid.equals(other.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  @Override
  public String toString() {
    return "PortalInfoData{"
        + "uuid="
        + uuid
        + ", dimension="
        + dimension
        + ", origin="
        + origin
        + ", creator="
        + creator
        + ", color="
        + color
        + ", edgeBlockType="
        + edgeBlockType
        + ", lastUsed="
        + lastUsed
        + '}';
  }
}
