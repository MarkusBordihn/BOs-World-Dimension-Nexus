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

package de.markusbordihn.worlddimensionnexus.resources;

import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class WorldDataPackResourceManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Resource Manager");

  private static final String DIMENSIONS_RESOURCE_PATH = "/data/world_dimension_nexus/dimensions";

  /**
   * Gets all dimension files (.wdn) from the resources directory dynamically.
   *
   * @return List of resource paths for dimension files
   */
  private static List<String> getDimensionResourceFiles() {
    List<String> dimensionFiles = new ArrayList<>();

    try {
      java.net.URL resourceUrl = WorldDataPackResourceManager.class.getResource(DIMENSIONS_RESOURCE_PATH);
      if (resourceUrl == null) {
        log.warn("Dimensions resource path not found: {}", DIMENSIONS_RESOURCE_PATH);
        return dimensionFiles;
      }

      URI resourceUri = resourceUrl.toURI();

      if (resourceUri.getScheme().equals("jar")) {
        // Running from JAR file
        try (FileSystem fileSystem = FileSystems.newFileSystem(resourceUri, Collections.emptyMap())) {
          Path dimensionsPath = fileSystem.getPath(DIMENSIONS_RESOURCE_PATH);
          if (Files.exists(dimensionsPath)) {
            try (Stream<Path> files = Files.list(dimensionsPath)) {
              files.filter(path -> path.toString().endsWith(".wdn"))
                  .forEach(path -> dimensionFiles.add(DIMENSIONS_RESOURCE_PATH + "/" + path.getFileName().toString()));
            }
          }
        }
      } else {
        // Running from development environment
        Path dimensionsPath = Paths.get(resourceUri);
        if (Files.exists(dimensionsPath)) {
          try (Stream<Path> files = Files.list(dimensionsPath)) {
            files.filter(path -> path.toString().endsWith(".wdn"))
                .forEach(path -> dimensionFiles.add(DIMENSIONS_RESOURCE_PATH + "/" + path.getFileName().toString()));
          }
        }
      }
    } catch (URISyntaxException | IOException e) {
      log.error("Failed to load dimension files from resources: {}", e.getMessage());
    }

    log.info("Found {} dimension files: {}", dimensionFiles.size(), dimensionFiles);
    return dimensionFiles;
  }

  public static void copyDimensionFilesToWorld(final MinecraftServer server) {
    Path targetPath = getDimensionDataPackPath(server);
    if (!Files.exists(targetPath)) {
      log.info("Creating target directory for dimension files: {}", targetPath);
      try {
        Files.createDirectories(targetPath);
      } catch (IOException e) {
        log.error("Failed to create target directory: {}", targetPath);
        return;
      }
    }

    List<String> worldDimensionFiles = getDimensionResourceFiles();
    for (String resourcePath : worldDimensionFiles) {
      String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
      Path fileTarget = targetPath.resolve(fileName);
      try (InputStream inputStream =
          WorldDataPackResourceManager.class.getResourceAsStream(resourcePath)) {
        if (inputStream != null) {
          Files.copy(inputStream, fileTarget, StandardCopyOption.REPLACE_EXISTING);
          log.info("Copied {} to {}", resourcePath, fileTarget);
        } else {
          log.warn("Resource {} not found in JAR!", resourcePath);
        }
      } catch (IOException e) {
        log.error(
            "Failed to copy dimension file {} to {}: {}", resourcePath, fileTarget, e.getMessage());
      }
    }
  }

  public static File getDataPackFile(final MinecraftServer server, final String fileName) {
    Path filePath = getDimensionDataPackPath(server, fileName);
    if (Files.exists(filePath)) {
      return filePath.toFile();
    } else {
      log.warn(
          "Data pack file {} does not exist in {}", fileName, getDimensionDataPackPath(server));
      return null;
    }
  }

  public static Path getDimensionDataPackPath(final MinecraftServer server) {
    return server
        .getWorldPath(LevelResource.ROOT)
        .resolve("datapacks")
        .resolve(Constants.MOD_ID)
        .resolve("dimensions");
  }

  public static Path getDimensionDataPackPath(final MinecraftServer server, final String fileName) {
    Path dataPackPath = getDimensionDataPackPath(server);
    return dataPackPath.resolve(fileName);
  }
}
