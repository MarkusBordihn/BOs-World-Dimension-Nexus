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

import com.mojang.brigadier.suggestion.SuggestionProvider;
import de.markusbordihn.worlddimensionnexus.data.portal.PortalType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;

public class PortalTypeSuggestion {

  public static final SuggestionProvider<CommandSourceStack> PORTAL_TYPE =
      (context, builder) -> {
        List<String> enabledPortalTypes =
            Arrays.stream(PortalType.values())
                .filter(PortalType::isEnabled)
                .filter(type -> type.isPlayerCreatable() || context.getSource().hasPermission(2))
                .map(PortalType::getName)
                .collect(Collectors.toList());

        return SharedSuggestionProvider.suggest(enabledPortalTypes, builder);
      };
  public static final SuggestionProvider<CommandSourceStack> ENABLED_PORTAL_TYPE =
      (context, builder) -> {
        List<String> enabledPortalTypes =
            Arrays.stream(PortalType.values())
                .filter(PortalType::isEnabled)
                .map(PortalType::getName)
                .collect(Collectors.toList());

        return SharedSuggestionProvider.suggest(enabledPortalTypes, builder);
      };

  protected PortalTypeSuggestion() {}
}
