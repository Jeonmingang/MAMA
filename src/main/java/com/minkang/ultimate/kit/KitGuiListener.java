package com.minkang.ultimate.kit;
import org.bukkit.event.*; import org.bukkit.event.inventory.InventoryCloseEvent;
public class KitGuiListener implements Listener {
    @EventHandler public void onClose(InventoryCloseEvent e){ String title=e.getView().getTitle(); if(title!=null && title.equalsIgnoreCase(KitManager.getInstance().getGuiTitle())){ KitManager.getInstance().saveItemsFromGUI(e.getInventory()); if(e.getPlayer()!=null) e.getPlayer().sendMessage("§a기본템 구성이 저장되었습니다."); } }
}
