
package com.minkang.ultimate.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.minkang.ultimate.Main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RaffleCommand implements CommandExecutor {

    private final Main plugin;
    private final Random rnd = new Random();

    public RaffleCommand(Main p){ this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        List<Player> list = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (list.isEmpty()){
            s.sendMessage(ChatColor.RED + "접속 중인 플레이어가 없습니다.");
            return true;
        }
        Collections.shuffle(list, rnd);

        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "====================");
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "🎲 추첨 시작!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "잠시 뒤 행운의 주인공이 공개됩니다...");
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "====================");

        new BukkitRunnable(){
            int n = 3;
            @Override public void run() {
                if (n > 0){
                    String msg = ChatColor.YELLOW + "" + ChatColor.BOLD + n;
                    for (Player p : Bukkit.getOnlinePlayers()){
                        p.sendTitle(msg, ChatColor.GRAY + "랜덤 추첨 진행 중...", 0, 20, 0);
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
                    }
                    n--;
                    return;
                }
                // pick winner
                Player winner = list.get(rnd.nextInt(list.size()));
                for (Player p : Bukkit.getOnlinePlayers()){
                    p.sendTitle(ChatColor.GOLD + "🥳 당첨!", ChatColor.AQUA + winner.getName() + ChatColor.GRAY + " 님 축하합니다!", 10, 50, 10);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                }
                Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "당첨자: " + ChatColor.AQUA + winner.getName());
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }
}
