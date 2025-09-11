package com.minkang.usp2.commands;

import com.minkang.usp2.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TradeCommand implements CommandExecutor {

    private final Main plugin;
    public TradeCommand(Main plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)){
            sender.sendMessage("§c플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 0){
            p.sendMessage("§e/거래 요청 <닉> §7| §e/거래 수락 §7| §e/거래 취소");
            return true;
        }
        String sub = args[0];
        if (sub.equalsIgnoreCase("요청") || sub.equalsIgnoreCase("request")){
            if (args.length < 2){ p.sendMessage("§c사용법: /거래 요청 <닉네임>"); return true; }
            Player to = Bukkit.getPlayerExact(args[1]);
            if (to == null){ p.sendMessage("§c해당 플레이어를 찾을 수 없습니다."); return true; }
            plugin.trade().request(p, to);
            return true;
        }
        if (sub.equalsIgnoreCase("수락") || sub.equalsIgnoreCase("accept")){
            plugin.trade().accept(p); // void 메서드 (내부에서 메시지 처리)
            return true;
        }
        if (sub.equalsIgnoreCase("취소") || sub.equalsIgnoreCase("cancel")){
            plugin.trade().cancel(p);
            return true;
        }
        p.sendMessage("§e/거래 요청 <닉> §7| §e/거래 수락 §7| §e/거래 취소");
        return true;
    }
}