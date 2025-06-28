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

package de.markusbordihn.worlddimensionnexus.utils;

import de.markusbordihn.worlddimensionnexus.data.teleport.TeleportHistory;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.service.TeleportService;
import de.markusbordihn.worlddimensionnexus.teleport.AutoTeleportManager;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;

/**
 * Manages static cache cleanup to prevent data bleeding between singleplayer worlds. When switching
 * worlds, static caches persist and cause incorrect data to appear in the new world.
 */
public class CacheManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Cache Manager");

  private CacheManager() {}

  /** Clears all static caches and data structures for clean world transitions. */
  public static void clearAllCaches() {
    log.info("Clearing all static caches for world switch...");

    TeleportHistory.clearAllHistory();
    TeleportService.clearAllState();
    DimensionManager.clearAllCache();
    AutoTeleportManager.clearAllState();

    log.info("All static caches cleared successfully.");
  }
}
