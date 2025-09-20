package com.minkang.ultimate.commands;

import com.minkang.ultimate.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

public class GlobalGiveCommand implements CommandExecutor {

    private final Main plugin;
    public GlobalGiveCommand(Main p){ this.plugin = p; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String l, String[] a) {
        if (!s.hasPermission("usp.globalgive")) { s.sendMessage("권한이 없습니다."); return true; }
        if (!(s instanceof Player)) { s.sendMessage("플레이어만 사용 가능합니다."); return true; }
        Player p = (Player) s;

        int amount = 1;
        if (a.length >= 1){
            try { amount = Math.max(1, Integer.parseInt(a[0])); } catch (Exception ignored){}
        }

        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR){
            p.sendMessage(ChatColor.RED + "손에 든 아이템이 없습니다.");
            return true;
        }

        boolean take = plugin.getConfig().getBoolean("global-give.take-from-sender", false);
        boolean fireworks = plugin.getConfig().getBoolean("global-give.fireworks", true);
        boolean title = plugin.getConfig().getBoolean("global-give.title", true);
        boolean sound = plugin.getConfig().getBoolean("global-give.sound", true);

        ItemStack gift = hand.clone();
        gift.setAmount(amount);

        if (take){
            int need = amount * Bukkit.getOnlinePlayers().size();
            int removed = removeFromPlayer(p, hand.getType(), need);
            if (removed < need){
                p.sendMessage(ChatColor.RED + "인벤토리에 수량이 부족합니다. 필요: " + need);
                return true;
            }
        }

        String itemName = gift.hasItemMeta() && gift.getItemMeta().hasDisplayName() ? gift.getItemMeta().getDisplayName() : gift.getType().name();
        String banner = ChatColor.DARK_GRAY + "====================\n"
                + ChatColor.GOLD + "" + ChatColor.BOLD + "📦 전체지급 이벤트!\n"
                + ChatColor.GRAY + "주최자: " + ChatColor.YELLOW + p.getName() + ChatColor.GRAY + "\n"
                + ChatColor.GRAY + "아이템: " + ChatColor.AQUA + itemName + ChatColor.GRAY + " × " + ChatColor.AQUA + amount + "\n"
                + ChatColor.DARK_GRAY + "====================";

        Bukkit.broadcastMessage(banner);

        for (Player t : Bukkit.getOnlinePlayers()){
            t.getInventory().addItem(gift.clone());
            if (title){
                t.sendTitle(ChatColor.GOLD + "전체지급!", ChatColor.YELLOW + p.getName() + ChatColor.GRAY + " → " + ChatColor.AQUA + itemName + " × " + amount, 10, 40, 10);
            }
            if (sound){
                t.playSound(t.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }

        if (fireworks){
            try {
                Firework fw = p.getWorld().spawn(p.getLocation().add(0, 1, 0), Firework.class);
                FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder()
                        .withColor(org.bukkit.Color.AQUA)
                        .withFade(org.bukkit.Color.WHITE)
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .trail(true).flicker(true).build());
                meta.setPower(1);
                fw.setFireworkMeta(meta);
            } catch (Throwable ignored){}
        }

        return true;
    }

    private int removeFromPlayer(Player p, Material type, int need){
        int remain = need;
        for (int i = 0; i < p.getInventory().getSize(); i++){
            ItemStack it = p.getInventory().getItem(i);
            if (it == null || it.getType() != type) continue;
            int take = Math.min(remain, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) p.getInventory().setItem(i, null);
            remain -= take;
            if (remain <= 0) break;
        }
        return need - remain;
    }
}
