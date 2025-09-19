package com.minkang.ultimate.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HealAliasCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용할 수 있습니다."); return true; }
        Player p = (Player)sender;
        // Forward to Pixelmon's pokeheal (no args)
        p.performCommand("pokeheal");
        return true;
    }
}
