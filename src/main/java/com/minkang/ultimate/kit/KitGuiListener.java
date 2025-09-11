package com.minkang.ultimate.kit;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class KitGuiListener implements Listener {

    public static final String TITLE = ChatColor.GOLD + "기본템 설정";
    private final Main plugin;
    private final KitManager kit;

    public KitGuiListener(Main p, KitManager k){
        this.plugin = p;
        this.kit = k;
    }

    public void open(Player p){
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        List<ItemStack> items = kit.getKit();
        int idx = 0;
        for (ItemStack it : items){
            if (it == null || it.getType() == Material.AIR) continue;
            if (idx < inv.getSize()) inv.setItem(idx++, it.clone());
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!TITLE.equals(e.getView().getTitle())) return;
        Inventory inv = e.getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack it : inv.getContents()){
            if (it == null || it.getType() == Material.AIR) continue;
            items.add(it.clone());
        }
        kit.saveKit(items);
        if (e.getPlayer() instanceof Player){
            ((Player)e.getPlayer()).sendMessage(ChatColor.GREEN + "기본템 구성이 저장되었습니다. (" + items.size() + "개)");
        }
    }
}
