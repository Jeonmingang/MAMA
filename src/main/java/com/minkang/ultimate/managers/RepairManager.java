
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
    public void onUse(PlayerInteractEvent e) 
{
        if (e.getItem() == null) return;
        ItemStack ticket = e.getItem();
        if (!ticket.hasItemMeta()) return;
        org.bukkit.inventory.meta.ItemMeta tMeta = ticket.getItemMeta();
        org.bukkit.persistence.PersistentDataContainer pdc = tMeta.getPersistentDataContainer();
        if (!pdc.has(this.key, org.bukkit.persistence.PersistentDataType.INTEGER)) return;

        e.setCancelled(true);
        Player player = e.getPlayer();

        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType() == Material.AIR) {
            player.sendMessage("§c손에 든 수리 가능한 아이템이 필요합니다.");
            return;
        }

        ItemMeta meta = inHand.getItemMeta();
        if (!(meta instanceof Damageable)) {
            player.sendMessage("§c수리 불가 아이템입니다.");
            return;
        }

        Damageable dmg = (Damageable) meta;
        dmg.setDamage(0);
        inHand.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        player.sendMessage("§b수리권 사용: 아이템이 수리되었습니다.");

        // consume one ticket
        if (ticket.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else ticket.setAmount(ticket.getAmount() - 1);
}

}
