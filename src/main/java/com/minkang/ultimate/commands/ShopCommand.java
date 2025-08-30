
package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    private final Main plugin;
    public ShopCommand(Main p){ this.plugin=p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (c.getName().equalsIgnoreCase("상점리로드")){
            plugin.shop().reload(); plugin.reloadConfig();
            s.sendMessage("§a상점/설정 리로드 완료"); return true;
        }
        if (!(s instanceof Player)){ s.sendMessage("플레이어만"); return true; }
        Player p = (Player)s;

        if (a.length<1){
            p.sendMessage("§7/상점 생성 <이름>");
            p.sendMessage("§7/상점 추가 <이름> <슬롯> <가격>  §8(손에 든 아이템/수량 등록)");
            p.sendMessage("§7/상점 삭제 <이름> <슬롯>");
            p.sendMessage("§7/상점 열기 <이름>");
            p.sendMessage("§7/상점 목록");
            return true;
        }

        if ("생성".equalsIgnoreCase(a[0]) && a.length>=2){ plugin.shop().createShop(a[1]); p.sendMessage("§a상점 생성: "+a[1]); return true; }
        if ("추가".equalsIgnoreCase(a[0]) && a.length>=4){ 
            try{
                int slot = Integer.parseInt(a[2]); double price = Double.parseDouble(a[3]);
                plugin.shop().addItem(p, a[1], slot, price);
            }catch(Exception ex){ p.sendMessage("§c숫자 확인"); }
            return true;
        }
        if ("삭제".equalsIgnoreCase(a[0]) && a.length>=3){ 
            try{ int slot=Integer.parseInt(a[2]); plugin.shop().removeItem(a[1], slot); p.sendMessage("§7삭제됨"); }
            catch(Exception ex){ p.sendMessage("§c숫자 확인"); }
            return true;
        }
        if ("열기".equalsIgnoreCase(a[0]) && a.length>=2){ plugin.shop().open(p, a[1]); return true; }
        if ("목록".equalsIgnoreCase(a[0])){ plugin.shop().list(p); return true; }

        plugin.shop().open(p, a[0]); return true;
    }
}
