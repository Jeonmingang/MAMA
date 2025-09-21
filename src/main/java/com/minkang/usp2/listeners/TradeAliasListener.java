package com.minkang.usp2.listeners;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class TradeAliasListener implements Listener {

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e){
        String msg = e.getMessage();
        if (msg.equalsIgnoreCase("/거래수락")) {
            e.setMessage("/trade accept");
        } else if (msg.equalsIgnoreCase("/거래거절")) {
            e.setMessage("/trade deny");
        }
    }
}
