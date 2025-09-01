package com.minkang.usp2.commands;

import com.minkang.usp2.Main;
import org.bukkit.command.*; import org.bukkit.entity.Player;

public class ChequeCommand implements CommandExecutor {
    private final Main plugin;
    public ChequeCommand(Main p){ this.plugin = p; }
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)){ s.sendMessage("플레이어만"); return true; }
        if (a.length < 1){ s.sendMessage("§7/수표 <금액> [수량]"); return true; }
        long amount; int qty = 1;
        try { amount = Long.parseLong(a[0]); if (a.length>=2) qty = Integer.parseInt(a[1]); }
        catch(Exception ex){ s.sendMessage("§c숫자를 입력하세요."); return true; }
        if (amount<=0 || qty<=0){ s.sendMessage("§c0보다 큰 값을 입력하세요."); return true; }
        Player p = (Player)s;
        double total = amount * (double)qty;
        if (!plugin.eco().withdraw(p, total)){
            s.sendMessage("§c잔액이 부족합니다. 필요: §f$"+((long)total)); return true;
        }
        p.getInventory().addItem(plugin.bank().create(amount, qty));
        p.sendMessage("§a수표 발급: §f$"+amount+" x"+qty+" §7(총 §f$"+((long)total)+"§7 차감)");
        return true;
    }
}
