package io.astralforge.astralitems;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
//import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import de.tr7zw.nbtapi.NBTItem;
import io.astralforge.astralitems.block.AbstractAstralBlockSpec;
import io.astralforge.astralitems.block.AstralBasicBlockSpec;
import io.astralforge.astralitems.block.BasicBlockEventListener;
import io.astralforge.astralitems.block.BasicBlockStateManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.util.HashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;

public class AstralItems extends JavaPlugin {

    private final Map<NamespacedKey, AstralItemSpec> items = new HashMap<>();
    private final Map<NamespacedKey, AbstractAstralBlockSpec> blocks = new HashMap<>();

    private BasicBlockStateManager basicBlockStateManager;

    @Override
    public void onEnable() {
        final int pluginId = 14458;
        this.saveDefaultConfig();

        //Metrics metrics = new Metrics(this, pluginId);
        new Metrics(this, pluginId);

        basicBlockStateManager = new BasicBlockStateManager(this);

        AstralItemListener itemEventListener = new AstralItemListener(this);
        getServer().getPluginManager().registerEvents(itemEventListener, this);

        BasicBlockEventListener blockEventListener = new BasicBlockEventListener(this, basicBlockStateManager);
        getServer().getPluginManager().registerEvents(blockEventListener, this);

        GiveCommand giveCommand = new GiveCommand(this);
        getCommand("giveai").setExecutor(giveCommand);


        NamespacedKey gayCrystalId = new NamespacedKey(this, "gay_crystal");
        AstralItemSpec gayCrystal = new AstralItemSpec(gayCrystalId, Material.NETHER_STAR, "Gay Crystal");
        items.put(gayCrystalId, gayCrystal);

        NamespacedKey lesbianIngotId = new NamespacedKey(this, "lesbian_ingot");
        BaseComponent[] lesbianIngotName = new ComponentBuilder("Lesbian Ingot").italic(false).bold(true).color(ChatColor.AQUA).create();
        AstralItemSpec lesbianIngot = new AstralItemSpec(lesbianIngotId, Material.NETHER_BRICK, lesbianIngotName);
        BaseComponent[][] lesbianIngotLore = {new ComponentBuilder("The gayest ingot around").italic(false).bold(true).color(ChatColor.RED).create()};
        lesbianIngot.setDefaultLore(lesbianIngotLore);
        lesbianIngot.setRenamable(true);
        addItem(lesbianIngot);

        // Block
        NamespacedKey gayBlockId = new NamespacedKey(this, "gay_block");
        AstralItemSpec gayBlockItem = new AstralItemSpec(gayBlockId, Material.DIAMOND_BLOCK, "Gay Block");
        AstralBasicBlockSpec gayBlock = new AstralBasicBlockSpec(gayBlockItem);
        addBlock(gayBlock);

        //FileConfiguration config = this.getConfig();
        getLogger().info("AstralItems running!");
        //Fired when the server enables the plugin
        /*CommandPK commandPK = new CommandPK(data, this);
        this.getCommand("pk").setExecutor(commandPK);
        this.getCommand("pk").setTabCompleter(commandPK);
        proxyListener = new ProxyListener(data, config, chat, discord);
        getServer().getPluginManager().registerEvents(proxyListener, this);*/
    }

    @Override
    public void onDisable() {
        //Fired when the server stops and disables all plugins
    }

    public void addItem(AstralItemSpec item) {
        items.put(item.getId(), item);
    }

    public void addBlock(AbstractAstralBlockSpec block) {
        items.put(block.itemSpec.getId(), block.itemSpec);
        blocks.put(block.itemSpec.getId(), block);
    }

    public boolean isAstralItem(ItemStack item) {
        if (item == null) return false;
        NBTItem nbti = new NBTItem(item);
        return nbti.hasKey("astral_id");
    }

    public AstralItemSpec getAstralItem(ItemStack item) {
        if (!isAstralItem(item)) return null;
        NBTItem nbti = new NBTItem(item);
        String id = nbti.getString("astral_id");
        return items.get(NamespacedKey.fromString(id));
    }

    public AstralItemSpec getAstralItem(NamespacedKey id) {
        if (items.containsKey(id)) return items.get(id);
        return null;
    }

    public AbstractAstralBlockSpec getAstralBlock(ItemStack item) {
        if (!isAstralItem(item)) return null;
        NBTItem nbti = new NBTItem(item);
        String id = nbti.getString("astral_id");
        return blocks.get(NamespacedKey.fromString(id));
    }

    public AbstractAstralBlockSpec getAstralBlock(NamespacedKey id) {
        if (blocks.containsKey(id)) return blocks.get(id);
        return null;
    }

    public Map<NamespacedKey,AstralItemSpec> getItems() {
        return this.items;
    }

    public Map<NamespacedKey,AbstractAstralBlockSpec> getBlocks() {
        return this.blocks;
    }

}
