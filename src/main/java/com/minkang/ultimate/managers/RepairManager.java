
package com.minkang.ultimate.managers;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.utils.Texts;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class RepairManager implements Listener {
    private final Main plugin;
    private final NamespacedKey key;
    private ItemStack template;

    public RepairManager(Main p){
        this.plugin=p;
        this.key = new NamespacedKey(p, "repair_ticket");
        reload();
    }

    public void reload(){
        FileConfiguration c = plugin.getConfig();
        Material m = Material.matchMaterial(c.getString("repairticket.material","NAME_TAG"));
        template = new ItemStack(m==null?Material.NAME_TAG:m);
    }

    public ItemStack create(int qty){
        FileConfiguration c = plugin.getConfig();
        ItemStack it = template.clone();
        it.setAmount(Math.max(1, qty));
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Texts.color(c.getString("repairticket.name","&b[수리권]")));
        List<String> lore = new ArrayList<String>();
        for(String s: c.getStringList("repairticket.lore")) lore.add(Texts.color(s));
        meta.setLore(lore);
        if (c.getBoolean("repairticket.glow", true)) meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e){
        ItemStack hand = e.getItem();
        if (hand==null || !hand.hasItemMeta()) return;
        ItemMeta meta = hand.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(key, PersistentDataType.INTEGER)) return;
        e.setCancelled(true);

        Player p = e.getPlayer();
        ItemStack target = p.getInventory().getItemInMainHand();
        if (target==null || !target.hasItemMeta() || !(target.getItemMeta() instanceof Damageable)) {
            p.sendMessage("§c손에 든 수리 가능한 아이템이 필요합니다.");
            return;
        }
        Damageable dmg = (Damageable) target.getItemMeta();
        dmg.setDamage(0);
        target.setItemMeta(dmg);
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        p.sendMessage("§b수리권 사용: 아이템이 수리되었습니다.");
        if (hand.getAmount()<=1) p.getInventory().setItemInOffHand(null);
        else hand.setAmount(hand.getAmount()-1);
    }
}
