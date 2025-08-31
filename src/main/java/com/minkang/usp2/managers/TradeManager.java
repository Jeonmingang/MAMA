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
    // target -> requester
    private final Map<UUID, UUID> pending = new HashMap<>();
    // player -> session
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    public TradeManager(Main plugin){ this.plugin = plugin; }

    // ===== Requests =====
    public void request(Player from, Player to){
        pending.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage("§e거래 요청을 보냈습니다: §f"+to.getName());
        to.sendMessage("§e"+from.getName()+"§7 가 거래를 요청했습니다: §a/거래 수락 §7또는 §c/거래 취소");
        // expire after 10s
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            UUID req = pending.remove(to.getUniqueId());
            if (req != null) {
                from.sendMessage("§7거래 요청이 시간초과되었습니다.");
                to.sendMessage("§7거래 요청이 만료되었습니다.");
            }
        }, 200L);
    }

    public boolean accept(Player target){
        UUID req = pending.remove(target.getUniqueId());
        if (req == null) return false;
        Player from = Bukkit.getPlayer(req);
        if (from == null) return false;
        open(from, target);
        return true;
    }

    public void cancel(String reason){
            if (finished) { return; }
            finished = true;
            // Return items safely with overflow drop
            java.util.function.BiConsumer<Player, Integer> back = (pl, slotIdx) -> {
                org.bukkit.inventory.ItemStack it = inv.getItem(slotIdx);
                if (it != null){
                    java.util.Map<Integer, org.bukkit.inventory.ItemStack> left = pl.getInventory().addItem(it);
                    for (org.bukkit.inventory.ItemStack rem: left.values()) pl.getWorld().dropItemNaturally(pl.getLocation(), rem);
                    inv.setItem(slotIdx, null);
                }
            };
            for (int sIdx : aSlots){ back.accept(a, sIdx); }
            for (int sIdx : bSlots){ back.accept(b, sIdx); }
            a.sendMessage("§c거래 취소: §7"+reason);
            b.sendMessage("§c거래 취소: §7"+reason);
            forceClose();
        }

    // ===== Session =====
    private void open(Player a, Player b){
        // close existing if any
        cancel(a); cancel(b);
        TradeSession s = new TradeSession(a, b);
        sessions.put(a.getUniqueId(), s);
        sessions.put(b.getUniqueId(), s);
        s.open();
    }

    public void closeAll(){
        for (TradeSession s: new HashSet<>(sessions.values())) s.forceClose();
        sessions.clear();
        pending.clear();
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
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player)e.getPlayer();
        TradeSession s = sessions.get(p.getUniqueId());
        if (s==null) return;
        s.cancel("상대가 거래 창을 닫았습니다.");
    }

    class TradeSession {
        final Player a, b;
        final Inventory inv;
        boolean aReady=false, bReady=false;
        final int[] aSlots, bSlots;
        final int aAccept=45, bAccept=53;
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
            inv.setItem(aAccept, button(Material.LIME_TERRACOTTA,"§a내 수락"));
            inv.setItem(bAccept, button(Material.LIME_TERRACOTTA,"§a상대 수락"));
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
            if (aReady){ aReady=false; inv.setItem(aAccept, button(Material.LIME_TERRACOTTA,"§a내 수락")); }
            if (bReady){ bReady=false; inv.setItem(bAccept, button(Material.LIME_TERRACOTTA,"§a상대 수락")); }
        }

        void handleClick(Player p, InventoryClickEvent e){
            int raw = e.getRawSlot();
            ClickType type = e.getClick();
            // Accept buttons
            if (raw==aAccept || raw==bAccept){
                if (raw==aAccept && p.getUniqueId().equals(a.getUniqueId())){
                    aReady = !aReady; inv.setItem(aAccept, button(Material.LIME_TERRACOTTA, aReady?"§2내 수락 완료":"§a내 수락"));
                } else if (raw==bAccept && p.getUniqueId().equals(b.getUniqueId())){
                    bReady = !bReady; inv.setItem(bAccept, button(Material.LIME_TERRACOTTA, bReady?"§2상대 수락 완료":"§a상대 수락"));
                }
                a.playSound(a.getLocation(), Sound.UI_BUTTON_CLICK,1,1);
                b.playSound(b.getLocation(), Sound.UI_BUTTON_CLICK,1,1);
                if (aReady && bReady) { finalizeTrade(); }
                return;
            }

            // Player inventory shift-click -> move to own trade area
            int topSize = e.getInventory().getSize();
            if (raw >= topSize){
                if (type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT){
                    ItemStack cur = e.getCurrentItem();
                    if (cur != null && cur.getType()!=Material.AIR){
                        int dst = firstEmptySlot(p);
                        if (dst != -1){
                            inv.setItem(dst, cur.clone());
                            if (e.getClickedInventory()!=null) {
                                e.getClickedInventory().setItem(e.getSlot(), null);
                            } else {
                                p.getInventory().removeItem(cur);
                            }
                            resetReady();
                        }
                    }
                }
                return;
            }

            // Trade area click
            if (!isMySlot(p, raw)) return;

            ItemStack cursor = e.getCursor();
            ItemStack slot = inv.getItem(raw);

            if (cursor != null && cursor.getType() != Material.AIR){
                // Place or merge/swap
                if (slot == null){
                    inv.setItem(raw, cursor.clone());
                    e.getView().setCursor(null);
                } else if (slot.isSimilar(cursor) && slot.getAmount() < slot.getMaxStackSize()){
                    int can = Math.min(cursor.getAmount(), slot.getMaxStackSize() - slot.getAmount());
                    slot.setAmount(slot.getAmount() + can);
                    cursor.setAmount(cursor.getAmount() - can);
                    inv.setItem(raw, slot);
                    if (cursor.getAmount() <= 0) e.getView().setCursor(null);
                    else e.getView().setCursor(cursor);
                } else {
                    inv.setItem(raw, cursor.clone());
                    e.getView().setCursor(slot);
                }
                resetReady();
                return;
            } else {
                // Pick up from slot
                if (slot != null){
                    e.getView().setCursor(slot);
                    inv.setItem(raw, null);
                    resetReady();
                }
            }
        }

        void give(Player to, List<ItemStack> items){
            for (ItemStack it: items){
                Map<Integer, ItemStack> left = to.getInventory().addItem(it);
                for (ItemStack rem: left.values()) to.getWorld().dropItemNaturally(to.getLocation(), rem);
            }
        }

        void finalizeTrade(){
            if (finished) return;
            finished = true;
            List<ItemStack> aItems = new ArrayList<>();
            for (int s : aSlots){ ItemStack it = inv.getItem(s); if (it!=null) { aItems.add(it); inv.setItem(s,null);} }
            List<ItemStack> bItems = new ArrayList<>();
            for (int s : bSlots){ ItemStack it = inv.getItem(s); if (it!=null) { bItems.add(it); inv.setItem(s,null);} }
            give(b, aItems);
            give(a, bItems);
            a.sendMessage("§a거래 완료!");
            b.sendMessage("§a거래 완료!");
            forceClose();
        }

        void cancel(String reason){
            for (int s : aSlots){ ItemStack it = inv.getItem(s); if (it!=null) a.getInventory().addItem(it); }
            for (int s : bSlots){ ItemStack it = inv.getItem(s); if (it!=null) b.getInventory().addItem(it); }
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
