package com.minkang.ultimate.tutorial;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TutorialCommand implements CommandExecutor {
    private final TutorialManager manager;
    public TutorialCommand(TutorialManager manager){ this.manager = manager; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(ChatColor.RED + "플레이어만 사용 가능합니다."); return true; }
        if (!sender.hasPermission("usp.tutorial.admin")) { sender.sendMessage(ChatColor.RED + "권한이 없습니다."); return true; }
        Player p = (Player)sender;
        if (args.length < 1){ sender.sendMessage(ChatColor.YELLOW + "사용법: /튜토리얼 위치 시작 | /튜토리얼 위치 종료"); return true; }
        String sub = args[0];
        if (sub.equalsIgnoreCase("위치")) {
            if (args.length >= 2 && args[1].equalsIgnoreCase("시작")){
                Location loc = p.getLocation();
                manager.setStartLocation(loc);
                sender.sendMessage(ChatColor.GREEN + "튜토리얼 시작 위치가 현재 위치로 설정되었습니다.");
                return true;
            } else if (args.length >= 2 && args[1].equalsIgnoreCase("종료")){
                manager.beginExitSelect(p);
                sender.sendMessage(ChatColor.GREEN + "블럭을 우클릭해서 '종료 트리거' 블럭을 지정하세요.");
                return true;
            }
        }
        sender.sendMessage(ChatColor.YELLOW + "사용법: /튜토리얼 위치 시작 | /튜토리얼 위치 종료");
        return true;
    }
}
