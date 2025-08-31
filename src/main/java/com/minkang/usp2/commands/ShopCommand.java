
package com.minkang.usp2.commands;

import com.minkang.usp2.Main;
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

        if ("연동".equalsIgnoreCase(a[0]) && a.length>=3){
            String shopName = a[1];
            String npcArg = a[2];
            if (plugin.getServer().getPluginManager().getPlugin("Citizens")==null){
                p.sendMessage("§cCitizens가 설치되어 있지 않습니다."); return true;
            }
            try {
                int id = Integer.parseInt(npcArg);
                plugin.shop().bindNpcToShop(id, shopName);
                p.sendMessage("§aNPC ID "+id+" ↔ 상점 '"+shopName+"' 연동");
            } catch(NumberFormatException ex){
                // find npc by name
                net.citizensnpcs.api.npc.NPC target = null;
                for (net.citizensnpcs.api.npc.NPC npc: net.citizensnpcs.api.CitizensAPI.getNPCRegistry()){
                    if (npc.getName()!=null && npc.getName().equalsIgnoreCase(npcArg)){ target = npc; break; }
                }
                if (target==null){ p.sendMessage("§cNPC를 찾지 못했습니다."); return true; }
                plugin.shop().bindNpcToShop(target.getId(), shopName);
                p.sendMessage("§aNPC '"+target.getName()+"' ↔ 상점 '"+shopName+"' 연동");
            }
            return true;
        }

        plugin.shop().open(p, a[0]); return true;
    }
}

// 연동: /상점 연동 <상점이름> <엔피시>
// Citizens가 설치되어 있으면 NPC 이름 또는 ID로 바인딩하여 클릭시 열리게 함
