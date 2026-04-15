package com.elmakers.mine.bukkit.magic;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Skull;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.elmakers.mine.bukkit.api.item.ItemUpdatedCallback;
import com.elmakers.mine.bukkit.block.MaterialAndData;
import com.elmakers.mine.bukkit.utility.CompatibilityLib;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import com.elmakers.mine.bukkit.utility.SkullLoadedCallback;

final class SkinAndSkullManager {
    private final Map<Material, String> blockSkins = new HashMap<>();
    private final Map<EntityType, String> mobSkins = new HashMap<>();
    private final Map<EntityType, MaterialAndData> skullItems = new HashMap<>();
    private final Map<EntityType, MaterialAndData> skullWallBlocks = new HashMap<>();
    private final Map<EntityType, MaterialAndData> skullGroundBlocks = new HashMap<>();
    private final Map<EntityType, Material> mobEggs = new HashMap<>();

    void loadMobEggs(@Nullable ConfigurationSection skins) {
        mobEggs.clear();
        if (skins == null) return;
        Set<String> keys = skins.getKeys(false);
        for (String key : keys) {
            try {
                EntityType entityType = EntityType.valueOf(key.toUpperCase());
                Material material = getVersionedMaterial(skins, key);
                if (material != null) {
                    mobEggs.put(entityType, material);
                }
            } catch (Exception ignore) {
            }
        }
    }

    void loadMobSkins(@Nullable ConfigurationSection skins) {
        mobSkins.clear();
        if (skins == null) return;
        Set<String> keys = skins.getKeys(false);
        for (String key : keys) {
            try {
                EntityType entityType = EntityType.valueOf(key.toUpperCase());
                mobSkins.put(entityType, skins.getString(key));
            } catch (Exception ignore) {
            }
        }
    }

    void loadBlockSkins(@Nullable ConfigurationSection skins) {
        blockSkins.clear();
        if (skins == null) return;
        Set<String> keys = skins.getKeys(false);
        for (String key : keys) {
            try {
                Material material = Material.getMaterial(key.toUpperCase());
                blockSkins.put(material, skins.getString(key));
            } catch (Exception ignore) {
            }
        }
    }

    void loadSkulls(ConfigurationSection skulls) {
        skullItems.clear();
        skullGroundBlocks.clear();
        skullWallBlocks.clear();
        Set<String> keys = skulls.getKeys(false);
        for (String key : keys) {
            try {
                ConfigurationSection types = skulls.getConfigurationSection(key);
                EntityType entityType = EntityType.valueOf(key.toUpperCase());
                MaterialAndData item = parseSkullCandidate(types, "item");
                if (item != null) {
                    skullItems.put(entityType, item);
                }
                MaterialAndData floor = parseSkullCandidate(types, "ground");
                if (floor != null) {
                    skullGroundBlocks.put(entityType, floor);
                }
                MaterialAndData wall = parseSkullCandidate(types, "wall");
                if (wall != null) {
                    skullWallBlocks.put(entityType, wall);
                }
            } catch (Exception ignore) {
            }
        }
    }

    @Nullable
    String getBlockSkin(Material blockType) {
        return blockSkins.get(blockType);
    }

    @Nullable
    MaterialAndData getSkullItem(EntityType entityType) {
        return skullItems.get(entityType);
    }

    @Nullable
    MaterialAndData getSkullWallBlock(EntityType entityType) {
        return skullWallBlocks.get(entityType);
    }

    @Nullable
    Material getMobEgg(EntityType mobType) {
        Material material = mobEggs.get(mobType);
        if (material == null) {
            try {
                material = Material.valueOf(mobType.name() + "_SPAWN_EGG");
                mobEggs.put(mobType, material);
            } catch (Exception ignore) {
            }
        }
        return material;
    }

    @Nullable
    String getMobSkin(EntityType mobType) {
        return mobSkins.get(mobType);
    }

    @Nullable
    String getPlayerSkin(Player player) {
        return null;
    }

    @Nonnull
    ItemStack getURLSkull(String url) {
        try {
            ItemStack stack = getURLSkull(new URL(url), UUID.nameUUIDFromBytes(url.getBytes()));
            return stack == null ? new ItemStack(Material.AIR) : stack;
        } catch (MalformedURLException e) {
            Bukkit.getLogger().log(Level.WARNING, "Malformed URL: " + url, e);
        }

        return new ItemStack(Material.AIR);
    }

    void setSkullOwner(Skull skull, String ownerName) {
        CompatibilityLib.getDeprecatedUtils().setOwner(skull, ownerName);
    }

    void setSkullOwner(Skull skull, UUID uuid) {
        CompatibilityLib.getDeprecatedUtils().setOwner(skull, uuid);
    }

    @Nonnull
    ItemStack getSkull(String ownerName, String itemName) {
        return getSkull(ownerName, itemName, null);
    }

    @Nonnull
    ItemStack getSkull(String ownerName, String itemName, @Nullable ItemUpdatedCallback callback) {
        MaterialAndData skullType = skullItems.get(EntityType.PLAYER);
        if (skullType == null) {
            ItemStack air = new ItemStack(Material.AIR);
            if (callback != null) {
                callback.updated(air);
            }
            return air;
        }
        ItemStack skull = createSkullItem(skullType, itemName);
        CompatibilityLib.getDeprecatedUtils().setSkullOwner(skull, ownerName, wrapCallback(callback));
        return skull;
    }

    @Nonnull
    ItemStack getSkull(UUID uuid, String itemName, @Nullable ItemUpdatedCallback callback) {
        MaterialAndData skullType = skullItems.get(EntityType.PLAYER);
        if (skullType == null) {
            return new ItemStack(Material.AIR);
        }
        ItemStack skull = createSkullItem(skullType, itemName);
        CompatibilityLib.getDeprecatedUtils().setSkullOwner(skull, uuid, wrapCallback(callback));
        return skull;
    }

    @Nonnull
    ItemStack getSkull(Player player, String itemName) {
        MaterialAndData skullType = skullItems.get(EntityType.PLAYER);
        if (skullType == null) {
            return new ItemStack(Material.AIR);
        }
        ItemStack skull = createSkullItem(skullType, itemName);
        CompatibilityLib.getDeprecatedUtils().setSkullOwner(skull, player.getName(), null);
        return skull;
    }

    @Nonnull
    ItemStack getSkull(Entity entity, String itemName) {
        if (entity instanceof Player) {
            return getSkull((Player) entity, itemName);
        }
        return getSkull(entity, itemName, null);
    }

    @Nonnull
    ItemStack getSkull(Entity entity, String itemName, @Nullable ItemUpdatedCallback callback) {
        String ownerName = null;
        MaterialAndData skullType = skullItems.get(entity.getType());
        if (skullType == null) {
            ownerName = getMobSkin(entity.getType());
            skullType = skullItems.get(EntityType.PLAYER);
            if (skullType == null || ownerName == null) {
                ItemStack air = new ItemStack(Material.AIR);
                if (callback != null) {
                    callback.updated(air);
                }
                return air;
            }
        }
        if (entity instanceof Player) {
            ownerName = entity.getName();
        }

        ItemStack skull = createSkullItem(skullType, itemName);
        if (ownerName != null) {
            if (ownerName.startsWith("http")) {
                skull = CompatibilityLib.getInventoryUtils().setSkullURL(skull, ownerName);
                if (callback != null) {
                    callback.updated(skull);
                }
            } else {
                CompatibilityLib.getDeprecatedUtils().setSkullOwner(skull, ownerName, wrapCallback(callback));
            }
        } else if (callback != null) {
            callback.updated(skull);
        }
        return skull;
    }

    @Nullable
    private static Material getVersionedMaterial(ConfigurationSection configuration, String key) {
        Material material = null;
        Collection<String> candidates = ConfigurationUtils.getStringList(configuration, key);
        for (String candidate : candidates) {
            try {
                material = Material.valueOf(candidate.toUpperCase());
                break;
            } catch (Exception ignore) {
            }
        }
        return material;
    }

    @Nullable
    private static MaterialAndData parseSkullCandidate(ConfigurationSection section, String key) {
        Collection<String> candidates = ConfigurationUtils.getStringList(section, key);
        for (String candidate : candidates) {
            MaterialAndData test = new MaterialAndData(candidate.trim());
            if (test.isValid()) {
                return test;
            }
        }
        return null;
    }

    @Nullable
    private ItemStack getURLSkull(URL url, UUID id) {
        MaterialAndData skullType = skullItems.get(EntityType.PLAYER);
        if (skullType == null) {
            return new ItemStack(Material.AIR);
        }
        ItemStack skull = skullType.getItemStack(1);
        return CompatibilityLib.getInventoryUtils().setSkullURL(skull, url, id);
    }

    @Nonnull
    private static ItemStack createSkullItem(MaterialAndData skullType, @Nullable String itemName) {
        ItemStack skull = skullType.getItemStack(1);
        if (itemName != null) {
            ItemMeta meta = skull.getItemMeta();
            meta.setDisplayName(itemName);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    @Nullable
    private static SkullLoadedCallback wrapCallback(@Nullable ItemUpdatedCallback callback) {
        return callback == null ? null : callback::updated;
    }
}
