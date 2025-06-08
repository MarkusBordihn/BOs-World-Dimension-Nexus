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

package de.markusbordihn.worlddimensionnexus.server.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import de.markusbordihn.worlddimensionnexus.commands.Command;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DimensionCommand extends Command {

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("dimension")
        .then(Commands.literal("list").executes(context -> listDimensions(context.getSource())))
        .then(
            Commands.literal("create")
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .executes(
                            context ->
                                createDimension(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "name")))));
  }

  public static int createDimension(CommandSourceStack context, String dimensionName) {
    DimensionManager.createFlatDimension(context.getServer(), dimensionName);
    return sendSuccessMessage(context, "Dimension '" + dimensionName + "' created successfully!");
  }

  public static int listDimensions(CommandSourceStack context) {
    List<ResourceKey<Level>> dimensions = DimensionManager.getDimensions(context.getServer());
    if (dimensions.isEmpty()) {
      return sendFailureMessage(context, "No custom dimensions available.");
    }
    sendSuccessMessage(context, "Dimensions\n" + "===========");
    for (ResourceKey<Level> dimension : dimensions) {
      sendSuccessMessage(context, "- " + dimension.location());
    }
    return Command.SINGLE_SUCCESS;
  }
}
