
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
    private final Main plugin;
    private final NamespacedKey key;
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

    public ItemStack createToken(int qty){
        ItemStack it = new ItemStack(Material.matchMaterial(plugin.getConfig().getString("lock.item.material","TRIPWIRE_HOOK")));
        it.setAmount(Math.max(1, qty));
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(Texts.color(plugin.getConfig().getString("lock.item.name","&6[잠금권]")));
        java.util.List<String> lore = new java.util.ArrayList<String>();
        for(String s: plugin.getConfig().getStringList("lock.item.lore")) lore.add(Texts.color(s));
        m.setLore(lore);
        m.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
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

        // Only protect certain blocks
        Material t = e.getClickedBlock().getType();
        java.util.List<String> allow = plugin.getConfig().getStringList("lock.protect-blocks");
        if (!allow.contains(t.name())) { e.getPlayer().sendMessage("§c이 블록은 잠금 대상이 아닙니다."); return; }

        // Register lock
        e.setCancelled(true);
        Player p = e.getPlayer();
        String k = keyOf(e.getClickedBlock());
        if (conf.contains("locks."+k)) { p.sendMessage("§c이미 잠금된 블록입니다."); return; }
        long expiresAt = -1L; // default permanent; time-based via command
        conf.set("locks."+k+".owner", p.getUniqueId().toString());
        conf.set("locks."+k+".ownerName", p.getName());
        conf.set("locks."+k+".allowed", new java.util.ArrayList<String>());
        conf.set("locks."+k+".expiresAt", expiresAt);
        save();
        p.sendMessage("§a잠금 완료!");
        // consume token
        if (hand.getAmount()<=1) p.getInventory().setItemInMainHand(null);
        else hand.setAmount(hand.getAmount()-1);
        // place sign above if air
        Block above = e.getClickedBlock().getLocation().add(new Vector(0,1,0)).getBlock();
        if (above.getType()==Material.AIR){
            above.setType(Material.OAK_SIGN);
            if (above.getState() instanceof Sign){
                Sign s = (Sign)above.getState();
                s.setLine(0, "[공용 잠금]");
                s.setLine(1, p.getName());
                s.update();
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        if (e.getClickedBlock()==null) return;
        String k = keyOf(e.getClickedBlock());
        if (!conf.contains("locks."+k)) return;
        if (canAccess(e.getPlayer(), k)) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage("§c잠금된 블록입니다.");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e){
        String k = keyOf(e.getBlock());
        if (!conf.contains("locks."+k)) return;
        if (canAccess(e.getPlayer(), k)) { conf.set("locks."+k, null); save(); e.getPlayer().sendMessage("§7잠금이 제거되었습니다."); return; }
        e.setCancelled(true);
        e.getPlayer().sendMessage("§c잠금된 블록은 파괴할 수 없습니다.");
    }

    private boolean canAccess(Player p, String key){
        String owner = conf.getString("locks."+key+".owner");
        if (owner!=null && owner.equalsIgnoreCase(p.getUniqueId().toString())) return true;
        java.util.List<String> allowed = conf.getStringList("locks."+key+".allowed");
        return allowed.contains(p.getUniqueId().toString());
    }

    private String keyOf(Block b){
        return b.getWorld().getName()+";"+b.getX()+";"+b.getY()+";"+b.getZ();
    }

    public void setExpire(Block b, long millis){
        String k = keyOf(b);
        if (!conf.contains("locks."+k)) return;
        conf.set("locks."+k+".expiresAt", millis);
        save();
    }

    public void addMember(Block b, OfflinePlayer op){
        String k = keyOf(b);
        java.util.List<String> allowed = conf.getStringList("locks."+k+".allowed");
        allowed.add(op.getUniqueId().toString());
        conf.set("locks."+k+".allowed", allowed);
        save();
    }

    public void list(Player p){
        if (!conf.contains("locks")){ p.sendMessage("§7잠근 블록 없음"); return; }
        for (String k : conf.getConfigurationSection("locks").getKeys(false)){
            if (p.getUniqueId().toString().equals(conf.getString("locks."+k+".owner"))){
                long exp = conf.getLong("locks."+k+".expiresAt",-1);
                long left = exp<0?-1:(exp-System.currentTimeMillis());
                p.sendMessage("§e"+k+" §7남은시간: "+(left<0?"영구":(left/3600000)+"시간"));
            }
        }
    }

    private void startExpiryTask(){
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override public void run() {
                if (!conf.contains("locks")) return;
                java.util.Set<String> keys = new java.util.HashSet<String>(conf.getConfigurationSection("locks").getKeys(false));
                long now = System.currentTimeMillis();
                boolean changed=false;
                for (String k: keys){
                    long exp = conf.getLong("locks."+k+".expiresAt",-1L);
                    if (exp>0 && now>exp){ conf.set("locks."+k, null); changed=true; }
                }
                if (changed) save();
            }
        }, 1200L, 1200L); // every minute
    }

    private void save(){
        try { conf.save(file);}catch(IOException e){ e.printStackTrace(); }
    }
}
