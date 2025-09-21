package com.minkang.ultimate.commands;
import com.minkang.ultimate.kit.KitManager;
import org.bukkit.ChatColor; import org.bukkit.command.*; import org.bukkit.entity.Player;
public class KitCommand implements CommandExecutor {
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(args.length>=1 && args[0].equalsIgnoreCase("설정")){ if(!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED+"플레이어만 사용할 수 있습니다."); return true; } if(!sender.hasPermission("usp.kit.admin")){ sender.sendMessage(ChatColor.RED+"권한이 없습니다. (usp.kit.admin)"); return true; } Player p=(Player)sender; KitManager.getInstance().openSetupGUI(p); return true; }
        if(args.length>=2 && args[0].equalsIgnoreCase("초기화")){ if(!sender.hasPermission("usp.kit.admin")){ sender.sendMessage(ChatColor.RED+"권한이 없습니다. (usp.kit.admin)"); return true; } String name=args[1]; boolean ok=KitManager.getInstance().resetPlayer(name); sender.sendMessage(ok?ChatColor.GREEN+"초기화되었습니다.":ChatColor.RED+"대상이 오프라인이거나 찾을 수 없습니다."); return true; }
        if(!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED+"플레이어만 사용할 수 있습니다."); return true; } Player p=(Player)sender;
        if(KitManager.getInstance().hasClaimed(p)){ p.sendMessage(ChatColor.RED+"이미 기본템을 받았습니다."); return true; }
        int c=KitManager.getInstance().giveKit(p); if(c<=0){ p.sendMessage(ChatColor.YELLOW+"기본템이 설정되어 있지 않습니다. OP가 '/기본템 설정'으로 등록해 주세요."); return true; }
        KitManager.getInstance().setClaimed(p,true); p.sendMessage(ChatColor.GREEN+"기본템을 지급받았습니다!"); return true;
    }
}
