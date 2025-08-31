
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
    public void save(){
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

        ItemStack icon = e.getCurrentItem();
        if (icon==null || icon.getType()==Material.AIR) return;

        org.bukkit.event.inventory.ClickType click = e.getClick();
        boolean isLeft = click.isLeftClick();
        boolean isRight = click.isRightClick();
        boolean shift = click.isShiftClick();

        // 유닛 = 아이콘 표기 수량
        int unitAmount = Math.max(1, icon.getAmount());
        int units = shift ? 64 : 1;
        int qty = unitAmount * units; // 실제 아이템 총량

        int slot = e.getSlot();
        String basePath = "shops."+shop+".items."+slot;
        String matn = conf.getString(basePath+".item");
        Material mat = matn==null?icon.getType():Material.matchMaterial(matn);
        if (mat==null) return;

        double buyPricePerUnit = conf.getDouble(basePath+".price", 0D);  // 단위(유닛) 가격
        double sellPricePerUnit = conf.getDouble(basePath+".sell", buyPricePerUnit); // 없으면 구매가와 동일

        if (isLeft) {
            // 구매
            if (buyPricePerUnit <= 0){ p.sendMessage("§c구매 불가"); return; }
            double total = buyPricePerUnit * units; // 유닛 가격 × 유닛 개수
            if (!plugin.eco().withdraw(p, total)){ p.sendMessage("§c잔액 부족"); return; }

            // 지급 (qty 만큼, 스택 분할 자동)
            java.util.HashMap<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(mat, qty));
            for(ItemStack rem : left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rem);

            p.sendMessage("§a구매: §f"+mat.name()+" §fx"+qty+" §7(§a$"+total+"§7)");
            return;
        }

        if (isRight) {
            // 판매
            if (sellPricePerUnit <= 0){ p.sendMessage("§c판매 불가"); return; }
            int owned = 0;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it!=null && it.getType()==mat) owned += it.getAmount();
            }
            // 오프핸드 포함
            ItemStack off = p.getInventory().getItemInOffHand();
            if (off!=null && off.getType()==mat) owned += off.getAmount();

            if (owned < qty) { p.sendMessage("§c판매할 아이템이 부족합니다. 보유: "+owned+"개 / 필요: "+qty+"개"); return; }

            // 정확히 qty 제거
            int toRemove = qty;
            org.bukkit.inventory.PlayerInventory inv = p.getInventory();
            for (int i=0;i<inv.getSize() && toRemove>0;i++){
                ItemStack it = inv.getItem(i);
                if (it==null || it.getType()!=mat) continue;
                int take = Math.min(it.getAmount(), toRemove);
                it.setAmount(it.getAmount()-take);
                if (it.getAmount()<=0) inv.setItem(i, null);
                toRemove -= take;
            }
            if (toRemove>0){
                ItemStack offH = inv.getItemInOffHand();
                if (offH!=null && offH.getType()==mat){
                    int take = Math.min(offH.getAmount(), toRemove);
                    offH.setAmount(offH.getAmount()-take);
                    if (offH.getAmount()<=0) inv.setItemInOffHand(null);
                    toRemove -= take;
                }
            }

            double total = sellPricePerUnit * units; // 유닛 가격 × 유닛 개수
            plugin.eco().deposit(p, total);
            p.sendMessage("§a판매: §f"+mat.name()+" §fx"+qty+" §7(§a+$"+total+"§7)");
            return;
        }
    }
}
