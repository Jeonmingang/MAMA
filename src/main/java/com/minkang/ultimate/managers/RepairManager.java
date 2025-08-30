
package com.minkang.ultimate.managers;

import com.minkang.ultimate.Main;
import com.minkang.ultimate.utils.Texts;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Repairs a damaged item using a "repair ticket".
 * The ticket is a PAPER with a persistent-data boolean tag.
 * Usage:
 *  - Hold the ticket in main hand and the item to repair in offhand, then right-click.
 *  - OR hold the ticket in offhand, item in main hand, then right-click.
 * The ticket is consumed by 1 and the target item durability is reset to 0.
 */
public class RepairManager implements Listener {

    private final Main plugin;
    private final NamespacedKey keyTicket;
    private Material ticketMaterial;
    private String ticketName;
    private List<String> ticketLore;

    public RepairManager(Main plugin) {
        this.plugin = plugin;
        this.keyTicket = new NamespacedKey(plugin, "repair_ticket");
        loadConfigValues();
    }

    private void loadConfigValues() {
        FileConfiguration c = plugin.getConfig();
        // Sensible defaults if not present
        this.ticketMaterial = Material.matchMaterial(c.getString("repair-ticket.material", "PAPER"));
        if (this.ticketMaterial == null) this.ticketMaterial = Material.PAPER;

        this.ticketName = Texts.color(c.getString("repair-ticket.name", "&b수리권"));
        this.ticketLore = new ArrayList<>();
        List<String> fromCfg = c.getStringList("repair-ticket.lore");
        if (fromCfg == null || fromCfg.isEmpty()) {
            this.ticketLore.add(Texts.color("&7우클릭으로 사용, 반대손 아이템 수리"));
        } else {
            for (String s : fromCfg) this.ticketLore.add(Texts.color(s));
        }
    }

    public ItemStack create(int amount) {
        ItemStack is = new ItemStack(ticketMaterial, Math.max(1, amount));
        ItemMeta meta = is.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ticketName);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.setLore(ticketLore);
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(keyTicket, PersistentDataType.BYTE, (byte) 1);
            is.setItemMeta(meta);
        }
        return is;
    }

    /** Register in Main.onEnable(): getServer().getPluginManager().registerEvents(repair(), this); */
    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        // Only handle main hand interactions to avoid double firing
        if (e.getHand() != EquipmentSlot.HAND) return;

        ItemStack used = e.getItem();
        if (used == null || !used.hasItemMeta()) return;

        // Check both main/offhand for the ticket tag – users might hold it in either hand.
        boolean mainIsTicket = hasTicketTag(used);
        boolean offIsTicket = false;
        ItemStack off = e.getPlayer().getInventory().getItemInOffHand();
        if (off != null && off.hasItemMeta()) offIsTicket = hasTicketTag(off);

        if (!mainIsTicket && !offIsTicket) return;

        Player p = e.getPlayer();
        e.setCancelled(true);

        // Determine target item to repair: if ticket in main hand -> repair offhand; else repair main hand.
        ItemStack target = mainIsTicket ? off : p.getInventory().getItemInMainHand();
        ItemStack ticket = mainIsTicket ? used : off;

        if (target == null || target.getType() == Material.AIR) {
            p.sendMessage("§c수리할 아이템을 반대 손에 들어주세요.");
            return;
        }
        if (!target.hasItemMeta() || !(target.getItemMeta() instanceof Damageable)) {
            p.sendMessage("§c이 아이템은 내구도가 없습니다.");
            return;
        }

        ItemMeta meta = target.getItemMeta();
        Damageable dmg = (Damageable) meta;
        if (dmg.getDamage() <= 0) {
            p.sendMessage("§7이미 내구도가 가득 찼습니다.");
            return;
        }

        // Repair
        dmg.setDamage(0);
        target.setItemMeta(meta);

        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        p.sendMessage("§b수리권 사용: 아이템이 수리되었습니다.");

        // Consume one ticket
        if (ticket.getAmount() <= 1) {
            if (mainIsTicket) p.getInventory().setItemInMainHand(null);
            else p.getInventory().setItemInOffHand(null);
        } else {
            ticket.setAmount(ticket.getAmount() - 1);
        }
    }

    private boolean hasTicketTag(ItemStack is) {
        ItemMeta meta = is.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte b = pdc.get(keyTicket, PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }
}
