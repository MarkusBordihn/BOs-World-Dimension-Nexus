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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

public class WorldDataPackResourceManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Resource Manager");

  private static final String[] worldDimensionFiles =
      new String[] {
        "/data/world_dimension_nexus/dimensions/lobby_dimension.wdn",
      };

  public static void copyDimensionFilesToWorld(MinecraftServer server) {
    // Create target directory for dimension files in the world datapack directory.
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

    // Copy dimension files from resources to the world datapack directory.
    for (String resourcePath : worldDimensionFiles) {
      String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
      Path fileTarget = targetPath.resolve(fileName);
      try (InputStream in = WorldDataPackResourceManager.class.getResourceAsStream(resourcePath)) {
        if (in != null) {
          Files.copy(in, fileTarget, StandardCopyOption.REPLACE_EXISTING);
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

  public static File getDataPackFile(MinecraftServer server, String fileName) {
    Path filePath = getDimensionDataPackPath(server, fileName);
    if (Files.exists(filePath)) {
      return filePath.toFile();
    } else {
      log.warn(
          "Data pack file {} does not exist in {}", fileName, getDimensionDataPackPath(server));
      return null;
    }
  }

  public static Path getDimensionDataPackPath(MinecraftServer server) {
    return server
        .getWorldPath(LevelResource.ROOT)
        .resolve("datapacks")
        .resolve(Constants.MOD_ID)
        .resolve("dimensions");
  }

  public static Path getDimensionDataPackPath(MinecraftServer server, String fileName) {
    Path dataPackPath = getDimensionDataPackPath(server);
    return dataPackPath.resolve(fileName);
  }
}
