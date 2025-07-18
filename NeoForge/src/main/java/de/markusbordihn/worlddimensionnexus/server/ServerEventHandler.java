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

package de.markusbordihn.worlddimensionnexus.server;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber
public class ServerEventHandler {

  private ServerEventHandler() {}

  @SubscribeEvent
  public static void onServerStarting(ServerStartingEvent event) {
    if (event.getServer() instanceof MinecraftServer minecraftServer) {
      ServerEvents.handleServerStartingEvent(minecraftServer);
    }
  }

  @SubscribeEvent
  public static void onServerStarted(ServerStartedEvent event) {
    if (event.getServer() instanceof MinecraftServer minecraftServer) {
      ServerEvents.handleServerStartedEvent(minecraftServer);
    }
  }

  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    if (event.getServer() instanceof MinecraftServer minecraftServer) {
      ServerEvents.handleServerStoppingEvent(minecraftServer);
    }
  }

  @SubscribeEvent
  public static void onServerTickPre(ServerTickEvent.Pre event) {
    if (event.getServer() instanceof MinecraftServer minecraftServer) {
      ServerEvents.handleServerTickPreEvent(minecraftServer);
    }
  }

  @SubscribeEvent
  public static void onServerTickPost(ServerTickEvent.Post event) {
    if (event.getServer() instanceof MinecraftServer minecraftServer) {
      ServerEvents.handleServerTickPostEvent(minecraftServer);
    }
  }
}
