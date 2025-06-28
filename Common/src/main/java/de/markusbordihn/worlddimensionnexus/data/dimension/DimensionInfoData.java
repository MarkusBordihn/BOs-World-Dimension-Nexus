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

package de.markusbordihn.worlddimensionnexus.data.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.levelgen.ChunkGeneratorHelper;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;

public record DimensionInfoData(
    UUID uuid,
    ResourceKey<Level> name,
    ResourceKey<DimensionType> type,
    ChunkGeneratorType chunkGeneratorType) {

  public static final Codec<DimensionInfoData> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC.fieldOf("uuid").forGetter(DimensionInfoData::uuid),
                      ResourceKey.codec(Registries.DIMENSION)
                          .fieldOf("dimension_name")
                          .forGetter(DimensionInfoData::name),
                      ResourceKey.codec(Registries.DIMENSION_TYPE)
                          .fieldOf("dimension_type")
                          .forGetter(DimensionInfoData::type),
                      ChunkGeneratorType.CODEC
                          .fieldOf("chunk_generator_type")
                          .forGetter(DimensionInfoData::chunkGeneratorType))
                  .apply(instance, DimensionInfoData::new));

  public DimensionInfoData(String name) {
    this(
        UUID.randomUUID(),
        ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name)),
        BuiltinDimensionTypes.OVERWORLD,
        ChunkGeneratorType.FLAT);
  }

  public DimensionInfoData(String name, ChunkGeneratorType chunkGeneratorType) {
    this(
        UUID.randomUUID(),
        ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, name)),
        BuiltinDimensionTypes.OVERWORLD,
        chunkGeneratorType);
  }

  public ChunkGenerator getChunkGenerator(MinecraftServer server) {
    return ChunkGeneratorHelper.getDefault(server, this.chunkGeneratorType);
  }

  public Holder<DimensionType> getDimensionTypeHolder(MinecraftServer server) {
    return server
        .registryAccess()
        .registryOrThrow(Registries.DIMENSION_TYPE)
        .getHolderOrThrow(this.type);
  }

  @Override
  public boolean equals(Object object) {
    return object instanceof DimensionInfoData other && this.uuid.equals(other.uuid);
  }

  @Override
  public int hashCode() {
    return uuid.hashCode();
  }
}
