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

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.markusbordihn.worlddimensionnexus.Constants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.storage.LevelResource;

public class DimensionImportFileSuggestion {

  public static CompletableFuture<Suggestions> suggestImportFiles(
      final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
    suggestFromDataFolder(context, builder);
    suggestFromDatapackFolder(context, builder);
    return builder.buildFuture();
  }

  public static CompletableFuture<Suggestions> suggestFromDataFolder(
      final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
    Path dataDir = context.getSource().getServer().getWorldPath(LevelResource.ROOT).resolve("data");
    try (Stream<Path> files = Files.walk(dataDir)) {
      files
          .filter(
              path ->
                  Files.isRegularFile(path)
                      && path.toString().endsWith(Constants.EXPORT_FILE_EXTENSION))
          .map(Path::getFileName)
          .map(Path::toString)
          .forEach(builder::suggest);
    } catch (Exception ignored) {
    }
    return builder.buildFuture();
  }

  private static void suggestFromDatapackFolder(
      final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {
    Path wdnFolder =
        context
            .getSource()
            .getServer()
            .getWorldPath(LevelResource.ROOT)
            .resolve("datapacks")
            .resolve(Constants.MOD_ID)
            .resolve("dimensions");

    if (Files.exists(wdnFolder)) {
      try (Stream<Path> files = Files.walk(wdnFolder, 1)) {
        files
            .filter(
                path ->
                    Files.isRegularFile(path)
                        && path.toString().endsWith(Constants.EXPORT_FILE_EXTENSION))
            .map(Path::getFileName)
            .map(Path::toString)
            .forEach(builder::suggest);
      } catch (Exception ignored) {
      }
    }
  }
}
