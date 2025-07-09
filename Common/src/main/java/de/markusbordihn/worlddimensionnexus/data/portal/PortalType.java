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

package de.markusbordihn.worlddimensionnexus.data.portal;

import com.mojang.serialization.Codec;
import de.markusbordihn.worlddimensionnexus.config.PortalConfig;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;

public enum PortalType implements StringRepresentable {
  PLAYER("player", true),
  WORLD("world", true),
  UNBOUND("unbound", true),
  EVENT("event", false);

  public static final Codec<PortalType> CODEC = StringRepresentable.fromEnum(PortalType::values);

  private final String name;
  private final boolean playerCreatable;

  PortalType(String name, boolean playerCreatable) {
    this.name = name;
    this.playerCreatable = playerCreatable;
  }

  public static PortalType fromCornerBlock(Block block) {
    for (PortalType type : values()) {
      if (type.isEnabled() && type.getCornerBlock() == block) {
        return type;
      }
    }
    return PLAYER; // Default fallback
  }

  public String getName() {
    return name;
  }

  public Block getCornerBlock() {
    return PortalConfig.getCornerBlockForPortalType(this);
  }

  public int getMaxPortalsPerLink() {
    return PortalConfig.getMaxLinksForPortalType(this);
  }

  public boolean isPlayerCreatable() {
    return playerCreatable;
  }

  public boolean isEnabled() {
    return PortalConfig.isPortalTypeEnabled(this);
  }

  public boolean hasPortalLimit() {
    return getMaxPortalsPerLink() > 0;
  }

  @Override
  public String getSerializedName() {
    return name;
  }
}
