package net.tandara.dupefix;

import java.awt.Color;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import net.tandara.dupefix.command.UnblockItem;
import net.tandara.dupefix.hook.DiscordWebhook;
import net.tandara.dupefix.hook.DiscordWebhook.EmbedObject;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * @author Tiko
 * @created 07.11.2024, 21:57
 */
public class DupeFixPlugin extends JavaPlugin {
    private final NamespacedKey unlocked = new NamespacedKey(this, "unlocked");

    @Override
    public void onEnable() {
        new UnblockItem(this);

        getServer().getPluginManager().registerEvents(new Listener() {
            /*@EventHandler
            public void handleBlockPlace(BlockPlaceEvent event) {
                var player = event.getPlayer();

                if (player.hasPermission("dupefix.bypass")) {
                    return;
                }

                var itemStackToBePlaced = event.getItemInHand();

                if (!isUnlocked(itemStackToBePlaced) && (isBlocked(itemStackToBePlaced))) {
                    event.setCancelled(true);
                    createWebHook(player, event.getBlock().getLocation(), new ItemStack[]{itemStackToBePlaced}, "Tried to place a blocked item.");
                    player.sendMessage("§cYou are not allowed to place this item.");
                    return;
                }

                 setUnlockedMeta(event.getBlock());
            }

            @EventHandler
            public void handleBlockBreak(BlockBreakEvent event) {
                var player = event.getPlayer();

                if (player.hasPermission("dupefix.bypass")) {
                    return;
                }

                if (hasBlockedMeta(event.getBlock())) {
                    event.setCancelled(true);
                    createWebHook(player, event.getBlock().getLocation(), new ItemStack[]{}, "Tried to break a block with a blocked item in it.");
                    player.sendMessage("§cYou are not allowed to break this block.");
                } else {
                    var drops = event.getBlock().getDrops();

                    event.setDropItems(false);

                    drops.forEach(item -> unblockItem(item));
                    drops.forEach(item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item));
                }
            }
             */

            @EventHandler
            public void handleContainer(InventoryOpenEvent event) {
                var player = event.getPlayer();

                if (player.hasPermission("dupefix.bypass")) {
                    return;
                }

                var contents = event.getInventory().getContents();

                for (var itemStacks : contents) {
                    if (itemStacks == null) {
                        continue;
                    }

                    if (isBlocked(itemStacks)) {
                        event.setCancelled(true);
                        createWebHook((Player) player,
                            event.getInventory().getLocation() != null
                                ? event.getInventory().getLocation()
                                : player.getLocation(), contents,
                            "Tried to open a container with a blocked item in it."
                        );
                        player.sendMessage("§cYou are not allowed to open this container with this item in it.");
                        // Lock container
                        return;
                    }
                }
            }

            @EventHandler
            public void pickUpItem(PlayerPickupItemEvent event) {
                var player = event.getPlayer();
                var itemStack = event.getItem().getItemStack();

                if (player.hasPermission("dupefix.bypass")) {
                    return;
                }

                if (isBlocked(itemStack)) {
                    event.setCancelled(true);
                    createWebHook(player, event.getItem().getLocation(), new ItemStack[]{itemStack}, "Tried to pick up a blocked item.");
                    player.sendMessage("§cYou are not allowed to pick up this item.");
                }
            }

            @EventHandler
            public void handlePortalEntrance(PlayerPortalEvent event) {
                event.setCancelled(true);

                Player player = event.getPlayer();
                if (player.isInsideVehicle()) {
                    event.setCancelled(true);
                }

                if (event.getCause() == PlayerPortalEvent.TeleportCause.NETHER_PORTAL
                    || event.getCause() == PlayerPortalEvent.TeleportCause.END_PORTAL) {
                    event.setCancelled(true);
                    createEndWebHook(player);
                }
            }

            @EventHandler
            public void handlePortalCreate(PortalCreateEvent event) {
                if (event.getEntity() != null) {
                    event.getEntity().getNearbyEntities(5, 5, 5).forEach(entity -> {
                        if (entity instanceof Player player && ((Player) entity).isInsideVehicle()) {
                            event.setCancelled(true);
                            createEndWebHook(player);
                        }
                    });
                }
                event.setCancelled(true);
            }

            @EventHandler
            public void onPlayerTeleport(PlayerTeleportEvent event) {
                if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                    event.setCancelled(true);
                    createEndWebHook(event.getPlayer());
                }
            }
        }, this);
    }

    private void createEndWebHook(Player player) {
        var url = getConfig().getString("webhook-url");
        var webhook = new DiscordWebhook(url);

        webhook.setAvatarUrl("https://i.ibb.co/FVq0Frm/tandara.jpg");
        webhook.setUsername("End-Detector 3000");
        webhook.addEmbed(new EmbedObject()
            .setTitle("End-Detector 3000")
            .setDescription("Tried to enter the end.")
            .setColor(Color.decode("#00cccc"))
            .addField("Spieler", player.getName(), false)
            .addField("Welt", player.getWorld().getName(), false)
            .addField("X", String.valueOf(player.getLocation().getBlockX()), false)
            .addField("Y", String.valueOf(player.getLocation().getBlockY()), false)
            .addField("Z", String.valueOf(player.getLocation().getBlockZ()), false)
            .setFooter("End-Detector 3000 | " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                "https://i.ibb.co/FVq0Frm/tandara.jpg"
            )
        );
    }

    private void createWebHook(Player player, Location location, ItemStack[] itemStacks, String type) {

        final var url = getConfig().getString("webhook-url");
        final var webhook = new DiscordWebhook(url);

        final var userName = player.getName();

        var blockedItems = Arrays.stream(itemStacks)
            .filter(this::isBlocked)
            .map(itemStack -> itemStack.getType().name())
            .toList();

        getServer().getOnlinePlayers().forEach(admin -> {
            if (admin.hasPermission("dupefix.notify")) {
                admin.sendMessage("§cA player tried to open a container / pick up a blocked item!");
                admin.sendMessage("§cWorld: " + player.getWorld().getName());
                admin.sendMessage(
                    "§cWhere? " + player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ()
                );
                admin.sendMessage("§cWho? " + player.getName());
                admin.sendMessage("§cWhat? " + String.join(", ", blockedItems));
            }
        });

        webhook.setAvatarUrl("https://i.ibb.co/FVq0Frm/tandara.jpg");
        webhook.setUsername("Dupe-Detector 3000");
        webhook.addEmbed(new EmbedObject()
            .setTitle("Dupe-Detector 3000")
            .setDescription(type)
            .setColor(Color.decode("#00cccc"))
            .addField("Spieler", userName, false)
            .addField("Welt", location.getWorld().getName(), false)
            .addField("X", String.valueOf(location.getBlockX()), false)
            .addField("Y", String.valueOf(location.getBlockY()), false)
            .addField("Z", String.valueOf(location.getBlockZ()), false)
            .addField("Gefundene Items", String.join(", ", blockedItems), false)
            .setFooter("Dupe-Detector 3000 | " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")),
                "https://i.ibb.co/FVq0Frm/tandara.jpg"
            )
        );

        try {
            webhook.execute();
        } catch (IOException | URISyntaxException noNoException) {
        }
    }

    public boolean isBlocked(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        if (itemStack.getItemMeta().getPersistentDataContainer().has(unlocked, PersistentDataType.INTEGER)) {
            return false;
        }

        for (var blocked : getConfig().getStringList("blocked-materials")) {
            if (itemStack.getType().name().contains(blocked)) {
                return !getConfig().getStringList("whitelisted-materials").contains(itemStack.getType().name());
            }
        }

        return false;
    }

    private void setUnlockedMeta(Block block) {
        BlockState blockState = block.getState();

        if (blockState instanceof TileState tileState) {
            PersistentDataContainer dataContainer = tileState.getPersistentDataContainer();
            dataContainer.set(unlocked, PersistentDataType.INTEGER, 1);
            tileState.update();
        } else {
            block.setMetadata("unlocked", new FixedMetadataValue(this, 1));
        }
    }

    private boolean hasBlockedMeta(Block block) {
        BlockState blockState = block.getState();

        if (blockState instanceof TileState tileState) {
            PersistentDataContainer dataContainer = tileState.getPersistentDataContainer();
            return dataContainer.has(unlocked, PersistentDataType.INTEGER);
        }

        return block.hasMetadata("unlocked");
    }

    public boolean isUnlocked(ItemStack itemStack) {
        return itemStack.getItemMeta().getPersistentDataContainer().has(unlocked, PersistentDataType.INTEGER);
    }

    public void unblockItem(ItemStack itemStack) {
        var meta = itemStack.getItemMeta();
        meta.getPersistentDataContainer().set(unlocked, PersistentDataType.INTEGER, 1);
        itemStack.setItemMeta(meta);
    }
}
