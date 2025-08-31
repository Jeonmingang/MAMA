package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.lang.reflect.Method;

public class TradeCommand implements CommandExecutor {
    private final Main plugin;
    public TradeCommand(Main p){ this.plugin=p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)) { s.sendMessage("플레이어만"); return true; }
        Player p = (Player)s;
        Object mgr = plugin.trade();

        if (a.length==0) {
            p.sendMessage("§e/거래수락 | /거래취소");
            return true;
        }
        String sub = a[0];
        if ("수락".equalsIgnoreCase(sub) || "accept".equalsIgnoreCase(sub)) {
            if (tryInvoke(mgr, "accept", Player.class, p)) return true;
            p.performCommand("trade accept");
            return true;
        }
        if ("취소".equalsIgnoreCase(sub) || "cancel".equalsIgnoreCase(sub)) {
            if (tryInvoke(mgr, "cancel", Player.class, p)) return true;
            p.performCommand("trade cancel");
            return true;
        }
        p.sendMessage("§e/거래수락 | /거래취소");
        return true;
    }

    private boolean tryInvoke(Object target, String name, Class<?> argType, Player p){
        if (target==null) return false;
        try {
            Method m = target.getClass().getDeclaredMethod(name, argType);
            m.setAccessible(true);
            m.invoke(target, p);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
