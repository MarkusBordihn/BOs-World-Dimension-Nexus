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

package de.markusbordihn.worlddimensionnexus.server.commands.suggestions;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.markusbordihn.worlddimensionnexus.data.warp.WarpData;
import de.markusbordihn.worlddimensionnexus.warp.WarpManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class WarpSuggestion {

  public static final SuggestionProvider<CommandSourceStack> PRIVATE_WARPS =
      WarpSuggestion::suggestPrivateWarps;

  public static final SuggestionProvider<CommandSourceStack> PUBLIC_WARPS =
      WarpSuggestion::suggestPublicWarps;

  public static final SuggestionProvider<CommandSourceStack> PLAYER_WARPS =
      WarpSuggestion::suggestPlayerWarps;

  public static final SuggestionProvider<CommandSourceStack> ACCESSIBLE_WARPS =
      WarpSuggestion::suggestAccessibleWarps;

  public static final SuggestionProvider<CommandSourceStack> ALL_WARPS =
      WarpSuggestion::suggestAllWarps;

  private WarpSuggestion() {}

  private static CompletableFuture<Suggestions> suggestPrivateWarps(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
    try {
      ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
      List<WarpData> privateWarps = WarpManager.getPrivateWarpsForPlayer(serverPlayer.getUUID());

      for (WarpData warpData : privateWarps) {
        if (warpData.name().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
          builder.suggest(warpData.name(), () -> warpData.getDisplayName());
        }
      }
    } catch (CommandSyntaxException e) {
      // Ignore, no suggestions available
    }
    return builder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestPublicWarps(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
    List<WarpData> publicWarps = WarpManager.getPublicWarps();

    for (WarpData warpData : publicWarps) {
      if (warpData.name().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
        builder.suggest(warpData.name(), warpData::getDisplayName);
      }
    }
    return builder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestPlayerWarps(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
    try {
      ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
      List<WarpData> playerWarps = WarpManager.getAllWarpsForPlayer(serverPlayer.getUUID());

      for (WarpData warpData : playerWarps) {
        if (warpData.enabled()
            && warpData.name().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
          builder.suggest(warpData.name(), warpData::getDisplayName);
        }
      }
    } catch (CommandSyntaxException e) {
      // Ignore, no suggestions available
    }
    return builder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestAccessibleWarps(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
    try {
      ServerPlayer serverPlayer = context.getSource().getPlayerOrException();

      // Add private warps
      List<WarpData> privateWarps = WarpManager.getPrivateWarpsForPlayer(serverPlayer.getUUID());
      for (WarpData warpData : privateWarps) {
        if (warpData.name().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
          builder.suggest(warpData.name(), warpData::getDisplayName);
        }
      }

      // Add public warps
      List<WarpData> publicWarps = WarpManager.getPublicWarps();
      for (WarpData warpData : publicWarps) {
        if (warpData.name().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
          builder.suggest(warpData.name(), warpData::getDisplayName);
        }
      }
    } catch (CommandSyntaxException e) {
      // Ignore, no suggestions available
    }
    return builder.buildFuture();
  }

  private static CompletableFuture<Suggestions> suggestAllWarps(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
    // For admin commands - suggest all warps regardless of ownership
    for (WarpData warpData : WarpManager.getAllWarps()) {
      if (warpData.enabled()
          && warpData.name().toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
        String tooltip = warpData.type().getDisplayName() + " - " + warpData.getDisplayName();
        builder.suggest(warpData.name(), () -> tooltip);
      }
    }
    return builder.buildFuture();
  }
}
