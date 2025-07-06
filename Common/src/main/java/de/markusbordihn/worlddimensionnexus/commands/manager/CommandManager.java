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

package de.markusbordihn.worlddimensionnexus.commands.manager;

import com.mojang.brigadier.CommandDispatcher;
import de.markusbordihn.worlddimensionnexus.Constants;
import de.markusbordihn.worlddimensionnexus.server.commands.AutoTeleportCommand;
import de.markusbordihn.worlddimensionnexus.server.commands.DebugCommand;
import de.markusbordihn.worlddimensionnexus.server.commands.DimensionCommand;
import de.markusbordihn.worlddimensionnexus.server.commands.PortalCommand;
import de.markusbordihn.worlddimensionnexus.server.commands.TeleportCommand;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandManager {

  protected static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  private CommandManager() {}

  public static void registerCommands(
      final CommandDispatcher<CommandSourceStack> commandDispatcher,
      final CommandBuildContext context) {
    log.info(
        "{} /{} commands for {} ...",
        Constants.LOG_REGISTER_PREFIX,
        Constants.MOD_COMMAND,
        Constants.MOD_NAME);
    commandDispatcher.register(
        Commands.literal(Constants.MOD_COMMAND)
            .then(AutoTeleportCommand.register())
            .then(DebugCommand.register())
            .then(DimensionCommand.register())
            .then(PortalCommand.register())
            .then(TeleportCommand.register()));
  }
}
