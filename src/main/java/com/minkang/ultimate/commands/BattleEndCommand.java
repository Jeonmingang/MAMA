
package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BattleEndCommand implements CommandExecutor {
    private final Main plugin;
    public BattleEndCommand(Main plugin) { this.plugin = plugin; }
    @Override public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        s.sendMessage("배틀 종료 처리(더미).");
        return true;
    }
}
