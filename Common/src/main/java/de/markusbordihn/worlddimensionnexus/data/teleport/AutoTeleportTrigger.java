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
import net.minecraft.util.StringRepresentable;

/**
 * Defines when auto-teleport should be triggered. Specifies the various conditions under which an
 * automatic teleportation can occur, ranging from one-time events to recurring schedules.
 */
public enum AutoTeleportTrigger implements StringRepresentable {
  /** Triggers once after the server restarts. */
  ONCE_AFTER_SERVER_RESTART("once_after_server_restart"),

  /** Triggers once each time a player joins the server. */
  ONCE_PER_SERVER_JOIN("once_per_server_join"),

  /** Triggers once per day. */
  ONCE_PER_DAY("once_per_day"),

  /** Triggers once per week. */
  ONCE_PER_WEEK("once_per_week"),

  /** Triggers once per month. */
  ONCE_PER_MONTH("once_per_month"),

  /** Triggers every time the condition is checked. */
  ALWAYS("always");

  /** Codec for serializing and deserializing AutoTeleportTrigger instances. */
  public static final Codec<AutoTeleportTrigger> CODEC =
      StringRepresentable.fromEnum(AutoTeleportTrigger::values);

  private final String name;

  /**
   * Creates a new auto-teleport trigger with the specified serialized name.
   *
   * @param name the serialized name used for data storage
   */
  AutoTeleportTrigger(final String name) {
    this.name = name;
  }

  /**
   * Returns the serialized name of this trigger for data storage.
   *
   * @return the serialized name
   */
  @Override
  public String getSerializedName() {
    return this.name;
  }
}
