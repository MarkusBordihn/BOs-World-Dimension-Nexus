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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportTrigger;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;

public class AutoTeleportTriggerSuggestion {

  public static final SuggestionProvider<CommandSourceStack> AUTO_TELEPORT_TRIGGERS =
      (CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder) -> {
        for (AutoTeleportTrigger trigger : AutoTeleportTrigger.values()) {
          suggestionsBuilder.suggest(trigger.getSerializedName());
        }
        return suggestionsBuilder.buildFuture();
      };

  private AutoTeleportTriggerSuggestion() {}

  public static CompletableFuture<Suggestions> suggestExistingTriggers(
      final SuggestionsBuilder suggestionsBuilder) {
    for (AutoTeleportTrigger trigger : AutoTeleportTrigger.values()) {
      suggestionsBuilder.suggest(trigger.getSerializedName());
    }
    return suggestionsBuilder.buildFuture();
  }

  public static AutoTeleportTrigger parseTriggerFromString(final String triggerString) {
    for (AutoTeleportTrigger trigger : AutoTeleportTrigger.values()) {
      if (trigger.getSerializedName().equalsIgnoreCase(triggerString)) {
        return trigger;
      }
    }
    return null;
  }
}
