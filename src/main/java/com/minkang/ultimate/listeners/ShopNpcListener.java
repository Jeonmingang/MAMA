package com.minkang.ultimate.listeners;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.managers.ShopManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC 연동 리스너: 바인딩/트리거가 있는 경우에만 상점을 열고,
 * 그렇지 않으면 이벤트를 손대지 않아서 다른 플러그인에 영향이 없습니다.
 */
public class ShopNpcListener implements Listener {
    private final Main plugin;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

    public ShopNpcListener(Main plugin){
        this.plugin = plugin;
    }

    private boolean enabled(){
        FileConfiguration c = plugin.getConfig();
        return c == null || c.getBoolean("npcshop.enabled", true);
    }

    private boolean throttled(Player p){
        long now = System.currentTimeMillis();
        Long prev = lastUse.get(p.getUniqueId());
        if (prev != null && (now - prev) < 250) return true;
        lastUse.put(p.getUniqueId(), now);
        return false;
    }

    private String resolveKeyByBindingOrTriggers(Entity e){
        FileConfiguration c = plugin.getConfig();
        if (c == null) return null;

        // 1) Citizens NPC id binding
        try {
            if (e.hasMetadata("NPC")) {
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(e);
                if (npc != null) {
                    String key = c.getString("npcshop.bindings." + npc.getId());
                    if (key != null && !key.trim().isEmpty()) return key.trim();
                }
            }
        } catch (Throwable ignored) {}

        // 2) Metadata override
        try {
            if (e.hasMetadata("npcshop_key")) {
                String key = e.getMetadata("npcshop_key").get(0).asString();
                if (key != null && !key.trim().isEmpty()) return key.trim();
            }
        } catch (Throwable ignored) {}

        // 3) Name/text triggers (optional)
        try {
            String nm = e.getCustomName();
            if (nm != null) {
                List<String> names = c.getStringList("npcshop.triggers.names");
                if (names != null) {
                    String lower = nm.toLowerCase();
                    for (String s : names) {
                        if (s != null && !s.isEmpty() && lower.contains(s.toLowerCase())) return s;
                    }
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private void openOnNextTick(final Player p, final String shopKey){
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                try { ShopManager.getInstance().open(p, shopKey); } catch (Throwable ignored) {}
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent e){
        if (!enabled()) return;
        Player p = e.getPlayer();
        String key = resolveKeyByBindingOrTriggers(e.getRightClicked());
        if (key == null) return;              // not ours → let others handle
        if (throttled(p)) { e.setCancelled(true); return; }
        e.setCancelled(true);
        openOnNextTick(p, key);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e){
        if (!enabled()) return;
        Player p = e.getPlayer();
        String key = resolveKeyByBindingOrTriggers(e.getRightClicked());
        if (key == null) return;
        if (throttled(p)) { e.setCancelled(true); return; }
        e.setCancelled(true);
        openOnNextTick(p, key);
    }
}
