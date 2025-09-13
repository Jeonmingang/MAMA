package com.minkang.usp2.shop;

import com.minkang.usp2.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public final class ShopIntegrationManager {
    private final Main plugin;

    public ShopIntegrationManager(Main plugin){
        this.plugin = plugin;
    }

    /** Reload shop integration (existing method from previous patch). */
    public void reload(CommandSender who){
        try { plugin.reloadConfig(); } catch (Throwable ignored) {}
        try {
            FileConfiguration c = plugin.getConfig();
            if (c != null && c.isList("shop.reload-commands")){
                List<String> cmds = c.getStringList("shop.reload-commands");
                for (String cmd : cmds){
                    if (cmd == null || cmd.trim().isEmpty()) continue;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        } catch (Throwable ex){
            if (who != null) who.sendMessage("§c상점 연동 리로드 중 오류: " + ex.getMessage());
        }
        if (who != null) who.sendMessage("§a상점 연동 리로드 완료");
    }

    /** Open shop for player without requiring player permissions by running configured console commands. */
    public void openFor(Player p){
        FileConfiguration c = plugin.getConfig();
        boolean ok = false;
        if (c != null && c.isList("shop.open-commands")){
            List<String> cmds = c.getStringList("shop.open-commands");
            for (String raw : cmds){
                if (raw == null || raw.trim().isEmpty()) continue;
                String cmd = raw.replace("{player}", p.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                ok = true;
            }
        }
        if (!ok){
            // Fallback: try running a generic shop open as console targeting player, if plugin supports it via selector
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "shop open " + p.getName());
        }
    }
}