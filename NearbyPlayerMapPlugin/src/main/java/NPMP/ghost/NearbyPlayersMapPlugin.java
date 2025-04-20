//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package NPMP.ghost;

import NPMP.ghost.MapRenderer.NearbyPlayersMapRenderer;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.map.MapView.Scale;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.ChatColor;
import java.text.MessageFormat;

public class NearbyPlayersMapPlugin extends JavaPlugin implements Listener {
    private String mapRemovalMessage;
    private String mapGivenMessage;
    private int mapRange;
    private int hideDuration;
    private int hideCost;
    private String hideMessage;
    private String unhideMessage;
    private String notEnoughXpMessage;
    private String hideInfoMessage;
    private String sneakRequiredMessage;
    private Map<UUID, Long> hiddenPlayers = new HashMap();
    private Map<UUID, Integer> playerMaps = new HashMap();
    private final Map<UUID, Long> interactCooldown = new HashMap<>();
    private final long COOLDOWN_MS = 500; // Cooldown de 500ms (medio segundo) - ajustable

    public void onEnable() {
        this.saveDefaultConfig();
        this.loadConfig();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("getplayermap").setExecutor(this);
        this.getCommand("hidefrommaps").setExecutor(this);
    }

    private void loadConfig() {
        FileConfiguration config = this.getConfig();
        this.mapRemovalMessage = config.getString("map_removal_message", "Has eliminado el mapa de jugadores. Puedes obtener uno nuevo usando el comando /getplayermap");
        this.mapGivenMessage = config.getString("map_given_message", "¡Has recibido un mapa de jugadores cercanos!");
        this.mapRange = config.getInt("map_range", 100);
        this.hideDuration = config.getInt("hide_duration", 120);
        this.hideCost = config.getInt("hide_cost", 10);
        this.hideMessage = config.getString("hide_message", "Te has ocultado del mapa por {0} minutos.");
        this.notEnoughXpMessage = config.getString("not_enough_xp_message", "No tienes suficiente experiencia para ocultarte. Necesitas al menos {0} niveles de experiencia.");
        this.sneakRequiredMessage = config.getString("sneak_required_message", "Debes estar agachado para activar la ocultación.");
        this.hideInfoMessage = config.getString("hide_info_message", "Puedes ocultarte de los mapas por 2 minutos usando /hidefrommaps (costo: 10 niveles de experiencia)");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> this.checkAndGiveMap(player), 20L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            if (this.isPlayerMap(clickedItem) && (event.getAction() == InventoryAction.DROP_ALL_SLOT || event.getAction() == InventoryAction.DROP_ONE_SLOT)) {
                Bukkit.getScheduler().runTaskLater(this, () -> this.notifyMapRemoval(player), 1L);
            }
        }

    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (this.isPlayerMap(event.getItemDrop().getItemStack())) {
            Bukkit.getScheduler().runTaskLater(this, () -> this.notifyMapRemoval(event.getPlayer()), 1L);
        }

    }

    private void notifyMapRemoval(Player player) {
        if (!this.playerHasMap(player)) {
            player.sendMessage(this.mapRemovalMessage);
        }

    }

    private boolean isPlayerMap(ItemStack item) {
        if (item != null && item.getType() == Material.FILLED_MAP) {
            MapMeta meta = (MapMeta)item.getItemMeta();
            return meta != null && meta.getMapView() != null && meta.getMapView().getRenderers().stream().anyMatch((r) -> r instanceof NearbyPlayersMapRenderer);
        } else {
            return false;
        }
    }

    private void checkAndGiveMap(Player player) {
        if (!this.playerHasMap(player)) {
            this.givePlayerMap(player);
        }

    }

    private boolean playerHasMap(Player player) {
        for(ItemStack item : player.getInventory().getContents()) {
            if (this.isPlayerMap(item)) {
                return true;
            }
        }

        return false;
    }

    private void givePlayerMap(Player player) {
        MapView view = Bukkit.createMap(player.getWorld());
        view.setScale(Scale.CLOSEST);
        view.addRenderer(new NearbyPlayersMapRenderer(this.mapRange, this));
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta)map.getItemMeta();
        meta.setMapView(view);
        map.setItemMeta(meta);
        player.getInventory().addItem(new ItemStack[]{map});
        this.playerMaps.put(player.getUniqueId(), view.getId());
        player.sendMessage(this.mapGivenMessage);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Integer mapId = (Integer)this.playerMaps.get(player.getUniqueId());
        if (mapId != null) {
            MapView view = Bukkit.getMap(mapId);
            if (view != null) {
                view.setCenterX(player.getLocation().getBlockX());
                view.setCenterZ(player.getLocation().getBlockZ());
            }
        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Action action = event.getAction();
        if (this.isPlayerMap(item) && action == Action.LEFT_CLICK_AIR) {
            long currentTime = System.currentTimeMillis(); // Obtener tiempo actual
            long lastInteractTime = interactCooldown.getOrDefault(player.getUniqueId(), 0L);
            if (currentTime - lastInteractTime < COOLDOWN_MS) {
                return; // Salir si el cooldown no ha pasado
            }
            if (player.isSneaking()) {
                if (player.getLevel() >= this.hideCost) {
                    this.hidePlayer(player);
                    interactCooldown.put(player.getUniqueId(), currentTime);
                } else {
                    ChatColor var10001 = ChatColor.RED;
                    player.sendMessage(var10001 + MessageFormat.format(this.notEnoughXpMessage, this.hideCost));
                    interactCooldown.put(player.getUniqueId(), currentTime);
                }
            } else {
                player.sendMessage(ChatColor.RED + this.sneakRequiredMessage);
                interactCooldown.put(player.getUniqueId(), currentTime);
            }
        }

    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("getplayermap")) {
            if (sender instanceof Player) {
                Player player = (Player)sender;
                this.givePlayerMap(player);
                return true;
            }
        } else if (command.getName().equalsIgnoreCase("hidefrommaps") && sender instanceof Player) {
            Player player = (Player)sender;
            this.hidePlayer(player);
            return true;
        }

        return false;
    }

    private void hidePlayer(Player player) {
        if (player.getLevel() >= this.hideCost) {
            player.setLevel(player.getLevel() - this.hideCost);
            this.hiddenPlayers.put(player.getUniqueId(), System.currentTimeMillis() + (long)(this.hideDuration * 1000));
            player.sendMessage(MessageFormat.format(this.hideMessage, this.hideDuration / 60));
            Bukkit.getScheduler().runTaskLater(this, () -> this.unhidePlayer(player), (long)this.hideDuration * 20L);
        } else {
            player.sendMessage(MessageFormat.format(this.notEnoughXpMessage, this.hideCost));
        }

    }

    private void unhidePlayer(Player player) {
        this.hiddenPlayers.remove(player.getUniqueId());
        player.sendMessage(this.unhideMessage);
    }

    public boolean isPlayerHidden(Player player) {
        Long unhideTime = (Long)this.hiddenPlayers.get(player.getUniqueId());
        if (unhideTime != null) {
            if (System.currentTimeMillis() > unhideTime) {
                this.hiddenPlayers.remove(player.getUniqueId());
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }
}

