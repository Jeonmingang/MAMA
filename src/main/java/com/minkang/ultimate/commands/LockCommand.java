
package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class LockCommand implements CommandExecutor {
    private final Main plugin;
    public LockCommand(Main p){ this.plugin=p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!(s instanceof Player)){ s.sendMessage("플레이어만"); return true; }
        Player p = (Player)s;
        if (a.length<1){
            p.sendMessage("§7/잠금 시간 <시간>  §8(바라보는 블록)");
            p.sendMessage("§7/잠금 영구  §8(바라보는 블록)");
            p.sendMessage("§7/잠금 추가 <닉>  §8(바라보는 블록)");
            p.sendMessage("§7/잠금 목록");
            p.sendMessage("§7/잠금 해제  §8(잠금 블록 파괴로도 가능)");
            return true;
        }
        if ("시간".equalsIgnoreCase(a[0]) && a.length>=2){
            Block b = p.getTargetBlock(null, 5);
            try{ long hours = Long.parseLong(a[1]); plugin.lock().setExpire(b, System.currentTimeMillis()+hours*3600000L); p.sendMessage("§a잠금 시간을 "+hours+"시간으로 설정"); }
            catch(Exception ex){ p.sendMessage("§c숫자 입력"); }
            return true;
        }
        if ("영구".equalsIgnoreCase(a[0])){
            Block b = p.getTargetBlock(null, 5);
            plugin.lock().setExpire(b, -1L); p.sendMessage("§a영구 잠금 설정"); return true;
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
