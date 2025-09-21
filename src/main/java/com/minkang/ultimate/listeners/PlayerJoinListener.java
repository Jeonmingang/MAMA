package com.minkang.ultimate.listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class PlayerJoinListener implements Listener {
    private final NamespacedKey KEY;
    public PlayerJoinListener(){
        Plugin pl = Bukkit.getPluginManager().getPlugin("UltimateServerPlugin2");
        this.KEY = new NamespacedKey(pl, "yatu");
    }
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getPersistentDataContainer().has(KEY, PersistentDataType.BYTE)) {
            event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
        } else {
            event.getPlayer().removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }
}
