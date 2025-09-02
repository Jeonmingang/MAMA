
package com.minkang.usp2.managers;

import com.minkang.usp2.Main;
import com.minkang.usp2.utils.Texts;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LockManager implements Listener {
    private final Main plugin;
    private final NamespacedKey key;
    private final NamespacedKey keyType;
    private final File file;
    private final YamlConfiguration conf;

    public LockManager(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "lock_token");
        this.keyType = new NamespacedKey(plugin, "lock_token_type");
        this.file = new File(plugin.getDataFolder(), "locks.yml");
        this.conf = YamlConfiguration.loadConfiguration(file);
        save();
    }

    public ItemStack createToken(int qty){ return createToken(qty, "perm"); }
    public ItemStack createToken(int qty, String type){
        ItemStack it = new ItemStack(Material.matchMaterial(plugin.getConfig().getString("lock.item.material","TRIPWIRE_HOOK")));
        it.setAmount(Math.max(1, qty));
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(Texts.color(plugin.getConfig().getString("lock.item.name","&6[잠금권]")));
            List<String> lore = new ArrayList<String>();
            for(String s: plugin.getConfig().getStringList("lock.item.lore")) lore.add(Texts.color(s));
            lore.add(Texts.color("&7종류: ")+("perm".equalsIgnoreCase(type)?"&a영구":"&e시간"));
            m.setLore(lore);
            PersistentDataContainer pdc = m.getPersistentDataContainer();
            pdc.set(key, PersistentDataType.INTEGER, 1);
            pdc.set(keyType, PersistentDataType.STRING, ("perm".equalsIgnoreCase(type)?"perm":"time"));
            it.setItemMeta(m);
        }
        return it;
    }

    private String keyOf(Block b){
        World w = b.getWorld();
        return w.getName()+";"+b.getX()+";"+b.getY()+";"+b.getZ();
    }
    private void save(){ try{ conf.save(file); } catch (IOException ignored){} }

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
        String id = op.getUniqueId().toString();
        if (!list.contains(id)){
            list.add(id);
            conf.set("locks."+k+".allowed", list);
            save();
        }
    }
    public void list(Player p){
        int cnt = 0;
        if (conf.isConfigurationSection("locks")){
            for(String k: conf.getConfigurationSection("locks").getKeys(false)){
                String ownerName = conf.getString("locks."+k+".ownerName","unknown");
                long exp = conf.getLong("locks."+k+".expiresAt",-1L);
                p.sendMessage("§7- "+k+" §f소유자: §a"+ownerName+" §7만료: §f"+(exp<0?"영구":String.valueOf((exp-System.currentTimeMillis())/60000)+"분 남음"));
                cnt++;
            }
        }
        p.sendMessage("§7총 §f"+cnt+"§7개");
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
            e.getPlayer().sendMessage("§7시간권은 §f/잠금 시간 <분> §7으로 사용하세요."); return;
        }
        // whitelist
        List<String> whitelist = plugin.getConfig().getStringList("lock.allowed-blocks");
        if (!whitelist.isEmpty() && !whitelist.contains(e.getClickedBlock().getType().name())) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        String k = keyOf(e.getClickedBlock());
        if (conf.contains("locks."+k)) { p.sendMessage("§c이미 잠금된 블록입니다."); return; }
        conf.set("locks."+k+".owner", p.getUniqueId().toString());
        conf.set("locks."+k+".ownerName", p.getName());
        conf.set("locks."+k+".allowed", new ArrayList<String>());
        conf.set("locks."+k+".expiresAt", -1L);
        save();
        p.sendMessage("§a잠금 완료!(영구)");
        if (hand.getAmount()<=1) e.getPlayer().getInventory().setItemInMainHand(null); else hand.setAmount(hand.getAmount()-1);
    }
}
