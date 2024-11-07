package net.tandara.dupefix.command;

import net.tandara.dupefix.DupeFixPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * @author Tiko
 * @created 07.11.2024, 22:50
 */
public class UnblockItem implements CommandExecutor {
    private final DupeFixPlugin plugin;

    public UnblockItem(DupeFixPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("unblockitem").setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            return false;
        }

        if (!player.hasPermission("dupefix.unblockitem")) {
            return false;
        }

        var itemInHand = player.getInventory().getItemInMainHand();

        if (!plugin.isBlocked(itemInHand)) {
            player.sendMessage("§cThis item is not blocked.");
            return false;
        }

        plugin.unblockItem(itemInHand);
        player.sendMessage("§aItem unblocked.");

        return false;
    }
}
