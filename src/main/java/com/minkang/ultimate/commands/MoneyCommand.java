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
            showMoneyHelp(s);
            // 또한 자동으로 /balance 실행
            if (s instanceof org.bukkit.entity.Player) ((org.bukkit.entity.Player)s).performCommand("balance");
            else org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "balance");
            return true;
        }

        // 무권한 사용 가능 (permission 체크 없음)
        if (a.length==0){
            // /돈 -> /balance 위임
            if (s instanceof Player) {
                ((Player)s).performCommand("balance");
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "balance");
            }
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

    private void showMoneyHelp(CommandSender s){
        String bar = "§8§m────────────────────────────────";
        s.sendMessage(bar);
        s.sendMessage("§b/돈 §8— §f계좌 잔액(/balance)");
        s.sendMessage("§b/돈 §7보내기 <닉> <금액> §8— §f송금(/pay)");
        s.sendMessage("§b/돈 §7순위 §8— §f부자 순위(/baltop)");
        s.sendMessage(bar);
    }

}
