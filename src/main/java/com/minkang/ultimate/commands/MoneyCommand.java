
package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {
    private final Main plugin;
    public MoneyCommand(Main p){ this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        
if (a.length==0){
    if (s instanceof Player){
        Player p = (Player)s;
        Double bal = plugin.eco().bal(p);
        if (bal != null){
            p.sendMessage("§e잔액: §f$" + ((long) Math.floor(bal)));
        } else {
            // fallback to /balance if Essentials present
            p.performCommand("balance");
        }
    } else {
        s.sendMessage("콘솔은 플레이어 잔액이 없습니다.");
    }
    return true;
}

            else s.sendMessage("/돈 순위 | /돈 보내기 <닉> <금액>");
            return true;
        }
        if ("보내기".equalsIgnoreCase(a[0]) && a.length>=3){
            if (!(s instanceof Player)) { s.sendMessage("플레이어만"); return true; }
            ((Player)s).performCommand("pay " + a[1] + " " + a[2]);
            return true;
        }
        if ("순위".equalsIgnoreCase(a[0])){
            if (s instanceof Player) ((Player)s).performCommand("baltop");
            else s.sendMessage("콘솔은 /baltop 사용");
            return true;
        }
        s.sendMessage("§7/돈, /돈 보내기 <닉> <금액>, /돈 순위");
        return true;
    }
}
