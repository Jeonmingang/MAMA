package com.minkang.usp2.shop;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class ShopLinkWrapperCommand implements CommandExecutor {
    private final Plugin plugin;
    public ShopLinkWrapperCommand(Plugin plugin){
        this.plugin = plugin;
        if (plugin.getConfig().getString("npcshop.open_command")==null){
            plugin.getConfig().set("npcshop.open_command", "상점 열기 {key}");
            plugin.saveConfig();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)){ sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
        // 관리자 전용: OP 또는 usp.shop.link
        if (!((Player)sender).isOp() && !sender.hasPermission("usp.shop.link")){
            sender.sendMessage("§c권한이 없습니다: usp.shop.link");
            return true;
        }
        Player p = (Player)sender;
        if (Bukkit.getPluginManager().getPlugin("Citizens")==null){
            p.sendMessage("§cCitizens가 설치되어 있지 않습니다."); return true;
        }
        if (args.length<1){ usage(p); return true; }
        String sub = args[0];
        if ("연동".equalsIgnoreCase(sub)){
            if (args.length<2){ p.sendMessage("§c사용법: /상점 연동 <상점키>"); return true; }
            String key = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            String template = plugin.getConfig().getString("npcshop.open_command", "상점 열기 {key}");
            String openCmd = template.replace("{key}", key).trim();
            String full = "npc command add -r -p " + openCmd;
            boolean ok = Bukkit.dispatchCommand(p, full);
            if (ok){
                p.sendMessage("§a연동 완료: 우클릭 시 §f/" + openCmd + " §f실행");
            }else{
                p.sendMessage("§c연동 실패. /npc select 상태 및 Citizens 권한을 확인하세요.");
            }
            return true;
        }
        if ("연동목록".equalsIgnoreCase(sub)){
            Bukkit.dispatchCommand(p, "npc command");
            return true;
        }
        if ("연동해제".equalsIgnoreCase(sub)){
            if (args.length<2){ p.sendMessage("§c사용법: /상점 연동해제 <ID>"); return true; }
            boolean ok = Bukkit.dispatchCommand(p, "npc command remove " + args[1]);
            p.sendMessage(ok? "§7삭제됨: #" + args[1] : "§c삭제 실패: ID 확인");
            return true;
        }
        usage(p);
        return true;
    }

    private void usage(Player p){
        p.sendMessage("§7사용법: §f/상점 연동 <상점키> | /상점 연동목록 | /상점 연동해제 <ID>");
        p.sendMessage("§7TIP: 먼저 §f/npc select §7로 NPC를 선택하세요.");
    }
}
