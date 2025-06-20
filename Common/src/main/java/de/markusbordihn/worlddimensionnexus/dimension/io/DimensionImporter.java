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

package de.markusbordihn.worlddimensionnexus.dimension.io;

import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class DimensionImporter {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Dimension Importer");

  public static boolean importDimension(
      MinecraftServer minecraftServer,
      File importFile,
      ResourceLocation dimensionId,
      ResourceLocation dimensionTypeId)
      throws IOException {
    Path tempDir = Files.createTempDirectory("import_dimension");

    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(importFile))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        Path filePath = tempDir.resolve(entry.getName());
        if (entry.isDirectory()) {
          Files.createDirectories(filePath);
        } else {
          Files.createDirectories(filePath.getParent());
          try (OutputStream os = Files.newOutputStream(filePath)) {
            zipInputStream.transferTo(os);
          }
        }
      }
    }

    // Default values
    String namespace = Constants.MOD_ID;
    String path = importFile.getName().replaceAll("\\..*$", "");
    String type = "minecraft:overworld";

    // Try reading dimension.info with Codec (preferred)
    Path infoFile = tempDir.resolve("dimension.info");
    if (!Files.exists(infoFile)) {
      infoFile = tempDir.resolve("dimension.json");
    }
    if (Files.exists(infoFile)) {
      try (Reader reader = Files.newBufferedReader(infoFile)) {
        com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(reader);
        var result =
            DimensionInfoData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, jsonElement);
        if (result.result().isPresent()) {
          DimensionInfoData info = result.result().get();
          namespace = info.name().location().getNamespace();
          path = info.name().location().getPath();
          type = info.type().location().toString();
        }
      }
    }

    // Overwrite with command parameters if given
    if (dimensionId != null) {
      namespace = dimensionId.getNamespace();
      path = dimensionId.getPath();
    }
    if (dimensionTypeId != null) {
      type = dimensionTypeId.toString();
    }

    Path worldDir = minecraftServer.getWorldPath(LevelResource.ROOT);
    Path targetDir = worldDir.resolve("dimensions").resolve(namespace).resolve(path);
    Files.createDirectories(targetDir);

    // Copy files from tempDir to targetDir
    try (Stream<Path> files = Files.walk(tempDir)) {
      files.forEach(
          source -> {
            Path dest = targetDir.resolve(tempDir.relativize(source).toString());
            if (DimensionIOUtils.shouldSkipFile(tempDir.relativize(source))) {
              return;
            }
            try {
              if (Files.isDirectory(source)) {
                Files.createDirectories(dest);
              } else {
                Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
              }
            } catch (IOException ignored) {
            }
          });
    }

    // Cleanup
    try (Stream<Path> walk = Files.walk(tempDir)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(
              p -> {
                try {
                  Files.delete(p);
                } catch (IOException ignored) {
                }
              });
    }

    log.info("Imported dimension {}:{} (type: {}) from {}", namespace, path, type, importFile);
    return true;
  }
}
