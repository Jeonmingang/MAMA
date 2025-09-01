package com.minkang.usp2.commands;

import com.minkang.usp2.Main;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    private final Main plugin;
    public ShopCommand(Main p){ this.plugin=p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 0){
            if (s.hasPermission("usp.shop.admin") || !(s instanceof org.bukkit.entity.Player)){
                showHelp(s);
            } else {
                s.sendMessage("§c권한이 없습니다.");
            }
            return true;
        }
        // admin-only
        if (!s.hasPermission("usp.shop.admin")){
            s.sendMessage("§c권한이 없습니다.");
            return true;
        }
        // /상점 생성 <이름>
        if (a[0].equalsIgnoreCase("생성")){
            if (a.length < 2){
                s.sendMessage("§c사용법: /상점 생성 <이름>");
                return true;
            }
            String name = a[1];
            try {
                plugin.shop().createOrUpdate(name, true, true, null);
                s.sendMessage("§a상점 생성됨: §f"+name);
            } catch (Throwable ex){
                s.sendMessage("§c상점 생성 실패: "+ex.getClass().getSimpleName());
            }
            return true;
        }

        if (c.getName().equalsIgnoreCase("상점리로드")){
            plugin.shop().reload(); plugin.reloadConfig();
            s.sendMessage("§a상점 리로드 완료");
            return true;
        }
        if (a.length==0){
            s.sendMessage("§e/상점 추가 <이름> <구매|판매> [가격]");
            return true;
        }
        String sub = a[0];
        if ("추가".equalsIgnoreCase(sub)){
            if (!s.hasPermission("usp.shop.admin")){
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
        Main pl = Main.getPlugin(Main.class);
        pl.shop().createOrUpdate(name, buy, sell, price);
        sender.sendMessage("§a상점 등록/갱신: §f"+name+" §7("+mode+(price!=null? " 가격="+price:"")+")");
        return true;
    }

    private void showHelp(CommandSender s){
        String bar = "§8§m────────────────────────────────";
        s.sendMessage(bar);
        s.sendMessage("§b/상점 생성 §7<이름> §8— §f빈 상점 생성");
        s.sendMessage("§b/상점 추가 §7<이름> <구매/판매> <슬롯> <가격> §8— §f손에 든 아이템 등록");
        s.sendMessage("§b/상점 열기 §7<이름> §8— §f상점 GUI 열기");
        s.sendMessage("§b/상점 목록 §8— §f모든 상점 이름 표시");
        s.sendMessage("§b/상점 삭제 §7<이름> §8— §f상점 삭제");
        s.sendMessage("§b/상점 연동 §7<npc> <이름> §8— §fCitizens NPC에 연결");
        s.sendMessage(bar);
    }

}
