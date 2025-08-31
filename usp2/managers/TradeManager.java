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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

/**
 * 거래 GUI (/거래) – CatServer/Spigot 1.16.5, Java 8
 * 강제 이벤트 취소 + 수동 이동 로직으로 복사버그 방지
 * - Shift/double/number-key/offhand/drop/drag 전부 차단
 * - 창 닫기 시 아이템 원복/이관 보장
 * - 두 명 모두 수락 시 교환 처리
 */
public class TradeManager implements Listener {
    private final Main plugin;
    // target -> requester
    private final Map<UUID, UUID> pending = new HashMap<>();
    // player -> session
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    public TradeManager(Main plugin){
        this.plugin = plugin;
    }

    // ===== Requests =====
    public void request(Player from, Player to){
        pending.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage("§e거래 요청을 보냈습니다: §f" + to.getName());
        to.sendMessage("§e" + from.getName() + "§7 가 거래를 요청했습니다: §a/거래 수락 §7또는 §c/거래 취소");
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

    public void cancel(Player who){
        TradeSession s = sessions.get(who.getUniqueId());
        if (s != null) s.cancel("플레이어가 /거래 취소를 실행했습니다.");
    }

    public void open(Player a, Player b){
        // close existing if any
        cancel(a); cancel(b);
        TradeSession s = new TradeSession(a, b);
        sessions.put(a.getUniqueId(), s);
        sessions.put(b.getUniqueId(), s);
        a.openInventory(s.inv);
        b.openInventory(s.inv);
        a.playSound(a.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.1f);
        b.playSound(b.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.1f);
    }

    // ====== LISTENERS ======
    private boolean isSessionInv(Player p, Inventory top){
        TradeSession s = sessions.get(p.getUniqueId());
        return s != null && s.inv.equals(top);
    }

    private TradeSession session(Player p){
        return sessions.get(p.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        Inventory top = e.getView().getTopInventory();
        TradeSession s = session(p);
        if (s == null || top == null || !s.inv.equals(top)) return;

        // 항상 서버 이동 취소 (바닐라 처리 = 복사 위험)
        e.setCancelled(true);

        // 위험한 클릭 전면 차단
        ClickType ct = e.getClick();
        if (ct == ClickType.SHIFT_LEFT || ct == ClickType.SHIFT_RIGHT ||
            ct == ClickType.DOUBLE_CLICK || ct == ClickType.NUMBER_KEY ||
            ct == ClickType.SWAP_OFFHAND || ct == ClickType.DROP || ct == ClickType.CONTROL_DROP ||
            ct == ClickType.UNKNOWN) {
            return;
        }

        int raw = e.getRawSlot();
        boolean inTop = raw < top.getSize();

        // 내 수락/상대 수락 버튼
        if (inTop){
            if (raw == s.buttonIndexFor(p)){
                s.toggleReady(p);
                return;
            }
            // 프레임 클릭 무시
            if (s.isFrame(raw)) return;

            // 내 영역만 조작 가능
            if (!s.isMySlot(p, raw)) return;

            // 커서/슬롯 수동 이동
            ItemStack cursor = e.getCursor();
            ItemStack slot = top.getItem(raw);

            if (cursor == null || cursor.getType() == Material.AIR){
                // 픽업
                if (slot != null){
                    e.getView().setCursor(slot.clone());
                    top.setItem(raw, null);
                    s.resetReady();
                }
            } else {
                if (slot == null){
                    top.setItem(raw, cursor.clone());
                    e.getView().setCursor(null);
                    s.resetReady();
                } else if (slot.isSimilar(cursor) && slot.getAmount() < slot.getMaxStackSize()){
                    int can = Math.min(cursor.getAmount(), slot.getMaxStackSize() - slot.getAmount());
                    slot.setAmount(slot.getAmount() + can);
                    cursor.setAmount(cursor.getAmount() - can);
                    top.setItem(raw, slot);
                    e.getView().setCursor(cursor.getAmount() <= 0 ? null : cursor);
                    s.resetReady();
                } else {
                    // 스왑
                    top.setItem(raw, cursor.clone());
                    e.getView().setCursor(slot.clone());
                    s.resetReady();
                }
            }
            return;
        }

        // 바닥 인벤토리에서 shift 이동 등은 이미 차단됨.
        // 바닥 -> 거래영역으로 단일칸 이동(좌클릭): 빈 슬롯에만 배치
        if (e.getCurrentItem() != null && e.getCurrentItem().getType() != Material.AIR){
            int dst = s.firstEmptyTradeSlotFor(p);
            if (dst != -1){
                ItemStack cur = e.getCurrentItem().clone();
                top.setItem(dst, cur);
                // 바닥 인벤토리에서 제거
                if (e.getClickedInventory() != null) {
                    e.getClickedInventory().setItem(e.getSlot(), null);
                } else {
                    p.getInventory().removeItem(cur);
                }
                s.resetReady();
            } else {
                p.sendMessage("§c거래 칸이 가득 찼습니다.");
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        TradeSession s = session(p);
        if (s == null) return;
        if (e.getInventory().equals(s.inv)){
            // 거래창 드래그 전면 차단 (멀티칸 드래그는 복사버그 원인)
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        TradeSession s = sessions.get(p.getUniqueId());
        if (s == null) return;
        // 한 명이라도 닫으면 취소
        if (!s.finished){
            s.cancel("상대가 거래 창을 닫았습니다.");
        }
    }

    // ====== SESSION ======
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
            ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta m = pane.getItemMeta(); if (m != null){ m.setDisplayName(" "); pane.setItemMeta(m); }

            int[] frame = new int[]{
                    0,1,2,3,4,5,6,7,8,
                    9,13,17,
                    18,22,26,
                    27,31,35,
                    36,37,38,39,40,41,42,43,44,
                    46,47,48,49,50,51,52
            };
            for (int i : frame) inv.setItem(i, pane);

            inv.setItem(aAccept, button(false, "§a내 수락"));
            inv.setItem(bAccept, button(false, "§b상대 수락"));
        }

        ItemStack button(boolean on, String name){
            Material mat = on ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            ItemStack it = new ItemStack(mat);
            ItemMeta im = it.getItemMeta();
            if (im != null){
                im.setDisplayName((on ? "§a[ 수락됨 ] " : "§c[ 대기중 ] ") + name);
                it.setItemMeta(im);
            }
            return it;
        }

        boolean isFrame(int slot){
            return inv.getItem(slot) != null
                    && inv.getItem(slot).getType() == Material.BLACK_STAINED_GLASS_PANE
                    && " ".equals(Optional.ofNullable(inv.getItem(slot).getItemMeta()).map(ItemMeta::getDisplayName).orElse(""));
        }

        int buttonIndexFor(Player p){ return p.equals(a) ? aAccept : bAccept; }

        boolean isMySlot(Player p, int raw){
            int[] arr = p.equals(a) ? aSlots : bSlots;
            for (int s : arr) if (s == raw) return true;
            return false;
        }

        int firstEmptyTradeSlotFor(Player p){
            int[] arr = p.equals(a) ? aSlots : bSlots;
            for (int s : arr) if (inv.getItem(s) == null) return s;
            return -1;
        }

        void resetReady(){
            if (aReady || bReady){
                aReady = bReady = false;
                inv.setItem(aAccept, button(false, "§a내 수락"));
                inv.setItem(bAccept, button(false, "§b상대 수락"));
                try { a.playSound(a.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f); } catch (Throwable ignore){}
                try { b.playSound(b.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.8f); } catch (Throwable ignore){}
            }
        }

        void toggleReady(Player p){
            if (p.equals(a)){ aReady = !aReady; inv.setItem(aAccept, button(aReady, "§a내 수락")); }
            else { bReady = !bReady; inv.setItem(bAccept, button(bReady, "§b상대 수락")); }

            try { p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.2f); } catch (Throwable ignore){}

            if (aReady && bReady){
                complete();
            }
        }

        void complete(){
            if (finished) return;
            finished = true;

            // 아이템 교환
            transfer(a, b, aSlots);
            transfer(b, a, bSlots);

            a.sendMessage("§a거래 완료!");
            b.sendMessage("§a거래 완료!");
            try { a.playSound(a.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f); } catch (Throwable ignore){}
            try { b.playSound(b.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f); } catch (Throwable ignore){}
            forceClose();
        }

        void transfer(Player from, Player to, int[] slots){
            for (int s : slots){
                ItemStack it = inv.getItem(s);
                if (it == null) continue;
                Map<Integer, ItemStack> left = to.getInventory().addItem(it);
                // 남으면 땅에 드랍
                for (ItemStack rem : left.values()){
                    to.getWorld().dropItemNaturally(to.getLocation(), rem);
                }
                inv.setItem(s, null);
            }
        }

        void cancel(String reason){
            if (finished) { return; }
            finished = true;
            // 원복
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

    public void closeAll(){
        for (TradeSession s : new HashSet<>(sessions.values())){
            if (!s.finished) s.cancel("서버 종료");
        }
    }
    public void forceClose(){ closeAll(); }
}
