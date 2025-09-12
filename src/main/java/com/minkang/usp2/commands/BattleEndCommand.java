
package com.minkang.usp2.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BattleEndCommand implements CommandExecutor {
    public BattleEndCommand(com.minkang.usp2.Main main) {
        // Main reference available if needed
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        Bukkit.dispatchCommand(s, "endbattle");
        return true;
    }
}
