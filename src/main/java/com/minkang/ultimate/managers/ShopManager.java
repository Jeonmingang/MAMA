package com.minkang.ultimate.managers;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.*;

import java.util.HashMap;
import java.util.Map;

public class ShopManager implements Listener {
    private final Main plugin;
    // Citizens NPC id -> shop key(name)
    private final Map<Integer, String> npcBindings = new HashMap<>();

    public ShopManager(Main plugin) {
        this.plugin = plugin;
        loadBindings();
    }

    /** 바인딩된 상점키 조회 */
    public String getBoundShop(int npcId) {
        return npcBindings.get(npcId);
    }

    /** NPC 바인딩 설정( null 로 해제 ) */
    public void setNpcBinding(int npcId, String shopKey) {
        if (shopKey == null || shopKey.trim().isEmpty()) npcBindings.remove(npcId);
        else npcBindings.put(npcId, shopKey);
        saveBindings();
    }

    /** 플레이어에게 상점 GUI 열기 (기존 커맨드 파이프라인 사용) */
    public void open(Player player, String shopKey) {
        if (player == null || shopKey == null || shopKey.trim().isEmpty()) return;
        try {
            Bukkit.dispatchCommand(player, "상점 열기 " + shopKey);
        } catch (Throwable ignored) {}
    }

    // ===== persistence =====
    private void loadBindings() {
        npcBindings.clear();
        try {
            FileConfiguration cfg = plugin.getConfig();
            ConfigurationSection sec = cfg.getConfigurationSection("npcshop.bindings");
            if (sec != null) {
                for (String k : sec.getKeys(false)) {
                    try { npcBindings.put(Integer.parseInt(k), sec.getString(k)); } catch (Exception ignored) {}
                }
            }
        } catch (Throwable ignored) {}
    }

    private void saveBindings() {
        try {
            FileConfiguration cfg = plugin.getConfig();
            cfg.set("npcshop.bindings", null);
            for (Map.Entry<Integer,String> e : npcBindings.entrySet()) {
                cfg.set("npcshop.bindings." + e.getKey(), e.getValue());
            }
            plugin.saveConfig();
        } catch (Throwable ignored) {}
    }

    // ===== compatibility hooks (optional older impls) =====
    private void saveConfigCompat() {
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("save");
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception ignored) { /* no-op if not present */ }
    }

    /** 외부에서 요청하는 리로드 훅: config 다시 읽고 바인딩 갱신 */
    public void reload(){
        try{
            plugin.reloadConfig();
        }catch(Throwable ignored){}
        loadBindings();
    }


    /** 상점 메타 생성/업데이트 (간단 저장 구현; 기존 로직이 있으면 그쪽이 우선) */
    public void createOrUpdate(String name, boolean allowBuy, boolean allowSell, Double price){
        try{
            FileConfiguration cfg = plugin.getConfig();
            String base = "shops." + name + ".";
            cfg.set(base + "allowBuy", allowBuy);
            cfg.set(base + "allowSell", allowSell);
            if (price != null) cfg.set(base + "price", price);
            plugin.saveConfig();
        }catch(Throwable ignored){}
    }



    /** 전체 바인딩 맵(읽기전용 복사본) */
    public java.util.Map<Integer, String> getAllBindings(){
        return new java.util.HashMap<Integer, String>(npcBindings);
    }



    private static final String TITLE_PREFIX = "상점: ";

    private static class ShopInventoryHolder implements InventoryHolder {
        private final String key;
        ShopInventoryHolder(String key){ this.key = key; }
        @Override public Inventory getInventory(){ return null; }
        public String getKey(){ return key; }
    }

    /** 상점 생성 (빈 GUI) */
    public void createEmpty(String name){
        try{
            FileConfiguration cfg = plugin.getConfig();
            String base = "shops." + name + ".";
            if (!cfg.isConfigurationSection("shops."+name)){
                cfg.createSection("shops."+name);
            }
            if (cfg.getInt(base + "rows", 0) <= 0) cfg.set(base + "rows", 6);
            if (cfg.getConfigurationSection(base + "items")==null){
                cfg.createSection(base + "items");
            }
            plugin.saveConfig();
        }catch(Throwable ignored){}
    }

    /** 손에 든 아이템을 특정 슬롯에 등록 */
    public boolean addItemToSlot(Player p, String name, int slot, boolean allowBuy, boolean allowSell, Double price){
        try{
            ItemStack inHand = p.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType() == Material.AIR) return false;
            FileConfiguration cfg = plugin.getConfig();
            String base = "shops." + name + ".items." + slot + ".";
            cfg.set(base + "item", inHand.clone());
            cfg.set(base + "allowBuy", allowBuy);
            cfg.set(base + "allowSell", allowSell);
            if (price != null) cfg.set(base + "price", price);
            plugin.saveConfig();
            return true;
        }catch(Throwable ignored){}
        return false;
    }

    /** 플레이어에게 상점 GUI 열기 */
    public void open(Player player, String shopKey) {
        if (player == null || shopKey == null || shopKey.trim().isEmpty()) return;
        try {
            FileConfiguration cfg = plugin.getConfig();
            ConfigurationSection shopSec = cfg.getConfigurationSection("shops."+shopKey);
            if (shopSec == null){
                player.sendMessage("§c상점이 존재하지 않습니다: " + shopKey);
                return;
            }
            int rows = shopSec.getInt("rows", 6);
            if (rows < 1) rows = 1; if (rows > 6) rows = 6;
            int size = rows * 9;
            Inventory inv = Bukkit.createInventory(new ShopInventoryHolder(shopKey), size, TITLE_PREFIX + shopKey);

            ConfigurationSection itemsSec = shopSec.getConfigurationSection("items");
            if (itemsSec != null){
                for (String k : itemsSec.getKeys(false)){
                    int slot = -1;
                    try{ slot = Integer.parseInt(k); }catch(Exception ignored){}
                    if (slot < 0 || slot >= size) continue;
                    ItemStack is = itemsSec.getItemStack(k + ".item");
                    if (is != null){
                        // 가격 라벨 붙이기
                        Double price = itemsSec.getDouble(k + ".price", -1);
                        boolean buy = itemsSec.getBoolean(k + ".allowBuy", false);
                        boolean sell = itemsSec.getBoolean(k + ".allowSell", false);
                        ItemStack copy = is.clone();
                        ItemMeta meta = copy.getItemMeta();
                        if (meta != null){
                            List<String> lore = meta.hasLore()? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                            lore.add("§7모드: " + (buy? "구매 " : "") + (sell? "판매" : ""));
                            if (price >= 0) lore.add("§7가격: §e" + price);
                            meta.setLore(lore);
                            copy.setItemMeta(meta);
                        }
                        inv.setItem(slot, copy);
                    }
                }
            }
            player.openInventory(inv);
        } catch (Throwable ignored) {}
    }

    // 인벤토리 클릭 방지 (디스플레이 전용)
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        if (e.getInventory()==null) return;
        InventoryHolder h = e.getInventory().getHolder();
        if (h instanceof ShopInventoryHolder){
            e.setCancelled(true);
        }
    }
}
