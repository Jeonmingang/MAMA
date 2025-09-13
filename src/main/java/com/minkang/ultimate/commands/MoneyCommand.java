package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {
    private final Main plugin;
    public MoneyCommand(Main p){ this.plugin = p; }

    private Economy getEcon(){
        try{
            return Bukkit.getServer().getServicesManager().load(Economy.class);
        }catch(Throwable t){ return null; }
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length==0){
            // /돈 -> 안전한 Vault 조회
            if (!(s instanceof Player)){
                s.sendMessage("콘솔은 /baltop 또는 /eco 명령을 사용하세요.");
                return true;
            }
            Economy econ = getEcon();
            if (econ == null){
                s.sendMessage("경제 플러그인이 연결되지 않았습니다.");
                return true;
            }
            Player p = (Player)s;
            double bal = 0.0;
            try { bal = econ.getBalance((OfflinePlayer)p); } catch (Exception ignored){}
            s.sendMessage("§6[경제] §f현재 잔액: " + String.format("%,.2f", bal));
            return true;
        }

        if ("보내기".equalsIgnoreCase(a[0]) && a.length>=3){
            if (!(s instanceof Player)) { s.sendMessage("플레이어만"); return true; }
            // Essentials /pay 위임
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
