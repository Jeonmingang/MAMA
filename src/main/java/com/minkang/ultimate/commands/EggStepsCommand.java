package com.minkang.ultimate.commands;



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

        if (args.length != 1) {
            p.sendMessage(ChatColor.YELLOW + "사용법: /알걸음 <1~6>");
            return true;
        }
        int slot;
        try {
            slot = Integer.parseInt(args[0]);
        } catch (Exception ex) {
            p.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)");
            return true;
        }
        if (slot < 1 || slot > 6) {
            p.sendMessage(ChatColor.RED + "슬롯은 1~6 입니다.");
            return true;
        }

        // Execute as requested: /eggesteps <player> <slot>, with fallback to /eggsteps
        boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eggesteps " + p.getName() + " " + slot);
        if (!ok) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eggsteps " + p.getName() + " " + slot);
        }
        return true;
    }
}
