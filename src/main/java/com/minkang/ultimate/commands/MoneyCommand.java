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
        // Admin adjust: /돈 설정 주기 <플레이어> <금액>  |  /돈 설정 차감 <플레이어> <금액>
if (a.length >= 3 && "설정".equalsIgnoreCase(a[0])){
    if (!(s.isOp() || s.hasPermission("usp.money.admin"))) { s.sendMessage("권한이 없습니다."); return true; }
    String mode = a[1];
    String targetName = a[2];
    String amountStr = (a.length >= 4 ? a[3] : "0");
    double amount = 0.0;
    try { amount = Double.parseDouble(amountStr.replaceAll(",", "")); } catch (Exception e){ s.sendMessage("금액이 올바르지 않습니다."); return true; }
    if (amount <= 0){ s.sendMessage("금액은 0보다 커야 합니다."); return true; }
    Economy econ = getEcon();
    if (econ == null){ s.sendMessage("경제 플러그인이 연결되지 않았습니다."); return true; }
    OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
    if (target == null){ s.sendMessage("대상 플레이어를 찾을 수 없습니다."); return true; }

    if ("주기".equalsIgnoreCase(mode)){
        try { econ.depositPlayer(target, amount); } catch(Exception ignored){}
        s.sendMessage("§6[경제] §f" + targetName + " 님에게 §e+" + String.format("%,.2f", amount) + "§f 지급");
        return true;
    }
    if ("차감".equalsIgnoreCase(mode)){
        try { econ.withdrawPlayer(target, amount); } catch(Exception ignored){}
        s.sendMessage("§6[경제] §f" + targetName + " 님에게서 §c-" + String.format("%,.2f", amount) + "§f 차감");
        return true;
    }
    s.sendMessage("§7사용법: /돈 설정 주기 <플레이어> <금액> | /돈 설정 차감 <플레이어> <금액>");
    return true;
}

        s.sendMessage("§7/돈, /돈 보내기 <닉> <금액>, /돈 순위, /돈 설정 주기 <닉> <금액>, /돈 설정 차감 <닉> <금액>");
        return true;
    }
}
