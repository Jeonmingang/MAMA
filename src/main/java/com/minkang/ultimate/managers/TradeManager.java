
package com.minkang.ultimate.managers;

import org.bukkit.inventory.meta.ItemMeta;
import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;

import java.util.*;

public class TradeManager implements Listener {
    private final Main plugin;
    private final Map<UUID, UUID> pending = new HashMap<UUID, UUID>(); // target -> requester
    private final Map<UUID, TradeSession> sessions = new HashMap<UUID, TradeSession>();

    public TradeManager(Main p){ this.plugin=p; }

    public void request(Player from, Player to){
        pending.put(to.getUniqueId(), from.getUniqueId());
        from.sendMessage("§e거래 요청을 보냈습니다: §f"+to.getName());
        to.sendMessage("§e"+from.getName()+"§7 가 거래를 요청했습니다: §a/거래 수락 "+from.getName()+" §7또는 §c/거래 거절 "+from.getName());
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                if (pending.remove(to.getUniqueId())!=null){
                    from.sendMessage("§7거래 요청이 시간초과되었습니다."); to.sendMessage("§7거래 요청이 만료되었습니다.");
                }
            }
        }, 200L); // 10초
    }

    public boolean accept(Player target, Player from){
        UUID req = pending.get(target.getUniqueId());
        if (req==null || !req.equals(from.getUniqueId())) return false;
        pending.remove(target.getUniqueId());
        open(from, target); return true;
    }

    private void open(Player a, Player b){
        TradeSession s = new TradeSession(a,b);
        sessions.put(a.getUniqueId(), s); sessions.put(b.getUniqueId(), s);
        s.open();
    }

    public void closeAll(){
        for (TradeSession s: new HashSet<TradeSession>(sessions.values())) s.forceClose();
        sessions.clear();
        pending.clear();
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        TradeSession s = sessions.get(p.getUniqueId());
        if (s==null || e.getInventory()!=s.inv) return;
        e.setCancelled(true);
        s.handleClick(p, e.getRawSlot(), e.getClick());
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

        TradeSession(Player a, Player b){
            this.a=a; this.b=b;
            this.inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY+"거래: "+a.getName()+" <> "+b.getName());
            aSlots = new int[]{10,11,12,19,20,21,28,29,30};
            bSlots = new int[]{16,15,14,25,24,23,34,33,32};
            drawFrame();
        }

        void drawFrame(){
            ItemStack g = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta m = g.getItemMeta(); m.setDisplayName(" "); g.setItemMeta(m);
            for(int i=0;i<54;i++){
                if (i%9==4 || i>=36) inv.setItem(i, g);
            }
            inv.setItem(aAccept, button(Material.LIME_TERRACOTTA,"§a내 수락"));
            inv.setItem(bAccept, button(Material.LIME_TERRACOTTA,"§a상대 수락"));
        }

        ItemStack button(Material mat, String name){
            ItemStack it = new ItemStack(mat);
            ItemMeta m = it.getItemMeta(); m.setDisplayName(name); it.setItemMeta(m);
            return it;
        }

        void open(){ a.openInventory(inv); b.openInventory(inv); }

        void handleClick(Player p, int raw, ClickType type){
            boolean isA = p.getUniqueId().equals(a.getUniqueId());
            int[] slots = isA? aSlots : bSlots;
            int accept = isA? aAccept : bAccept;

            if (raw==accept){
                if (isA){ aReady=!aReady; inv.setItem(aAccept, button(aReady?Material.GREEN_CONCRETE:Material.LIME_TERRACOTTA, aReady?"§2내 수락 완료":"§a내 수락")); }
                else { bReady=!bReady; inv.setItem(bAccept, button(bReady?Material.GREEN_CONCRETE:Material.LIME_TERRACOTTA, bReady?"§2상대 수락 완료":"§a상대 수락")); }
                a.playSound(a.getLocation(), Sound.UI_BUTTON_CLICK,1,1);
                b.playSound(b.getLocation(), Sound.UI_BUTTON_CLICK,1,1);
                if (aReady && bReady) finalizeTrade();
                return;
            }

            boolean allowed=false;
            for(int s: slots) if (s==raw) allowed=true;
            if (!allowed) return;

            if (aReady){ aReady=false; inv.setItem(aAccept, button(Material.LIME_TERRACOTTA,"§a내 수락")); }
            if (bReady){ bReady=false; inv.setItem(bAccept, button(Material.LIME_TERRACOTTA,"§a상대 수락")); }

            ItemStack cursor = p.getItemOnCursor();
            ItemStack slot = inv.getItem(raw);
            inv.setItem(raw, cursor);
            p.setItemOnCursor(slot);
        }

        void give(Player to, java.util.List<ItemStack> items){
            for(ItemStack it : items){
                java.util.HashMap<Integer, ItemStack> left = to.getInventory().addItem(it);
                for(ItemStack rem : left.values()) to.getWorld().dropItemNaturally(to.getLocation(), rem);
            }
        }

        void finalizeTrade(){
            java.util.List<ItemStack> aItems = new java.util.ArrayList<ItemStack>();
            for(int s:aSlots){ ItemStack it = inv.getItem(s); if (it!=null) { aItems.add(it); inv.setItem(s,null);} }
            java.util.List<ItemStack> bItems = new java.util.ArrayList<ItemStack>();
            for(int s:bSlots){ ItemStack it = inv.getItem(s); if (it!=null) { bItems.add(it); inv.setItem(s,null);} }
            give(b, aItems);
            give(a, bItems);
            a.sendMessage("§a거래 완료!");
            b.sendMessage("§a거래 완료!");
            forceClose();
        }

        void cancel(String reason){
            for(int s:aSlots){ ItemStack it = inv.getItem(s); if (it!=null) a.getInventory().addItem(it); }
            for(int s:bSlots){ ItemStack it = inv.getItem(s); if (it!=null) b.getInventory().addItem(it); }
            a.sendMessage("§c거래 취소: §7"+reason);
            b.sendMessage("§c거래 취소: §7"+reason);
            forceClose();
        }

        void forceClose(){
            sessions.remove(a.getUniqueId());
            sessions.remove(b.getUniqueId());
            try{ a.closeInventory(); }catch(Exception ignored){}
            try{ b.closeInventory(); }catch(Exception ignored){}
        }
    }
}
