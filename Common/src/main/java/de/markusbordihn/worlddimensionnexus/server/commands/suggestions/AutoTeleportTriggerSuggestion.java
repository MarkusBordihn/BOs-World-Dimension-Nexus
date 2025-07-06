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

  private static final String TRIGGER_ALWAYS = "always";
  private static final String TRIGGER_DAILY = "daily";
  private static final String TRIGGER_WEEKLY = "weekly";
  private static final String TRIGGER_MONTHLY = "monthly";
  private static final String TRIGGER_JOIN = "join";
  private static final String TRIGGER_RESTART = "restart";

  private AutoTeleportTriggerSuggestion() {}

  public static final SuggestionProvider<CommandSourceStack> AUTO_TELEPORT_TRIGGERS =
      (CommandContext<CommandSourceStack> context, SuggestionsBuilder suggestionsBuilder) -> {
        suggestionsBuilder.suggest(TRIGGER_ALWAYS);
        suggestionsBuilder.suggest(TRIGGER_DAILY);
        suggestionsBuilder.suggest(TRIGGER_WEEKLY);
        suggestionsBuilder.suggest(TRIGGER_MONTHLY);
        suggestionsBuilder.suggest(TRIGGER_JOIN);
        suggestionsBuilder.suggest(TRIGGER_RESTART);
        return suggestionsBuilder.buildFuture();
      };

  public static CompletableFuture<Suggestions> suggestExistingTriggers(
      final CommandContext<CommandSourceStack> context,
      final SuggestionsBuilder suggestionsBuilder) {
    for (AutoTeleportTrigger trigger : AutoTeleportTrigger.values()) {
      String triggerName = mapTriggerToCommandName(trigger);
      if (triggerName != null) {
        suggestionsBuilder.suggest(triggerName);
      }
    }

    return suggestionsBuilder.buildFuture();
  }

  private static String mapTriggerToCommandName(final AutoTeleportTrigger trigger) {
    return switch (trigger) {
      case ALWAYS -> TRIGGER_ALWAYS;
      case ONCE_PER_DAY -> TRIGGER_DAILY;
      case ONCE_PER_WEEK -> TRIGGER_WEEKLY;
      case ONCE_PER_MONTH -> TRIGGER_MONTHLY;
      case ONCE_PER_SERVER_JOIN -> TRIGGER_JOIN;
      case ONCE_AFTER_SERVER_RESTART -> TRIGGER_RESTART;
      default -> null;
    };
  }

  public static AutoTeleportTrigger parseTriggerFromString(final String triggerString) {
    return switch (triggerString.toLowerCase()) {
      case TRIGGER_ALWAYS -> AutoTeleportTrigger.ALWAYS;
      case TRIGGER_DAILY -> AutoTeleportTrigger.ONCE_PER_DAY;
      case TRIGGER_WEEKLY -> AutoTeleportTrigger.ONCE_PER_WEEK;
      case TRIGGER_MONTHLY -> AutoTeleportTrigger.ONCE_PER_MONTH;
      case TRIGGER_JOIN -> AutoTeleportTrigger.ONCE_PER_SERVER_JOIN;
      case TRIGGER_RESTART -> AutoTeleportTrigger.ONCE_AFTER_SERVER_RESTART;
      default -> null;
    };
  }
}
