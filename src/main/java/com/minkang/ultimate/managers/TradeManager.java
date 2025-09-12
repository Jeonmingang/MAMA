package com.minkang.ultimate.managers;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TradeManager implements Listener {
    private final java.util.Set<org.bukkit.inventory.Inventory> tradeInvs = new java.util.HashSet<>();
    private boolean isTradeInventory(org.bukkit.inventory.Inventory inv){
        if (inv == null) return false;
        if (tradeInvs.contains(inv)) return true;
        try { String t = inv.getTitle(); if (t != null && (t.contains("거래") || t.toLowerCase().contains("trade"))) return true; } catch (Throwable ignored) {}
        return false;
    }


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
        int sec;
        try { sec = Math.max(0, plugin.getConfig().getInt("trade.request-timeout-seconds", 20)); } catch (Throwable t) { sec = 20; }
        if (sec > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                java.util.UUID removed = pending.remove(target.getUniqueId());
                if (removed != null) {
                    from.sendMessage("§7거래 요청이 만료되었습니다.");
                    target.sendMessage("§7거래 요청이 시간초과되었습니다.");
                }
            }, sec * 20L);
        }
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
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
        cancel(a); cancel(b);
        TradeSession s = new TradeSession(a, b);
        sessions.put(a.getUniqueId(), s);
        sessions.put(b.getUniqueId(), s);
        s.open();
    }

    class TradeSession {
        private boolean aReady=false, bReady=false;
        final Player a, b;
        final Inventory inv;
        final int[] aSlots, bSlots;
        final int aAccept=45, bAccept=53;
        final int cancelSlot=49;
        boolean finished=false;

        TradeSession(Player a, Player b){
            this.a=a; this.b=b;
            this.inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY+"거래: "+a.getName()+" <> "+b.getName());
            aSlots = new int[]{10,11,12,19,20,21,28,29,30};
            bSlots = new int[]{16,15,14,25,24,23,34,33,32};
            drawFrame();
        }

        void drawFrame(){
            ItemStack g = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta m = g.getItemMeta(); if (m != null) { m.setDisplayName(" "); g.setItemMeta(m); }
            for(int i=0;i<54;i++){
                if (i%9==4 || i>=36) inv.setItem(i, g);
            }
            inv.setItem(aAccept, button(Material.LIME_CONCRETE,"§a내 수락"));
            inv.setItem(bAccept, button(Material.LIME_CONCRETE,"§a상대 수락"));
                    inv.setItem(cancelSlot, button(Material.BARRIER,"§c거래 취소"));
}

        ItemStack button(Material mat, String name){
            ItemStack it = new ItemStack(mat);
            ItemMeta m = it.getItemMeta(); if (m!=null){ m.setDisplayName(name); it.setItemMeta(m); }
            return it;
        }

        void open(){
            a.openInventory(inv);
            b.openInventory(inv);
        }

        boolean isMySlot(Player p, int raw){
            boolean isA = p.getUniqueId().equals(a.getUniqueId());
            int[] slots = isA? aSlots : bSlots;
            for (int s: slots) if (s==raw) return true;
            return false;
        }

        int firstEmptySlot(Player p){
            boolean isA = p.getUniqueId().equals(a.getUniqueId());
            int[] slots = isA? aSlots : bSlots;
            for (int s: slots) if (inv.getItem(s)==null) return s;
            return -1;
        }

        void resetReady(){
            aReady = false;
            bReady = false;
            updateButtons();
        }

        void handleClick(Player p, InventoryClickEvent e){
            int raw = e.getRawSlot();
            ClickType type = e.getClick();

            
            if (raw==cancelSlot) { cancel("플레이어가 거래를 취소했습니다."); return; }
if (raw==aAccept && p.getUniqueId().equals(a.getUniqueId())){
                aReady = !aReady;
                updateButtons();
                checkBothReady();
                return;
            }
            if (raw==bAccept && p.getUniqueId().equals(b.getUniqueId())){
                bReady = !bReady;
                updateButtons();
                checkBothReady();
                return;
            }
            if (raw < 54){
                if (!isMySlot(p, raw)) return;
                resetReady();
                return;
            } else {
                ItemStack cursor = e.getCurrentItem();
                if (cursor == null || cursor.getType()==Material.AIR) return;
                int dst = firstEmptySlot(p);
                if (dst < 0){ p.sendMessage("§c자신의 거래 슬롯이 가득 찼습니다."); return; }
                inv.setItem(dst, cursor.clone());
                e.getView().getBottomInventory().setItem(e.getSlot(), null);
                resetReady();
            }

            if (aReady && bReady){
                finish();
            }
        }

        void finish(){
            if (finished) return;
            finished = true;

            for (int s : aSlots){
                ItemStack it = inv.getItem(s);
                if (it!=null) b.getInventory().addItem(it);
                inv.setItem(s, null);
            }
            for (int s : bSlots){
                ItemStack it = inv.getItem(s);
                if (it!=null) a.getInventory().addItem(it);
                inv.setItem(s, null);
            }
            a.sendMessage("§a거래 완료!");
            b.sendMessage("§a거래 완료!");
            forceClose();
        }

        void cancel(String reason){
            if (finished) { return; }
            finished = true;
            java.util.function.BiConsumer<Player, Integer> back = (pl, slotIdx) -> {
                ItemStack it = inv.getItem(slotIdx);
                if (it != null) {
                    Map<Integer, ItemStack> overflow = pl.getInventory().addItem(it);
                    if (!overflow.isEmpty()) for (ItemStack rem : overflow.values()) pl.getWorld().dropItemNaturally(pl.getLocation(), rem);
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

        void toggleReady(Player p){
            if (p.getUniqueId().equals(a.getUniqueId())) aReady = !aReady;
            else if (p.getUniqueId().equals(b.getUniqueId())) bReady = !bReady;
            updateButtons();
            checkBothReady();
        }

        void updateButtons(){
            // READY = RED, NOT READY = LIME
            ItemStack btnA = button(aReady ? Material.RED_CONCRETE : Material.LIME_CONCRETE,
                    aReady ? "§c수락완료" : "§a내 수락");
            inv.setItem(aAccept, btnA);

            ItemStack btnB = button(bReady ? Material.RED_CONCRETE : Material.LIME_CONCRETE,
                    bReady ? "§c상대 수락완료" : "§a상대 수락");
            inv.setItem(bAccept, btnB);

            // keep cancel button intact
            inv.setItem(cancelSlot, button(Material.BARRIER,"§c거래 취소"));
        }

        void checkBothReady(){
            if (aReady && bReady){
                finish();
            }
        }
    }

    // Trailing fragment moved to prevent compile errors
    // 
    //     @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    //     public void onDrag(InventoryDragEvent e){
    //         if (!isTradeInventory(e.getInventory())) return;
    //         e.setCancelled(true);
    //     }}
    // Trailing fragment commented
    // 
    //     @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    //     public void onDrag(InventoryDragEvent e){
    //         if (!isTradeInventory(e.getInventory())) return;
    //         e.setCancelled(true);
    //     }
