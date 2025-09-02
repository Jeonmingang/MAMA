
package com.minkang.usp2.managers;

import com.minkang.usp2.Main;
import com.minkang.usp2.utils.Texts;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class BanknoteManager implements Listener {
    private final Main plugin;
    private final NamespacedKey keyValue;
    private ItemStack template;

    public BanknoteManager(Main p){
        this.plugin = p;
        this.keyValue = new NamespacedKey(p, "banknote_value");
        reload();
    }

    public void reload(){
        FileConfiguration c = plugin.getConfig();
        Material m = Material.matchMaterial(c.getString("cheque.material","PAPER"));
        template = new ItemStack(m==null?Material.PAPER:m);
    }

    public ItemStack create(long amount, int qty){
        FileConfiguration c = plugin.getConfig();
        ItemStack it = template.clone();
        it.setAmount(Math.max(1, qty));
        ItemMeta meta = it.getItemMeta();
        String name = Texts.color(c.getString("cheque.name","&a&l수표"))
                .replace("%amount%", String.valueOf(amount));
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<String>();
        for(String s: c.getStringList("cheque.lore")) lore.add(Texts.color(s.replace("%amount%", String.valueOf(amount))));
        meta.setLore(lore);
        if (c.getBoolean("cheque.glow", true)) meta.addEnchant(org.bukkit.enchantments.Enchantment.LUCK, 1, true);
        meta.getPersistentDataContainer().set(keyValue, PersistentDataType.LONG, amount);
        it.setItemMeta(meta);
        return it;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onUse(PlayerInteractEvent e){
        if (e.getItem()==null) return;
        ItemStack is = e.getItem();
        if (!is.hasItemMeta()) return;
        ItemMeta meta = is.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keyValue, PersistentDataType.LONG)) return;
        e.setCancelled(true);
        long amt = pdc.get(keyValue, PersistentDataType.LONG).longValue();
        Player p = e.getPlayer();
        plugin.eco().deposit(p, amt);
        p.sendMessage("§a수표 상환: §f$"+amt);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        if (is.getAmount()<=1) p.getInventory().setItemInMainHand(null);
        else is.setAmount(is.getAmount()-1);
    }
}
