
package com.minkang.ultimate.managers;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ShopManager implements Listener {
    private final Main plugin;
    // shopName -> GUI
    private final Map<String, Inventory> shops = new ConcurrentHashMap<>();
    // Citizens NPC id -> shopName
    private final Map<Integer, String> npcBindings = new ConcurrentHashMap<>();

    public ShopManager(Main plugin) {
        this.plugin = plugin;
    }

    // --- Basic API used by other parts ---
    public void reload() {
        // no-op placeholder; extend to read from file if you use persistence
    }

    public void save() {
        // no-op placeholder; extend to write to file if you use persistence
    }

    /** create or update a shop stub (simple GUI) */
    public void createOrUpdate(String name, boolean buy, boolean sell, Double price) {
        String title = ChatColor.DARK_GREEN + "상점: " + name;
        Inventory inv = shops.get(name);
        if (inv == null) inv = Bukkit.createInventory(null, 54, title);
        // Put a sample item into slot 22 to ensure non-empty GUI
        ItemStack it = new ItemStack(Material.EMERALD);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(ChatColor.GOLD + name);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "구매 가능: " + (buy? "예":"아니오"));
            lore.add(ChatColor.YELLOW + "판매 가능: " + (sell? "예":"아니오"));
            if (price != null) lore.add(ChatColor.AQUA + "기본 가격: " + price);
            m.setLore(lore);
            it.setItemMeta(m);
        }
        inv.setItem(22, it);
        shops.put(name, inv);
    }

    public void open(Player player, String shopName) {
        Inventory inv = shops.get(shopName);
        if (inv == null) {
            player.sendMessage(ChatColor.RED + "상점이 존재하지 않습니다: " + shopName);
            return;
        }
        player.openInventory(inv);
    }

    public String getBoundShop(int npcId) {
        return npcBindings.get(npcId);
    }

    public void bindNpc(int npcId, String shopName) {
        if (!shops.containsKey(shopName)) return;
        npcBindings.put(npcId, shopName);
    }

    // Utility for commands that need list
    public Set<String> listNames() { return new TreeSet<>(shops.keySet()); }
}
