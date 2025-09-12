package com.minkang.ultimate.listeners;

import com.minkang.ultimate.managers.ShopManager;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.NPCRightClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CitizensBridge implements Listener {
    private final ShopManager shops;
    public CitizensBridge(ShopManager shops){ this.shops = shops; }
    @EventHandler
    public void onClick(NPCRightClickEvent e){
        int id = e.getNPC().getId();
        String shop = shops.getBoundShop(id);
        if (shop != null) {
            shops.open(e.getClicker(), shop);
        }
    }
}
