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

package de.markusbordihn.worlddimensionnexus.data.warp;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum WarpType implements StringRepresentable {
  PRIVATE("private", "Private Warp"),
  PUBLIC("public", "Public Warp");

  public static final Codec<WarpType> CODEC = StringRepresentable.fromEnum(WarpType::values);

  private final String name;
  private final String displayName;

  WarpType(final String name, final String displayName) {
    this.name = name;
    this.displayName = displayName;
  }

  public static WarpType fromString(String name) {
    for (WarpType type : values()) {
      if (type.name.equalsIgnoreCase(name)) {
        return type;
      }
    }
    return PRIVATE; // Default fallback
  }

  public String getName() {
    return name;
  }

  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String getSerializedName() {
    return name;
  }
}
