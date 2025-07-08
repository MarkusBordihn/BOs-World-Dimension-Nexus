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

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.phys.Vec3;

public record AutoTeleportEntry(
    String targetDimension,
    Vec3 position,
    AutoTeleportTrigger trigger,
    int countdownSeconds,
    boolean skipMovementDetection) {

  private static final int DEFAULT_COUNTDOWN_SECONDS = 5;
  public static final Codec<AutoTeleportEntry> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.STRING
                          .fieldOf("targetDimension")
                          .forGetter(AutoTeleportEntry::targetDimension),
                      Vec3.CODEC.fieldOf("position").forGetter(AutoTeleportEntry::position),
                      AutoTeleportTrigger.CODEC
                          .fieldOf("trigger")
                          .forGetter(AutoTeleportEntry::trigger),
                      Codec.INT
                          .optionalFieldOf("countdownSeconds", DEFAULT_COUNTDOWN_SECONDS)
                          .forGetter(AutoTeleportEntry::countdownSeconds),
                      Codec.BOOL
                          .optionalFieldOf("skipMovementDetection", false)
                          .forGetter(AutoTeleportEntry::skipMovementDetection))
                  .apply(instance, AutoTeleportEntry::new));
  private static final int ALWAYS_TRIGGER_COUNTDOWN = 3;
  private static final int DEATH_TRIGGER_COUNTDOWN = 0;

  public AutoTeleportEntry(String targetDimension, Vec3 position, AutoTeleportTrigger trigger) {
    this(
        targetDimension,
        position,
        trigger,
        getDefaultCountdownForTrigger(trigger),
        shouldSkipMovementDetectionForTrigger(trigger));
  }

  private static int getDefaultCountdownForTrigger(AutoTeleportTrigger trigger) {
    return switch (trigger) {
      case ON_DEATH -> DEATH_TRIGGER_COUNTDOWN;
      case ALWAYS -> ALWAYS_TRIGGER_COUNTDOWN;
      default -> DEFAULT_COUNTDOWN_SECONDS;
    };
  }

  private static boolean shouldSkipMovementDetectionForTrigger(AutoTeleportTrigger trigger) {
    return trigger == AutoTeleportTrigger.ON_DEATH;
  }

  public AutoTeleportEntry withCountdown(int newCountdownSeconds) {
    return new AutoTeleportEntry(
        targetDimension, position, trigger, newCountdownSeconds, skipMovementDetection);
  }

  public AutoTeleportEntry withMovementDetection(boolean newSkipMovementDetection) {
    return new AutoTeleportEntry(
        targetDimension, position, trigger, countdownSeconds, newSkipMovementDetection);
  }

  public AutoTeleportEntry withPosition(Vec3 newPosition) {
    return new AutoTeleportEntry(
        targetDimension, newPosition, trigger, countdownSeconds, skipMovementDetection);
  }
}
