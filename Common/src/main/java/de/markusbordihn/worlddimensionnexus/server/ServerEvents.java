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

package de.markusbordihn.worlddimensionnexus.server;

import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import de.markusbordihn.worlddimensionnexus.portal.PortalManager;
import de.markusbordihn.worlddimensionnexus.portal.PortalTargetManager;
import de.markusbordihn.worlddimensionnexus.saveddata.DimensionDataStorage;
import de.markusbordihn.worlddimensionnexus.saveddata.PortalDataStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerEvents {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  public static void handleServerStartingEvent(final MinecraftServer minecraftServer) {
    log.info("Server starting {} ...", minecraftServer);

    ServerLevel overworld = minecraftServer.getLevel(Level.OVERWORLD);
    if (overworld == null) {
      log.error("Overworld not found, unable to register global data storage!");
      return;
    }

    // Register global Dimension Data Storage on the Overworld.
    DimensionDataStorage.init(overworld);

    // Register global Portal Data Storage on the Overworld.
    PortalDataStorage.init(overworld);

    // Synchronize Dimension Data Storage to Dimension Manager and register all dimensions.
    DimensionManager.sync(minecraftServer, DimensionDataStorage.get().getDimensions());
  }

  public static void handleServerStartedEvent(final MinecraftServer minecraftServer) {
    log.info("Server started {} ...", minecraftServer);

    // Synchronize Portal Data Storage to Portal Manager.
    PortalManager.sync(PortalDataStorage.get().getPortals());
    PortalTargetManager.sync(PortalDataStorage.get().getTargets());
  }
}
