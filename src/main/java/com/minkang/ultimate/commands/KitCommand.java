package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.kit.KitGuiListener;
import com.minkang.ultimate.kit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class KitCommand implements CommandExecutor {

    private final Main plugin;
    private final KitManager kit;
    private final KitGuiListener gui;

    public KitCommand(Main p, KitManager manager, KitGuiListener gui){
        this.plugin = p;
        this.kit = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (a.length >= 1 && "설정".equalsIgnoreCase(a[0])){
            if (!s.hasPermission("usp.kit.setup")){ s.sendMessage("권한이 없습니다."); return true; }
            if (!(s instanceof Player)){ s.sendMessage("플레이어만 사용 가능합니다."); return true; }
            gui.open((Player)s);
            return true;
        }
        if (a.length >= 1 && "초기화".equalsIgnoreCase(a[0])){
            if (!s.hasPermission("usp.kit.reset")){ s.sendMessage("권한이 없습니다."); return true; }
            if (a.length < 2){ s.sendMessage("사용법: /기본템 초기화 <플레이어닉>"); return true; }
            OfflinePlayer op = Bukkit.getOfflinePlayer(a[1]);
            if (op == null || (op.getName()==null && !op.hasPlayedBefore())){ s.sendMessage("플레이어를 찾을 수 없습니다."); return true; }
            kit.resetClaim(op.getUniqueId());
            s.sendMessage(ChatColor.YELLOW + (op.getName()!=null? op.getName():a[1]) + ChatColor.GRAY + "님의 기본템 수령 기록을 초기화했습니다.");
            return true;
        }

        if (!(s instanceof Player)){ s.sendMessage("플레이어만 사용 가능합니다."); return true; }
        Player p = (Player) s;
        if (kit.hasClaimed(p.getUniqueId())){
            p.sendMessage(ChatColor.RED + "이미 기본템을 수령했습니다.");
            return true;
        }
        java.util.List<ItemStack> items = kit.getKit();
        if (items.isEmpty()){
            p.sendMessage(ChatColor.RED + "설정된 기본템이 없습니다. 운영자에게 문의하세요.");
            return true;
        }
        for (ItemStack it : items){
            if (it == null || it.getType() == Material.AIR) continue;
            p.getInventory().addItem(it.clone());
        }
        kit.setClaimed(p.getUniqueId(), true);
        p.sendMessage(ChatColor.GREEN + "기본템이 지급되었습니다!");
        return true;
    }
}
