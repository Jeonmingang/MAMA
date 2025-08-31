
package com.minkang.ultimate.managers;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.utils.Texts;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
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
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LockManager implements Listener {
    private java.util.UUID safeUUID(Object o){
        try {
            if (o==null) return null;
            String s = String.valueOf(o).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
            return java.util.safeUUID(s);
        } catch (Exception ex){ return null; }
    }

    private final Main plugin;
    private final NamespacedKey key; private final NamespacedKey keyType;
    private final File file;
    private final YamlConfiguration conf;

    public LockManager(Main p){
        this.plugin=p;
        this.key = new NamespacedKey(p, "lock_token");
        this.file = new File(p.getDataFolder(), "locks.yml");
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); } catch(Exception ignored){}
        }
        this.conf = YamlConfiguration.loadConfiguration(file);
        ensureItemTemplate();
        startExpiryTask();
    }

    private void ensureItemTemplate(){}

    public ItemStack createToken(int qty){ return createToken(qty, "perm"); }
    public ItemStack createToken(int qty, String type){
        ItemStack it = new ItemStack(Material.matchMaterial(plugin.getConfig().getString("lock.item.material","TRIPWIRE_HOOK")));
        it.setAmount(Math.max(1, qty));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(Texts.color(plugin.getConfig().getString("lock.item.name","&6[잠금권]")));
        java.util.List<String> lore = new java.util.ArrayList<String>();
        for(String s: plugin.getConfig().getStringList("lock.item.lore")) lore.add(Texts.color(s));
        lore.add(Texts.color("&7종류: ")+("perm".equalsIgnoreCase(type)?"&a영구":"&e시간"));
        m.setLore(lore);
        m.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
        m.getPersistentDataContainer().set(keyType, PersistentDataType.STRING, ("perm".equalsIgnoreCase(type)?"perm":"time"));
        it.setItemMeta(m);
        return it;
    }

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
            e.getPlayer().sendMessage("§7시간권은 §f/잠금 시간 <분|시간> §7으로 사용하세요."); return;
        }
        // Only protect certain blocks
        Material type = e.getClickedBlock().getType();
        java.util.List<String> whitelist = plugin.getConfig().getStringList("lock.allowed-blocks");
        if (!whitelist.isEmpty() && !whitelist.contains(type.name())) return;
        // Register lock permanent
        e.setCancelled(true);
        Player p = e.getPlayer();
        String k = keyOf(e.getClickedBlock());
        if (conf.contains("locks."+k)) { p.sendMessage("§c이미 잠금된 블록입니다."); return; }
        long expiresAt = -1L;
        conf.set("locks."+k+".owner", p.getUniqueId().toString());
        conf.set("locks."+k+".ownerName", p.getName());
        conf.set("locks."+k+".allowed", new java.util.ArrayList<String>());
        conf.set("locks."+k+".expiresAt", expiresAt);
        save();
        p.sendMessage("§a잠금 완료!(영구)");
        // consume token
        if (hand.getAmount()<=1) e.getPlayer().getInventory().setItemInMainHand(null); else hand.setAmount(hand.getAmount()-1);
    }
