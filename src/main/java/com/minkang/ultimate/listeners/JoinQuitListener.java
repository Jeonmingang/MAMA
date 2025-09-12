
package com.minkang.ultimate.listeners;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {

    private final Main plugin;

    public JoinQuitListener(Main plugin){
        this.plugin = plugin;
    }

    private String colorize(String s){
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        if (!plugin.getConfig().getBoolean("joinquit.enabled", true)) return;
        e.setJoinMessage(null);
        int online = Bukkit.getOnlinePlayers().size();
        String fmt = plugin.getConfig().getString("joinquit.join",
                "&a[+] &f%player% &7님이 서버에 접속했습니다! &7[ &f%online% &7]");
        String msg = fmt.replace("%player%", e.getPlayer().getName())
                        .replace("%online%", String.valueOf(online));
        Bukkit.getServer().broadcastMessage(colorize(msg));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e){
        if (!plugin.getConfig().getBoolean("joinquit.enabled", true)) return;
        e.setQuitMessage(null);
        int online = Math.max(0, Bukkit.getOnlinePlayers().size() - 1);
        String fmt = plugin.getConfig().getString("joinquit.quit",
                "&c[-] &f%player% &7님이 서버를 나가셨습니다! &7[ &f%online% &7]");
        String msg = fmt.replace("%player%", e.getPlayer().getName())
                        .replace("%online%", String.valueOf(online));
        Bukkit.getServer().broadcastMessage(colorize(msg));
    }
}
