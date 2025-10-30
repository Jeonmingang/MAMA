package com.minkang.ultimate.listeners;

import com.minkang.ultimate.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;

public class ShopGuiListener implements Listener {

    private final Main plugin;
    private Economy econ;

    public ShopGuiListener(Main plugin){
        this.plugin = plugin;
        setupVault();
    }

    private void setupVault(){
        try{
            RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) econ = rsp.getProvider();
        }catch(Throwable ignored){}
    }

    private boolean isShopInv(org.bukkit.event.inventory.InventoryClickEvent e){
        String title = e.getView().getTitle();
        return title != null && title.startsWith("상점: ");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent e){
        Inventory inv = e.getInventory();
        if (!isShopInv(e)) return;

        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();

        String title = e.getView().getTitle(); // "상점: <키>"
        String key = title.substring("상점: ".length()).trim();

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        int slot = e.getRawSlot();
        // Only consider top inventory slots
        if (slot >= inv.getSize()) return;

        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection itemSec = cfg.getConfigurationSection("shops."+key+".items."+slot);
        if (itemSec == null){
            p.sendMessage(ChatColor.RED + "이 슬롯은 판매 항목이 아닙니다.");
            return;
        }

        int unit = itemSec.getInt("amount", 1);
        boolean buyEnabled = itemSec.getBoolean("buy.enabled", false);
        boolean sellEnabled = itemSec.getBoolean("sell.enabled", false);
        double buyPrice = itemSec.getDouble("buy.price", 0.0);
        double sellPrice = itemSec.getDouble("sell.price", 0.0);

        ClickType type = e.getClick();
        boolean shift = type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT;
        boolean left = type == ClickType.LEFT || type == ClickType.SHIFT_LEFT;
        boolean right = type == ClickType.RIGHT || type == ClickType.SHIFT_RIGHT;

        int qty = shift ? 64 : unit;
        if (qty <= 0) qty = 1;

        if (left){ // BUY
    if (!buyEnabled){ p.sendMessage(ChatColor.RED + "구매가 비활성화된 항목입니다."); return; }
    double total = (buyPrice / unit) * qty;

    if (econ == null){ p.sendMessage(ChatColor.RED + "Vault 연동이 없습니다."); return; }
    if (econ.getBalance(p) < total){ p.sendMessage(ChatColor.RED + "잔액이 부족합니다. 필요: " + total); return; }

    // Build a clean delivery item: use original item from config, not the GUI-decorated copy
    ItemStack base = itemSec.getItemStack("item");
    ItemStack give;
    if (base != null) {
        give = base.clone();
    } else {
        // Fallback: strip GUI hint lores from the clicked item
        give = clicked.clone();
        if (give.hasItemMeta()) {
            ItemMeta _m = give.getItemMeta();
            if (_m != null && _m.hasLore()) {
                java.util.List<String> _l = new java.util.ArrayList<>(_m.getLore());
                java.util.Iterator<String> it = _l.iterator();
                while (it.hasNext()) {
                    String line = ChatColor.stripColor(it.next());
                    if (line == null) continue;
                    line = line.toLowerCase();
                    if (line.contains("좌클릭") || line.contains("우클릭") || line.contains("쉬프트") ||
                        line.contains("구매가") || line.contains("판매가") || line.contains("단위수량") ||
                        line.contains("mode") || line.contains("unit") || line.contains("buy") || line.contains("sell")) {
                        it.remove();
                    }
                }
                _m.setLore(_l);
                give.setItemMeta(_m);
            }
        }
    }
    give.setAmount(qty);
    if (p.getInventory().firstEmpty() == -1){
        p.sendMessage(ChatColor.RED + "인벤토리에 공간이 부족합니다.");
        return;
    }
    econ.withdrawPlayer(p, total);
    p.getInventory().addItem(give);
    p.sendMessage(ChatColor.GREEN + "" + qty + "개 구매 ( -" + total + " )");
    return;
}

        if (right){ // SELL
            if (!sellEnabled){ p.sendMessage(ChatColor.RED + "판매가 비활성화된 항목입니다."); return; }
            // 판매는 '아이템 이름과 로어'가 모두 같은 아이템만 인정
            ItemStack baseForSell = itemSec.getItemStack("item");
            if (baseForSell != null){
                int have = countItems(p.getInventory(), baseForSell);
                if (shift) { qty = have; }
                if (have < qty){ p.sendMessage(ChatColor.RED + "판매 수량이 부족합니다. 보유: " + have); return; }
                removeItems(p.getInventory(), baseForSell, qty);
                double total = (sellPrice / unit) * qty;
                if (econ == null){ p.sendMessage(ChatColor.RED + "Vault 연동이 없습니다."); return; }
                econ.depositPlayer(p, total);
                p.sendMessage(ChatColor.GREEN + "" + qty + "개 판매 ( +" + total + " )");
                return;
            } else {
                // 안전장치: 설정에 원본 아이템이 없으면 기존 동작(동일 재질)으로 처리
                Material mat = clicked.getType();
                int have = countItems(p.getInventory(), mat);
                if (shift) { qty = have; }
                if (have < qty){ p.sendMessage(ChatColor.RED + "판매 수량이 부족합니다. 보유: " + have); return; }
                removeItems(p.getInventory(), mat, qty);
                double total = (sellPrice / unit) * qty;
                if (econ == null){ p.sendMessage(ChatColor.RED + "Vault 연동이 없습니다."); return; }
                econ.depositPlayer(p, total);
                p.sendMessage(ChatColor.GREEN + "" + qty + "개 판매 ( +" + total + " )");
                return;
            }
        }
    }

    
    private boolean matchesForSell(org.bukkit.inventory.ItemStack invItem, org.bukkit.inventory.ItemStack expected){
        if (invItem == null || expected == null) return false;
        if (invItem.getType() != expected.getType()) return false;
        org.bukkit.inventory.meta.ItemMeta a = invItem.getItemMeta();
        org.bukkit.inventory.meta.ItemMeta b = expected.getItemMeta();
        String an = (a != null && a.hasDisplayName()) ? a.getDisplayName() : null;
        String bn = (b != null && b.hasDisplayName()) ? b.getDisplayName() : null;
        java.util.List<String> al = (a != null && a.hasLore()) ? a.getLore() : null;
        java.util.List<String> bl = (b != null && b.hasLore()) ? b.getLore() : null;
        // 이름과 로어가 모두 동일해야만 판매 허용
        return java.util.Objects.equals(an, bn) && java.util.Objects.equals(al, bl);
    }
    private int countItems(org.bukkit.inventory.PlayerInventory inv, org.bukkit.inventory.ItemStack expected){
        int c = 0;
        for (org.bukkit.inventory.ItemStack it : inv.getContents()){
            if (matchesForSell(it, expected)) c += (it == null ? 0 : it.getAmount());
        }
        return c;
    }
    private void removeItems(org.bukkit.inventory.PlayerInventory inv, org.bukkit.inventory.ItemStack expected, int qty){
        for (int i=0; i<inv.getSize() && qty>0; i++){
            org.bukkit.inventory.ItemStack it = inv.getItem(i);
            if (matchesForSell(it, expected)){
                int take = Math.min(qty, it.getAmount());
                it.setAmount(it.getAmount() - take);
                if (it.getAmount() <= 0) inv.setItem(i, null);
                qty -= take;
            }
        }
    }
private int countItems(PlayerInventory inv, Material mat){
        int c = 0;
        for (ItemStack it : inv.getContents()){
            if (it != null && it.getType() == mat) c += it.getAmount();
        }
        return c;
    }
    private void removeItems(PlayerInventory inv, Material mat, int qty){
        for (int i=0; i<inv.getSize() && qty>0; i++){
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != mat) continue;
            int take = Math.min(qty, it.getAmount());
            it.setAmount(it.getAmount()-take);
            if (it.getAmount() <= 0) inv.setItem(i, null);
            qty -= take;
        }
    }
}