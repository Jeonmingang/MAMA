package com.minkang.ultimate.commands;

import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class YatuCommand implements CommandExecutor {
    private final JavaPlugin plugin;
    private final NamespacedKey KEY;
    public YatuCommand(JavaPlugin plugin, NamespacedKey key){ this.plugin=plugin; this.KEY=key; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("플레이어만 사용 가능합니다."); return true; }
        Player p = (Player)sender;
        Boolean target = null;
        if (args.length>=1){
            if ("on".equalsIgnoreCase(args[0])) target = true;
            if ("off".equalsIgnoreCase(args[0])) target = false;
        }
        if (target==null) target = !isEnabled(p);
        setEnabled(p, target);
        if (target) {
            applyNV(p);
            p.sendMessage("§a야간투시: §f켜짐");
        } else {
            clearNV(p);
            p.sendMessage("§c야간투시: §f꺼짐");
        }
        return true;
    }
    private boolean isEnabled(Player p){
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        Byte b = pdc.get(KEY, PersistentDataType.BYTE);
        return b!=null && b== (byte)1;
    }
    private void setEnabled(Player p, boolean v){
        PersistentDataContainer pdc = p.getPersistentDataContainer();
        if (v) pdc.set(KEY, PersistentDataType.BYTE, (byte)1);
        else pdc.remove(KEY);
    }
    public static void applyNV(Player p){
        p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 9_999_999, 0, false, false));
    }
    public static void clearNV(Player p){
        p.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }
}
