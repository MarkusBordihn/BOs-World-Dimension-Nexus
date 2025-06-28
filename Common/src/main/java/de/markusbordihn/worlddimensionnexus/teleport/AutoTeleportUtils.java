package de.markusbordihn.worlddimensionnexus.teleport;

import de.markusbordihn.worlddimensionnexus.data.teleport.AutoTeleportTrigger;
import net.minecraft.server.level.ServerPlayer;

public class AutoTeleportUtils {

  public static void setSpawnTeleport(
      ServerPlayer player, String dimension, double x, double y, double z) {
    AutoTeleportManager.registerAutoTeleport(
        player, AutoTeleportTrigger.ALWAYS, dimension, x, y, z);
  }

  public static void setDailyTeleport(
      ServerPlayer player, String dimension, double x, double y, double z) {
    AutoTeleportManager.registerAutoTeleport(
        player, AutoTeleportTrigger.ONCE_PER_DAY, dimension, x, y, z);
  }

  public static void setJoinTeleport(
      ServerPlayer player, String dimension, double x, double y, double z) {
    AutoTeleportManager.registerAutoTeleport(
        player, AutoTeleportTrigger.ONCE_PER_SERVER_JOIN, dimension, x, y, z);
  }

  public static void setServerRestartTeleport(
      ServerPlayer player, String dimension, double x, double y, double z) {
    AutoTeleportManager.registerAutoTeleport(
        player, AutoTeleportTrigger.ONCE_AFTER_SERVER_RESTART, dimension, x, y, z);
  }

  public static void setWeeklyTeleport(
      ServerPlayer player, String dimension, double x, double y, double z) {
    AutoTeleportManager.registerAutoTeleport(
        player, AutoTeleportTrigger.ONCE_PER_WEEK, dimension, x, y, z);
  }

  public static void setMonthlyTeleport(
      ServerPlayer player, String dimension, double x, double y, double z) {
    AutoTeleportManager.registerAutoTeleport(
        player, AutoTeleportTrigger.ONCE_PER_MONTH, dimension, x, y, z);
  }

  public static void removeAutoTeleport(ServerPlayer player, AutoTeleportTrigger trigger) {
    AutoTeleportManager.removeAutoTeleport(player, trigger);
  }

  public static void removeAllAutoTeleports(ServerPlayer player) {
    AutoTeleportManager.removeAllAutoTeleports(player);
  }

  public static void setupAutoTeleport(
      ServerPlayer player, String triggerType, String dimension, double x, double y, double z) {
    AutoTeleportTrigger trigger =
        switch (triggerType.toLowerCase()) {
          case "always" -> AutoTeleportTrigger.ALWAYS;
          case "daily" -> AutoTeleportTrigger.ONCE_PER_DAY;
          case "weekly" -> AutoTeleportTrigger.ONCE_PER_WEEK;
          case "monthly" -> AutoTeleportTrigger.ONCE_PER_MONTH;
          case "join" -> AutoTeleportTrigger.ONCE_PER_SERVER_JOIN;
          case "restart" -> AutoTeleportTrigger.ONCE_AFTER_SERVER_RESTART;
          default -> throw new IllegalArgumentException("Invalid trigger type: " + triggerType);
        };

    AutoTeleportManager.registerAutoTeleport(player, trigger, dimension, x, y, z);
  }
}
