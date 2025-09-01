package com.minkang.usp2.commands;

import com.minkang.usp2.Main;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.CitizensAPI;

public class ShopCommand implements CommandExecutor {
    private NPC getLookedNPC(Player p, double max){
        try {
            if (p == null) return null;
            if (p.getServer().getPluginManager().getPlugin("Citizens")==null) return null;
            RayTraceResult rt = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), max, e -> e != p);
            if (rt != null && rt.getHitEntity() != null){
                NPC npc = CitizensAPI.getNPCRegistry().getNPC(rt.getHitEntity());
                if (npc != null && npc.isSpawned()) return npc;
            }
        } catch (Throwable ignored){}
        return null;
    }
    
    private final Main plugin;
    public ShopCommand(Main p){ this.plugin=p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        // Citizens look-bind subcommands
        if (a.length >= 1 && (a[0].equalsIgnoreCase("연동") || a[0].equalsIgnoreCase("연동해제") || a[0].equalsIgnoreCase("연동목록"))) {
            if (!(s instanceof Player)) { s.sendMessage("§c플레이어만 사용 가능합니다."); return true; }
            Player p = (Player)s;
            if (c.getName().equalsIgnoreCase("상점")) {
                if (p.getServer().getPluginManager().getPlugin("Citizens")==null) { p.sendMessage("§cCitizens가 설치되어 있지 않습니다."); return true; }
                if (a[0].equalsIgnoreCase("연동")) {
                    if (a.length < 2) { p.sendMessage("§c사용법: /상점 연동 <상점키>"); return true; }
                    String key = String.join(" ", java.util.Arrays.copyOfRange(a, 1, a.length));
                    NPC npc = getLookedNPC(p, 8.0);
                    if (npc == null) { p.sendMessage("§c바라보는 NPC를 찾을 수 없습니다. 가까이서 다시 시도하세요."); return true; }
                    com.minkang.usp2.Main.getPlugin(com.minkang.usp2.Main.class).shop().setNpcBinding(npc.getId(), key);
                    p.sendMessage("§a연동 완료: NPC#" + npc.getId() + " → " + key);
                    return true;
                } else if (a[0].equalsIgnoreCase("연동해제")) {
                    NPC npc = getLookedNPC(p, 8.0);
                    if (npc == null) { p.sendMessage("§c바라보는 NPC를 찾을 수 없습니다."); return true; }
                    com.minkang.usp2.Main.getPlugin(com.minkang.usp2.Main.class).shop().setNpcBinding(npc.getId(), null);
                    p.sendMessage("§7연동 해제: NPC#" + npc.getId());
                    return true;
                } else if (a[0].equalsIgnoreCase("연동목록")) {
                    p.sendMessage("§7연동목록은 config.yml의 npcshop.bindings 섹션을 확인하세요.");
                    return true;
                }
            }
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
}
