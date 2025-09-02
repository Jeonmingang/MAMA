package com.minkang.usp2.commands;

import com.minkang.usp2.Main;
import org.bukkit.command.*; import org.bukkit.entity.Player;

public class LockTokenCommand implements CommandExecutor {
    private final Main plugin;
    public LockTokenCommand(Main p){ this.plugin=p; }
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)){ s.sendMessage("플레이어만"); return true; }
        if (a.length<1 || !"발급".equalsIgnoreCase(a[0])){ s.sendMessage("§7/잠금권 발급 <수량> <영구|시간>"); return true; }
        int qty = 1; if (a.length>=2) try{ qty=Integer.parseInt(a[1]); }catch(Exception ignored){}
        String type = "perm"; if (a.length>=3){ String t=a[2].toLowerCase(); if (t.startsWith("영")) type="perm"; else if (t.startsWith("시")) type="time"; }
        Player p = (Player)s;
        p.getInventory().addItem(plugin.lock().createToken(qty, type));
        p.sendMessage("§a잠금권("+("perm".equals(type)?"영구":"시간")+") x"+qty+" 발급");
        return true;
    }
}