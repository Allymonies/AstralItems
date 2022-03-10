package io.astralforge.astralitems;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.astralforge.astralitems.block.*;
import io.astralforge.astralitems.block.tile.AstralTileEntity;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import de.tr7zw.nbtapi.NBTItem;
import io.astralforge.astralitems.recipe.AstralRecipeEvaluator;
import io.astralforge.astralitems.recipe.CraftingListener;
import lombok.Getter;

public class AstralItems extends JavaPlugin {

    private final Map<NamespacedKey, AstralItemSpec> items = new HashMap<>();
    private final Map<NamespacedKey, AbstractAstralBlockSpec> blocks = new HashMap<>();

    private boolean pendingHydrate = false;

    private BasicBlockStateManager basicBlockStateManager;
    
    @Getter
    private AstralRecipeEvaluator recipeEvaluator;

    public static AstralItems getInstance() {
        return (AstralItems) Bukkit.getPluginManager().getPlugin("AstralItems");
    }

    @Override
    public void onEnable() {
        final int pluginId = 14458;
        this.saveDefaultConfig();

        //Metrics metrics = new Metrics(this, pluginId);
        new Metrics(this, pluginId);

        basicBlockStateManager = new BasicBlockStateManager(this);

        recipeEvaluator = new AstralRecipeEvaluator();

        AstralItemListener itemEventListener = new AstralItemListener(this);
        getServer().getPluginManager().registerEvents(itemEventListener, this);

        CraftingListener craftingListener = new CraftingListener(this);
        getServer().getPluginManager().registerEvents(craftingListener, this);

        BasicBlockEventListener blockEventListener = new BasicBlockEventListener(this, basicBlockStateManager);
        getServer().getPluginManager().registerEvents(blockEventListener, this);

        GiveCommand giveCommand = new GiveCommand(this);
        getCommand("giveai").setExecutor(giveCommand);

        PurgePlaceholderCommand purgePlaceholderCommand = new PurgePlaceholderCommand(basicBlockStateManager);
        getCommand("purgeplaceholder").setExecutor(purgePlaceholderCommand);

        //FileConfiguration config = this.getConfig();
        hydrate();
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
        recipeEvaluator.unregisterAllRecipes();
    }

    private void hydrate() {
        basicBlockStateManager.refreshTickCache();
    }

    private void scheduleHydrate() {
        if (!pendingHydrate) {
            pendingHydrate = true;
            getServer().getScheduler().runTaskLater(this, () -> {
                pendingHydrate = false;
                hydrate();
            }, 1);
        }
    }

    public void addItem(AstralItemSpec item) {
        if (items.containsKey(item.getId())) {
            scheduleHydrate();
        }
        items.put(item.getId(), item);
    }

    public void addBlock(AbstractAstralBlockSpec block) {
        if (blocks.containsKey(block.itemSpec.getId())) {
            scheduleHydrate();
        }
        items.put(block.itemSpec.getId(), block.itemSpec);
        blocks.put(block.itemSpec.getId(), block);
    }

    public boolean isAstralItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        NBTItem nbti = new NBTItem(item);
        return nbti.hasKey("astral_id");
    }

    public boolean isAstralItem(NamespacedKey id) {
        return items.containsKey(id);
    }

    public boolean isAstralBlock(NamespacedKey id) {
        return blocks.containsKey(id);
    }

    private AstralPlaceholderItemSpec createPlaceholderItem(NamespacedKey id) {
        AstralPlaceholderItemSpec placeholderItem = new AstralPlaceholderItemSpec(id);
        items.put(placeholderItem.getId(), placeholderItem);
        return placeholderItem;
    }

    private AstralPlaceholderBlockSpec createPlaceholderBlock(NamespacedKey id, AstralItemSpec itemSpec) {
        AstralPlaceholderBlockSpec blockSpec = AstralPlaceholderBlockSpec.builder()
            .itemSpec(itemSpec)
            .build();
        blocks.put(blockSpec.itemSpec.getId(), blockSpec);
        return blockSpec;
    }

    public AstralItemSpec getAstralItem(ItemStack item) {
        if (!isAstralItem(item)) return null;
        NBTItem nbti = new NBTItem(item);
        String id = nbti.getString("astral_id");
        AstralItemSpec astralItemSpec = items.get(NamespacedKey.fromString(id));
        if (astralItemSpec == null) {
            astralItemSpec = createPlaceholderItem(NamespacedKey.fromString(id));
        }
        return astralItemSpec;
    }

    public AstralItemSpec getAstralItem(NamespacedKey id) {
        if (items.containsKey(id)) return items.get(id);
        return createPlaceholderItem(id);
    }

    public Optional<AbstractAstralBlockSpec> getAstralBlock(Block block) {
        return basicBlockStateManager.getSpecFromBlock(block).map(spec -> /* Downcast */ spec);
    }

    public AbstractAstralBlockSpec getAstralBlock(ItemStack item) {
        if (!isAstralItem(item)) return null;
        NBTItem nbti = new NBTItem(item);
        String id = nbti.getString("astral_id");
        AbstractAstralBlockSpec astralBlockSpec = blocks.get(NamespacedKey.fromString(id));
        if (astralBlockSpec == null) {
            AstralItemSpec itemSpec = getAstralItem(item);
            if (itemSpec instanceof AstralPlaceholderItemSpec) {
                astralBlockSpec = createPlaceholderBlock(NamespacedKey.fromString(id), itemSpec);
            }
        }

        return blocks.get(NamespacedKey.fromString(id));
    }

    public AbstractAstralBlockSpec getAstralBlock(NamespacedKey id) {
        if (blocks.containsKey(id)) return blocks.get(id);
        return createPlaceholderBlock(id, getAstralItem(id));
    }

    public Optional<AstralTileEntity> getTileEntity(Block block) {
        return basicBlockStateManager.getTileEntityFromBlock(block);
    }

    public Map<NamespacedKey,AstralItemSpec> getItems() {
        return this.items;
    }

    public Map<NamespacedKey,AbstractAstralBlockSpec> getBlocks() {
        return this.blocks;
    }

}
