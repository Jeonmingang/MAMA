package com.minkang.usp2.commands;

import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NightVisionCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
        Player p = (Player)sender;
        boolean has = p.hasPotionEffect(PotionEffectType.NIGHT_VISION);
        if (args.length == 1) {
            String a = args[0].toLowerCase();
            if (a.equals("on")) has = false;
            else if (a.equals("off")) has = true;
        }
        if (has) {
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            p.sendMessage("§7야간투시: §coff");
        } else {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
            p.sendMessage("§7야간투시: §aon");
        }
        return true;
    }
}
