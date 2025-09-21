package com.minkang.ultimate.commands;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.minkang.ultimate.managers.ShopManager;
public class RightOpenCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length < 1) {
            p.sendMessage(ChatColor.YELLOW + "사용법: /우클릭상점 <상점키>");
            return true;
        }
        String key = args[0];
        try {
            ShopManager.getInstance().open(p, key);
        } catch (Throwable t) {
            p.sendMessage(ChatColor.RED + "상점을 열 수 없습니다.");
        }
        return true;
    }
}
