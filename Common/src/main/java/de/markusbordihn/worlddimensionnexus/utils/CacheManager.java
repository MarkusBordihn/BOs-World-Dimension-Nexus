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

import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.saveddata.AutoTeleportDataStorage;
import de.markusbordihn.worlddimensionnexus.saveddata.DimensionDataStorage;
import de.markusbordihn.worlddimensionnexus.saveddata.PortalDataStorage;
import de.markusbordihn.worlddimensionnexus.saveddata.TeleportHistoryDataStorage;
import de.markusbordihn.worlddimensionnexus.service.TeleportService;
import de.markusbordihn.worlddimensionnexus.teleport.AutoTeleportManager;
import de.markusbordihn.worlddimensionnexus.teleport.TeleportHistory;
import de.markusbordihn.worlddimensionnexus.utils.ModLogger.PrefixLogger;

public class CacheManager {

  private static final PrefixLogger log = ModLogger.getPrefixLogger("Cache Manager");

  private CacheManager() {}

  public static void clearAllCaches() {
    log.info("Clearing Data Storage caches...");
    AutoTeleportDataStorage.clearInstance();
    DimensionDataStorage.clearInstance();
    PortalDataStorage.clearInstance();
    TeleportHistoryDataStorage.clearInstance();

    log.info("Clearing Data Manager caches...");
    TeleportHistory.clearAllCache();
    TeleportService.clearAllCache();
    DimensionManager.clearAllCache();
    AutoTeleportManager.clearAllCache();
  }
}
