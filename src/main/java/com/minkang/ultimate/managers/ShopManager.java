package com.minkang.ultimate.managers;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

public class ShopManager implements Listener {
    private static ShopManager instance;
    public static ShopManager getInstance(){ return instance; }
    public static void setInstance(ShopManager m){ instance = m; }

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


    /** NPC ↔ 상점 연동 목록 반환 (config: npcshop.bindings) */
    public Map<Integer, String> getAllBindings(){
        HashMap<Integer, String> map = new HashMap<>();
        try{
            ConfigurationSection sec = plugin.getConfig().getConfigurationSection("npcshop.bindings");
            if (sec != null){
                for (String k : sec.getKeys(false)){
                    try{
                        int id = Integer.parseInt(k);
                        String shop = sec.getString(k);
                        if (shop != null && !shop.trim().isEmpty()){
                            map.put(id, shop);
                        }
                    }catch (Exception ignored){}
                }
            }
        }catch (Throwable ignored){}
        return map;
    }

    /** NPC ↔ 상점 연동 저장/해제 */
    

}
