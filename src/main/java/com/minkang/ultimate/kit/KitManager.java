package com.minkang.ultimate.kit;
import org.bukkit.Bukkit; import org.bukkit.configuration.file.FileConfiguration; import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player; import org.bukkit.inventory.Inventory; import org.bukkit.inventory.ItemStack;
import java.io.File; import java.io.IOException; import java.util.*;
public class KitManager {
    private static KitManager instance; public static KitManager getInstance(){ if(instance==null) instance=new KitManager(); return instance; }
    private File kitFile; private FileConfiguration kitCfg; private final String GUI_TITLE="기본템 설정";
    public void init(File dataFolder){ kitFile=new File(dataFolder,"kits.yml"); if(!kitFile.exists()){ try{ kitFile.createNewFile(); }catch(IOException ignored){} } kitCfg=YamlConfiguration.loadConfiguration(kitFile); if(!kitCfg.isConfigurationSection("kit")) kitCfg.createSection("kit"); save(); }
    public void save(){ try{ kitCfg.save(kitFile); }catch(IOException ignored){} }
    public String getGuiTitle(){ return GUI_TITLE; }
    public void openSetupGUI(Player p){ Inventory inv=Bukkit.createInventory(null,27,GUI_TITLE); java.util.List<ItemStack> items=getSavedItems(); for(int i=0;i<items.size()&&i<27;i++){ inv.setItem(i, items.get(i)); } p.openInventory(inv); }
    @SuppressWarnings("unchecked") public java.util.List<ItemStack> getSavedItems(){ java.util.List<ItemStack> list=(java.util.List<ItemStack>)kitCfg.getList("kit.items"); if(list==null) list=new java.util.ArrayList<ItemStack>(); return list; }
    public void saveItemsFromGUI(Inventory inv){ java.util.List<ItemStack> items=new java.util.ArrayList<ItemStack>(); for(int i=0;i<inv.getSize();i++){ ItemStack it=inv.getItem(i); if(it!=null) items.add(it); } kitCfg.set("kit.items", items); save(); }
    public boolean hasClaimed(Player p){ java.util.List<String> claimed=kitCfg.getStringList("claimed"); return claimed.contains(p.getUniqueId().toString()); }
    public void setClaimed(Player p, boolean v){ java.util.List<String> claimed=kitCfg.getStringList("claimed"); if(v){ if(!claimed.contains(p.getUniqueId().toString())) claimed.add(p.getUniqueId().toString()); } else { claimed.remove(p.getUniqueId().toString()); } kitCfg.set("claimed", claimed); save(); }
    public int giveKit(Player p){ java.util.List<ItemStack> items=getSavedItems(); if(items.isEmpty()) return 0; int c=0; for(ItemStack it:items){ if(it!=null){ p.getInventory().addItem(it.clone()); c++; } } return c; }
    public boolean resetPlayer(String name){ Player online=Bukkit.getPlayerExact(name); if(online!=null){ setClaimed(online,false); return true; } return false; }
}
