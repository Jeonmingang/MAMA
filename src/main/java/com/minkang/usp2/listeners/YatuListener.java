package com.minkang.usp2.listeners;

import com.minkang.usp2.commands.YatuCommand;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class YatuListener implements Listener {
    private final JavaPlugin plugin;
    private final NamespacedKey KEY;
    public YatuListener(JavaPlugin plugin, NamespacedKey key){ this.plugin=plugin; this.KEY=key; }

    @EventHandler public void onJoin(PlayerJoinEvent e){ reapplyLater(e.getPlayer()); }
    @EventHandler public void onRespawn(PlayerRespawnEvent e){ Bukkit.getScheduler().runTaskLater(plugin, ()-> reapply(e.getPlayer()), 2L); }
    @EventHandler public void onWorldChange(PlayerChangedWorldEvent e){ reapplyLater(e.getPlayer()); }

    private void reapplyLater(Player p){ Bukkit.getScheduler().runTask(plugin, ()-> reapply(p)); }
    private void reapply(Player p){
        Byte b = p.getPersistentDataContainer().get(KEY, PersistentDataType.BYTE);
        if (b!=null && b==(byte)1) YatuCommand.applyNV(p);
    }
}
