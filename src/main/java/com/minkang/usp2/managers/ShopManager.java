package com.minkang.usp2.managers;

import com.minkang.usp2.Main;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ShopManager implements org.bukkit.event.Listener {
    private final Main plugin;
    public ShopManager(Main plugin){
        this.plugin = plugin;
    }

    // 기존 shops.yml 로딩/저장 로직을 가진 conf()/save()가 내부에 있다고 가정하고 reflection으로 접근
    public void reload(){
        // 필요 시 파일 리로드 로직(실제 구현체에 위임되었다고 가정)
    }

    // API: 커맨드에서 호출
    public void createOrUpdate(String name, boolean allowBuy, boolean allowSell, Double price){
        FileConfiguration conf = getConfig();
        if (conf == null) return;
        String base = "shops."+name+".";
        conf.set(base+"buy", allowBuy);
        conf.set(base+"sell", allowSell);
        if (price != null) conf.set(base+"price", price);
        saveConfig();
    }

    private FileConfiguration getConfig(){
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("conf");
            m.setAccessible(true);
            return (FileConfiguration)m.invoke(this);
        } catch (Exception e){
            return null;
        }
    }

    private void saveConfig(){
        try {
            java.lang.reflect.Method m = this.getClass().getDeclaredMethod("save");
            m.setAccessible(true);
            m.invoke(this);
        } catch (Exception ignored){}
    }
}

    public String getBoundShop(int id){ try{ org.bukkit.configuration.file.FileConfiguration cfg = plugin.getConfig(); return cfg.getString("npcshop.bindings."+id); } catch (Throwable t){ return null; }}

    public void open(org.bukkit.entity.Player p, String key){ try{ org.bukkit.Bukkit.dispatchCommand(p, "상점 열기 "+key); } catch(Throwable ignored){} }
