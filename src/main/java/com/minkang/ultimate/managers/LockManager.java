package com.minkang.ultimate.managers;

import com.minkang.ultimate.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LockManager implements Listener {
    private final Main plugin;
    private final NamespacedKey key;
    private final NamespacedKey keyType;

    private File file;
    private FileConfiguration conf;

    public LockManager(Main plugin){
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "lock_token");
        this.keyType = new NamespacedKey(plugin, "lock_token_type");
        reload();
    }

    public void reload(){
        try{
            file = new File(plugin.getDataFolder(), "locks.yml");
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            if (!file.exists()) file.createNewFile();
            conf = YamlConfiguration.loadConfiguration(file);
        }catch (Exception e){
            conf = new YamlConfiguration();
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void save(){
        try{ conf.save(file);}catch(IOException ignored){}
    }

    // ======= Token creation =======
    public ItemStack createToken(int qty){ return createToken(qty, "perm"); }

    public ItemStack createToken(int qty, String type){
        ItemStack it = new ItemStack(Material.TRIPWIRE_HOOK, Math.max(1, qty));
        ItemMeta m = it.getItemMeta();
        if (m != null){
            m.setDisplayName("§6[잠금권]");
            List<String> lore = new ArrayList<>();
            lore.add("§7종류: "+("perm".equalsIgnoreCase(type)?"§a영구":"§e시간"));
            m.setLore(lore);
            PersistentDataContainer pdc = m.getPersistentDataContainer();
            pdc.set(key, PersistentDataType.INTEGER, 1);
            pdc.set(keyType, PersistentDataType.STRING, ("perm".equalsIgnoreCase(type)?"perm":"time"));
            it.setItemMeta(m);
        }
        return it;
    }

    // ======= Helpers =======
    private String keyOf(Block b){
        return b.getWorld().getName()+","+b.getX()+","+b.getY()+","+b.getZ();
    }
    private UUID safeUUID(Object o){
        try {
            if (o==null) return null;
            String s = String.valueOf(o).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
            return UUID.fromString(s);
        } catch (Exception ex){ return null; }
    }

    // ======= Public operations used by commands =======
    public void protectPerm(Block b, Player p){
        String k = keyOf(b);
        if (conf.contains("locks."+k)) { p.sendMessage("§c이미 잠금된 블록입니다."); return; }
        conf.set("locks."+k+".owner", p.getUniqueId().toString());
        conf.set("locks."+k+".ownerName", p.getName());
        conf.set("locks."+k+".allowed", new ArrayList<String>());
        conf.set("locks."+k+".expiresAt", -1L);
        save();
    }

    public void protectTime(Block b, Player p, long mins){
        String k = keyOf(b);
        if (conf.contains("locks."+k)) { p.sendMessage("§c이미 잠금된 블록입니다."); return; }
        long expiresAt = System.currentTimeMillis() + Math.max(1, mins)*60_000L;
        conf.set("locks."+k+".owner", p.getUniqueId().toString());
        conf.set("locks."+k+".ownerName", p.getName());
        conf.set("locks."+k+".allowed", new ArrayList<String>());
        conf.set("locks."+k+".expiresAt", expiresAt);
        save();
    }

    public void addMember(Block b, OfflinePlayer op){
        String k = keyOf(b);
        List<String> list = conf.getStringList("locks."+k+".allowed");
        if (!list.contains(op.getUniqueId().toString())){
            list.add(op.getUniqueId().toString());
            conf.set("locks."+k+".allowed", list);
            save();
        }
    }

    public void list(Player p){
        // Simple listing: total count
        int count = 0;
        if (conf.isConfigurationSection("locks")){
            count = conf.getConfigurationSection("locks").getKeys(false).size();
        }
        p.sendMessage("§7등록된 잠금 수: §a"+count);
    }

    // ======= Events =======
    @EventHandler
    public void onUse(PlayerInteractEvent e){
        if (e.getHand()!=EquipmentSlot.HAND) return;
        if (e.getClickedBlock()==null) return;
        ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
        if (hand==null || !hand.hasItemMeta()) return;
        PersistentDataContainer pdc = hand.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(key, PersistentDataType.INTEGER)) return;
        String t = pdc.getOrDefault(keyType, PersistentDataType.STRING, "perm");
        if (!"perm".equalsIgnoreCase(t)){
            e.getPlayer().sendMessage("§7시간권은 §f/잠금 시간 <분> §7으로 사용하세요.");
            return;
        }
        e.setCancelled(true);
        protectPerm(e.getClickedBlock(), e.getPlayer());
        // consume
        if (hand.getAmount()<=1) e.getPlayer().getInventory().setItemInMainHand(null);
        else hand.setAmount(hand.getAmount()-1);
        e.getPlayer().sendMessage("§a잠금 완료!(영구)");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        String k = keyOf(e.getBlock());
        if (!conf.contains("locks."+k)) return;
        String owner = conf.getString("locks."+k+".owner");
        long exp = conf.getLong("locks."+k+".expiresAt", -1L);
        if (exp > 0 && System.currentTimeMillis() > exp){
            // expired
            conf.set("locks."+k, null); save();
            return;
        }
        if (owner==null) return;
        if (!e.getPlayer().getUniqueId().toString().equals(owner) && !e.getPlayer().hasPermission("usp.lock.admin")){
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c이 블록은 잠금되어 있습니다.");
        } else {
            conf.set("locks."+k, null); save();
            e.getPlayer().sendMessage("§7잠금 해제됨.");
        }
    }
}
