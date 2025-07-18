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

package de.markusbordihn.worlddimensionnexus.data.teleport;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record CountdownTeleportData(
    ServerPlayer serverPlayer,
    ResourceKey<Level> targetDimensionKey,
    String targetDimension,
    Vec3 startPosition,
    int remainingSeconds,
    boolean enableMovementDetection) {

  private static final double MOVEMENT_THRESHOLD = 0.5;

  public CountdownTeleportData(
      ServerPlayer serverPlayer,
      ResourceKey<Level> targetDimensionKey,
      int countdownSeconds,
      boolean enableMovementDetection) {
    this(
        serverPlayer,
        targetDimensionKey,
        targetDimensionKey.location().toString(),
        serverPlayer.position(),
        countdownSeconds,
        enableMovementDetection);
  }

  public boolean hasPlayerMoved() {
    return enableMovementDetection
        && serverPlayer.position().distanceTo(startPosition) > MOVEMENT_THRESHOLD;
  }

  public CountdownTeleportData decrementCountdown() {
    return new CountdownTeleportData(
        serverPlayer,
        targetDimensionKey,
        targetDimension,
        startPosition,
        remainingSeconds - 1,
        enableMovementDetection);
  }

  public boolean isCountdownFinished() {
    return remainingSeconds <= 0;
  }

  public ServerPlayer getServerPlayer() {
    return serverPlayer;
  }

  public String getTargetDimension() {
    return targetDimension;
  }

  public ResourceKey<Level> getTargetDimensionKey() {
    return targetDimensionKey;
  }

  public int getRemainingSeconds() {
    return remainingSeconds;
  }

  public boolean isMovementDetectionEnabled() {
    return enableMovementDetection;
  }

  public Vec3 getStartPosition() {
    return startPosition;
  }
}
