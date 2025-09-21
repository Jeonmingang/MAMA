package com.minkang.usp2.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EggStepsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player p = (Player) sender;

        // /알걸음 <숫자>  (자기 자신)
        if (args.length == 1) {
            try {
                int slot = Integer.parseInt(args[0]);
                if (slot < 1 || slot > 6) { p.sendMessage(ChatColor.RED + "슬롯은 1~6 입니다."); return true; }
                boolean ok = p.performCommand("eggsteps " + slot);
                if (!ok) ok = p.performCommand("pixelmon:eggsteps " + slot);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)");
            }
            return true;
        }

        // /알걸음 <플레이어> <숫자>  (대상 지정)
        if (args.length == 2) {
            String targetName = args[0];
            int slot;
            try { slot = Integer.parseInt(args[1]); }
            catch (Exception ex) { p.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)"); return true; }
            if (slot < 1 || slot > 6) { p.sendMessage(ChatColor.RED + "슬롯은 1~6 입니다."); return true; }

            boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eggsteps " + targetName + " " + slot);
            if (!ok) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pixelmon:eggsteps " + targetName + " " + slot); }
            return true;
        }

        p.sendMessage(ChatColor.YELLOW + "사용법: /알걸음 <플레이어> <1~6> 또는 /알걸음 <1~6>");
        return true;
    }
}
