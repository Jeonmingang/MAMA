package com.minkang.ultimate.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "/스탯 보기|이동 <1~6>");
            return true;
        }

        String sub = args[0];
        if ("보기".equalsIgnoreCase(sub)) {
            // 픽셀몬 스탯 보기 위임
            tryPixelmonCommand(player, "showspecs", 0);
            return true;
        }
        if ("이동".equalsIgnoreCase(sub)) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)");
                return true;
            }
            try {
                int slot = Integer.parseInt(args[1]);
                if (slot < 1 || slot > 6) {
                    player.sendMessage(ChatColor.RED + "1~6 사이 숫자!");
                    return true;
                }
                tryPixelmonCommand(player, "switch", slot);
            } catch (NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)");
            }
            return true;
        }
        return true;
    }

    private void tryPixelmonCommand(Player player, String base, int slot){
        String pName = player.getName();
        // 1) 콘솔로 시도
        boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), base + " " + pName + (slot>0? " "+slot : ""));
        // 2) 네임스페이스가 붙은 커맨드도 시도
        if (!ok) ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pixelmon:" + base + " " + pName + (slot>0? " "+slot : ""));
        // 3) 마지막으로 플레이어 권한에서 직접 시도
        if (!ok) player.performCommand(base + (slot>0? " "+slot : ""));
    }
}
