package com.minkang.usp2.managers;

import com.minkang.usp2.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
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
    private FileConfiguration conf;
    private final Map<UUID, String> current = new HashMap<>();
    private final Map<Integer,String> npcBindings = new HashMap<>();

    public ShopManager(Main plugin){
        this.plugin = plugin;
        reload();
    }

    public void reload(){
        try {
            file = new File(plugin.getDataFolder(), "shops.yml");
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            if (!file.exists()) { file.createNewFile(); conf = new YamlConfiguration(); conf.save(file); }
            conf = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e){
            e.printStackTrace();
            conf = new YamlConfiguration();
        }
        loadBindings();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void save(){
        try { conf.save(file); } catch (IOException ignored) {}
    }

    // ===== Citizens binding =====
    public void bindNpcToShop(int npcId, String shopName){
        npcBindings.put(npcId, shopName);
        FileConfiguration c = plugin.getConfig();
        c.set("citizens_bindings."+npcId, shopName);
        plugin.saveConfig();
    }
    public String getBoundShop(int id){ return npcBindings.get(id); }
    private void loadBindings(){
        FileConfiguration c = plugin.getConfig();
        if (c.isConfigurationSection("citizens_bindings")){
            for (String k : c.getConfigurationSection("citizens_bindings").getKeys(false)){
                try { npcBindings.put(Integer.parseInt(k), c.getString("citizens_bindings."+k)); } catch(Exception ignored){}
            }
        }
    }

    // ===== CRUD =====
    public void createShop(String name){
        conf.set("shops."+name+".title", name);
        save();
    }
    public void removeItem(String name, int slot){
        conf.set("shops."+name+".items."+slot, null); save();
    }
    public void addItem(Player p, String name, int slot, double price){
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand==null || hand.getType()==Material.AIR){ p.sendMessage("§c손에 아이템을 들어주세요."); return; }
        ItemStack templ = hand.clone();
        int amount = Math.max(1, templ.getAmount());
        templ.setAmount(amount);
        conf.set("shops."+name+".items."+slot+".material", templ.getType().name());
        conf.set("shops."+name+".items."+slot+".amount", amount);
        conf.set("shops."+name+".items."+slot+".price", price);
        // Optional: keep display name/lore
        if (templ.hasItemMeta()){
            ItemMeta m = templ.getItemMeta();
            if (m.hasDisplayName()) conf.set("shops."+name+".items."+slot+".name", m.getDisplayName());
            if (m.hasLore()) conf.set("shops."+name+".items."+slot+".lore", m.getLore());
        }
        save();
        p.sendMessage("§a상점 등록 완료: "+name+" @"+slot+" (x"+amount+", $"+price+")");
    }

    public void list(Player p){
        Set<String> shops = conf.getConfigurationSection("shops")!=null ? conf.getConfigurationSection("shops").getKeys(false) : new HashSet<>();
        p.sendMessage("§6상점 목록: §f"+String.join(", ", shops));
    }

    public void open(Player p, String name){
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GREEN + name);
        if (conf.isConfigurationSection("shops."+name+".items")){
            for (String k : conf.getConfigurationSection("shops."+name+".items").getKeys(false)){
                try{
                    int slot = Integer.parseInt(k);
                    String mat = conf.getString("shops."+name+".items."+k+".material","STONE");
                    int amount = conf.getInt("shops."+name+".items."+k+".amount", 1);
                    double price = conf.getDouble("shops."+name+".items."+k+".price", 0);
                    ItemStack it = new ItemStack(Material.matchMaterial(mat), Math.max(1, amount));
                    ItemMeta m = it.getItemMeta();
                    List<String> lore = new ArrayList<>();
                    lore.add("§7구매가: §a$"+price);
                    lore.add("§7좌클릭=구매  /  쉬프트+좌클릭=64개");
                    lore.add("§7우클릭=판매  /  쉬프트+우클릭=64개");
                    if (m!=null){
                        if (conf.isString("shops."+name+".items."+k+".name")) m.setDisplayName(conf.getString("shops."+name+".items."+k+".name"));
                        m.setLore(lore); it.setItemMeta(m);
                    }
                    inv.setItem(slot, it);
                }catch(Exception ignored){}
            }
        }
        current.put(p.getUniqueId(), name);
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e){
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player p = (Player)e.getWhoClicked();
        String shop = current.get(p.getUniqueId());
        if (shop==null) return;
        if (!e.getView().getTitle().equals(ChatColor.DARK_GREEN + shop)) return;
        e.setCancelled(true);

        ItemStack it = e.getCurrentItem();
        if (it==null || it.getType()==Material.AIR) return;
        int slot = e.getSlot();
        if (!conf.isConfigurationSection("shops."+shop+".items."+slot)) return;

        int bundle = conf.getInt("shops."+shop+".items."+slot+".amount", Math.max(1, it.getAmount()));
        double price = conf.getDouble("shops."+shop+".items."+slot+".price", 0);

        ClickType type = e.getClick();
        boolean buy=false, sell=false;
        int bundles = 1;

        if (type == ClickType.LEFT) { buy = true; bundles = 1; }
        else if (type == ClickType.SHIFT_LEFT) { buy = true; bundles = 64; }
        else if (type == ClickType.RIGHT) { sell = true; bundles = 1; }
        else if (type == ClickType.SHIFT_RIGHT) { sell = true; bundles = 64; }
        else return;

        int totalItems = bundle * bundles;

        if (buy){
            double totalPrice = price * bundles;
            if (!plugin.eco().withdraw(p, totalPrice)){ p.sendMessage("§c잔액이 부족합니다."); return; }
            ItemStack give = new ItemStack(it.getType(), totalItems);
            Map<Integer, ItemStack> left = p.getInventory().addItem(give);
            for (ItemStack rem: left.values()) p.getWorld().dropItemNaturally(p.getLocation(), rem);
            p.sendMessage("§a구매: §f"+it.getType().name()+" x"+totalItems+" §7(§a$"+totalPrice+"§7)");
        } else if (sell){
            // Count player's items of this type
            int have = 0;
            for (ItemStack invIt : p.getInventory().getContents()){
                if (invIt!=null && invIt.getType()==it.getType()) have += invIt.getAmount();
            }
            if (have < totalItems){ p.sendMessage("§c판매할 아이템이 부족합니다."); return; }
            // Remove items
            int need = totalItems;
            ItemStack[] contents = p.getInventory().getContents();
            for (int i=0;i<contents.length && need>0;i++){
                ItemStack invIt = contents[i];
                if (invIt==null || invIt.getType()!=it.getType()) continue;
                int take = Math.min(invIt.getAmount(), need);
                invIt.setAmount(invIt.getAmount()-take);
                if (invIt.getAmount()<=0) contents[i] = null;
                need -= take;
            }
            p.getInventory().setContents(contents);
            double totalPrice = price * bundles;
            plugin.eco().deposit(p, totalPrice);
            p.sendMessage("§a판매: §f"+it.getType().name()+" x"+totalItems+" §7(§a$"+totalPrice+"§7 입금)");
        }
    }
}
