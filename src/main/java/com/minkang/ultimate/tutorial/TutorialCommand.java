
package com.minkang.ultimate.tutorial;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TutorialCommand implements CommandExecutor {
    private final TutorialManager manager;
    public TutorialCommand(TutorialManager m){ this.manager = m; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0){
            sender.sendMessage("§7/튜토리얼 시작 | /튜토리얼 열기 | /튜토리얼 스킵");
            return true;
        }
        String sub = args[0];
        if ("시작".equalsIgnoreCase(sub)){
            if (!(sender instanceof Player)){ sender.sendMessage("플레이어만 사용"); return true; }
            Player p = (Player)sender;
            manager.startTutorial(p);
            sender.sendMessage("§a튜토리얼을 시작합니다.");
            return true;
        }
        if ("열기".equalsIgnoreCase(sub)){
            if (!(sender instanceof Player)){ sender.sendMessage("플레이어만 사용"); return true; }
            Player p = (Player)sender;
            p.openInventory(manager.buildPromptGui());
            return true;
        }
        if ("스킵".equalsIgnoreCase(sub)){
            if (!(sender instanceof Player)){ sender.sendMessage("플레이어만 사용"); return true; }
            Player p = (Player)sender;
            manager.setSkipped(p.getUniqueId(), true);
            sender.sendMessage("§7튜토리얼을 스킵했습니다. /튜토리얼 시작 으로 다시 진행할 수 있습니다.");
            return true;
        }
        sender.sendMessage("§c알 수 없는 하위명령어.");
        return true;
    }
}
