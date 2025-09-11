package com.minkang.ultimate.kit;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {
    private final Plugin plugin;
    private final File kitFile;
    private final File claimFile;
    private FileConfiguration kitCfg;
    private FileConfiguration claimCfg;

    public static final String KIT_PATH = "kit.items";
    public static final String CLAIM_PATH = "claimed";

    public KitManager(Plugin p){
        this.plugin = p;
        this.kitFile = new File(p.getDataFolder(), "kits.yml");
        this.claimFile = new File(p.getDataFolder(), "claimed.yml");
        load();
    }

    public void load(){
        try {
            if (!kitFile.exists()) { kitFile.getParentFile().mkdirs(); kitFile.createNewFile(); }
            if (!claimFile.exists()) { claimFile.getParentFile().mkdirs(); claimFile.createNewFile(); }
        } catch (IOException ignored){}
        kitCfg = YamlConfiguration.loadConfiguration(kitFile);
        claimCfg = YamlConfiguration.loadConfiguration(claimFile);
        if (!kitCfg.isList(KIT_PATH)) kitCfg.set(KIT_PATH, new ArrayList<ItemStack>());
        if (!claimCfg.isConfigurationSection(CLAIM_PATH)) claimCfg.createSection(CLAIM_PATH);
        save();
    }

    public void save(){
        try { kitCfg.save(kitFile); } catch (IOException ignored){}
        try { claimCfg.save(claimFile); } catch (IOException ignored){}
    }

    @SuppressWarnings("unchecked")
    public List<ItemStack> getKit(){
        List<ItemStack> list = (List<ItemStack>) kitCfg.getList(KIT_PATH, new ArrayList<ItemStack>());
        if (list == null) list = new ArrayList<>();
        return list;
    }

    public void saveKit(List<ItemStack> items){
        kitCfg.set(KIT_PATH, items);
        save();
    }

    public boolean hasClaimed(UUID uuid){
        return claimCfg.getBoolean(CLAIM_PATH + "." + uuid.toString(), false);
    }

    public void setClaimed(UUID uuid, boolean val){
        claimCfg.set(CLAIM_PATH + "." + uuid.toString(), val);
        save();
    }

    public void resetClaim(UUID uuid){
        claimCfg.set(CLAIM_PATH + "." + uuid.toString(), null);
        save();
    }
}
