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

package de.markusbordihn.worlddimensionnexus.server.commands.suggestions;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

public class ChunkGeneratorTypeSuggestion {

  public static final SuggestionProvider<CommandSourceStack> CHUNK_GENERATOR_TYPES =
      (context, builder) -> {
        for (ChunkGeneratorType type : ChunkGeneratorType.values()) {
          builder.suggest(type.getName());
        }
        return builder.buildFuture();
      };

  public static final SuggestionProvider<CommandSourceStack> CHUNK_GENERATOR_NAMES =
      (context, builder) -> {
        String[] typeNames = new String[ChunkGeneratorType.values().length];
        for (int i = 0; i < ChunkGeneratorType.values().length; i++) {
          typeNames[i] = ChunkGeneratorType.values()[i].getName();
        }
        return SharedSuggestionProvider.suggest(typeNames, builder);
      };

  public static ChunkGeneratorType parseChunkGeneratorType(String typeName) {
    for (ChunkGeneratorType type : ChunkGeneratorType.values()) {
      if (type.getName().equalsIgnoreCase(typeName)) {
        return type;
      }
    }
    return null;
  }
}
