package com.minkang.usp2.commands;

import com.minkang.usp2.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.BlockFace;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class LockCommand implements CommandExecutor {
    private final Main plugin;
    private final NamespacedKey key;
    private final NamespacedKey keyType;
    public LockCommand(Main p){ this.plugin=p; this.key=new NamespacedKey(p,"lock_token"); this.keyType=new NamespacedKey(p,"lock_token_type"); }

    private boolean hasToken(Player p, String type){
        ItemStack it = p.getInventory().getItemInMainHand();
        if (it==null || !it.hasItemMeta()) return false;
        PersistentDataContainer c = it.getItemMeta().getPersistentDataContainer();
        if (!c.has(key, PersistentDataType.INTEGER)) return false;
        String t = c.getOrDefault(keyType, PersistentDataType.STRING, "perm");
        return type.equalsIgnoreCase(t);
    }
    private boolean consume(Player p){
        ItemStack it = p.getInventory().getItemInMainHand(); if (it==null) return false;
        if (it.getAmount()<=1){ p.getInventory().setItemInMainHand(null); } else it.setAmount(it.getAmount()-1);
        return true;
    }
    private void tryPlaceSign(Player p, Block target){
        BlockFace face = p.getFacing();
        Block adj = target.getRelative(face);
        if (adj.getType()==Material.AIR){
            adj.setType(Material.OAK_WALL_SIGN);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) adj.getState();
            sign.setLine(0, "§6[LOCKED]");
            sign.setLine(1, p.getName());
            sign.update(true, false);
            try {
                org.bukkit.block.data.BlockData data = adj.getBlockData();
                if (data instanceof WallSign){
                    ((WallSign)data).setFacing(face);
                    adj.setBlockData(data);
                }
            } catch(Exception ignore){}
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)){ s.sendMessage("플레이어만"); return true; }
        Player p = (Player)s;
        if (a.length==0){ s.sendMessage("§7/잠금 시간 <분|시간> | /잠금 영구 | /잠금 추가 <닉> | /잠금 목록 | /잠금 해제"); return true; }
        if ("시간".equalsIgnoreCase(a[0]) && a.length>=2){
            if (!hasToken(p,"time")){ p.sendMessage("§c손에 시간 잠금권이 필요합니다."); return true; }
            long mins=0; try{ mins = Long.parseLong(a[1]); }catch(Exception ex){ p.sendMessage("§c숫자 입력"); return true; }
            Block b = p.getTargetBlock(null, 5);
            if (b==null){ p.sendMessage("§c대상 블록을 바라보세요."); return true; }
            plugin.lock().protectTime(b, p, mins);
            consume(p); tryPlaceSign(p,b);
            p.sendMessage("§a시간 잠금 적용: "+mins+"분");
            return true;
        }
        if ("영구".equalsIgnoreCase(a[0])){
            if (!hasToken(p,"perm")){ p.sendMessage("§c손에 영구 잠금권이 필요합니다."); return true; }
            Block b = p.getTargetBlock(null, 5);
            if (b==null){ p.sendMessage("§c대상 블록을 바라보세요."); return true; }
            plugin.lock().protectPerm(b, p);
            consume(p); tryPlaceSign(p,b);
            p.sendMessage("§a영구 잠금 적용");
            return true;
        }
        if ("추가".equalsIgnoreCase(a[0]) && a.length>=2){
            Block b = p.getTargetBlock(null, 5);
            OfflinePlayer op = Bukkit.getOfflinePlayer(a[1]);
            plugin.lock().addMember(b, op); p.sendMessage("§a공유자 추가: "+op.getName()); return true;
        }
        if ("목록".equalsIgnoreCase(a[0])){ plugin.lock().list(p); return true; }
        if ("해제".equalsIgnoreCase(a[0])){ p.sendMessage("§7잠금 블록을 파괴하면 해제됩니다."); return true; }
        return true;
    }
}
