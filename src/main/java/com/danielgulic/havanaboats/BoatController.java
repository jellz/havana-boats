package com.danielgulic.havanaboats;

import org.bukkit.Bukkit;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class BoatController {
    private final HavanaBoats instance;
    private final ConcurrentMap<String, Integer> fuelLevel;
    private final ArrayList<String> disabledEngines = new ArrayList<String>();

    // map of boat UUIDs and fuel levels (out of 100)
//    private Map<String, Integer> fuelLevel = new HashMap<>();

    public BoatController(HavanaBoats instance, ConcurrentMap<String, Integer> map) {
        this.instance = instance;
        this.fuelLevel = map;
    }

    public void registerBoat(UUID uuid) {
        fuelLevel.put(uuid.toString(), 0);
    }

    public void unregisterBoat(UUID uuid) {
        fuelLevel.remove(uuid.toString());
        disabledEngines.remove(uuid.toString());
    }

    public void addFuelToBoat(UUID uuid, int fuel) {
        int currentFuel = fuelLevel.get(uuid.toString());
        int newFuel = currentFuel + fuel;
        if (newFuel > 100) newFuel = 100;
        fuelLevel.put(uuid.toString(), newFuel);
        HavanaBoats.getDb().commit();
    }

    public void removeFuelFromBoat(UUID uuid, int fuel) {
        int currentFuel = fuelLevel.get(uuid.toString());
        int newFuel = currentFuel - fuel;
        if (newFuel < 0) newFuel = 0;
        fuelLevel.put(uuid.toString(), newFuel);
        HavanaBoats.getDb().commit();
    }

    public int getFuelLevel(UUID uuid) {
        return fuelLevel.get(uuid.toString());
    }

    public boolean isFuelTankFull(UUID uuid) {
        return getFuelLevel(uuid) >= 100;
    }

    public boolean isBoatRegistered(UUID uuid) {
        return fuelLevel.containsKey(uuid.toString());
    }

    public boolean isEngineDisabled(UUID uuid) {
        return disabledEngines.contains(uuid.toString());
    }

    // returns false if engine is now off, true if engine is now on
    public boolean toggleEngine(UUID uuid) {
        if (disabledEngines.contains(uuid.toString())) {
            disabledEngines.remove(uuid.toString());
            return true;
        }
        disabledEngines.add(uuid.toString());
        return false;
    }

    // fuel consumption
    public void startBoatTimer() {
        BukkitScheduler scheduler = this.instance.getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this.instance, () -> {
            // Loop through all registered boats every 120 ticks
//            for (Map.Entry<String, Integer> entry : fuelLevel.entrySet()) {
//                String uuidString = entry.getKey();
//                Integer fuel = entry.getValue();
//                UUID uuid = UUID.fromString(uuidString);
//                Entity entity = Util.getEntityByUniqueId(uuid);
//                if (entity == null) {
//                    HavanaBoats.get().getServer().broadcastMessage("entity == null");
//                    // remove from db
////                    unregisterBoat(uuid);
//                    return;
//                }
//                if (isEngineDisabled(uuid)) {
//                    HavanaBoats.get().getServer().broadcastMessage("isEngineDisabled = true");
//                    return;
//                }
//
//                ArrayList<Entity> playersInBoat = new ArrayList<Entity>();
//                for (Entity e : entity.getPassengers())
//                    if (e instanceof Player) playersInBoat.add(e);
//                if (playersInBoat.size() > 0) { // someone is using the boat so it will consume fuel
//                    removeFuelFromBoat(uuid, 1);
//                    HavanaBoats.get().getServer().broadcastMessage("removed 1 fuel from boat");
//                } else {
//                    HavanaBoats.get().getServer().broadcastMessage("passengers <= 0 so didnt remove fuel");
//                }
//            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isInsideVehicle() && (p.getVehicle() instanceof Boat) && isBoatRegistered(p.getVehicle().getUniqueId()) && !isEngineDisabled(p.getVehicle().getUniqueId())) {
                    removeFuelFromBoat(p.getVehicle().getUniqueId(), 1);
                }
            }
        }, 0L, 80L);
    }
}
