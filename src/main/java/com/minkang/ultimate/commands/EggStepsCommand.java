package com.minkang.ultimate.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EggStepsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "플레이어만 사용할 수 있습니다.");
            return true;
        }
        Player p = (Player) sender;
        if (args.length == 1) {
            try {
                int slot = Integer.parseInt(args[0]);
                if (slot < 1 || slot > 6) { p.sendMessage(ChatColor.RED + "슬롯은 1~6 입니다."); return true; }
                tryEggsteps(p, slot);
            } catch (NumberFormatException e) {
                p.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)");
            }
            return true;
        }
        if (args.length == 2) {
            String targetName = args[0];
            int slot;
            try { slot = Integer.parseInt(args[1]); } catch (Exception ex) { p.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)"); return true; }
            if (slot < 1 || slot > 6) { p.sendMessage(ChatColor.RED + "슬롯은 1~6 입니다."); return true; }
            // 콘솔 타겟 실행 (pixelmon eggsteps는 콘솔에서 <player> <slot> 지원)
            boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eggsteps " + targetName + " " + slot);
            if (!ok) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pixelmon:eggsteps " + targetName + " " + slot); }
            return true;
        }
        p.sendMessage(ChatColor.YELLOW + "사용법: /알걸음 <플레이어> <1~6> 또는 /알걸음 <1~6>");
        return true;
    }
Player p = (Player) sender;

        if (args.length < 1) {
            p.sendMessage(ChatColor.YELLOW + "사용법: /알걸음 <1~6>");
            return true;
        }

        try {
            int slot = Integer.parseInt(args[0]);
            if (slot < 1 || slot > 6) {
                p.sendMessage(ChatColor.RED + "슬롯은 1~6 입니다.");
                return true;
            }
            tryEggsteps(p, slot);
        } catch (NumberFormatException e) {
            p.sendMessage(ChatColor.RED + "숫자를 입력하세요! (1~6)");
        }
        return true;
    }

    
    private void tryEggsteps(Player p, int slot){
        tryEggstepsTarget(p.getName(), p, slot);
    }

    private void tryEggstepsTarget(String targetName, Player sender, int slot){
        // 우선 플레이어 권한으로 실행하여 출력/피드백을 플레이어가 직접 보도록 함
        boolean ok;
        if (sender != null) {
            ok = sender.performCommand("eggsteps " + slot);
            if (!ok) ok = sender.performCommand("pixelmon:eggsteps " + slot);
        } else {
            ok = false;
        }
        if (!ok) ok = p.performCommand("pixelmon:eggsteps " + slot);
        if (!ok) {
            // 실패 시 콘솔로도 시도 (레거시/권한 문제 대비)
            String pName = targetName;
            ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "eggsteps " + pName + " " + slot);
            if (!ok) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "pixelmon:eggsteps " + pName + " " + slot);
        }
    }

}
