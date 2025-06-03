package de.markusbordihn.worlddimensionnexus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.markusbordihn.worlddimensionnexus.dimension.DimensionManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class CreateDimensionCommand {
  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("wdn")
            .then(
                Commands.literal("create")
                    .then(
                        Commands.argument("name", StringArgumentType.word())
                            .executes(
                                context -> {
                                  String name = StringArgumentType.getString(context, "name");
                                  ServerLevel dimension =
                                      DimensionManager.createFlatDimension(
                                          context.getSource().getServer(), name);

                                  context
                                      .getSource()
                                      .sendSuccess(
                                          () ->
                                              Component.literal(
                                                  "Dimension '" + name + "' created successfully!"),
                                          true);
                                  return 1;
                                }))));
  }
}
