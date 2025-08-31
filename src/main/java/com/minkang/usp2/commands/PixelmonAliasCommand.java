package com.minkang.usp2.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.*;

public class PixelmonAliasCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용할 수 있습니다."); return true; }
        Player p = (Player)sender;
        String joined = String.join(" ", args);
        String forward;
        if (label.equalsIgnoreCase("개체값")) {
            forward = "ivs" + (joined.isEmpty() ? " 1" : " " + joined);
        } else if (label.equalsIgnoreCase("노력치")) {
            forward = "evs" + (joined.isEmpty() ? " 1" : " " + joined);
        } else {
            forward = label + (joined.isEmpty() ? "" : " " + joined);
        }
        p.performCommand(forward);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return Arrays.asList("1","2","3","4","5","6");
        return Collections.emptyList();
    }
}
