package com.minkang.ultimate.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CheckSpawnsAliasCommand implements CommandExecutor {
    private final String mode;
    public CheckSpawnsAliasCommand(String mode){ this.mode = mode; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player p = (Player) sender;

// Permission gate
if (mode.equalsIgnoreCase("legendary") && !sender.hasPermission("usp.legendarytime")) { sender.sendMessage("권한이 없습니다."); return true; }
if (mode.equalsIgnoreCase("megaboss") && !sender.hasPermission("usp.megabosstime")) { sender.sendMessage("권한이 없습니다."); return true; }

        boolean wasOp = p.isOp();
        try {
            if (!wasOp) p.setOp(true); // 무권한 보장: 잠시 OP로 실행
            // 플레이어 컨텍스트에서 실행되어야 위치/바이옴 계산이 정확함
            p.performCommand("checkspawns " + mode);
        } finally {
            if (!wasOp) p.setOp(false);
        }
        return true;
    }
}