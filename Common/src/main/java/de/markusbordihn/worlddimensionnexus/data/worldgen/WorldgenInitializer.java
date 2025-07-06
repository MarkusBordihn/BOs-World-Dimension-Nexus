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

package de.markusbordihn.worlddimensionnexus.data.worldgen;

import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.server.MinecraftServer;

public class WorldgenInitializer {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Worldgen Initializer");

  private WorldgenInitializer() {}

  public static void initialize(final MinecraftServer server) {
    log.info("Initializing worldgen configuration system...");

    try {
      // Load configurations from resources
      WorldgenConfigLoader.loadConfigsFromResources();

      // Load additional configurations from data folder (if present)
      Path dataPath =
          server
              .getServerDirectory()
              .resolve("config")
              .resolve(Constants.MOD_ID)
              .resolve("worldgen");

      WorldgenConfigLoader.loadConfigsFromPath(dataPath);

      // Load from standard data directory as well
      Path modDataPath = Paths.get("data", Constants.MOD_ID, "worldgen");
      WorldgenConfigLoader.loadConfigsFromPath(modDataPath);

      log.info("Worldgen configuration system successfully initialized");

    } catch (Exception e) {
      log.error("Error initializing worldgen system: {}", e.getMessage());
    }
  }

  public static void reload(final MinecraftServer server) {
    log.info("Reloading worldgen configurations...");

    // Clear existing configurations
    WorldgenConfigLoader.clear();

    // Reinitialize
    initialize(server);
  }
}
