
package com.minkang.usp2.commands;

import com.minkang.usp2.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NightVisionCommand implements CommandExecutor {
    private final Main plugin;
    public NightVisionCommand(Main plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
        Player p = (Player)sender;
        boolean enable = true;
        if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) enable = false;
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("off")) enable = false;
            if (args[0].equalsIgnoreCase("on")) enable = true;
        }
        if (enable) p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20*60*30, 0, true, false));
        else p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        p.sendMessage(enable? "§a야간투시 ON" : "§c야간투시 OFF");
        return true;
    }
}
