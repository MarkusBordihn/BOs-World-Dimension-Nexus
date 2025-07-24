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
import net.minecraft.world.entity.EntityType;

public record SpawnRules(
    boolean disableMobSpawn,
    boolean disableSpawner,
    List<ResourceLocation> allowMobSpawn,
    List<ResourceLocation> denyMobSpawn) {

  public static final String DISABLE_MOB_SPAWN_TAG = "disableMobSpawn";
  public static final String DISABLE_SPAWNER_TAG = "disableSpawner";
  public static final String ALLOW_MOB_SPAWN_TAG = "allowMobSpawn";
  public static final String DENY_MOB_SPAWN_TAG = "denyMobSpawn";

  public static final boolean DEFAULT_DISABLE_MOB_SPAWN = false;
  public static final boolean DEFAULT_DISABLE_SPAWNER = false;
  public static final List<ResourceLocation> DEFAULT_EMPTY_LIST = List.of();

  public static final Codec<SpawnRules> CODEC =
      RecordCodecBuilder.create(
          instance ->
              instance
                  .group(
                      Codec.BOOL
                          .optionalFieldOf(DISABLE_MOB_SPAWN_TAG, DEFAULT_DISABLE_MOB_SPAWN)
                          .forGetter(SpawnRules::disableMobSpawn),
                      Codec.BOOL
                          .optionalFieldOf(DISABLE_SPAWNER_TAG, DEFAULT_DISABLE_SPAWNER)
                          .forGetter(SpawnRules::disableSpawner),
                      ResourceLocation.CODEC
                          .listOf()
                          .optionalFieldOf(ALLOW_MOB_SPAWN_TAG, DEFAULT_EMPTY_LIST)
                          .forGetter(SpawnRules::allowMobSpawn),
                      ResourceLocation.CODEC
                          .listOf()
                          .optionalFieldOf(DENY_MOB_SPAWN_TAG, DEFAULT_EMPTY_LIST)
                          .forGetter(SpawnRules::denyMobSpawn))
                  .apply(instance, SpawnRules::new));

  public static SpawnRules getDefault() {
    return new SpawnRules(
        DEFAULT_DISABLE_MOB_SPAWN, DEFAULT_DISABLE_SPAWNER, DEFAULT_EMPTY_LIST, DEFAULT_EMPTY_LIST);
  }

  public static SpawnRules fromJson(final JsonObject jsonObject) {
    boolean disableMobSpawn =
        jsonObject.has(DISABLE_MOB_SPAWN_TAG)
            ? jsonObject.get(DISABLE_MOB_SPAWN_TAG).getAsBoolean()
            : DEFAULT_DISABLE_MOB_SPAWN;

    boolean disableSpawner =
        jsonObject.has(DISABLE_SPAWNER_TAG)
            ? jsonObject.get(DISABLE_SPAWNER_TAG).getAsBoolean()
            : DEFAULT_DISABLE_SPAWNER;

    List<ResourceLocation> allowMobSpawn = parseEntityList(jsonObject, ALLOW_MOB_SPAWN_TAG);
    List<ResourceLocation> denyMobSpawn = parseEntityList(jsonObject, DENY_MOB_SPAWN_TAG);

    return new SpawnRules(disableMobSpawn, disableSpawner, allowMobSpawn, denyMobSpawn);
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
    json.addProperty(DISABLE_MOB_SPAWN_TAG, disableMobSpawn);
    json.addProperty(DISABLE_SPAWNER_TAG, disableSpawner);

    if (!allowMobSpawn.isEmpty()) {
      JsonArray allowArray = new JsonArray();
      for (ResourceLocation entity : allowMobSpawn) {
        allowArray.add(entity.toString());
      }
      json.add(ALLOW_MOB_SPAWN_TAG, allowArray);
    }

    if (!denyMobSpawn.isEmpty()) {
      JsonArray denyArray = new JsonArray();
      for (ResourceLocation entity : denyMobSpawn) {
        denyArray.add(entity.toString());
      }
      json.add(DENY_MOB_SPAWN_TAG, denyArray);
    }

    return json;
  }

  public SpawnRules withDisableMobSpawn(boolean newDisableMobSpawn) {
    return new SpawnRules(newDisableMobSpawn, disableSpawner, allowMobSpawn, denyMobSpawn);
  }

  public SpawnRules withDisableSpawner(boolean newDisableSpawner) {
    return new SpawnRules(disableMobSpawn, newDisableSpawner, allowMobSpawn, denyMobSpawn);
  }

  public SpawnRules withAllowMobSpawn(List<ResourceLocation> newAllowMobSpawn) {
    return new SpawnRules(disableMobSpawn, disableSpawner, newAllowMobSpawn, denyMobSpawn);
  }

  public SpawnRules withDenyMobSpawn(List<ResourceLocation> newDenyMobSpawn) {
    return new SpawnRules(disableMobSpawn, disableSpawner, allowMobSpawn, newDenyMobSpawn);
  }

  public SpawnRules withMobSpawnDisabled(boolean newDisableMobSpawn) {
    return new SpawnRules(newDisableMobSpawn, disableSpawner, allowMobSpawn, denyMobSpawn);
  }

  public SpawnRules withSpawningDisabled(boolean newDisableSpawner) {
    return new SpawnRules(disableMobSpawn, newDisableSpawner, allowMobSpawn, denyMobSpawn);
  }

  public SpawnRules withMobAllowed(EntityType<?> entityType) {
    ResourceLocation entityLocation = EntityType.getKey(entityType);
    List<ResourceLocation> newAllowedMobs = new ArrayList<>(allowMobSpawn);
    if (!newAllowedMobs.contains(entityLocation)) {
      newAllowedMobs.add(entityLocation);
    }
    return new SpawnRules(disableMobSpawn, disableSpawner, newAllowedMobs, denyMobSpawn);
  }

  public SpawnRules withMobDenied(EntityType<?> entityType) {
    ResourceLocation entityLocation = EntityType.getKey(entityType);
    List<ResourceLocation> newDeniedMobs = new ArrayList<>(denyMobSpawn);
    if (!newDeniedMobs.contains(entityLocation)) {
      newDeniedMobs.add(entityLocation);
    }
    return new SpawnRules(disableMobSpawn, disableSpawner, allowMobSpawn, newDeniedMobs);
  }

  public SpawnRules withoutMob(EntityType<?> entityType) {
    ResourceLocation entityLocation = EntityType.getKey(entityType);
    List<ResourceLocation> newAllowedMobs = new ArrayList<>(allowMobSpawn);
    List<ResourceLocation> newDeniedMobs = new ArrayList<>(denyMobSpawn);
    newAllowedMobs.remove(entityLocation);
    newDeniedMobs.remove(entityLocation);
    return new SpawnRules(disableMobSpawn, disableSpawner, newAllowedMobs, newDeniedMobs);
  }

  public SpawnRules withMobAllowedByLocation(ResourceLocation entityLocation) {
    List<ResourceLocation> newAllowedMobs = new ArrayList<>(allowMobSpawn);
    if (!newAllowedMobs.contains(entityLocation)) {
      newAllowedMobs.add(entityLocation);
    }
    return new SpawnRules(disableMobSpawn, disableSpawner, newAllowedMobs, denyMobSpawn);
  }

  public SpawnRules withMobDeniedByLocation(ResourceLocation entityLocation) {
    List<ResourceLocation> newDeniedMobs = new ArrayList<>(denyMobSpawn);
    if (!newDeniedMobs.contains(entityLocation)) {
      newDeniedMobs.add(entityLocation);
    }
    return new SpawnRules(disableMobSpawn, disableSpawner, allowMobSpawn, newDeniedMobs);
  }

  public SpawnRules withoutMobByLocation(ResourceLocation entityLocation) {
    List<ResourceLocation> newAllowedMobs = new ArrayList<>(allowMobSpawn);
    List<ResourceLocation> newDeniedMobs = new ArrayList<>(denyMobSpawn);
    newAllowedMobs.remove(entityLocation);
    newDeniedMobs.remove(entityLocation);
    return new SpawnRules(disableMobSpawn, disableSpawner, newAllowedMobs, newDeniedMobs);
  }

  public SpawnRules clearAllowedMobs() {
    return new SpawnRules(disableMobSpawn, disableSpawner, DEFAULT_EMPTY_LIST, denyMobSpawn);
  }

  public SpawnRules clearDeniedMobs() {
    return new SpawnRules(disableMobSpawn, disableSpawner, allowMobSpawn, DEFAULT_EMPTY_LIST);
  }

  public List<ResourceLocation> getAllowedMobs() {
    return allowMobSpawn;
  }

  public List<ResourceLocation> getDeniedMobs() {
    return denyMobSpawn;
  }

  public boolean isMobSpawnDisabled() {
    return disableMobSpawn;
  }

  public boolean isSpawningDisabled() {
    return disableSpawner;
  }

  public boolean isEntityAllowed(ResourceLocation entityType) {
    // If mob spawn is globally disabled, check allow list
    if (disableMobSpawn) {
      return allowMobSpawn.contains(entityType);
    }

    // If entity is in deny list, it's not allowed
    if (denyMobSpawn.contains(entityType)) {
      return false;
    }

    // If allow list is specified and entity is not in it, it's not allowed
    if (!allowMobSpawn.isEmpty() && !allowMobSpawn.contains(entityType)) {
      return false;
    }

    return true;
  }

  public boolean shouldDisableSpawners() {
    return disableSpawner;
  }
}
