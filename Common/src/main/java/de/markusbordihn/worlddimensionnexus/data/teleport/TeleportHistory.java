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

package de.markusbordihn.worlddimensionnexus.data.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Manages teleport history for players to enable /back functionality. */
public class TeleportHistory {

  private static final Map<UUID, List<TeleportLocation>> playerHistory = new HashMap<>();
  private static final int MAX_HISTORY_SIZE = 10;

  public static void recordLocation(
      UUID playerId, ResourceKey<Level> dimension, BlockPos position, float yRot, float xRot) {
    List<TeleportLocation> history =
        playerHistory.computeIfAbsent(playerId, k -> new ArrayList<>());

    history.add(0, new TeleportLocation(dimension, position, yRot, xRot));

    if (history.size() > MAX_HISTORY_SIZE) {
      history.remove(history.size() - 1);
    }
  }

  public static TeleportLocation getLastLocation(UUID playerId) {
    List<TeleportLocation> history = playerHistory.get(playerId);
    if (history == null || history.isEmpty()) {
      return null;
    }
    return history.get(0);
  }

  public static TeleportLocation popLastLocation(UUID playerId) {
    List<TeleportLocation> history = playerHistory.get(playerId);
    if (history == null || history.isEmpty()) {
      return null;
    }
    return history.remove(0);
  }

  public static List<TeleportLocation> getHistory(UUID playerId) {
    return playerHistory.getOrDefault(playerId, new ArrayList<>());
  }

  public static void clearHistory(UUID playerId) {
    playerHistory.remove(playerId);
  }

  /** Clears all teleport history data (used when switching worlds). */
  public static void clearAllHistory() {
    playerHistory.clear();
  }
}
