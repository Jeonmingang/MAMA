package com.minkang.usp2.managers;

import com.minkang.usp2.Main;
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
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Clean, robust trade system (1.16.5 / Java 8) for USP2 tree.
 * Only trade logic is handled here; other features untouched.
 */
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
    private final Map<UUID, UUID> pending = new HashMap<UUID, UUID>();
    /** active sessions indexed by each player's UUID */
    private final Map<UUID, TradeSession> sessions = new HashMap<UUID, TradeSession>();

    public TradeManager(Main plugin){
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /* =====================
     * Public API for commands
     * ===================== */
    public void request(Player from, Player to){
        if (from == null || to == null) return;
        if (from.equals(to)){ from.sendMessage("§c자기 자신과는 거래할 수 없습니다."); return; }
        if (sessions.containsKey(from.getUniqueId()) || sessions.containsKey(to.getUniqueId())){
            from.sendMessage("§c이미 진행 중인 거래가 있습니다.");
            return;
        }
        pending.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage("§a" + to.getName() + " 님에게 거래 요청을 보냈습니다.");
        to.sendMessage("§e" + from.getName() + " §f님이 거래를 요청했습니다. §a/거래 수락 §f으로 수락하세요.");
        to.playSound(to.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
        // 60초 타임아웃
        final UUID key = to.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                UUID who = pending.remove(key);
                if (who != null){
                    Player req = Bukkit.getPlayer(who);
                    Player tgt = Bukkit.getPlayer(key);
                    if (req != null) req.sendMessage("§7[거래] §c요청이 만료되었습니다.");
                    if (tgt != null) tgt.sendMessage("§7[거래] §c요청이 만료되었습니다.");
                }
            }
        }, 20L * 60L);
    }

    public void accept(Player who){
        if (who == null) return;
        UUID reqId = pending.remove(who.getUniqueId());
        if (reqId == null){ who.sendMessage("§c유효한 요청이 없습니다."); return; }
        Player req = Bukkit.getPlayer(reqId);
        if (req == null){ who.sendMessage("§c요청자가 오프라인입니다."); return; }
        if (sessions.containsKey(who.getUniqueId()) || sessions.containsKey(req.getUniqueId())){
            who.sendMessage("§c이미 진행 중인 거래가 있습니다."); return;
        }
        TradeSession s = new TradeSession(req, who);
        sessions.put(req.getUniqueId(), s);
        sessions.put(who.getUniqueId(), s);
        s.open();
    }

    public void cancel(Player who){
        if (who == null) return;
        TradeSession s = sessions.get(who.getUniqueId());
        if (s != null){ s.cancel("플레이어가 거래를 취소했습니다."); return; }
        // 아니면 대기중 요청 취소
        UUID other = pending.remove(who.getUniqueId());
        if (other != null) who.sendMessage("§7[거래] §c대기 중인 요청을 취소했습니다.");
    }

    public void closeAll(){
        // 안전하게 복사본으로 순회
        HashSet<TradeSession> ss = new HashSet<TradeSession>(sessions.values());
        for (TradeSession s : ss){
            try { s.cancel("서버 종료로 거래가 취소되었습니다."); } catch (Throwable ignored) {}
        }
    }

    /* =====================
     * Events
     * ===================== */

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
        if (e.getInventory()==s.inv && !s.finished){
            s.cancel("상대가 거래 창을 닫았습니다.");
        }
    }

    /* =====================
     * Inner session
     * ===================== */
    class TradeSession {
        final Player a, b;
        final Inventory inv;
        boolean aReady=false, bReady=false;
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

        ItemStack button(Material mat, String name){
            ItemStack it = new ItemStack(mat);
            ItemMeta m = it.getItemMeta(); if (m!=null){ m.setDisplayName(name); it.setItemMeta(m); }
            return it;
        }

        void drawFrame(){
            ItemStack g = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta m = g.getItemMeta(); if (m != null) { m.setDisplayName(" "); g.setItemMeta(m); }
            for(int i=0;i<54;i++){
                if (i%9==4 || i>=36) inv.setItem(i, g);
            }
            inv.setItem(aAccept, button(Material.LIME_CONCRETE,"§a내 수락"));
            inv.setItem(bAccept, button(Material.LIME_CONCRETE,"§a상대 수락"));
            inv.setItem(cancelSlot, button(Material.BARRIER, "§c거래 취소"));
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
            int[] slots = p.getUniqueId().equals(a.getUniqueId()) ? aSlots : bSlots;
            for (int s: slots){ ItemStack it = inv.getItem(s); if (it == null) return s; }
            return -1;
        }

        void updateButtons(){
            inv.setItem(aAccept, button(aReady ? Material.RED_CONCRETE  : Material.LIME_CONCRETE,
                                        aReady ? "§c수락완료"            : "§a내 수락"));
            inv.setItem(bAccept, button(bReady ? Material.RED_CONCRETE  : Material.LIME_CONCRETE,
                                        bReady ? "§c상대 수락완료"       : "§a상대 수락"));
            inv.setItem(cancelSlot, button(Material.BARRIER,"§c거래 취소"));
        }

        void resetReady(){
            aReady = false;
            bReady = false;
            updateButtons();
        }

        void handleClick(Player p, InventoryClickEvent e){
            int raw = e.getRawSlot();
            ClickType type = e.getClick();

            // 취소
            if (raw==cancelSlot) { cancel("플레이어가 거래를 취소했습니다."); return; }

            // 수락 버튼
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

            int topSize = e.getInventory().getSize();

            // 플레이어 인벤터리쪽 클릭 -> 자신의 거래 영역으로 이동(스택 단위)
            if (raw >= topSize){
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
                return;
            }

            // 거래창 내부 클릭
            if (!isMySlot(p, raw)) return;

            ItemStack cursor = e.getCursor();
            ItemStack slot = inv.getItem(raw);

            if (cursor != null && cursor.getType() != Material.AIR){
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
                if (slot != null){
                    e.getView().setCursor(slot);
                    inv.setItem(raw, null);
                    resetReady();
                }
            }
        }

        void give(Player to, List<ItemStack> items){
            for (ItemStack it: items){
                if (it == null) continue;
                Map<Integer, ItemStack> left = to.getInventory().addItem(it);
                for (ItemStack rem: left.values()){
                    if (rem == null) continue;
                    to.getWorld().dropItemNaturally(to.getLocation(), rem).setVelocity(new Vector(0,0,0));
                }
            }
        }

        void finish(){
            if (finished) return;
            finished = true;
            List<ItemStack> aItems = new ArrayList<ItemStack>();
            for (int s : aSlots){ ItemStack it = inv.getItem(s); if (it!=null) { aItems.add(it); inv.setItem(s,null);} }
            List<ItemStack> bItems = new ArrayList<ItemStack>();
            for (int s : bSlots){ ItemStack it = inv.getItem(s); if (it!=null) { bItems.add(it); inv.setItem(s,null);} }
            give(b, aItems);
            give(a, bItems);
            a.sendMessage("§a거래 완료!");
            b.sendMessage("§a거래 완료!");
            forceClose();
        }

        void cancel(String reason){
            if (finished) return;
            finished = true;
            a.sendMessage("§c거래 취소: " + reason);
            b.sendMessage("§c거래 취소: " + reason);
            // 돌려주기
            for (int s : aSlots){ ItemStack it = inv.getItem(s); if (it!=null) { a.getInventory().addItem(it); inv.setItem(s,null);} }
            for (int s : bSlots){ ItemStack it = inv.getItem(s); if (it!=null) { b.getInventory().addItem(it); inv.setItem(s,null);} }
            forceClose();
        }

        void forceClose(){
            sessions.remove(a.getUniqueId());
            sessions.remove(b.getUniqueId());
            try { a.closeInventory(); } catch (Exception ignored) {}
            try { b.closeInventory(); } catch (Exception ignored) {}
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
