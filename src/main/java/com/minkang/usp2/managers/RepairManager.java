package com.minkang.usp2.managers;

import com.minkang.usp2.Main;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class RepairManager implements Listener {
    private final Main plugin;
    private final NamespacedKey key;

    public RepairManager(Main plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "repair_ticket");
    }

    public ItemStack create(int amount) {
        if (amount < 1) amount = 1;
        ItemStack ticket = new ItemStack(Material.PAPER, amount);
        ItemMeta meta = ticket.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b수리권");
            meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
            ticket.setItemMeta(meta);
        }
        return ticket;
    }

    private boolean isTicket(ItemStack s){
        if (s==null || s.getType()==Material.AIR || !s.hasItemMeta()) return false;
        Integer v = s.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return v != null && v == 1;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e){
        if (e.getHand()!=EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack main = p.getInventory().getItemInMainHand();
        ItemStack off  = p.getInventory().getItemInOffHand();
        boolean mainT = isTicket(main), offT = isTicket(off);
        if (!mainT && !offT) return;

        ItemStack ticket = mainT? main : off;
        ItemStack target = mainT? off  : main;
        if (target==null || target.getType()==Material.AIR){ p.sendMessage("§c반대 손에 수리할 아이템을 들어주세요."); return; }
        ItemMeta meta = target.getItemMeta();
        if (!(meta instanceof Damageable)){ p.sendMessage("§7이 아이템은 내구도가 없습니다."); return; }
        Damageable dmg = (Damageable) meta;
        if (dmg.getDamage() <= 0){ p.sendMessage("§7이미 내구도가 가득 찼습니다."); return; }
        dmg.setDamage(0);
        target.setItemMeta((ItemMeta)dmg);

        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        p.sendMessage("§b수리권 사용: 아이템이 수리되었습니다.");

        if (ticket.getAmount()<=1){
            if (mainT) p.getInventory().setItemInMainHand(null); else p.getInventory().setItemInOffHand(null);
        } else ticket.setAmount(ticket.getAmount()-1);
    }
}
