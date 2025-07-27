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

package de.markusbordihn.worlddimensionnexus.data.warp;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record WarpData(
    UUID uuid,
    String name,
    String description,
    UUID owner,
    ResourceKey<Level> dimension,
    BlockPos position,
    float yaw,
    float pitch,
    WarpType type,
    long createdTime,
    boolean enabled,
    long disabledUntil) {

  public static final String UUID_TAG = "uuid";
  public static final String NAME_TAG = "name";
  public static final String DESCRIPTION_TAG = "description";
  public static final String OWNER_TAG = "owner";
  public static final String DIMENSION_TAG = "dimension";
  public static final String POSITION_TAG = "position";
  public static final String YAW_TAG = "yaw";
  public static final String PITCH_TAG = "pitch";
  public static final String TYPE_TAG = "type";
  public static final String CREATED_TIME_TAG = "createdTime";
  public static final String ENABLED_TAG = "enabled";
  public static final String DISABLED_UNTIL_TAG = "disabledUntil";

  public static final String DEFAULT_DESCRIPTION = "";
  public static final boolean DEFAULT_ENABLED = true;
  public static final long DEFAULT_DISABLED_UNTIL = 0L;

  public static final Codec<ResourceKey<Level>> LEVEL_KEY_CODEC =
      net.minecraft.resources.ResourceKey.codec(Registries.DIMENSION);

  public static final Codec<WarpData> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf(UUID_TAG).forGetter(WarpData::uuid),
                      Codec.STRING.fieldOf(NAME_TAG).forGetter(WarpData::name),
                      Codec.STRING
                          .optionalFieldOf(DESCRIPTION_TAG, DEFAULT_DESCRIPTION)
                          .forGetter(WarpData::description),
                      UUIDUtil.CODEC.fieldOf(OWNER_TAG).forGetter(WarpData::owner),
                      LEVEL_KEY_CODEC.fieldOf(DIMENSION_TAG).forGetter(WarpData::dimension),
                      BlockPos.CODEC.fieldOf(POSITION_TAG).forGetter(WarpData::position),
                      Codec.FLOAT.fieldOf(YAW_TAG).forGetter(WarpData::yaw),
                      Codec.FLOAT.fieldOf(PITCH_TAG).forGetter(WarpData::pitch),
                      WarpType.CODEC.fieldOf(TYPE_TAG).forGetter(WarpData::type),
                      Codec.LONG
                          .optionalFieldOf(CREATED_TIME_TAG, System.currentTimeMillis())
                          .forGetter(WarpData::createdTime),
                      Codec.BOOL
                          .optionalFieldOf(ENABLED_TAG, DEFAULT_ENABLED)
                          .forGetter(WarpData::enabled),
                      Codec.LONG
                          .optionalFieldOf(DISABLED_UNTIL_TAG, DEFAULT_DISABLED_UNTIL)
                          .forGetter(WarpData::disabledUntil))
                  .apply(instance, WarpData::new));

  public WarpData(
      final String name,
      final String description,
      final UUID owner,
      final ResourceKey<Level> dimension,
      final BlockPos position,
      final float yaw,
      final float pitch,
      final WarpType type) {
    this(
        UUID.randomUUID(),
        name,
        description,
        owner,
        dimension,
        position,
        yaw,
        pitch,
        type,
        System.currentTimeMillis(),
        true,
        0L);
  }

  public WarpData withDisabled(long disabledUntilTime) {
    return new WarpData(
        this.uuid,
        this.name,
        this.description,
        this.owner,
        this.dimension,
        this.position,
        this.yaw,
        this.pitch,
        this.type,
        this.createdTime,
        false,
        disabledUntilTime);
  }

  public WarpData withEnabled() {
    return new WarpData(
        this.uuid,
        this.name,
        this.description,
        this.owner,
        this.dimension,
        this.position,
        this.yaw,
        this.pitch,
        this.type,
        this.createdTime,
        true,
        0L);
  }

  public boolean isExpired() {
    return !enabled && disabledUntil > 0 && System.currentTimeMillis() > disabledUntil;
  }

  public boolean isAccessibleBy(UUID playerUuid) {
    if (!enabled) {
      return false;
    }

    return switch (type) {
      case PRIVATE -> owner.equals(playerUuid);
      case PUBLIC -> true;
    };
  }

  public String getDisplayName() {
    if (description != null && !description.trim().isEmpty()) {
      return name + " - " + description;
    }
    return name;
  }

  @Override
  public boolean equals(final Object object) {
    return object instanceof WarpData other && this.uuid.equals(other.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }

  @Override
  public String toString() {
    return "WarpData{"
        + "uuid="
        + uuid
        + ", name='"
        + name
        + "'"
        + ", owner="
        + owner
        + ", dimension="
        + dimension
        + ", position="
        + position
        + ", type="
        + type
        + ", enabled="
        + enabled
        + '}';
  }
}
