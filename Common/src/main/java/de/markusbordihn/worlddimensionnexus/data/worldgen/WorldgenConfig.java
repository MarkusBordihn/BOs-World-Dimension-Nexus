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

package de.markusbordihn.worlddimensionnexus.data.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public record WorldgenConfig(
    ChunkGeneratorType type,
    Optional<ResourceLocation> noiseSettings,
    Optional<ResourceLocation> biomeSource,
    Map<String, String> customSettings) {

  public static final Codec<WorldgenConfig> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      ChunkGeneratorType.CODEC.fieldOf("type").forGetter(WorldgenConfig::type),
                      ResourceLocation.CODEC
                          .optionalFieldOf("noise_settings")
                          .forGetter(WorldgenConfig::noiseSettings),
                      ResourceLocation.CODEC
                          .optionalFieldOf("biome_source")
                          .forGetter(WorldgenConfig::biomeSource),
                      Codec.unboundedMap(Codec.STRING, Codec.STRING)
                          .optionalFieldOf("custom_settings", Map.of())
                          .forGetter(WorldgenConfig::customSettings))
                  .apply(instance, WorldgenConfig::new));
}
