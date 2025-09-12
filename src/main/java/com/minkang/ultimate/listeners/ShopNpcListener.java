package com.minkang.ultimate.listeners;

import com.minkang.ultimate.Main;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NPC 우클릭 시 우리 플러그인 상점만 열어주는 리스너.
 * - 매핑이 존재하는 NPC에만 동작함 (다른 플러그인 커맨드에 영향 X)
 * - Citizens가 없을 경우에도 안전하게 무시
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
        // Citizens NPC id 기반 매핑: npcshop.bindings.<npcId>: <shopKey>
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
        // 우리 플러그인 내부 커맨드로 오픈 (플레이어 권한 필요 없음: Citizens에서 OP 또는 콘솔 실행 권장)
        try {
            Bukkit.dispatchCommand(p, "상점 열기 " + key);
        }catch (Throwable ignored){}
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent e){
        if (!isEnabled()) return;
        Player p = e.getPlayer();
        String key = boundKey(e.getRightClicked());
        if (key == null) return;            // 바인딩 없으면 다른 플러그인에게 맡김
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
