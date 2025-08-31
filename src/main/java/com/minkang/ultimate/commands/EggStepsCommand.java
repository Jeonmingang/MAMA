
package com.minkang.ultimate.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class EggStepsCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만"); return true; }
        Player p = (Player)sender;
        if (args.length<1){ p.sendMessage(ChatColor.YELLOW+"사용법: /알걸음 <1~6>"); return true; }
        try {
            int slot = Integer.parseInt(args[0]);
            if (slot<1 || slot>6){ p.sendMessage("§c1~6 사이"); return true; }
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "eggsteps " + p.getName() + " " + slot) ||
            org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), "pixelmon:eggsteps " + p.getName() + " " + slot) ||
            p.performCommand("eggsteps " + slot);
        } catch (NumberFormatException ex){
            p.sendMessage("§c숫자 입력");
        }
        return true;
    }
}
