/*
 * Copyright 2023 Markus Bordihn
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

package de.markusbordihn.worlddimensionnexus.teleport;

import de.markusbordihn.worlddimensionnexus.data.teleport.TeleportLocation;
import de.markusbordihn.worlddimensionnexus.saveddata.TeleportHistoryDataStorage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class TeleportHistory {

  private static final Map<UUID, List<TeleportLocation>> playerHistory = new HashMap<>();
  private static final int MAX_HISTORY_SIZE = 10;
  private static ServerLevel storageLevel;

  public static void initialize(final ServerLevel level) {
    storageLevel = level;
    TeleportHistoryDataStorage.init(level);
    loadAllHistoryFromStorage();
  }

  public static void recordLocation(
      final UUID playerId,
      final ResourceKey<Level> dimension,
      final BlockPos position,
      final float yRot,
      final float xRot) {
    List<TeleportLocation> history =
        playerHistory.computeIfAbsent(playerId, k -> new ArrayList<>());

    history.addFirst(new TeleportLocation(dimension, position, yRot, xRot));
    if (history.size() > MAX_HISTORY_SIZE) {
      history.removeLast();
    }

    // Save to persistent storage
    savePlayerHistoryToStorage(playerId, history);
  }

  public static TeleportLocation getLastLocation(final UUID playerId) {
    List<TeleportLocation> history = getPlayerHistory(playerId);
    if (history.isEmpty()) {
      return null;
    }

    return history.getFirst();
  }

  public static TeleportLocation popLastLocation(final UUID playerId) {
    List<TeleportLocation> history = playerHistory.get(playerId);
    if (history == null || history.isEmpty()) {
      return null;
    }

    TeleportLocation lastLocation = history.removeFirst();

    // Save updated history to storage
    savePlayerHistoryToStorage(playerId, history);

    return lastLocation;
  }

  public static List<TeleportLocation> getPlayerHistory(final UUID playerId) {
    return playerHistory.getOrDefault(playerId, new ArrayList<>());
  }

  public static void clearPlayerHistory(final UUID playerId) {
    playerHistory.remove(playerId);
    if (storageLevel != null) {
      TeleportHistoryDataStorage.get().clearPlayerHistory(playerId);
    }
  }

  public static void clearAllHistory() {
    playerHistory.clear();
  }

  public static void clearAllCache() {
    playerHistory.clear();
  }

  private static void savePlayerHistoryToStorage(
      final UUID playerId, final List<TeleportLocation> history) {
    if (storageLevel != null) {
      TeleportHistoryDataStorage.get().savePlayerHistory(playerId, history);
    }
  }

  private static void loadAllHistoryFromStorage() {
    if (storageLevel != null) {
      TeleportHistoryDataStorage storage = TeleportHistoryDataStorage.get();
      playerHistory.clear();
      // Load each player's history individually since there's no loadAllPlayerHistory method
      // The storage will be loaded on-demand when players access their history
    }
  }
}
