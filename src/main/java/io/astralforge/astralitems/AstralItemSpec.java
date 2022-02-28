package io.astralforge.astralitems;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTList;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.chat.ComponentSerializer;

@Data
@Builder
public class AstralItemSpec implements Keyed {

    public static final class AstralItemSpecBuilder {
        private String displayName;

        public AstralItemSpecBuilder displayName(String displayName) {
            if (displayName.charAt(0) != '[' && displayName.charAt(0) != '{') {
                BaseComponent[] displayNameComponents = new ComponentBuilder(displayName).italic(false).color(ChatColor.WHITE).create();
                displayName = baseComponentsToString(displayNameComponents);
            }
            this.displayName = displayName;
            return this;
        }

        public AstralItemSpecBuilder displayName(BaseComponent[] displayName) {
            this.displayName = baseComponentsToString(displayName);
            return this;
        }
    }

    @NonNull
    public final NamespacedKey id;
    @NonNull
    public final Material material;
    @NonNull
    public final String displayName;

    /* Specific item attributes */
    @Builder.Default
    private Boolean vanillaCraftable = false;
    @Builder.Default
    private Boolean renamable = false;
    @Builder.Default
    private BaseComponent[][] defaultLore = null;
    //Integer burnTime = null;

    static private String baseComponentsToString(BaseComponent[] components) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Boolean first = true;
        for (BaseComponent component : components) {
            if (!first) {
                sb.append(",");
            }
            sb.append(ComponentSerializer.toString(component));
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    public ItemStack createItemStack(int amount) {
        ItemStack item = new ItemStack(material, amount);
        NBTItem nbti = new NBTItem(item);
        nbti.setString("astral_id", id.toString());
        nbti.addCompound("display").setString("Name", displayName);
        if (defaultLore != null) {
            return setLore(nbti.getItem(), defaultLore);
        }
        return nbti.getItem();
    }

    public boolean hasNameInLore(ItemStack item) {
        NBTItem nbti = new NBTItem(item);
        if (!nbti.hasKey("display")) return false;
        NBTCompound display = nbti.getCompound("display");

        if (!display.hasKey("Lore")) return false;
        NBTList<String> loreLines = display.getStringList("Lore");

        if (loreLines.size() == 0) return false;
        return loreLines.get(0).equals(displayName);
    }

    public boolean isRenamed(ItemStack item) {
        NBTItem nbti = new NBTItem(item);
        if (!nbti.hasKey("display")) return true;
        NBTCompound display = nbti.getCompound("display");

        if (!display.hasKey("Name")) return true;
        return !display.getString("Name").equals(displayName);
    }

    public ItemStack setLore(ItemStack item, BaseComponent[][] components) {
        NBTItem nbti = new NBTItem(item);
        if (!nbti.hasKey("display")) nbti.addCompound("display");
        NBTCompound display = nbti.getCompound("display");

        NBTList<String> loreLines = display.getStringList("Lore");
        loreLines.clear();
        if (isRenamed(item)) {
            loreLines.add(displayName);
        }
        for (BaseComponent[] line : components) {
            loreLines.add(baseComponentsToString(line));
        }
        item = nbti.getItem();
        return item;
    }

    public BaseComponent[][] getLore(ItemStack item) {
        List<BaseComponent[]> lines = new ArrayList<>();
        NBTItem nbti = new NBTItem(item);
        if (!nbti.hasKey("display")) return lines.toArray(new BaseComponent[0][]);
        NBTCompound display = nbti.getCompound("display");

        NBTList<String> loreLines = display.getStringList("Lore");
        if (loreLines.size() == 0) return lines.toArray(new BaseComponent[0][]);
        boolean first = true;
        for (String line : loreLines) {
            if (first && line.equals(displayName)) {
                first = false;
                continue;
            }
            lines.add(ComponentSerializer.parse(line));
            first = false;
        }

        return lines.toArray(new BaseComponent[0][]);
    }

    public ItemStack createItemStack() {
        return createItemStack(1);
    }

    @Override
    public NamespacedKey getKey() {
        return id;
    }

    public static boolean isVanillaCraftable(AstralItems plugin, ItemStack item) {
        AstralItemSpec astralItem = plugin.getAstralItem(item);
        if (astralItem == null) return true;
        return astralItem.getVanillaCraftable();
    }
}
