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

package de.markusbordihn.worlddimensionnexus.gamemode;

import de.markusbordihn.worlddimensionnexus.teleport.TeleportHistory;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public class GameModeHistory {

  private GameModeHistory() {}

  public static void applyGameTypeForPlayer(
      final ServerPlayer player,
      final ResourceKey<Level> dimension,
      final GameType dimensionGameType) {

    if (player.hasPermissions(2)) {
      return;
    }

    player.setGameMode(dimensionGameType);
  }

  public static void restoreGameTypeFromHistory(
      final ServerPlayer player, final ResourceKey<Level> targetDimension) {

    if (player.hasPermissions(2)) {
      return;
    }

    GameType previousGameType =
        TeleportHistory.getLastGameTypeForDimension(player.getUUID(), targetDimension);
    if (previousGameType != null) {
      player.setGameMode(previousGameType);
    }
  }
}
