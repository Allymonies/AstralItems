package io.astralforge.astralitems;

import java.util.List;

import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class GiveCommand implements CommandExecutor, TabCompleter {

    AstralItems plugin;

    GiveCommand(AstralItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            BaseComponent[] result = new ComponentBuilder("Usage: /giveai <item> [amount]").color(ChatColor.RED).create();
            sender.spigot().sendMessage(result);
            return true;
        }
        int amount;
        if (args.length == 1) {
            amount = 1;
        } else {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                BaseComponent[] result = new ComponentBuilder("Invalid amount " + args[1]).color(ChatColor.RED).create();
                sender.spigot().sendMessage(result);
                return true;
            }
        }

        NamespacedKey id = NamespacedKey.fromString(args[0], plugin);

        AstralItemSpec item = plugin.getAstralItem(id);

        if (item instanceof AstralPlaceholderItemSpec) {
            BaseComponent[] result = new ComponentBuilder("Unknown Item " + args[0]).color(ChatColor.RED).create();
            sender.spigot().sendMessage(result);
            return true;
        }

        ItemStack itemStack = item.createItemStack(amount);

        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.getInventory().addItem(itemStack);
            BaseComponent[] result = new ComponentBuilder("Gave item!").color(ChatColor.GREEN).create();
            sender.spigot().sendMessage(result);
        } else {
            BaseComponent[] result = new ComponentBuilder("This command must be executed by a player!").color(ChatColor.RED).create();
            sender.spigot().sendMessage(result);
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return plugin
                .getItems()
                .entrySet()
                .stream()
                .filter((e) -> {
                    return !(e.getValue() instanceof AstralPlaceholderItemSpec) 
                        && (e.getKey().toString().startsWith(args[0])
                            || e.getKey().toString().split(":")[1].startsWith(args[0]));
                })
                .map(e -> e.getKey().toString())
                .collect(java.util.stream.Collectors.toList());
        }
        return null;
    }
    
}
