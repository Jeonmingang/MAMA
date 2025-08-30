
package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
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
        if (a.length == 0){ p.sendMessage("§7/거래 <플레이어> | /거래 수락 <플레이어> | /거래 거절 <플레이어>"); return true; }
        if ("수락".equalsIgnoreCase(a[0]) && a.length>=2){
            Player from = Bukkit.getPlayerExact(a[1]);
            if (from==null){ p.sendMessage("§c대상이 오프라인"); return true; }
            if (plugin.trade().accept(p, from)) { p.sendMessage("§a거래 수락"); from.sendMessage("§a거래 수락됨"); }
            else p.sendMessage("§c유효한 요청이 없습니다.");
            return true;
        }
        if ("거절".equalsIgnoreCase(a[0]) && a.length>=2){
            p.sendMessage("§7거절되었습니다.");
            return true;
        }
        Player to = Bukkit.getPlayerExact(a[0]);
        if (to==null || to==p){ p.sendMessage("§c대상 오류"); return true; }
        plugin.trade().request(p, to);
        return true;
    }
}
