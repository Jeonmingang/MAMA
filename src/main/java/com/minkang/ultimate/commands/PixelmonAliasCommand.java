package com.minkang.ultimate.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PixelmonAliasCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player p = (Player) sender;
        String joined = String.join(" ", args);
        String forward;
        if (label.equalsIgnoreCase("개체값")) {
            forward = "ivs" + (joined.isEmpty() ? "" : " " + joined);
        } else if (label.equalsIgnoreCase("노력치")) {
            forward = "evs" + (joined.isEmpty() ? "" : " " + joined);
        } else {
            forward = label + (joined.isEmpty() ? "" : " " + joined);
        }
        p.performCommand(forward);
        return true;
    }
}
