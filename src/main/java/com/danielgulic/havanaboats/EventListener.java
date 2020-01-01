package com.danielgulic.havanaboats;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Callable;

public class EventListener implements Listener {
    private final HavanaBoats instance;
    private HashMap<UUID, Long> interactCooldown = new HashMap<>();

    public EventListener(HavanaBoats instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onBoatPlace(PlayerInteractEvent event) {
        BoatController boatController = HavanaBoats.getBoatController();
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            if (player.getInventory().getItemInMainHand().getType().toString().endsWith("BOAT") || player.getInventory().getItemInOffHand().getType().toString().endsWith("BOAT")) {
                // player placed a boat
                event.setCancelled(true);
                for (Block b : player.getLineOfSight(null, 5)) {
                    if (b.getType() != Material.AIR) {
                        Location loc = b.getLocation().add(0, 1, 0);
                        Entity entity = player.getWorld().spawnEntity(loc, EntityType.BOAT);
                        Boat boat = (Boat) entity;
                        switch (player.getInventory().getItemInMainHand().getType()) {
                            case SPRUCE_BOAT:
                                boat.setWoodType(TreeSpecies.REDWOOD);
                                break;
                            case DARK_OAK_BOAT:
                                boat.setWoodType(TreeSpecies.DARK_OAK);
                                break;
                            case ACACIA_BOAT:
                                boat.setWoodType(TreeSpecies.ACACIA);
                                break;
                            case JUNGLE_BOAT:
                                boat.setWoodType(TreeSpecies.JUNGLE);
                                break;
                            case BIRCH_BOAT:
                                boat.setWoodType(TreeSpecies.BIRCH);
                                break;
                            case OAK_BOAT:
                            default:
                                boat.setWoodType(TreeSpecies.GENERIC);
                                break;
                        }
                        boatController.registerBoat(boat.getUniqueId());
                        if (player.getGameMode() != GameMode.CREATIVE) player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                        break;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (!player.isSneaking() || entity.getType() != EntityType.BOAT || player.getInventory().getItemInMainHand().getType() != Material.COAL_BLOCK) return;
        // player clicked a boat while sneaking holding a coal block
        if (!player.hasPermission("havanaboats.fuel")) return;
        BoatController boatController = HavanaBoats.getBoatController();
        if (!boatController.isBoatRegistered(entity.getUniqueId())) return;
        if (interactCooldown.containsKey(player.getUniqueId()) && interactCooldown.get(player.getUniqueId()) > System.currentTimeMillis()) {
            event.setCancelled(true);
        } else {
            interactCooldown.put(player.getUniqueId(), System.currentTimeMillis() + 50);
            if (boatController.isFuelTankFull(entity.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "This fuel tank is full.");
            } else {
                if (player.getInventory().getItemInMainHand().getAmount() > 1) {
                    player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
                boatController.addFuelToBoat(entity.getUniqueId(), 20);
                player.sendMessage(ChatColor.GREEN + "Fuel increased to " + boatController.getFuelLevel(entity.getUniqueId()) + ChatColor.GREEN + "%");
            }
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (vehicle.getType() != EntityType.BOAT) return;
        BoatController boatController = HavanaBoats.getBoatController();
        if (!boatController.isBoatRegistered(vehicle.getUniqueId())) return;
        int droppedBlocks = boatController.getFuelLevel(vehicle.getUniqueId()) / 20;
        if (droppedBlocks > 0) vehicle.getWorld().dropItemNaturally(vehicle.getLocation(), new ItemStack(Material.COAL_BLOCK, droppedBlocks));
        boatController.unregisterBoat(vehicle.getUniqueId());
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Vehicle vehicle = event.getVehicle();
        if (vehicle instanceof Boat) {
            if (vehicle.getPassengers().size() > 0) { // someone is using the boat so it will have super speed
                BoatController boatController = HavanaBoats.getBoatController();
                if (!boatController.isBoatRegistered(vehicle.getUniqueId())) return;
                int fuel = HavanaBoats.getBoatController().getFuelLevel(vehicle.getUniqueId());

                boolean engineDisabled = boatController.isEngineDisabled(vehicle.getUniqueId());

                boolean hasPlayer = false;
                for (Entity e : vehicle.getPassengers()) {
                    if (e instanceof Player) {
                        hasPlayer = true;
                        Player player = (Player) e;
                        player.sendActionBar(!engineDisabled ?
                                ChatColor.GREEN + "Fuel: " + ChatColor.YELLOW + fuel + ChatColor.YELLOW + "%"
                                : ChatColor.RED + "Engine is off");
                    }
                }

                if (engineDisabled) return;

                if (hasPlayer) {
                    Player driver = (Player) vehicle.getPassengers().get(0);
                    if (fuel > 0) { // if the boat has fuel, it will have enhanced speed
                        // increased speed
                        Boat boat = (Boat) vehicle;
                        if (!HavanaBoats.get().getConfig().getBoolean("allow-plane-mode") && boat.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) return;
                        Vector vec = new Vector(boat.getLocation().getDirection().getX(), 0.0,
                                boat.getLocation().getDirection().getZ());
                        boat.setVelocity(vec.multiply(1.5));
                    } else {
                        // normal speed
                    }
                }
            }
        }
    }

}
