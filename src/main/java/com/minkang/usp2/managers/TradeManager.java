
package com.minkang.usp2.managers;

import com.minkang.usp2.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TradeManager implements Listener {

    private final Main plugin;
    /** trade request: targetUUID -> requesterUUID */
    private final Map<UUID, UUID> pending = new HashMap<>();
    /** active sessions indexed by each player's UUID */
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    public TradeManager(Main plugin){
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Clean up stray sessions on reload after a short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (TradeSession s : new HashSet<>(sessions.values())) {
                s.cancel("서버 리로드");
            }
            sessions.clear();
        }, 200L);
    }

    /** Send a request from 'from' to 'target' */
    public void request(Player from, Player target){
        if (from.getUniqueId().equals(target.getUniqueId())){
            from.sendMessage("§c자기 자신과는 거래할 수 없습니다.");
            return;
        }
        if (sessions.containsKey(from.getUniqueId()) || sessions.containsKey(target.getUniqueId())){
            from.sendMessage("§c이미 진행 중인 거래가 있습니다.");
            return;
        }
        pending.put(target.getUniqueId(), from.getUniqueId());
        from.sendMessage("§a" + target.getName() + "§7에게 거래 요청을 보냈습니다.");
        target.sendMessage("§e" + from.getName() + "§7님이 거래를 요청했습니다. §a/거래 수락 §7또는 §c/거래 거절");
        target.playSound(target.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

        public boolean accept(Player target){
        UUID req = pending.remove(target.getUniqueId());
        if (req == null) return false;
        Player from = Bukkit.getPlayer(req);
        if (from == null) return false;
        open(from, target);
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        try {
            if (!(e.getWhoClicked() instanceof Player)) return;
            Player p = (Player) e.getWhoClicked();
            TradeSession s = sessions.get(p.getUniqueId());
            if (s==null || e.getView().getTopInventory()!=s.inv) return;
            e.setCancelled(true);
            s.handleClick(p, e);
        } catch (Throwable ex) {
            e.setCancelled(true);
            try { ((Player)e.getWhoClicked()).sendMessage("§c거래 오류: 이벤트가 취소되었습니다."); } catch (Throwable ignore) {}
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player)e.getPlayer();
        TradeSession s = sessions.get(p.getUniqueId());
        if (s==null) return;
        s.cancel("상대가 거래 창을 닫았습니다.");
    }

    
    public void cancel(Player p){
        if (p == null) return;
        TradeSession s = sessions.get(p.getUniqueId());
        if (s != null) {
            s.cancel("플레이어가 거래를 취소했습니다.");
        }
        sessions.remove(p.getUniqueId());
    }

    public void closeAll(){
        java.util.Set<TradeSession> uniq = new java.util.HashSet<>(sessions.values());
        for (TradeSession s : uniq){
            try { s.forceClose(); } catch (Throwable ignored) {}
        }
        sessions.clear();
        pending.clear();
    }

    private void open(Player a, Player b){
        // close existing if any for both players
        cancel(a); cancel(b);
        TradeSession s = new TradeSession(a, b);
        sessions.put(a.getUniqueId(), s);
        sessions.put(b.getUniqueId(), s);
        s.open();
    }
    class TradeSession {
        private final Player a, b;
        private final Inventory inv;
        private final int[] aSlots, bSlots;
        private boolean aReady=false, bReady=false, finished=false;

        TradeSession(Player a, Player b){
            this.a=a; this.b=b;
            this.inv = Bukkit.createInventory(null, 54, "§0거래");
            this.aSlots = new int[]{10,11,12,19,20,21,28,29,30};
            this.bSlots = new int[]{14,15,16,23,24,25,32,33,34};
            updateButtons();
            a.openInventory(inv); b.openInventory(inv);
        }

        int buttonIndexFor(Player p){ return p.getUniqueId().equals(a.getUniqueId())? 38 : 42; }

        void toggleReady(Player p){
            if (p.getUniqueId().equals(a.getUniqueId())) aReady = !aReady;
            else if (p.getUniqueId().equals(b.getUniqueId())) bReady = !bReady;
            updateButtons();
            checkBothReady();
        }

        void resetReady(){ aReady=false; bReady=false; updateButtons(); }

        void updateButtons(){
            ItemStack btnA = new ItemStack(aReady? Material.LIME_WOOL : Material.RED_WOOL, 1);
            ItemMeta ma = btnA.getItemMeta(); ma.setDisplayName(aReady? "§a내 수락" : "§c내 수락"); btnA.setItemMeta(ma);
            inv.setItem(buttonIndexFor(a), btnA);

            ItemStack btnB = new ItemStack(bReady? Material.LIME_WOOL : Material.RED_WOOL, 1);
            ItemMeta mb = btnB.getItemMeta(); mb.setDisplayName(bReady? "§a상대 수락" : "§c상대 수락"); btnB.setItemMeta(mb);
            inv.setItem(buttonIndexFor(b), btnB);
        }

        void checkBothReady(){
            if (aReady && bReady){
                complete();
            }
        }

        void complete(){
            if (finished) return; finished = true;
            // move items from aSlots to b, and bSlots to a
            for (int sIdx : aSlots){
                ItemStack it = inv.getItem(sIdx);
                if (it!=null && it.getType()!=Material.AIR) b.getInventory().addItem(it.clone());
                inv.setItem(sIdx, null);
            }
            for (int sIdx : bSlots){
                ItemStack it = inv.getItem(sIdx);
                if (it!=null && it.getType()!=Material.AIR) a.getInventory().addItem(it.clone());
                inv.setItem(sIdx, null);
            }
            a.sendMessage("§a거래 완료!"); b.sendMessage("§a거래 완료!");
            forceClose();
        }

        void cancel(String reason){
            if (finished) return;
            java.util.function.BiConsumer<Player,Integer> back = (pl,slotIdx)->{
                ItemStack it = inv.getItem(slotIdx);
                if (it!=null && it.getType()!=Material.AIR){
                    java.util.Map<Integer, ItemStack> overflow = pl.getInventory().addItem(it.clone());
                    if (!overflow.isEmpty()){
                        for (ItemStack rem : overflow.values()){
                            pl.getWorld().dropItemNaturally(pl.getLocation(), rem);
                        }
                    }
                    inv.setItem(slotIdx, null);
                }
            };
            for (int s : aSlots) back.accept(a, s);
            for (int s : bSlots) back.accept(b, s);
            a.sendMessage("§c거래 취소: §7"+reason);
            b.sendMessage("§c거래 취소: §7"+reason);
            forceClose();
        }

        void forceClose(){
            sessions.remove(a.getUniqueId());
            sessions.remove(b.getUniqueId());
            try { a.closeInventory(); } catch (Exception ignored) {}
            try { b.closeInventory(); } catch (Exception ignored) {}
        }
    }

}

        void toggleReady(Player p){
            if (p.getUniqueId().equals(a.getUniqueId())) aReady = !aReady;
            else if (p.getUniqueId().equals(b.getUniqueId())) bReady = !bReady;
            updateButtons();
            checkBothReady();
        }
        void updateButtons(){
            ItemStack btnA = new ItemStack(aReady? Material.LIME_WOOL : Material.RED_WOOL, 1);
            ItemMeta ma = btnA.getItemMeta(); ma.setDisplayName(aReady? "§a내 수락" : "§c내 수락"); btnA.setItemMeta(ma);
            inv.setItem(buttonIndexFor(a), btnA);

            ItemStack btnB = new ItemStack(bReady? Material.LIME_WOOL : Material.RED_WOOL, 1);
            ItemMeta mb = btnB.getItemMeta(); mb.setDisplayName(bReady? "§a상대 수락" : "§c상대 수락"); btnB.setItemMeta(mb);
            inv.setItem(buttonIndexFor(b), btnB);
        }
        void checkBothReady(){
            if (aReady && bReady){
                // execute trade
                complete();
            }
        }