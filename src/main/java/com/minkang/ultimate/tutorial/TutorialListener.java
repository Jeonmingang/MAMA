package com.minkang.ultimate.tutorial;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;

public class TutorialListener implements Listener {
    private final TutorialManager manager;
    public TutorialListener(TutorialManager m){ this.manager = m; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e){
        Player p = e.getPlayer();
        if (!p.hasPlayedBefore()){
            Location start = manager.getStartLocation();
            if (start != null) { p.teleport(start); }
        }
    }

    @EventHandler
    public void onSelect(PlayerInteractEvent e){
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (!manager.isSelecting(p)) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        manager.finishExitSelect(p, b.getLocation());
        p.sendMessage(ChatColor.GREEN + "튜토리얼 종료 트리거 블럭 지정 완료: " + b.getType().name());
        e.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e){
        Player p = e.getPlayer();
        Location exit = manager.getExitBlock();
        if (exit == null) return;
        // detect step onto (center of block); compare block coords
        Location to = e.getTo();
        Location from = e.getFrom();
        if (to == null || from == null) return;
        boolean nowInside = to.getBlockX()==(int)Math.floor(exit.getX()) && to.getBlockY()==(int)Math.floor(exit.getY()) && to.getBlockZ()==(int)Math.floor(exit.getZ());
        boolean wasInside = from.getBlockX()==(int)Math.floor(exit.getX()) && from.getBlockY()==(int)Math.floor(exit.getY()) && from.getBlockZ()==(int)Math.floor(exit.getZ());
        if (!wasInside && nowInside){
            for (String cmd : manager.getExitCommands()){
                String run = cmd.replace("%player%", p.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), run);
            }
        }
        manager.setInsideExit(p, nowInside);
    }
}
