package com.minkang.ultimate.commands;
import org.bukkit.Bukkit; import org.bukkit.ChatColor; import org.bukkit.Sound;
import org.bukkit.command.*; import org.bukkit.entity.Player; import org.bukkit.scheduler.BukkitRunnable;
import com.minkang.ultimate.Main; import java.util.*;
public class DrawCommand implements CommandExecutor {
    private final Main plugin; private final Random random=new Random();
    public DrawCommand(Main plugin){ this.plugin=plugin; }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(!sender.hasPermission("usp.draw")){ sender.sendMessage(ChatColor.RED+"권한이 없습니다. (usp.draw)"); return true; }
        java.util.List<Player> list=new java.util.ArrayList<Player>(); for(Player p:Bukkit.getOnlinePlayers()) list.add(p);
        if(list.isEmpty()){ sender.sendMessage(ChatColor.RED+"온라인 플레이어가 없습니다."); return true; }
        new BukkitRunnable(){ int t=3; @Override public void run(){ if(t>0){ Bukkit.broadcastMessage(ChatColor.AQUA+""+t); for(Player p:list) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING,1f,1f); t--; } else { Player w=list.get(random.nextInt(list.size())); Bukkit.broadcastMessage(ChatColor.GOLD+"추첨 결과 ▶ "+ChatColor.YELLOW+w.getName()); for(Player p:list) p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE,1f,1f); cancel(); } } }.runTaskTimer(plugin,0L,20L);
        return true;
    }
}
