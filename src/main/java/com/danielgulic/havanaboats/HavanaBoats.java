package com.danielgulic.havanaboats;

//import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.mapdb.*;

import java.io.File;
import java.util.concurrent.ConcurrentMap;

public final class HavanaBoats extends JavaPlugin {

    private static HavanaBoats instance;
    private static BoatController boatController;
    private static DB db;

    public static HavanaBoats get() { return instance; }
    public static BoatController getBoatController() { return boatController; }

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

//        FileConfiguration fileConfiguration = getConfig();
        saveDefaultConfig();

        db = DBMaker
                .fileDB(new File(getDataFolder(), "fuel.db"))
                .fileMmapEnable()
                .make();
        ConcurrentMap<String,Integer> map = db
                .hashMap("map", Serializer.STRING, Serializer.INTEGER)
                .createOrOpen();

        boatController = new BoatController(this, map);
        boatController.startBoatTimer();

        getServer().getPluginManager().registerEvents(new EventListener(this), this);
        getCommand("engine").setExecutor(new BoatCommands());
        getCommand("reloadhavanaboats").setExecutor(new BoatCommands());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        instance = null;
        db.close();
    }
}
