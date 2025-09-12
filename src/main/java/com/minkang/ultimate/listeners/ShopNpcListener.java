package com.minkang.ultimate.listeners;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.shop.ShopIntegrationManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ShopNpcListener implements Listener {

    private final Main plugin;
    private final ShopIntegrationManager shop;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<UUID, Long>();

    public ShopNpcListener(Main plugin, ShopIntegrationManager shop){
        this.plugin = plugin;
        this.shop = shop;
    }

    private boolean isEnabled(){
        FileConfiguration c = plugin.getConfig();
        return c == null || c.getBoolean("shop.npc.enabled", true);
    }

    private boolean match(Entity e){
        FileConfiguration c = plugin.getConfig();
        if (c == null) return true; // default allow
        // 1) If all-npcs true and entity is Citizens NPC (has metadata "NPC"), allow
        if (c.getBoolean("shop.npc.triggers.all-npcs", false)){
            try {
                if (e.hasMetadata("NPC")) return true;
            } catch (Throwable ignored){}
        }
        // 2) By name contains (case-insensitive) any of triggers
        List<String> names = c.getStringList("shop.npc.triggers.names");
        String nm = e.getCustomName();
        if (nm != null && !names.isEmpty()){
            String lower = nm.toLowerCase();
            for (String key : names){
                if (key == null) continue;
                if (lower.contains(key.toLowerCase())) return true;
            }
        }
        // 3) By scoreboard tags
        List<String> tags = c.getStringList("shop.npc.triggers.tags");
        if (!tags.isEmpty()){
            try {
                for (String t : tags){
                    if (t == null) continue;
                    if (e.getScoreboardTags().contains(t)) return true;
                }
            } catch (Throwable ignored){}
        }
        // 4) If it is a Citizens NPC (has metadata "NPC") and names/tags are empty, default allow
        try {
            if ((names == null || names.isEmpty()) && (tags == null || tags.isEmpty()) && e.hasMetadata("NPC"))
                return true;
        } catch (Throwable ignored){}

        return false;
    }

    private boolean throttled(Player p){
        long now = System.currentTimeMillis();
        Long prev = lastUse.get(p.getUniqueId());
        if (prev != null && (now - prev) < 250){
            return true;
        }
        lastUse.put(p.getUniqueId(), now);
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent e){
        if (!isEnabled()) return;
        Player p = e.getPlayer();
        Entity en = e.getRightClicked();
        if (en == null) return;
        if (!match(en)) return;
        if (throttled(p)) { e.setCancelled(true); return; }
        e.setCancelled(true);
        shop.openFor(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e){
        if (!isEnabled()) return;
        Player p = e.getPlayer();
        Entity en = e.getRightClicked();
        if (en == null) return;
        if (!match(en)) return;
        if (throttled(p)) { e.setCancelled(true); return; }
        e.setCancelled(true);
        shop.openFor(p);
    }
}