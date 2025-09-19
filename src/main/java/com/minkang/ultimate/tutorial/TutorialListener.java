
package com.minkang.ultimate.tutorial;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class TutorialListener implements Listener {
    private final TutorialManager manager;
    public TutorialListener(TutorialManager m){ this.manager = m; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        if (!manager.isEnabled()) return;
        Player p = e.getPlayer();
        if (p.hasPlayedBefore()) return;
        if (!manager.shouldPromptOnFirstJoin()) return;
        if (manager.isSkipped(p.getUniqueId()) || manager.isCompleted(p.getUniqueId())) return;
        p.openInventory(manager.buildPromptGui());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Inventory v = e.getView().getTopInventory();
        if (v == null) return;
        String title = ChatColor.stripColor(e.getView().getTitle());
        if (!title.contains("튜토리얼")) return;
        e.setCancelled(true);
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;
        String name = ChatColor.stripColor(it.getItemMeta().getDisplayName());
        Player p = (Player)e.getWhoClicked();
        if (it.getType() == Material.LIME_DYE && name.contains("튜토리얼 시작")){
            p.closeInventory();
            manager.startTutorial(p);
        } else if (it.getType() == Material.BARRIER && name.contains("나중에")){
            p.closeInventory();
            manager.setSkipped(p.getUniqueId(), true);
            p.sendMessage("§7튜토리얼은 나중에 /튜토리얼 시작 으로 다시 진행할 수 있습니다.");
        }
    }
}
