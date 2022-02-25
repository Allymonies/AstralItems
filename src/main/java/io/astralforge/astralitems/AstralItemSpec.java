package io.astralforge.astralitems;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTList;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.chat.ComponentSerializer;

public class AstralItemSpec {

    public final NamespacedKey id;
    public final Material material;
    public final String displayName;

    /* Specific item attributes */
    private Boolean vanillaCraftable = false;
    private Boolean renamable = false;
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

    public AstralItemSpec(NamespacedKey id, Material material, BaseComponent[] displayName) {
        this(id, material, baseComponentsToString(displayName));
    }

    public AstralItemSpec(NamespacedKey id, Material material, String displayName) {
        if (displayName.charAt(0) != '[' && displayName.charAt(0) != '{') {
            BaseComponent[] displayNameComponents = new ComponentBuilder(displayName).italic(false).color(ChatColor.WHITE).create();
            displayName = baseComponentsToString(displayNameComponents);
        }
        this.id = id;
        this.material = material;
        this.displayName = displayName;
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
        Bukkit.getServer().getLogger().info("isRenamed");
        NBTItem nbti = new NBTItem(item);
        if (!nbti.hasKey("display")) return true;
        NBTCompound display = nbti.getCompound("display");

        if (!display.hasKey("Name")) return true;
        Bukkit.getServer().getLogger().info("isRenamed = " + ((Boolean) !display.getString("Name").equals(displayName)).toString());
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

    public NamespacedKey getId() {
        return this.id;
    }


    public Material getMaterial() {
        return this.material;
    }


    public Boolean isVanillaCraftable() {
        return this.vanillaCraftable;
    }

    public Boolean getVanillaCraftable() {
        return this.vanillaCraftable;
    }

    public void setVanillaCraftable(Boolean vanillaCraftable) {
        this.vanillaCraftable = vanillaCraftable;
    }


    public String getDisplayName() {
        return this.displayName;
    }


    public Boolean isRenamable() {
        return this.renamable;
    }

    public Boolean getRenamable() {
        return this.renamable;
    }

    public void setRenamable(Boolean renamable) {
        this.renamable = renamable;
    }

    public BaseComponent[][] getDefaultLore() {
        return this.defaultLore;
    }

    public void setDefaultLore(BaseComponent[][] defaultLore) {
        this.defaultLore = defaultLore;
    }

    /*public Integer getBurnTime() {
        return this.burnTime;
    }

    public void setBurnTime(Integer burnTime) {
        this.burnTime = burnTime;
    }*/

}
