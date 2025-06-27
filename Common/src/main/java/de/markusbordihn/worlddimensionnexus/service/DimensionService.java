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

package de.markusbordihn.worlddimensionnexus.service;

import de.markusbordihn.worlddimensionnexus.config.DimensionConfig;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Service class for managing dimensions. This class handles the core business logic related to
 * dimension operations.
 */
public class DimensionService {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Dimension Service");

  private DimensionService() {
    // Private constructor to prevent instantiation
  }

  /**
   * Checks if a dimension exists.
   *
   * @param server The Minecraft server
   * @param dimensionId The dimension ID to check
   * @return true if the dimension exists, false otherwise
   */
  public static boolean dimensionExists(MinecraftServer server, ResourceKey<Level> dimensionId) {
    if (server == null || dimensionId == null) {
      return false;
    }
    return server.getLevel(dimensionId) != null;
  }

  /**
   * Gets a dimension server level by name.
   *
   * @param server The Minecraft server
   * @param dimensionName The dimension name
   * @return The ServerLevel or null if not found
   */
  public static ServerLevel getDimensionLevel(MinecraftServer server, String dimensionName) {
    if (server == null || dimensionName == null || dimensionName.isEmpty()) {
      return null;
    }

    // Handle "minecraft:" prefix if missing
    if (!dimensionName.contains(":")) {
      dimensionName = "minecraft:" + dimensionName;
    }

    ResourceLocation dimensionLocation = ResourceLocation.parse(dimensionName);
    ResourceKey<Level> dimensionKey =
        ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimensionLocation);

    return server.getLevel(dimensionKey);
  }

  /**
   * Gets all custom dimensions from the server.
   *
   * @param server The Minecraft server
   * @return A list of dimension ResourceKeys
   */
  public static List<ResourceKey<Level>> getCustomDimensions(MinecraftServer server) {
    if (server == null) {
      return new ArrayList<>();
    }

    List<ResourceKey<Level>> customDimensions = new ArrayList<>();
    for (ServerLevel level : server.getAllLevels()) {
      ResourceKey<Level> levelKey = level.dimension();

      // Skip vanilla dimensions (overworld, nether, end)
      ResourceLocation location = levelKey.location();
      if (location.getNamespace().equals("minecraft")
          && (location.getPath().equals("overworld")
              || location.getPath().equals("the_nether")
              || location.getPath().equals("the_end"))) {
        continue;
      }

      customDimensions.add(levelKey);
    }

    return customDimensions;
  }

  /**
   * Creates a backup of a dimension if needed.
   *
   * @param server The Minecraft server
   * @param dimensionName The dimension name to backup
   * @return true if backup was successful or not needed, false on error
   */
  public static boolean backupDimensionIfNeeded(MinecraftServer server, String dimensionName) {
    if (!DimensionConfig.BACKUP_DELETED_DIMENSIONS) {
      return true; // Backups disabled, return true for "success"
    }

    if (server == null || dimensionName == null || dimensionName.isEmpty()) {
      return false;
    }

    try {
      // Get dimension directory
      Path worldPath = server.getWorldPath(LevelResource.ROOT);
      Path dimensionPath = worldPath.resolve(dimensionName);
      if (!Files.exists(dimensionPath)) {
        return true; // Nothing to backup
      }

      // Create backup directory
      Path backupDir = worldPath.resolve("backups").resolve("dimensions");
      Files.createDirectories(backupDir);

      // Create timestamp for backup folder name
      String timestamp =
          java.time.LocalDateTime.now()
              .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

      Path backupPath = backupDir.resolve(dimensionName + "_" + timestamp);
      Files.createDirectories(backupPath);

      // Copy all files from dimension directory to backup
      Files.walk(dimensionPath)
          .filter(source -> !Files.isDirectory(source))
          .forEach(
              source -> {
                try {
                  Path relativePath = dimensionPath.relativize(source);
                  Path target = backupPath.resolve(relativePath);
                  Files.createDirectories(target.getParent());
                  Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                  log.error("Failed to copy file during backup: {}", e.getMessage());
                }
              });

      log.info("Successfully created backup of dimension {} at {}", dimensionName, backupPath);
      return true;
    } catch (IOException e) {
      log.error("Failed to create backup of dimension {}: {}", dimensionName, e.getMessage());
      return false;
    }
  }

  /**
   * Validates a dimension name.
   *
   * @param dimensionName The dimension name to validate
   * @return true if valid, false otherwise
   */
  public static boolean isValidDimensionName(String dimensionName) {
    if (dimensionName == null || dimensionName.isEmpty()) {
      return false;
    }

    // Check length
    if (dimensionName.length() > DimensionConfig.MAX_DIMENSION_NAME_LENGTH) {
      return false;
    }

    // Check for valid resource location format (namespace:path)
    String[] parts = dimensionName.split(":", 2);
    String namespace = parts.length > 1 ? parts[0] : "minecraft";
    String path = parts.length > 1 ? parts[1] : dimensionName;

    // Validate namespace and path using ResourceLocation's validation rules
    return namespace != null
        && !namespace.isEmpty()
        && path != null
        && !path.isEmpty()
        && ResourceLocation.isValidNamespace(namespace)
        && ResourceLocation.isValidPath(path);
  }
}
