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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.data.chunk.ChunkGeneratorType;
import de.markusbordihn.worlddimensionnexus.data.dimension.DimensionInfoData;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

public class DimensionImporter {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Dimension Importer");
  private static final String DIMENSION_JSON_FILE = "dimension.json";
  private static final String FILE_EXTENSION_PATTERN = "\\..*$";
  private static final String TEMP_DIR_PREFIX = "import_dimension";
  private static final String DIMENSIONS_DIR = "dimensions";

  public static boolean importDimension(
      final MinecraftServer minecraftServer,
      final File importFile,
      final ResourceLocation dimensionId,
      final ResourceLocation dimensionTypeId)
      throws IOException {
    Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);

    try {
      extractZipFile(importFile, tempDir);
      DimensionInfoData fileBasedDimensionInfo = readDimensionInfoFromFile(tempDir);

      String finalDimensionName =
          determineFinalDimensionName(
              importFile,
              fileBasedDimensionInfo,
              dimensionId != null ? dimensionId.getPath() : null);

      ChunkGeneratorType chunkGeneratorTypeOverride = null;
      if (dimensionTypeId != null) {
        try {
          chunkGeneratorTypeOverride = ChunkGeneratorType.fromString(dimensionTypeId.getPath());
        } catch (Exception e) {
          log.warn("Could not convert dimension type '{}' to ChunkGeneratorType", dimensionTypeId);
        }
      }

      ChunkGeneratorType finalChunkGeneratorType =
          determineFinalChunkGeneratorType(fileBasedDimensionInfo, chunkGeneratorTypeOverride);

      String fullDimensionName = Constants.MOD_ID + ":" + finalDimensionName;
      if (dimensionExists(minecraftServer, fullDimensionName)) {
        log.warn("Dimension '{}' already exists, import cancelled", fullDimensionName);
        return false;
      }

      DimensionInfoData finalDimensionInfo =
          createFinalDimensionInfo(
              fileBasedDimensionInfo, finalDimensionName, finalChunkGeneratorType);

      copyDimensionFiles(minecraftServer, tempDir, finalDimensionInfo);

      // Register the imported dimension with the correct spawn point
      ServerLevel importedLevel = DimensionManager.addOrCreateDimension(finalDimensionInfo, true);
      if (importedLevel == null) {
        log.warn("Failed to create dimension {}", finalDimensionInfo.getResourceLocation());
        return false;
      }

      log.info(
          "Imported dimension {} from {}",
          finalDimensionInfo.resourceLocation(),
          importFile.getName());
      return true;
    } finally {
      cleanupTempDirectory(tempDir);
    }
  }

  public static boolean importDimension(
      final MinecraftServer minecraftServer,
      final File importFile,
      final String dimensionNameOverride,
      final ChunkGeneratorType chunkGeneratorTypeOverride)
      throws IOException {
    ResourceLocation dimensionId = null;
    if (dimensionNameOverride != null && !dimensionNameOverride.trim().isEmpty()) {
      dimensionId = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, dimensionNameOverride);
    }

    ResourceLocation dimensionTypeId = null;
    if (chunkGeneratorTypeOverride != null) {
      dimensionTypeId =
          ResourceLocation.fromNamespaceAndPath("minecraft", chunkGeneratorTypeOverride.getName());
    }

    return importDimension(minecraftServer, importFile, dimensionId, dimensionTypeId);
  }

  private static void extractZipFile(final File importFile, final Path tempDir) throws IOException {
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(importFile))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        extractZipEntry(zipInputStream, entry, tempDir);
      }
    }
  }

  private static void extractZipEntry(
      final ZipInputStream zipInputStream, final ZipEntry entry, final Path tempDir)
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

  private static DimensionInfoData readDimensionInfoFromFile(final Path tempDir) {
    Path infoFile = findDimensionInfoFile(tempDir);
    if (infoFile == null) {
      return null;
    }

    try (Reader reader = Files.newBufferedReader(infoFile)) {
      JsonElement jsonElement = JsonParser.parseReader(reader);
      if (jsonElement.isJsonObject()) {
        return DimensionInfoData.fromJson(jsonElement.getAsJsonObject());
      }
    } catch (IOException e) {
      log.warn("Failed to read dimension info file: {}", e.getMessage());
    } catch (Exception e) {
      log.warn("Failed to parse dimension info JSON: {}", e.getMessage());
    }
    return null;
  }

  private static Path findDimensionInfoFile(final Path tempDir) {
    Path infoFile = tempDir.resolve(DIMENSION_JSON_FILE);
    return Files.exists(infoFile) ? infoFile : null;
  }

  private static void copyDimensionFiles(
      final MinecraftServer minecraftServer, final Path tempDir, final DimensionInfoData importData)
      throws IOException {
    Path worldDir = minecraftServer.getWorldPath(LevelResource.ROOT);
    Path targetDir =
        worldDir
            .resolve(DIMENSIONS_DIR)
            .resolve(importData.resourceLocation().getNamespace())
            .resolve(importData.resourceLocation().getPath());
    Files.createDirectories(targetDir);

    try (Stream<Path> files = Files.walk(tempDir)) {
      files.forEach(source -> copyFileIfNeeded(source, tempDir, targetDir));
    }
  }

  private static void copyFileIfNeeded(
      final Path source, final Path tempDir, final Path targetDir) {
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

  private static void cleanupTempDirectory(final Path tempDir) {
    try (Stream<Path> walk = Files.walk(tempDir)) {
      walk.sorted(Comparator.reverseOrder()).forEach(DimensionImporter::deleteQuietly);
    } catch (IOException e) {
      log.warn("Failed to cleanup temp directory {}: {}", tempDir, e.getMessage());
    }
  }

  private static void deleteQuietly(final Path path) {
    try {
      Files.delete(path);
    } catch (IOException e) {
      log.debug("Could not delete {}: {}", path, e.getMessage());
    }
  }

  private static String determineFinalDimensionName(
      final File importFile,
      final DimensionInfoData fileBasedDimensionInfo,
      final String dimensionNameOverride) {

    if (dimensionNameOverride != null && !dimensionNameOverride.trim().isEmpty()) {
      return dimensionNameOverride;
    }

    if (fileBasedDimensionInfo != null
        && fileBasedDimensionInfo.displayName() != null
        && !fileBasedDimensionInfo.displayName().trim().isEmpty()) {
      return fileBasedDimensionInfo.displayName();
    }

    if (fileBasedDimensionInfo != null
        && fileBasedDimensionInfo.resourceLocation().getPath() != null
        && !fileBasedDimensionInfo.resourceLocation().getPath().trim().isEmpty()) {
      return fileBasedDimensionInfo.resourceLocation().getPath();
    }

    return importFile.getName().replaceAll(FILE_EXTENSION_PATTERN, "");
  }

  private static ChunkGeneratorType determineFinalChunkGeneratorType(
      final DimensionInfoData fileBasedDimensionInfo,
      final ChunkGeneratorType chunkGeneratorTypeOverride) {

    if (chunkGeneratorTypeOverride != null) {
      return chunkGeneratorTypeOverride;
    }

    if (fileBasedDimensionInfo != null && fileBasedDimensionInfo.chunkGeneratorType() != null) {
      return fileBasedDimensionInfo.chunkGeneratorType();
    }

    return DimensionInfoData.DEFAULT_CHUNK_GENERATOR_TYPE;
  }

  private static DimensionInfoData createFinalDimensionInfo(
      final DimensionInfoData fileBasedDimensionInfo,
      final String finalDimensionName,
      final ChunkGeneratorType finalChunkGeneratorType) {

    if (fileBasedDimensionInfo != null) {
      return new DimensionInfoData(
          ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, finalDimensionName),
          fileBasedDimensionInfo.dimensionTypeKey(),
          finalDimensionName,
          fileBasedDimensionInfo.description(),
          fileBasedDimensionInfo.isCustom(),
          finalChunkGeneratorType,
          true,
          fileBasedDimensionInfo.spawnPoint(),
          fileBasedDimensionInfo.gameType());
    }

    return DimensionInfoData.fromDimensionNameAndType(finalDimensionName, finalChunkGeneratorType);
  }

  private static boolean dimensionExists(final MinecraftServer server, final String dimensionName) {
    try {
      return DimensionManager.dimensionExists(server, dimensionName);
    } catch (Exception e) {
      log.warn("Error checking if dimension exists: {}", e.getMessage());
      return false;
    }
  }
}
