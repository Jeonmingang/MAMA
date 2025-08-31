package com.minkang.usp2.managers;

import com.minkang.usp2.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
    private final Map<UUID, TradeSession> sessions = new HashMap<>();

    public TradeManager(Main plugin){
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void request(Player a, Player b){
        // 간단: 즉시 세션 생성 (요청/수락 절차는 별도 구현 가능)
        start(a, b);
    }

    private void start(Player a, Player b){
        TradeSession s = new TradeSession(a, b);
        sessions.put(a.getUniqueId(), s);
        sessions.put(b.getUniqueId(), s);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        TradeSession s = by(e.getWhoClicked().getUniqueId());
        if (s==null) return;
        if (!e.getView().getTopInventory().equals(s.inv)) return;

        e.setCancelled(true);
        Player p = (Player)e.getWhoClicked();

        // 준비 버튼 클릭
        if (e.getClickedInventory() == s.inv){
            int raw = e.getRawSlot();
            if (raw == s.buttonIndexFor(p)){
                s.toggleReady(p);
                return;
            }
        }

        // 아이템 이동 처리: 좌클릭으로 자신의 슬롯으로 올리기
        if (e.getClickedInventory() != s.inv && e.getClick()== ClickType.LEFT){
            int dst = s.firstEmptySlotFor(p);
            if (dst < 0){ p.sendMessage("§c자신의 거래 슬롯이 가득 찼습니다."); return; }
            ItemStack cursor = e.getCurrentItem();
            if (cursor != null && cursor.getType()!=Material.AIR){
                s.inv.setItem(dst, cursor.clone());
                e.getView().getBottomInventory().setItem(e.getSlot(), null);
                s.resetReady();
            }
        }

        // 양측 준비 시 완료
        s.checkBothReady();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e){
        TradeSession s = by(e.getPlayer().getUniqueId());
        if (s==null) return;
        if (!e.getInventory().equals(s.inv)) return;
        s.cancel("창 닫힘");
    }

    private TradeSession by(UUID id){ return sessions.get(id); }

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
        int[] slotsFor(Player p){ return p.getUniqueId().equals(a.getUniqueId())? aSlots : bSlots; }

        int firstEmptySlotFor(Player p){
            for (int idx : slotsFor(p)){
                ItemStack it = inv.getItem(idx);
                if (it==null || it.getType()==Material.AIR) return idx;
            }
            return -1;
        }

        void toggleReady(Player p){
            if (p.getUniqueId().equals(a.getUniqueId())) aReady = !aReady;
            else if (p.getUniqueId().equals(b.getUniqueId())) bReady = !bReady;
            updateButtons();
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
            // move items
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
                    Map<Integer, ItemStack> overflow = pl.getInventory().addItem(it.clone());
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
