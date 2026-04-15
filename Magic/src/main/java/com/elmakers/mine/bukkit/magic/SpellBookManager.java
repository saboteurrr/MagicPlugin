package com.elmakers.mine.bukkit.magic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.elmakers.mine.bukkit.api.magic.Messages;
import com.elmakers.mine.bukkit.api.spell.CastingCost;
import com.elmakers.mine.bukkit.api.spell.SpellKey;
import com.elmakers.mine.bukkit.api.spell.SpellTemplate;
import com.elmakers.mine.bukkit.utility.ConfigurationUtils;
import com.elmakers.mine.bukkit.wand.Wand;
import com.elmakers.mine.bukkit.wand.WandUpgradePath;

final class SpellBookManager {
    private final MagicController controller;
    private final Map<String, ?> categories;
    private final Messages messages;
    private final Map<String, SpellTemplate> spells;

    SpellBookManager(MagicController controller, Messages messages, Map<String, SpellTemplate> spells, Map<String, ?> categories) {
        this.controller = controller;
        this.categories = categories;
        this.messages = messages;
        this.spells = spells;
    }

    @Nonnull
    ItemStack getSpellBook() {
        return getSpellBook((com.elmakers.mine.bukkit.api.spell.SpellCategory)null);
    }

    @Nonnull
    ItemStack getSpellBook(@Nullable com.elmakers.mine.bukkit.api.spell.SpellCategory category) {
        Map<String, List<SpellTemplate>> categories = new HashMap<>();
        String categoryKey = category == null ? null : category.getKey();
        for (SpellTemplate spell : spells.values()) {
            if (spell.isHidden() || spell.getSpellKey().isVariant()) continue;
            com.elmakers.mine.bukkit.api.spell.SpellCategory spellCategory = spell.getCategory();
            if (spellCategory == null) continue;

            String spellCategoryKey = spellCategory.getKey();
            if (categoryKey == null || spellCategoryKey.equalsIgnoreCase(categoryKey)) {
                List<SpellTemplate> categorySpells = categories.get(spellCategoryKey);
                if (categorySpells == null) {
                    categorySpells = new ArrayList<>();
                    categories.put(spellCategoryKey, categorySpells);
                }
                categorySpells.add(spell);
            }
        }

        List<String> categoryKeys = new ArrayList<>(categories.keySet());
        Collections.sort(categoryKeys);

        ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta book = (BookMeta) bookItem.getItemMeta();
        book.setAuthor(messages.get("books.default.author"));
        String title;
        if (category != null) {
            title = messages.get("books.default.title").replace("$category", category.getName());
        } else {
            title = messages.get("books.all.title");
        }
        book.setTitle(title);
        List<String> pages = new ArrayList<>();
        for (String key : categoryKeys) {
            category = controller.getCategory(key);
            title = messages.get("books.default.title").replace("$category", category.getName());
            String description = "" + ChatColor.BOLD + ChatColor.BLUE + title + "\n\n";
            description += "" + ChatColor.RESET + ChatColor.DARK_BLUE + category.getDescription();
            pages.add(description);

            List<SpellTemplate> categorySpells = categories.get(key);
            Collections.sort(categorySpells);

            for (SpellTemplate spell : categorySpells) {
                pages.add(StringUtils.join(getSpellBookDescription(spell), "\n"));
            }
        }

        book.setPages(pages);
        bookItem.setItemMeta(book);
        return bookItem;
    }

    @Nonnull
    ItemStack getSpellBook(SpellTemplate spell) {
        ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta book = (BookMeta) bookItem.getItemMeta();
        book.setAuthor(messages.get("books.default.author"));
        book.setTitle(messages.get("books.spell.title").replace("$spell", spell.getName()));
        List<String> pages = new ArrayList<>();
        pages.add(StringUtils.join(getSpellBookDescription(spell), "\n"));
        book.setPages(pages);
        bookItem.setItemMeta(book);
        return bookItem;
    }

    @Nonnull
    ItemStack getSpellCategoriesBook() {
        List<String> categoryKeys = new ArrayList<>(categories.keySet());
        Collections.sort(categoryKeys);

        ItemStack bookItem = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta book = (BookMeta) bookItem.getItemMeta();
        book.setAuthor(messages.get("books.default.author"));
        book.setTitle(messages.get("books.categories.title"));
        List<String> pages = new ArrayList<>();
        for (String key : categoryKeys) {
            com.elmakers.mine.bukkit.api.spell.SpellCategory category = controller.getCategory(key);
            String description = messages.get("books.categories.category").replace("$category", category.getName());
            description += "\n\n" + ChatColor.RESET + category.getDescription();
            pages.add(description);
        }

        book.setPages(pages);
        bookItem.setItemMeta(book);
        return bookItem;
    }

    @Nonnull
    ItemStack getLearnSpellBook(SpellTemplate spell) {
        ConfigurationSection wandConfiguration = ConfigurationUtils.newConfigurationSection();
        wandConfiguration.set("template", "learnspell");
        wandConfiguration.set("icon", "book:" + spell.getKey());
        wandConfiguration.set("name", messages.get("books.learnspell.name").replace("$spell", spell.getName()));
        wandConfiguration.set("description", messages.get("books.learnspell.description").replace("$spell", spell.getName()));
        wandConfiguration.set("overrides", "spell " + spell.getKey());
        Wand wand = new Wand(controller, wandConfiguration);
        return wand.getItem();
    }

    @Nonnull
    List<String> getSpellBookDescription(SpellTemplate spell) {
        Set<String> paths = WandUpgradePath.getPathKeys();
        List<String> lines = new ArrayList<>();
        lines.add("" + ChatColor.GOLD + ChatColor.BOLD + spell.getName());
        lines.add("" + ChatColor.RESET);

        String spellDescription = spell.getDescription();
        if (spellDescription != null && spellDescription.length() > 0) {
            lines.add("" + ChatColor.BLACK + spellDescription);
            lines.add("");
        }

        int charges = spell.getMaxCharges();
        String description = messages.get("charges.description");
        if (charges > 1 && !description.isEmpty()) {
            String chargesDescription = description.replace("$count", Integer.toString(charges));
            lines.add("" + ChatColor.DARK_PURPLE + chargesDescription);
        }

        String spellCooldownDescription = spell.getCooldownDescription();
        description = messages.get("cooldown.description");
        if (spellCooldownDescription != null && spellCooldownDescription.length() > 0 && !description.isEmpty()) {
            spellCooldownDescription = description.replace("$time", spellCooldownDescription);
            lines.add("" + ChatColor.DARK_PURPLE + spellCooldownDescription);
        }

        String spellMageCooldownDescription = spell.getMageCooldownDescription();
        description = messages.get("cooldown.mage_description");
        if (spellMageCooldownDescription != null && spellMageCooldownDescription.length() > 0 && !description.isEmpty()) {
            spellMageCooldownDescription = description.replace("$time", spellMageCooldownDescription);
            lines.add("" + ChatColor.RED + spellMageCooldownDescription);
        }

        Collection<CastingCost> costs = spell.getCosts();
        description = messages.get("wand.costs_description");
        if (costs != null && !description.isEmpty()) {
            for (CastingCost cost : costs) {
                if (!cost.isEmpty()) {
                    lines.add(ChatColor.DARK_PURPLE + description.replace("$description", cost.getFullDescription(messages)));
                }
            }
        }
        Collection<CastingCost> activeCosts = spell.getActiveCosts();
        description = messages.get("wand.active_costs_description");
        if (activeCosts != null) {
            for (CastingCost cost : activeCosts) {
                if (!cost.isEmpty()) {
                    lines.add(ChatColor.DARK_PURPLE + description.replace("$description", cost.getFullDescription(messages)));
                }
            }
        }

        description = messages.get("spell.available_path");
        if (!description.isEmpty()) {
            for (String pathKey : paths) {
                WandUpgradePath checkPath = WandUpgradePath.getPath(pathKey);
                if (!checkPath.isHidden() && (checkPath.hasSpell(spell.getKey()) || checkPath.hasExtraSpell(spell.getKey()))) {
                    lines.add(ChatColor.DARK_BLUE + description.replace("$path", checkPath.getName()));
                    break;
                }
            }
        }

        description = messages.get("spell.required_path");
        if (!description.isEmpty()) {
            for (String pathKey : paths) {
                WandUpgradePath checkPath = WandUpgradePath.getPath(pathKey);
                if (checkPath.requiresSpell(spell.getKey())) {
                    lines.add(ChatColor.DARK_RED + description.replace("$path", checkPath.getName()));
                    break;
                }
            }
        }

        String duration = spell.getDurationDescription(messages);
        if (duration != null) {
            lines.add(ChatColor.DARK_GREEN + duration);
        } else if (spell.showUndoable()) {
            if (spell.isUndoable()) {
                String undoable = messages.get("spell.undoable", "");
                if (!undoable.isEmpty()) {
                    lines.add(undoable);
                }
            } else {
                String notUndoable = messages.get("spell.not_undoable", "");
                if (!notUndoable.isEmpty()) {
                    lines.add(notUndoable);
                }
            }
        }

        description = messages.get("spell.brush");
        if (spell.usesBrush() && !description.isEmpty()) {
            lines.add(ChatColor.DARK_GRAY + description);
        }

        SpellKey baseKey = spell.getSpellKey();
        SpellKey upgradeKey = new SpellKey(baseKey.getBaseKey(), baseKey.getLevel() + 1);
        SpellTemplate upgradeSpell = controller.getSpellTemplate(upgradeKey.getKey());
        int spellLevels = 0;
        while (upgradeSpell != null) {
            spellLevels++;
            upgradeKey = new SpellKey(upgradeKey.getBaseKey(), upgradeKey.getLevel() + 1);
            upgradeSpell = controller.getSpellTemplate(upgradeKey.getKey());
        }
        description = messages.get("spell.levels_available");
        if (spellLevels > 0 && !description.isEmpty()) {
            spellLevels++;
            lines.add(ChatColor.DARK_AQUA + description.replace("$levels", Integer.toString(spellLevels)));
        }

        String usage = spell.getUsage();
        if (usage != null && usage.length() > 0) {
            lines.add("" + ChatColor.GRAY + ChatColor.ITALIC + usage + ChatColor.RESET);
            lines.add("");
        }

        String spellExtendedDescription = spell.getExtendedDescription();
        if (spellExtendedDescription != null && spellExtendedDescription.length() > 0) {
            lines.add("" + ChatColor.BLACK + spellExtendedDescription);
            lines.add("");
        }

        return lines;
    }
}
