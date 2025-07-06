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
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.io.*;
import java.nio.file.*;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

public class DimensionExporter {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Dimension Exporter");

  private static final String DIMENSION_JSON_FILE = "dimension.json";
  private static final String DIMENSIONS_DIR = "dimensions";
  private static final String PATH_SEPARATOR = "/";
  private static final String BACKSLASH_SEPARATOR = "\\";

  public static boolean exportDimension(
      final MinecraftServer minecraftServer, final ResourceKey<Level> dimension, File exportFile) {
    ResourceLocation resourceLocation = dimension.location();

    // Check if prefix is correct.
    if (!exportFile.getName().endsWith(Constants.EXPORT_FILE_EXTENSION)) {
      String filename = exportFile.getAbsolutePath().replaceFirst("\\.zip$", "");
      exportFile = new File(filename + Constants.EXPORT_FILE_EXTENSION);
    }

    // Validate dimension folder.
    Path worldDir = minecraftServer.getWorldPath(LevelResource.ROOT);
    Path dimensionFolder =
        worldDir
            .resolve(DIMENSIONS_DIR)
            .resolve(resourceLocation.getNamespace())
            .resolve(resourceLocation.getPath());
    if (!Files.exists(dimensionFolder)) {
      log.error(
          "Dimension export failed: Dimension folder does not exist for dimension {}.",
          resourceLocation);
      return false;
    }

    // Get Dimension info data, if available.
    DimensionInfoData dimensionInfoData = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfoData == null) {
      dimensionInfoData =
          DimensionInfoData.forImport(
              resourceLocation.getNamespace(),
              resourceLocation.getPath(),
              DimensionInfoData.DEFAULT_TYPE);
      log.info("Created default dimension info data for dimension {} export.", resourceLocation);
    }

    // Export dimension folder to wdn file.
    try {
      log.info("Exporting dimension {} ...", resourceLocation);
      try (ZipOutputStream zipOutputStream =
          new ZipOutputStream(new FileOutputStream(exportFile))) {
        zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);
        addDimensionInfoToZip(zipOutputStream, dimensionInfoData);
        addDimensionFilesToZip(zipOutputStream, dimensionFolder, resourceLocation);
      }
      log.info("Dimension {} exported successfully to {}.", resourceLocation, exportFile);
      return true;
    } catch (IOException e) {
      log.error("Failed to export dimension {} to {}: {}", resourceLocation, exportFile, e);
      return false;
    }
  }

  private static void addDimensionInfoToZip(
      final ZipOutputStream zipOutputStream, final DimensionInfoData dimensionInfoData)
      throws IOException {
    zipOutputStream.putNextEntry(new ZipEntry(DIMENSION_JSON_FILE));
    String jsonContent = dimensionInfoData.toJson().toString();
    zipOutputStream.write(jsonContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    zipOutputStream.closeEntry();
  }

  private static void addDimensionFilesToZip(
      final ZipOutputStream zipOutputStream,
      final Path dimensionFolder,
      final ResourceLocation resourceLocation)
      throws IOException {
    Files.walk(dimensionFolder)
        .forEach(
            filePath -> {
              if (!Files.isRegularFile(filePath)
                  || DimensionIOUtils.shouldSkipFile(dimensionFolder.relativize(filePath))) {
                return;
              }
              String zipEntryName =
                  dimensionFolder
                      .relativize(filePath)
                      .toString()
                      .replace(BACKSLASH_SEPARATOR, PATH_SEPARATOR);
              try (InputStream inputStream = Files.newInputStream(filePath)) {
                zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                inputStream.transferTo(zipOutputStream);
                zipOutputStream.closeEntry();
              } catch (IOException e) {
                log.error(
                    "Failed to add file {} to export zip for dimension {}: {}",
                    filePath,
                    resourceLocation,
                    e);
              }
            });
  }
}
