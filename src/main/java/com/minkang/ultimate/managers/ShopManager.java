
package com.minkang.ultimate.managers;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ShopManager implements Listener {
    private final Main plugin;
    private File file;
    private YamlConfiguration conf;
    private final Map<UUID, String> current = new HashMap<UUID, String>();

    public ShopManager(Main p){
        this.plugin=p;
        reload();
    }
    public void reload(){
        file = new File(plugin.getDataFolder(), "shops.yml");
        if (!file.exists()) plugin.saveResource("shops.yml", false);
        conf = YamlConfiguration.loadConfiguration(file);
    }
    private void save(){
        try { conf.save(file);}catch(IOException e){ e.printStackTrace(); }
    }

    public void createShop(String name){
        if (!conf.contains("shops."+name)){
            conf.createSection("shops."+name+".items");
            save();
        }
    }

    public void addItem(Player p, String shop, int slot, double price){
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand==null || hand.getType()==Material.AIR){ p.sendMessage("§c손에 아이템을 들어주세요."); return; }
        int amount = hand.getAmount();
        conf.set("shops."+shop+".items."+slot+".item", hand.getType().name());
        conf.set("shops."+shop+".items."+slot+".price", price);
        conf.set("shops."+shop+".items."+slot+".amount", amount);
        save();
        p.sendMessage("§a상점 등록: §f"+shop+" §7슬롯 "+slot+" §7아이템 "+hand.getType().name()+" §7가격 "+price+" §7수량 "+amount);
    }

    public void removeItem(String shop, int slot){
        conf.set("shops."+shop+".items."+slot, null); save();
    }

    public void open(Player p, String shop){
        if (!conf.contains("shops."+shop)){ p.sendMessage("§c해당 상점이 없습니다."); return; }
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN+shop);
        if (conf.getConfigurationSection("shops."+shop+".items")!=null){
            for (String key : conf.getConfigurationSection("shops."+shop+".items").getKeys(false)){
                int slot = Integer.parseInt(key);
                String matn = conf.getString("shops."+shop+".items."+key+".item");
                double price = conf.getDouble("shops."+shop+".items."+key+".price");
                int amt = conf.getInt("shops."+shop+".items."+key+".amount");
                Material mat = Material.matchMaterial(matn);
                if (mat==null) continue;
                ItemStack it = new ItemStack(mat, Math.max(1, amt));
                ItemMeta m = it.getItemMeta();
                java.util.List<String> lore = new java.util.ArrayList<String>();
                lore.add("§c구매가: $"+price);
                lore.add("§7좌클릭=구매 / 쉬프트+좌클릭=64개");
                m.setLore(lore); it.setItemMeta(m);
                inv.setItem(slot, it);
            }
        }
        p.openInventory(inv);
        current.put(p.getUniqueId(), shop);
    }

    public void list(Player p){
        if (!conf.contains("shops")){ p.sendMessage("§7상점 없음"); return; }
        Set<String> names = conf.getConfigurationSection("shops").getKeys(false);
        p.sendMessage("§6[상점 목록] §f"+String.join(", ", names));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        String shop = current.get(p.getUniqueId());
        if (shop==null) return;
        String title = e.getView().getTitle();
        if (!title.equals(org.bukkit.ChatColor.DARK_GREEN+shop)) return;
        e.setCancelled(true);
        ItemStack it = e.getCurrentItem(); if (it==null || !it.hasItemMeta()) return;
        Material mat = it.getType();
        boolean shift = e.isShiftClick();
        int amount = shift?64:1;
        double priceEach = conf.getDouble("shops."+shop+".items."+e.getSlot()+".price", 0);
        if (priceEach<=0){ p.sendMessage("§c구매 불가"); return; }
        double total = priceEach * amount;
        if (!plugin.eco().withdraw(p, total)){ p.sendMessage("§c잔액 부족"); return; }
        java.util.HashMap<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(mat, amount));
        for(ItemStack rem : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rem);
        p.sendMessage("§a구매: §f"+mat.name()+" x"+amount+" §7(§a$"+total+"§7)");
    }
}
