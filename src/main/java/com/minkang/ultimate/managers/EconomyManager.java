
package com.minkang.ultimate.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.Plugin;

public class EconomyManager {
    private static Economy econ;

    public EconomyManager(Plugin p){
        setupVault(p);
    }
    private boolean setupVault(Plugin p) {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = p.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider(); return econ != null;
    }
    public double bal(OfflinePlayer op){ return econ==null?0:econ.getBalance(op); }
    public boolean withdraw(OfflinePlayer op, double amt){ return econ!=null && econ.withdrawPlayer(op, amt).transactionSuccess(); }
    public boolean deposit(OfflinePlayer op, double amt){ return econ!=null && econ.depositPlayer(op, amt).transactionSuccess(); }
}
