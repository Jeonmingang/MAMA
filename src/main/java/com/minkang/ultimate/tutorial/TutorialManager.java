
package com.minkang.ultimate.tutorial;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TutorialManager {
    private final JavaPlugin plugin;
    private File confFile;
    private FileConfiguration conf;
    private File dataFile;
    private FileConfiguration data;

    public TutorialManager(JavaPlugin plugin){
        this.plugin = plugin;
        // load config (tutorial.yml)
        confFile = new File(plugin.getDataFolder(), "tutorial.yml");
        if (!confFile.exists()){
            plugin.saveResource("tutorial.yml", false);
        }
        conf = YamlConfiguration.loadConfiguration(confFile);
        // load data (tutorial_players.yml)
        dataFile = new File(plugin.getDataFolder(), "tutorial_players.yml");
        if (!dataFile.exists()){
            try { dataFile.getParentFile().mkdirs(); dataFile.createNewFile(); }
            catch (IOException ignored){}
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public boolean isEnabled(){ return conf.getBoolean("tutorial.enabled", true); }
    public boolean shouldPromptOnFirstJoin(){ return conf.getBoolean("tutorial.first_join_prompt", true); }

    public boolean isCompleted(UUID u){
        return data.getBoolean(u.toString()+".completed", false);
    }
    public void setCompleted(UUID u, boolean v){
        data.set(u.toString()+".completed", v);
        saveData();
    }
    public boolean isSkipped(UUID u){
        return data.getBoolean(u.toString()+".skipped", false);
    }
    public void setSkipped(UUID u, boolean v){
        data.set(u.toString()+".skipped", v);
        saveData();
    }

    public Inventory buildPromptGui(){
        String title = ChatColor.translateAlternateColorCodes('&', conf.getString("gui.title","&a튜토리얼"));
        Inventory inv = Bukkit.createInventory(null, 27, title);
        ItemStack start = new ItemStack(Material.LIME_DYE);
        ItemMeta sm = start.getItemMeta();
        sm.setDisplayName(ChatColor.GREEN + "튜토리얼 시작");
        List<String> sl = new ArrayList<>();
        sl.add(ChatColor.GRAY + "처음 오신 분들을 위한 안내를 시작합니다.");
        sm.setLore(sl);
        start.setItemMeta(sm);
        inv.setItem(11, start);

        ItemStack skip = new ItemStack(Material.BARRIER);
        ItemMeta sk = skip.getItemMeta();
        sk.setDisplayName(ChatColor.RED + "나중에");
        skip.setItemMeta(sk);
        inv.setItem(15, skip);
        return inv;
    }

    public void startTutorial(Player p){
        // Teleport to tutorial spawn
        String world = conf.getString("spawn.world","world");
        double x = conf.getDouble("spawn.x", 0.5);
        double y = conf.getDouble("spawn.y", 64);
        double z = conf.getDouble("spawn.z", 0.5);
        float yaw = (float) conf.getDouble("spawn.yaw", 0.0);
        float pitch = (float) conf.getDouble("spawn.pitch", 0.0);
        World w = Bukkit.getWorld(world);
        if (w != null){
            p.teleport(new Location(w, x, y, z, yaw, pitch));
        }
        // Steps
        List<String> steps = conf.getStringList("steps");
        long delay = 0L;
        for (String s : steps){
            String msg = ChatColor.translateAlternateColorCodes('&', s);
            Bukkit.getScheduler().runTaskLater(plugin, ()-> p.sendMessage(msg), delay);
            delay += 40L; // 2s per step
        }
        // Rewards (console commands)
        List<String> cmds = conf.getStringList("rewards.commands");
        if (cmds != null && !cmds.isEmpty()){
            Bukkit.getScheduler().runTaskLater(plugin, ()->{
                for (String c : cmds){
                    String cmd = c.replace("{player}", p.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }, 20L);
        }
        setCompleted(p.getUniqueId(), true);
    }

    public void saveData(){
        try { data.save(dataFile); }
        catch (IOException e){ plugin.getLogger().warning("tutorial_players.yml 저장 실패: " + e.getMessage()); }
    }
}
