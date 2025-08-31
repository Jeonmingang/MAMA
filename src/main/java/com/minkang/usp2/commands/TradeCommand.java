package com.minkang.usp2.commands;

import com.minkang.usp2.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class TradeCommand implements CommandExecutor {
    private final Main plugin;
    public TradeCommand(Main p){ this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)){ s.sendMessage("플레이어만"); return true; }
        Player p = (Player)s;

        if (a.length == 0){
            p.sendMessage("§7/거래 <플레이어>  §8— 거래 요청");
            p.sendMessage("§7/거래 수락      §8— 들어온 요청 수락");
            p.sendMessage("§7/거래 취소      §8— 진행 중/대기 중 거래 취소");
            return true;
        }

        if ("수락".equalsIgnoreCase(a[0])){
            if (plugin.trade().accept(p)){ p.sendMessage("§a거래 수락"); }
            else p.sendMessage("§c유효한 요청이 없습니다.");
            return true;
        }
        if ("취소".equalsIgnoreCase(a[0])){
            plugin.trade().cancel(p);
            return true;
        }

        Player to = Bukkit.getPlayerExact(a[0]);
        if (to == null || to == p){ p.sendMessage("§c대상 오류"); return true; }
        plugin.trade().request(p, to);
        return true;
    }
}
