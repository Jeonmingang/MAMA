package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class ShopCommand implements CommandExecutor {
    private final Main plugin;
    public ShopCommand(Main p){ this.plugin=p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (c.getName().equalsIgnoreCase("상점리로드")) {
            tryInvoke(plugin.shop(), "reload");
            plugin.reloadConfig();
            s.sendMessage("§a상점 리로드 완료");
            return true;
        }
        if (a.length==0) {
            s.sendMessage("§e/상점 추가 <이름> <구매|판매> [가격]");
            return true;
        }
        String sub = a[0];
        if ("추가".equalsIgnoreCase(sub)) {
            if (!s.hasPermission("usp.shop.admin")) {
                s.sendMessage("§c권한 없음");
                return true;
            }
            return handleAdd(s, a);
        }
        s.sendMessage("§e/상점 추가 <이름> <구매|판매> [가격]");
        return true;
    }

    private boolean handleAdd(CommandSender sender, String[] args){
        if (args.length < 3){
            sender.sendMessage("§e/상점 추가 <이름> <구매|판매> [가격]");
            return true;
        }
        String name = args[1];
        String mode = args[2];
        Double price = null;
        if (args.length >= 4){
            try { price = Double.parseDouble(args[3]); } catch (Exception ignored){}
        }
        boolean buy = "구매".equalsIgnoreCase(mode);
        boolean sell = "판매".equalsIgnoreCase(mode);
        if (!buy && !sell){
            sender.sendMessage("§c모드는 '구매' 또는 '판매'만 가능합니다.");
            return true;
        }

        Object mgr = plugin.shop();
        if (tryInvoke(mgr, "createOrUpdate", name, buy, sell, price)) {
            sender.sendMessage("§a상점 등록/갱신: §f"+name+" §7("+mode+(price!=null? " 가격="+price:"")+")");
            return true;
        }
        if (tryInvoke(mgr, "create", name, buy, sell, price)) {
            sender.sendMessage("§a상점 등록: §f"+name+" §7("+mode+(price!=null? " 가격="+price:"")+")");
            return true;
        }
        sender.sendMessage("§c상점 매니저 API를 찾을 수 없습니다.");
        return true;
    }

    private boolean tryInvoke(Object target, String method, Object...args){
        if (target==null) return false;
        try {
            // 동명이지만 시그니처가 다른 경우를 고려하여 루프 탐색
            Method m = null;
            for (Method cand : target.getClass().getDeclaredMethods()) {
                if (!cand.getName().equals(method)) continue;
                if (cand.getParameterTypes().length != args.length) continue;
                m = cand; break;
            }
            if (m==null) return false;
            m.setAccessible(true);
            m.invoke(target, args);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
