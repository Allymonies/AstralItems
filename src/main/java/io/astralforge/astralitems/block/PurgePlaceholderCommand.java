package io.astralforge.astralitems.block;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import lombok.AllArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;

@AllArgsConstructor
public class PurgePlaceholderCommand implements CommandExecutor {

    BasicBlockStateManager stateManager;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 3) {
            BaseComponent[] result = new ComponentBuilder("Usage: /purgeplaceholder <x> <y> <z>").color(ChatColor.RED).create();
            sender.spigot().sendMessage(result);
            return true;
        }
        int x, y, z;
        try {
            x = Integer.parseInt(args[0]);
            y = Integer.parseInt(args[1]);
            z = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            BaseComponent[] result = new ComponentBuilder("Coordinates must be integers").color(ChatColor.RED).create();
            sender.spigot().sendMessage(result);
            return true;
        }
        if (!(sender instanceof Player)) {
            BaseComponent[] result = new ComponentBuilder("You must be a player to run this command").color(ChatColor.RED).create();
            sender.spigot().sendMessage(result);
            return true;
        }

        Player player = (Player) sender;
        Block block = player.getWorld().getBlockAt(x, y, z);
        BlockState blockState = block.getState();

        if (!stateManager.isPlaceholder(blockState)) {
            BaseComponent[] result = new ComponentBuilder("Block is not a placeholder").color(ChatColor.RED).create();
            sender.spigot().sendMessage(result);
            return true;
        }
        
        stateManager.forceProcessBlockRemoval(blockState);
        block.setType(Material.AIR, true);

        BaseComponent[] result = new ComponentBuilder("Placeholder block purged").color(ChatColor.GREEN).create();
        sender.spigot().sendMessage(result);
        return true;

    }
    
}
