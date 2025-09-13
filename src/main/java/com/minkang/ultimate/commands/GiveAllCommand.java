package com.minkang.ultimate.commands;
import org.bukkit.Bukkit; import org.bukkit.ChatColor; import org.bukkit.Material; import org.bukkit.Sound;
import org.bukkit.command.*; import org.bukkit.entity.Player; import org.bukkit.inventory.ItemStack;
import com.minkang.ultimate.Main;
public class GiveAllCommand implements CommandExecutor {
    private final Main plugin; public GiveAllCommand(Main plugin){ this.plugin=plugin; }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(!(sender instanceof Player)){ sender.sendMessage(ChatColor.RED+"플레이어만 사용할 수 있습니다."); return true; }
        Player p=(Player)sender; if(!p.hasPermission("usp.giveall")){ p.sendMessage(ChatColor.RED+"권한이 없습니다. (usp.giveall)"); return true; }
        if(args.length<1){ p.sendMessage(ChatColor.YELLOW+"사용법: /전체지급 <수량>"); return true; }
        int amount=1; try{ amount=Math.max(1,Integer.parseInt(args[0])); }catch(Exception ignored){}
        ItemStack hand=p.getInventory().getItemInMainHand(); if(hand==null||hand.getType()==Material.AIR){ p.sendMessage(ChatColor.RED+"손에 아이템을 들어주세요."); return true; }
        ItemStack toGive=hand.clone(); toGive.setAmount(amount);
        int cnt=0; for(Player t:Bukkit.getOnlinePlayers()){ t.getInventory().addItem(toGive.clone()); t.playSound(t.getLocation(), Sound.ENTITY_ITEM_PICKUP,1f,1f); cnt++; }
        Bukkit.broadcastMessage(ChatColor.GREEN+"[전체지급] "+ChatColor.AQUA+p.getName()+ChatColor.WHITE+" 님이 "+ChatColor.YELLOW+toGive.getType().name()+" x"+amount+ChatColor.WHITE+" 을(를) 전원에게 지급했습니다! ("+cnt+"명)");
        return true;
    }
}
