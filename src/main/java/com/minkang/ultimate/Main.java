package com.minkang.ultimate;

import com.minkang.ultimate.managers.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private static Main instance;

    // Vault economy
    private Economy economy;

    // Managers
    private TradeManager trade;
    private ShopManager shop;
    private LockManager lock;
    private RepairTicketManager repair;
    private BanknoteManager bank;

    @Override
    public void onEnable() {
        instance = this;

        // Setup Vault economy if present
        setupEconomy();

        // Instantiate managers
        shop   = new ShopManager(this);
        trade  = new TradeManager(this);
        lock   = new LockManager(this);
        repair = new RepairTicketManager(this);
        bank   = new BanknoteManager(this);

        // Register listeners only for classes that actually implement Listener
        if (trade instanceof Listener) Bukkit.getPluginManager().registerEvents((Listener) trade, this);
        if (shop  instanceof Listener) Bukkit.getPluginManager().registerEvents((Listener) shop,  this);
        if (lock  instanceof Listener) Bukkit.getPluginManager().registerEvents((Listener) lock,  this);
        if (repair instanceof Listener) Bukkit.getPluginManager().registerEvents((Listener) repair,this);
        if (bank  instanceof Listener) Bukkit.getPluginManager().registerEvents((Listener) bank,  this);

        getLogger().info("[Ultimate] enabled");
    }

    @Override
    public void onDisable() {
        getLogger().info("[Ultimate] disabled");
    }

    private void setupEconomy() {
        try {
            if (getServer().getPluginManager().getPlugin("Vault") == null) return;
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        } catch (Throwable ignored) { }
    }

    // ===== Static =====
    public static Main getInstance() { return instance; }

    // ===== Accessors used by commands =====
    public Economy eco() { return economy; }
    public BanknoteManager bank() { return bank; }
    public ShopManager shop() { return shop; }
    public TradeManager trade() { return trade; }
    public LockManager lock() { return lock; }
    public RepairTicketManager repair() { return repair; }
}
