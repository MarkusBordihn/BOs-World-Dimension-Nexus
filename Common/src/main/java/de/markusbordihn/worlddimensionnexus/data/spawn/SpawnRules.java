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

package de.markusbordihn.worlddimensionnexus.data.spawn;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record SpawnRules(
    boolean disableNaturalMobSpawning,
    boolean disableSpawnerBlocks,
    List<ResourceLocation> allowedEntityTypes,
    List<ResourceLocation> deniedEntityTypes) {

  public static final String DISABLE_NATURAL_MOB_SPAWNING_TAG = "disableNaturalMobSpawning";
  public static final String DISABLE_SPAWNER_BLOCKS_TAG = "disableSpawnerBlocks";
  public static final String ALLOWED_ENTITY_TYPES_TAG = "allowedEntityTypes";
  public static final String DENIED_ENTITY_TYPES_TAG = "deniedEntityTypes";

  public static final boolean DEFAULT_DISABLE_NATURAL_MOB_SPAWNING = false;
  public static final boolean DEFAULT_DISABLE_SPAWNER_BLOCKS = false;
  public static final List<ResourceLocation> DEFAULT_EMPTY_LIST = List.of();

  public static final Codec<SpawnRules> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.BOOL
                          .optionalFieldOf(
                              DISABLE_NATURAL_MOB_SPAWNING_TAG,
                              DEFAULT_DISABLE_NATURAL_MOB_SPAWNING)
                          .forGetter(SpawnRules::disableNaturalMobSpawning),
                      Codec.BOOL
                          .optionalFieldOf(
                              DISABLE_SPAWNER_BLOCKS_TAG, DEFAULT_DISABLE_SPAWNER_BLOCKS)
                          .forGetter(SpawnRules::disableSpawnerBlocks),
                      ResourceLocation.CODEC
                          .listOf()
                          .optionalFieldOf(ALLOWED_ENTITY_TYPES_TAG, DEFAULT_EMPTY_LIST)
                          .forGetter(SpawnRules::allowedEntityTypes),
                      ResourceLocation.CODEC
                          .listOf()
                          .optionalFieldOf(DENIED_ENTITY_TYPES_TAG, DEFAULT_EMPTY_LIST)
                          .forGetter(SpawnRules::deniedEntityTypes))
                  .apply(instance, SpawnRules::new));

  public static SpawnRules getDefault() {
    return new SpawnRules(
        DEFAULT_DISABLE_NATURAL_MOB_SPAWNING,
        DEFAULT_DISABLE_SPAWNER_BLOCKS,
        DEFAULT_EMPTY_LIST,
        DEFAULT_EMPTY_LIST);
  }

  public static SpawnRules fromJson(final JsonObject jsonObject) {
    // Check for new field names first, then fall back to legacy names
    boolean disableNaturalMobSpawning =
        jsonObject.has(DISABLE_NATURAL_MOB_SPAWNING_TAG)
            ? jsonObject.get(DISABLE_NATURAL_MOB_SPAWNING_TAG).getAsBoolean()
            : DEFAULT_DISABLE_NATURAL_MOB_SPAWNING;

    boolean disableSpawnerBlocks =
        jsonObject.has(DISABLE_SPAWNER_BLOCKS_TAG)
            ? jsonObject.get(DISABLE_SPAWNER_BLOCKS_TAG).getAsBoolean()
            : DEFAULT_DISABLE_SPAWNER_BLOCKS;

    List<ResourceLocation> allowedEntityTypes =
        parseEntityList(jsonObject, ALLOWED_ENTITY_TYPES_TAG);
    List<ResourceLocation> deniedEntityTypes = parseEntityList(jsonObject, DENIED_ENTITY_TYPES_TAG);

    return new SpawnRules(
        disableNaturalMobSpawning, disableSpawnerBlocks, allowedEntityTypes, deniedEntityTypes);
  }

  private static List<ResourceLocation> parseEntityList(
      final JsonObject jsonObject, final String tagName) {
    if (!jsonObject.has(tagName)) {
      return DEFAULT_EMPTY_LIST;
    }

    List<ResourceLocation> entityList = new ArrayList<>();

    try {
      if (jsonObject.get(tagName).isJsonArray()) {
        JsonArray jsonArray = jsonObject.getAsJsonArray(tagName);
        for (int i = 0; i < jsonArray.size(); i++) {
          try {
            String entityString = jsonArray.get(i).getAsString();
            ResourceLocation entityLocation = ResourceLocation.parse(entityString);
            entityList.add(entityLocation);
          } catch (Exception e) {
            // Skip invalid entries
          }
        }
      } else {
        // Handle single string value
        String entityString = jsonObject.get(tagName).getAsString();
        ResourceLocation entityLocation = ResourceLocation.parse(entityString);
        entityList.add(entityLocation);
      }
    } catch (Exception e) {
      // Return empty list on any parsing error
    }

    return entityList;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty(DISABLE_NATURAL_MOB_SPAWNING_TAG, disableNaturalMobSpawning);
    json.addProperty(DISABLE_SPAWNER_BLOCKS_TAG, disableSpawnerBlocks);

    if (!allowedEntityTypes.isEmpty()) {
      JsonArray allowArray = new JsonArray();
      for (ResourceLocation entity : allowedEntityTypes) {
        allowArray.add(entity.toString());
      }
      json.add(ALLOWED_ENTITY_TYPES_TAG, allowArray);
    }

    if (!deniedEntityTypes.isEmpty()) {
      JsonArray denyArray = new JsonArray();
      for (ResourceLocation entity : deniedEntityTypes) {
        denyArray.add(entity.toString());
      }
      json.add(DENIED_ENTITY_TYPES_TAG, denyArray);
    }

    return json;
  }

  public SpawnRules withNaturalMobSpawningDisabled(boolean disableNaturalSpawning) {
    return new SpawnRules(
        disableNaturalSpawning, disableSpawnerBlocks, allowedEntityTypes, deniedEntityTypes);
  }

  public SpawnRules withSpawnerBlocksDisabled(boolean disableSpawners) {
    return new SpawnRules(
        disableNaturalMobSpawning, disableSpawners, allowedEntityTypes, deniedEntityTypes);
  }

  public SpawnRules withMobAllowedByLocation(ResourceLocation entityLocation) {
    List<ResourceLocation> newAllowedMobs = new ArrayList<>(allowedEntityTypes);
    if (!newAllowedMobs.contains(entityLocation)) {
      newAllowedMobs.add(entityLocation);
    }
    return new SpawnRules(
        disableNaturalMobSpawning, disableSpawnerBlocks, newAllowedMobs, deniedEntityTypes);
  }

  public SpawnRules withMobDeniedByLocation(ResourceLocation entityLocation) {
    List<ResourceLocation> newDeniedMobs = new ArrayList<>(deniedEntityTypes);
    if (!newDeniedMobs.contains(entityLocation)) {
      newDeniedMobs.add(entityLocation);
    }
    return new SpawnRules(
        disableNaturalMobSpawning, disableSpawnerBlocks, allowedEntityTypes, newDeniedMobs);
  }

  public SpawnRules withoutMobByLocation(ResourceLocation entityLocation) {
    List<ResourceLocation> newAllowedMobs = new ArrayList<>(allowedEntityTypes);
    List<ResourceLocation> newDeniedMobs = new ArrayList<>(deniedEntityTypes);
    newAllowedMobs.remove(entityLocation);
    newDeniedMobs.remove(entityLocation);
    return new SpawnRules(
        disableNaturalMobSpawning, disableSpawnerBlocks, newAllowedMobs, newDeniedMobs);
  }

  public SpawnRules clearAllowedMobs() {
    return new SpawnRules(
        disableNaturalMobSpawning, disableSpawnerBlocks, DEFAULT_EMPTY_LIST, deniedEntityTypes);
  }

  public SpawnRules clearDeniedMobs() {
    return new SpawnRules(
        disableNaturalMobSpawning, disableSpawnerBlocks, allowedEntityTypes, DEFAULT_EMPTY_LIST);
  }

  public List<ResourceLocation> getAllowedMobs() {
    return allowedEntityTypes;
  }

  public List<ResourceLocation> getDeniedMobs() {
    return deniedEntityTypes;
  }

  public boolean isMobSpawnDisabled() {
    return disableNaturalMobSpawning;
  }

  public boolean isSpawningDisabled() {
    return disableSpawnerBlocks;
  }

  public boolean shouldDisableSpawners() {
    return disableSpawnerBlocks;
  }
}
