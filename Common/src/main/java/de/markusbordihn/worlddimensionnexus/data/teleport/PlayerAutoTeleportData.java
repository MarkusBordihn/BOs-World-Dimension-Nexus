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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record PlayerAutoTeleportData(
    UUID playerId,
    Map<AutoTeleportTrigger, AutoTeleportEntry> autoTeleports,
    Map<AutoTeleportTrigger, Long> lastExecutions) {

  public static final Codec<PlayerAutoTeleportData> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      UUIDUtil.CODEC
                          .fieldOf("playerId")
                          .forGetter(PlayerAutoTeleportData::playerId),
                      Codec.unboundedMap(AutoTeleportTrigger.CODEC, AutoTeleportEntry.CODEC)
                          .fieldOf("autoTeleports")
                          .forGetter(PlayerAutoTeleportData::autoTeleports),
                      Codec.unboundedMap(AutoTeleportTrigger.CODEC, Codec.LONG)
                          .fieldOf("lastExecutions")
                          .forGetter(PlayerAutoTeleportData::lastExecutions))
                  .apply(instance, PlayerAutoTeleportData::new));

  public static PlayerAutoTeleportData empty(final UUID playerId) {
    return new PlayerAutoTeleportData(playerId, Map.of(), Map.of());
  }

  public PlayerAutoTeleportData withTeleport(final AutoTeleportEntry entry) {
    Map<AutoTeleportTrigger, AutoTeleportEntry> newTeleports = new HashMap<>(autoTeleports);
    newTeleports.put(entry.trigger(), entry);
    return new PlayerAutoTeleportData(playerId, newTeleports, lastExecutions);
  }

  public PlayerAutoTeleportData withoutTeleport(final AutoTeleportTrigger trigger) {
    Map<AutoTeleportTrigger, AutoTeleportEntry> newTeleports = new HashMap<>(autoTeleports);
    Map<AutoTeleportTrigger, Long> newExecutions = new HashMap<>(lastExecutions);
    newTeleports.remove(trigger);
    newExecutions.remove(trigger);
    return new PlayerAutoTeleportData(playerId, newTeleports, newExecutions);
  }

  public PlayerAutoTeleportData withExecution(
      final AutoTeleportTrigger trigger, final long timestamp) {
    Map<AutoTeleportTrigger, Long> newExecutions = new HashMap<>(lastExecutions);
    newExecutions.put(trigger, timestamp);
    return new PlayerAutoTeleportData(playerId, autoTeleports, newExecutions);
  }

  public boolean hasAnyTeleports() {
    return !autoTeleports.isEmpty();
  }

  public long getLastExecution(final AutoTeleportTrigger trigger) {
    return lastExecutions.getOrDefault(trigger, 0L);
  }
}
