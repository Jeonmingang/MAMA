package com.minkang.ultimate.listeners;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.lang.reflect.Method;

public class TradeAliasListener implements Listener {
    private final Main plugin;
    public TradeAliasListener(Main plugin){ this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPreprocess(PlayerCommandPreprocessEvent e){
        String msg = e.getMessage(); // includes leading '/'
        if (!msg.startsWith("/거래 ")) return;
        String[] parts = msg.substring(1).split(" ", 3); // [거래, sub, rest]
        if (parts.length < 2) return;
        String sub = parts[1];

        Player p = e.getPlayer();

        if ("신청".equalsIgnoreCase(sub)){
            if (parts.length < 3){ p.sendMessage("§c사용법: /거래 신청 <닉>"); e.setCancelled(true); return; }
            String target = parts[2];
            // 원래 커맨드로 리라이트: "/거래 <닉>"
            e.setMessage("/거래 " + target);
            return;
        }

        if ("수락".equalsIgnoreCase(sub)){
            e.setCancelled(true);
            // 1) 원래 명령 지원 시 통과
            boolean ok = Bukkit.dispatchCommand(p, "거래 수락");
            if (ok) return;
            // 2) TradeManager 리플렉션 호출 시도
            tryCallTrade(plugin, "accept", p);
            return;
        }

        if ("거절".equalsIgnoreCase(sub)){
            e.setCancelled(true);
            boolean ok = Bukkit.dispatchCommand(p, "거래 거절");
            if (ok) return;
            tryCallTrade(plugin, "deny", p);
            return;
        }
    }

    private void tryCallTrade(Main plugin, String method, Player p){
        try {
            Method getter = plugin.getClass().getMethod("trade");
            Object tm = getter.invoke(plugin);
            Class<?> cl = tm.getClass();
            // 여러 시그니처 시도
            Method m = null;
            try { m = cl.getMethod(method, Player.class); }
            catch (NoSuchMethodException ex){
                try { m = cl.getMethod(method); } catch (NoSuchMethodException ex2) { /* ignore */ }
            }
            if (m != null){
                if (m.getParameterTypes().length == 1) m.invoke(tm, p); else m.invoke(tm);
                p.sendMessage("§a거래 " + ("accept".equals(method) ? "수락" : "거절") + " 처리됨");
            } else {
                p.sendMessage("§7거래 시스템이 해당 명령을 직접 지원하지 않습니다. GUI에서 수락/거절을 눌러주세요.");
            }
        } catch (Throwable t){
            p.sendMessage("§7거래 시스템에 접근할 수 없습니다. GUI에서 처리해주세요.");
        }
    }
}
