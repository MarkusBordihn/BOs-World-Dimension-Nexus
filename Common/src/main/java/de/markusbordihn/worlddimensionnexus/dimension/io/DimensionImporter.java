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

    try {
      extractZipFile(importFile, tempDir);
      DimensionImportData importData =
          extractDimensionInfo(importFile, tempDir, dimensionId, dimensionTypeId);
      copyDimensionFiles(minecraftServer, tempDir, importData);

      log.info(
          "Imported dimension {}:{} (type: {}) from {}",
          importData.namespace,
          importData.path,
          importData.type,
          importFile);
      return true;
    } finally {
      cleanupTempDirectory(tempDir);
    }
  }

  private static void extractZipFile(File importFile, Path tempDir) throws IOException {
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(importFile))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        extractZipEntry(zipInputStream, entry, tempDir);
      }
    }
  }

  private static void extractZipEntry(ZipInputStream zipInputStream, ZipEntry entry, Path tempDir)
      throws IOException {
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

  private static DimensionImportData extractDimensionInfo(
      File importFile, Path tempDir, ResourceLocation dimensionId, ResourceLocation dimensionTypeId)
      throws IOException {
    // Default values
    String namespace = Constants.MOD_ID;
    String path = importFile.getName().replaceAll("\\..*$", "");
    String type = "minecraft:overworld";

    // Try reading dimension info from files
    DimensionImportData fileData = readDimensionInfoFromFile(tempDir);
    if (fileData != null) {
      namespace = fileData.namespace;
      path = fileData.path;
      type = fileData.type;
    }

    // Override with command parameters if provided
    if (dimensionId != null) {
      namespace = dimensionId.getNamespace();
      path = dimensionId.getPath();
    }
    if (dimensionTypeId != null) {
      type = dimensionTypeId.toString();
    }

    return new DimensionImportData(namespace, path, type);
  }

  private static DimensionImportData readDimensionInfoFromFile(Path tempDir) {
    Path infoFile = findDimensionInfoFile(tempDir);
    if (infoFile == null) {
      return null;
    }

    try (Reader reader = Files.newBufferedReader(infoFile)) {
      com.google.gson.JsonElement jsonElement = com.google.gson.JsonParser.parseReader(reader);
      var result =
          DimensionInfoData.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, jsonElement);

      if (result.result().isPresent()) {
        DimensionInfoData info = result.result().get();
        return new DimensionImportData(
            info.name().location().getNamespace(),
            info.name().location().getPath(),
            info.type().location().toString());
      }
    } catch (IOException e) {
      log.warn("Failed to read dimension info file: {}", e.getMessage());
    }
    return null;
  }

  private static Path findDimensionInfoFile(Path tempDir) {
    Path infoFile = tempDir.resolve("dimension.info");
    if (Files.exists(infoFile)) {
      return infoFile;
    }

    infoFile = tempDir.resolve("dimension.json");
    if (Files.exists(infoFile)) {
      return infoFile;
    }

    return null;
  }

  private static void copyDimensionFiles(
      MinecraftServer minecraftServer, Path tempDir, DimensionImportData importData)
      throws IOException {
    Path worldDir = minecraftServer.getWorldPath(LevelResource.ROOT);
    Path targetDir =
        worldDir.resolve("dimensions").resolve(importData.namespace).resolve(importData.path);
    Files.createDirectories(targetDir);

    try (Stream<Path> files = Files.walk(tempDir)) {
      files.forEach(source -> copyFileIfNeeded(source, tempDir, targetDir));
    }
  }

  private static void copyFileIfNeeded(Path source, Path tempDir, Path targetDir) {
    Path relativePath = tempDir.relativize(source);
    if (DimensionIOUtils.shouldSkipFile(relativePath)) {
      return;
    }

    Path dest = targetDir.resolve(relativePath.toString());
    try {
      if (Files.isDirectory(source)) {
        Files.createDirectories(dest);
      } else {
        Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      log.warn("Failed to copy file {}: {}", source, e.getMessage());
    }
  }

  private static void cleanupTempDirectory(Path tempDir) {
    try (Stream<Path> walk = Files.walk(tempDir)) {
      walk.sorted(Comparator.reverseOrder()).forEach(DimensionImporter::deleteQuietly);
    } catch (IOException e) {
      log.warn("Failed to cleanup temp directory {}: {}", tempDir, e.getMessage());
    }
  }

  private static void deleteQuietly(Path path) {
    try {
      Files.delete(path);
    } catch (IOException e) {
      log.debug("Could not delete {}: {}", path, e.getMessage());
    }
  }

  private static class DimensionImportData {
    final String namespace;
    final String path;
    final String type;

    DimensionImportData(String namespace, String path, String type) {
      this.namespace = namespace;
      this.path = path;
      this.type = type;
    }
  }
}
