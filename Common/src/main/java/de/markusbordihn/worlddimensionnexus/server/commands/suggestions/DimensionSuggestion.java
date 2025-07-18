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
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class DimensionSuggestion {

  public static final SuggestionProvider<CommandSourceStack> DIMENSION_NAMES =
      (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        for (ServerLevel level : server.getAllLevels()) {
          String dimensionResourceKey = level.dimension().location().toString();
          builder.suggest(dimensionResourceKey);
        }
        return builder.buildFuture();
      };

  public static final SuggestionProvider<CommandSourceStack> ALL_DIMENSIONS =
      (context, builder) -> {
        MinecraftServer server = context.getSource().getServer();
        for (ServerLevel level : server.getAllLevels()) {
          String dimensionResourceKey = level.dimension().location().toString();
          builder.suggest(dimensionResourceKey);
        }
        return builder.buildFuture();
      };

  public static final SuggestionProvider<CommandSourceStack> CUSTOM_DIMENSIONS =
      (context, builder) ->
          SharedSuggestionProvider.suggest(DimensionManager.getDimensionNames(), builder);
}
