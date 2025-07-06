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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.markusbordihn.worlddimensionnexus.resources.WorldDataPackResourceManager;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.commands.CommandSourceStack;

public class DimensionTypeSuggestion {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Dimension Type Suggestion");

  public static CompletableFuture<Suggestions> suggestDimensionTypesFromFile(
      final CommandContext<CommandSourceStack> context, final SuggestionsBuilder builder) {

    try {
      String fileName = StringArgumentType.getString(context, "file");
      Set<String> extractedDimensionTypes = extractDimensionTypesFromWdnFile(context, fileName);

      String currentInput = builder.getRemaining().toLowerCase();

      // Add extracted dimension types from the file
      extractedDimensionTypes.stream()
          .filter(dimensionType -> dimensionType.toLowerCase().startsWith(currentInput))
          .sorted()
          .forEach(builder::suggest);

      // If no dimension types were found in file, suggest common fallbacks
      if (extractedDimensionTypes.isEmpty()) {
        suggestCommonDimensionTypes(builder, currentInput);
      }

    } catch (Exception e) {
      log.warn("Failed to extract dimension types from file: {}", e.getMessage());
      suggestCommonDimensionTypes(builder, builder.getRemaining().toLowerCase());
    }

    return builder.buildFuture();
  }

  private static Set<String> extractDimensionTypesFromWdnFile(
      final CommandContext<CommandSourceStack> context, final String fileName) {
    Set<String> dimensionTypes = new HashSet<>();

    File importFile =
        WorldDataPackResourceManager.getDataPackFile(context.getSource().getServer(), fileName);

    if (importFile == null || !importFile.exists()) {
      log.debug("Import file not found: {}", fileName);
      return dimensionTypes;
    }

    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(importFile))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        String entryName = entry.getName();

        // Look for dimension_type files in the structure
        // Expected pattern: data/{namespace}/dimension_type/{type_name}.json
        if (entryName.contains("dimension_type") && entryName.endsWith(".json")) {
          String extractedType = extractDimensionTypeFromPath(entryName);
          if (extractedType != null) {
            dimensionTypes.add(extractedType);
          }
        }
      }
    } catch (IOException e) {
      log.warn("Failed to read WDN file {}: {}", fileName, e.getMessage());
    }

    return dimensionTypes;
  }

  private static String extractDimensionTypeFromPath(final String zipEntryPath) {
    // Extract dimension type from path like: data/minecraft/dimension_type/overworld.json
    // or data/world_dimension_nexus/dimension_type/custom_type.json
    String[] pathParts = zipEntryPath.split("/");

    if (pathParts.length >= 4 && "dimension_type".equals(pathParts[2])) {
      String namespace = pathParts[1];
      String typeName = pathParts[3].replaceAll("\\.json$", "");
      return namespace + ":" + typeName;
    }

    return null;
  }

  private static void suggestCommonDimensionTypes(
      final SuggestionsBuilder builder, final String currentInput) {
    String[] commonTypes = {"minecraft:overworld", "minecraft:nether", "minecraft:the_end"};

    for (String type : commonTypes) {
      if (type.toLowerCase().startsWith(currentInput)) {
        builder.suggest(type);
      }
    }
  }
}
