package com.minkang.ultimate.listeners;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.managers.ShopManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC 바인딩이 있는 경우에만 상점을 열어주는 리스너 (ultimate 패키지용)
 */
public class ShopNpcListener implements Listener {
    private final Main plugin;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    public ShopNpcListener(Main plugin){
        this.plugin = plugin;
    }

    private boolean isEnabled(){
        FileConfiguration c = plugin.getConfig();
        return c == null || c.getBoolean("npcshop.enabled", true);
    }

    private String boundKey(Entity e){
        try {
            if (e.hasMetadata("NPC")) {
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(e);
                if (npc != null){
                    FileConfiguration cfg = plugin.getConfig();
                    if (cfg != null){
                        String key = cfg.getString("npcshop.bindings."+npc.getId());
                        if (key != null && !key.trim().isEmpty()) return key.trim();
                    }
                }
            }
        } catch (Throwable ignored){}
        return null;
    }

    private boolean throttled(Player p){
        long now = System.currentTimeMillis();
        Long prev = lastUse.get(p.getUniqueId());
        if (prev != null && (now - prev) < 250) return true;
        lastUse.put(p.getUniqueId(), now);
        return false;
    }

    private void open(Player p, String key){
        try {
            ShopManager.getInstance().open(p, key);
        } catch (Throwable ignored){}
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent e){
        if (!isEnabled()) return;
        Player p = e.getPlayer();
        String key = boundKey(e.getRightClicked());
        if (key == null) return;
        if (throttled(p)) { e.setCancelled(true); return; }
        e.setCancelled(true);
        open(p, key);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e){
        if (!isEnabled()) return;
        Player p = e.getPlayer();
        String key = boundKey(e.getRightClicked());
        if (key == null) return;
        if (throttled(p)) { e.setCancelled(true); return; }
        e.setCancelled(true);
        open(p, key);
    }
}
