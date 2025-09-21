package com.minkang.ultimate.tutorial;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.*;

public class TutorialManager {
    private final org.bukkit.plugin.Plugin plugin;
    private final Set<java.util.UUID> selecting = new HashSet<>();
    private final Set<java.util.UUID> insideExit = new HashSet<>();

    public TutorialManager(org.bukkit.plugin.Plugin plugin){ this.plugin = plugin; }

    public void setStartLocation(Location loc){
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("tutorial.start.world", loc.getWorld().getName());
        cfg.set("tutorial.start.x", loc.getX());
        cfg.set("tutorial.start.y", loc.getY());
        cfg.set("tutorial.start.z", loc.getZ());
        cfg.set("tutorial.start.yaw", loc.getYaw());
        cfg.set("tutorial.start.pitch", loc.getPitch());
        plugin.saveConfig();
    }
    public Location getStartLocation(){
        FileConfiguration cfg = plugin.getConfig();
        String w = cfg.getString("tutorial.start.world", null);
        if (w == null) return null;
        World world = Bukkit.getWorld(w);
        if (world == null) return null;
        double x = cfg.getDouble("tutorial.start.x", 0);
        double y = cfg.getDouble("tutorial.start.y", 64);
        double z = cfg.getDouble("tutorial.start.z", 0);
        float yaw = (float)cfg.getDouble("tutorial.start.yaw", 0);
        float pitch = (float)cfg.getDouble("tutorial.start.pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }
    public void beginExitSelect(Player p){ selecting.add(p.getUniqueId()); }
    public boolean isSelecting(Player p){ return selecting.contains(p.getUniqueId()); }
    public void finishExitSelect(Player p, Location blockLoc){
        selecting.remove(p.getUniqueId());
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("tutorial.exit.world", blockLoc.getWorld().getName());
        cfg.set("tutorial.exit.x", blockLoc.getBlockX());
        cfg.set("tutorial.exit.y", blockLoc.getBlockY());
        cfg.set("tutorial.exit.z", blockLoc.getBlockZ());
        // Create defaults for commands if absent
        if (!cfg.isList("tutorial.exit.commands")){
            java.util.List<String> def = new java.util.ArrayList<>();
            def.add("spawn");
            cfg.set("tutorial.exit.commands", def);
        }
        plugin.saveConfig();
    }
    public Location getExitBlock(){
        FileConfiguration cfg = plugin.getConfig();
        String w = cfg.getString("tutorial.exit.world", null);
        if (w == null) return null;
        World world = Bukkit.getWorld(w);
        if (world == null) return null;
        int x = cfg.getInt("tutorial.exit.x", 0);
        int y = cfg.getInt("tutorial.exit.y", 64);
        int z = cfg.getInt("tutorial.exit.z", 0);
        return new Location(world, x + 0.5, y, z + 0.5);
    }
    public java.util.List<String> getStartCommands(){
    FileConfiguration cfg = plugin.getConfig();
    java.util.List<String> list = cfg.getStringList("tutorial.start.commands");
    if (list == null || list.isEmpty()){
        java.util.List<String> def = new java.util.ArrayList<>();
        def.add("warp 튜토리얼");
        cfg.set("tutorial.start.commands", def);
        plugin.saveConfig();
        list = def;
    }
    return list;
}

    public java.util.List<String> getExitCommands(){
        FileConfiguration cfg = plugin.getConfig();
        return cfg.getStringList("tutorial.exit.commands");
    }
    public boolean isInsideExit(Player p){ return insideExit.contains(p.getUniqueId()); }
    public void setInsideExit(Player p, boolean v){
        if (v) insideExit.add(p.getUniqueId());
        else insideExit.remove(p.getUniqueId());
    }
}
