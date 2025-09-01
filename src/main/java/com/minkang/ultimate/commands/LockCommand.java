package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LockCommand implements CommandExecutor {

    private void sendHelp(CommandSender s){
        s.sendMessage("§8====================");
        s.sendMessage("§6§l잠금 명령어");
        s.sendMessage("§7  /잠금 시간 <분>");
        s.sendMessage("§7  /잠금 영구");
        s.sendMessage("§7  /잠금 추가 <플레이어>");
        s.sendMessage("§7  /잠금 목록");
        s.sendMessage("§7  /잠금 해제");
        s.sendMessage("§8====================");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용할 수 있습니다."); return true; }
        Player p = (Player) sender;
        Main plugin = Main.getPlugin(Main.class);

        if (args.length == 0) { sendHelp(sender); return true; }

        String sub = args[0];

        if ("시간".equalsIgnoreCase(sub)) {
            if (args.length < 2) { sender.sendMessage("§c사용법: /잠금 시간 <분>"); return true; }
            int minutes;
            try { minutes = Integer.parseInt(args[1]); } catch (Exception e) { sender.sendMessage("§c정수 분을 입력하세요."); return true; }
            Block b = p.getTargetBlock(null, 5);
            if (b == null) { sender.sendMessage("§c바라보는 블록이 없습니다."); return true; }
            plugin.lock().protectForMinutes(b, p, minutes);
            sender.sendMessage("§a" + minutes + "분 잠금 적용");
            return true;
        }

        if ("영구".equalsIgnoreCase(sub)) {
            Block b = p.getTargetBlock(null, 5);
            if (b == null) { sender.sendMessage("§c바라보는 블록이 없습니다."); return true; }
            plugin.lock().protectPerm(b, p);
            sender.sendMessage("§a영구 잠금 적용");
            return true;
        }

        if ("추가".equalsIgnoreCase(sub)) {
            if (args.length < 2) { sender.sendMessage("§c사용법: /잠금 추가 <플레이어>"); return true; }
            Block b = p.getTargetBlock(null, 5);
            if (b == null) { sender.sendMessage("§c바라보는 블록이 없습니다."); return true; }
            OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
            plugin.lock().addMember(b, op);
            sender.sendMessage("§a공유자 추가: " + op.getName());
            return true;
        }

        if ("목록".equalsIgnoreCase(sub)) {
            plugin.lock().list(p);
            return true;
        }

        if ("해제".equalsIgnoreCase(sub)) {
            sender.sendMessage("§7잠금 블록을 파괴하면 해제됩니다.");
            return true;
        }

        sendHelp(sender);
        return true;
    }
}
