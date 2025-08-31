package com.minkang.ultimate.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player player = (Player) sender;

        // 어떤 명령인지 식별: 개체값 → ivs, 노력치 → evs
        String base = "ivs";
        String name = cmd.getName();
        if ("노력치".equalsIgnoreCase(name)) base = "evs";
        else if ("개체값".equalsIgnoreCase(name)) base = "ivs";
        // 혹시 label로 들어와도 방어
        else if ("evs".equalsIgnoreCase(label)) base = "evs";
        else if ("ivs".equalsIgnoreCase(label)) base = "ivs";

        if (args.length < 1) {
            player.sendMessage(ChatColor.YELLOW + "사용법: /" + name + " <1~6>");
            return true;
        }

        try {
            int slot = Integer.parseInt(args[0]);
            if (slot < 1 || slot > 6) {
                player.sendMessage(ChatColor.RED + "슬롯은 1~6 입니다.");
                return true;
            }
            tryPixelmonCommand(player, base, slot);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)");
        }
        return true;
    }

    private void tryPixelmonCommand(Player player, String base, int slot){
        String pName = player.getName();
        // 1) 콘솔로 시도
        boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), base + " " + pName + " " + slot);
        // 2) 네임스페이스가 붙은 커맨드도 시도
        if (!ok) ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pixelmon:" + base + " " + pName + " " + slot);
        // 3) 마지막으로 플레이어 권한에서 직접 시도
        if (!ok) player.performCommand(base + " " + slot);
    }
}
