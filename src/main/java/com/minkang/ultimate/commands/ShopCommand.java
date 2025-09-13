package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.managers.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Set;

public class ShopCommand implements CommandExecutor {
    private final Main plugin;
    public ShopCommand(Main plugin){ this.plugin = plugin; }

    private boolean hasPerm(CommandSender s, String perm){
        if (s.hasPermission(perm)) return true;
        s.sendMessage(ChatColor.RED + "권한이 없습니다. (" + perm + ")");
        return false;
    }
    private void sendHelp(CommandSender s){
        s.sendMessage(ChatColor.DARK_GRAY + "===== §f상점 도움말 §8=====");
        s.sendMessage(ChatColor.GRAY + " /상점 열기 <이름>  §7← 상점 열기 (OP)");
        s.sendMessage(ChatColor.GRAY + " /상점 목록        §7← 상점 키 목록");
        s.sendMessage(ChatColor.GRAY + " /상점 생성 <이름> §7← 상점 생성 (OP)");
        s.sendMessage(ChatColor.DARK_GRAY + "=========================");
        s.sendMessage(ChatColor.GRAY + "※ NPC 우클릭은 /우클릭열기 <상점키> 로 연결됨");
    }
    private void listShops(CommandSender s){
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("shops");
        if (sec == null){ s.sendMessage(ChatColor.YELLOW + "등록된 상점이 없습니다."); return; }
        Set<String> keys = sec.getKeys(false);
        if (keys.isEmpty()){ s.sendMessage(ChatColor.YELLOW + "등록된 상점이 없습니다."); return; }
        s.sendMessage(ChatColor.GREEN + "상점 목록: " + ChatColor.WHITE + String.join(", ", keys));
    }
    private void openGui(Player p, String name){
        try { ShopManager.getInstance().open(p, name); }
        catch (Throwable t){ p.sendMessage(ChatColor.RED + "상점을 열 수 없습니다."); }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }
        String sub = args[0];

        if ("목록".equalsIgnoreCase(sub)) { listShops(sender); return true; }

        if ("열기".equalsIgnoreCase(sub)) {
            if (!(sender instanceof Player)) { sender.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
            if (!hasPerm(sender, "usp.shop.open")) return true;
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 열기 <이름>"); return true; }
            openGui((Player)sender, args[1]);
            return true;
        }

        if ("생성".equalsIgnoreCase(sub)) {
            if (!hasPerm(sender, "usp.shop.admin")) return true;
            if (args.length < 2) { sender.sendMessage("§c사용법: /상점 생성 <이름>"); return true; }
            String name = args[1];
            FileConfiguration cfg = plugin.getConfig();
            String base = "shops."+name+".";
            if (!cfg.isConfigurationSection("shops."+name)) cfg.createSection("shops."+name);
            if (cfg.getInt(base+"rows", 0) <= 0) cfg.set(base+"rows", 6);
            if (cfg.getConfigurationSection(base+"items") == null) cfg.createSection(base+"items");
            plugin.saveConfig();
            sender.sendMessage("§a상점 생성됨: §f" + name + " §7(기본 6행)");
            return true;
        }

        sendHelp(sender);
        return true;
    }
}
