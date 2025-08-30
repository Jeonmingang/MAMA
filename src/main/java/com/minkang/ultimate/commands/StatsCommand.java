
package com.minkang.ultimate.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length < 1) {
            player.sendMessage(ChatColor.YELLOW + "사용법: /개체값 <1~6>, /노력치 <1~6>");
            return true;
        }
        try {
            int slot = Integer.parseInt(args[0]);
            if (slot < 1 || slot > 6) {
                player.sendMessage(ChatColor.RED + "포켓몬 슬롯은 1~6 사이여야 합니다.");
                return true;
            }
            if (label.equalsIgnoreCase("개체값")) player.performCommand("ivs " + slot);
            else if (label.equalsIgnoreCase("노력치")) player.performCommand("evs " + slot);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)");
        }
        return true;
    }
}
