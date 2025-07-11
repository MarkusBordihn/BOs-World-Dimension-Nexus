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

package de.markusbordihn.worlddimensionnexus.server.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.debug.DebugManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DebugCommand extends Command {

  private DebugCommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("debug")
        .requires(cs -> cs.hasPermission(Commands.LEVEL_GAMEMASTERS))
        .then(
            Commands.literal("log")
                .then(
                    Commands.argument("enable", BoolArgumentType.bool())
                        .executes(
                            context ->
                                setDebug(
                                    context.getSource(),
                                    BoolArgumentType.getBool(context, "enable")))));
  }

  public static int setDebug(final CommandSourceStack context, final boolean enable) {
    if (enable) {
      sendSuccessMessage(
          context,
          String.format(
              "► Enable debug for %s, please check debug.log for the full output.",
              Constants.MOD_NAME),
          ChatFormatting.GREEN);
      sendSuccessMessage(
          context,
          String.format("> Use '/%s debug false' to disable the debug!", Constants.MOD_COMMAND),
          ChatFormatting.WHITE);
    } else {
      sendSuccessMessage(
          context,
          String.format("■ Disable debug for %s!", Constants.MOD_NAME),
          ChatFormatting.RED);
      sendSuccessMessage(
          context,
          "> Please check the latest.log and/or debug.log for the full output.",
          ChatFormatting.WHITE);
    }
    DebugManager.enableDebugLevel(enable);
    return Command.SINGLE_SUCCESS;
  }
}
