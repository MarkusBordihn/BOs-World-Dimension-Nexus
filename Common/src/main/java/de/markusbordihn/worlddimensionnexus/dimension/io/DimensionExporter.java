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

  public static boolean exportDimension(
      MinecraftServer minecraftServer, ResourceKey<Level> dimension, File exportFile) {
    ResourceLocation resourceLocation = dimension.location();

    // Check if prefix is correct.
    if (!exportFile.getName().endsWith(Constants.EXPORT_FILE_EXTENSION)) {
      String filename = exportFile.getAbsolutePath().replaceFirst("\\.zip$", "");
      exportFile = new File(filename + Constants.EXPORT_FILE_EXTENSION);
    }

    // Validate dimension folder.
    Path worldDir = minecraftServer.getWorldPath(LevelResource.ROOT);
    Path dimFolder =
        worldDir
            .resolve("dimensions")
            .resolve(resourceLocation.getNamespace())
            .resolve(resourceLocation.getPath());
    if (!Files.exists(dimFolder)) {
      log.error(
          "Dimension export failed: Dimension folder does not exist for dimension {}.",
          resourceLocation);
      return false;
    }

    // Get Dimension info data, if available.
    DimensionInfoData dimensionInfoData = DimensionManager.getDimensionInfo(dimension);
    if (dimensionInfoData == null) {
      log.warn(
          "Dimension info data not found for dimension {}. Exporting without default info.",
          resourceLocation);
    }

    // Export dimension folder to wdn file.
    try {
      log.info("Exporting dimension {} ...", resourceLocation);
      try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(exportFile))) {
        zos.setLevel(Deflater.BEST_COMPRESSION);

        // Add dimension info file, if available.
        if (dimensionInfoData != null) {
          zos.putNextEntry(new ZipEntry("dimension.info"));
          DimensionInfoData info = new DimensionInfoData(resourceLocation.getPath());
          var result =
              DimensionInfoData.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, info);
          if (result.result().isPresent()) {
            String json = result.result().get().toString();
            zos.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
          }
          zos.closeEntry();
        }

        // Add dimension files to the zip archive.
        Files.walk(dimFolder)
            .forEach(
                path -> {
                  if (!Files.isRegularFile(path)
                      || DimensionIOUtils.shouldSkipFile(dimFolder.relativize(path))) {
                    return;
                  }
                  String entryName = dimFolder.relativize(path).toString().replace("\\", "/");
                  try (InputStream is = Files.newInputStream(path)) {
                    zos.putNextEntry(new ZipEntry(entryName));
                    is.transferTo(zos);
                    zos.closeEntry();
                  } catch (IOException e) {
                    log.error(
                        "Failed to add file {} to export zip for dimension {}: {}",
                        path,
                        resourceLocation,
                        e);
                  }
                });
      }
      log.info("Dimension {} exported successfully to {}.", resourceLocation, exportFile);
      return true;
    } catch (IOException e) {
      log.error("Failed to export dimension {} to {}: {}", resourceLocation, exportFile, e);
      return false;
    }
  }

  public static boolean shouldSkipFile(Path relativePath) {
    String relName = relativePath.toString().replace("\\", "/").toLowerCase();

    // Skip known files and directories that should not be imported into the dimension folder.
    return relName.equals("dimension.info")
        || relName.equals("dimension.json")
        || relName.equals("session.lock")
        || relName.equals("icon.png")
        || relName.equals("level.dat_old")
        || relName.startsWith("playerdata/")
        || relName.startsWith("advancements/")
        || relName.startsWith("stats/")
        || relName.startsWith("poi/")
        || relName.startsWith(".")
        || relName.endsWith(".bak")
        || relName.endsWith(".tmp")
        || relName.endsWith(".swp");
  }
}
