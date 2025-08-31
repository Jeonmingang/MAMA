package com.minkang.ultimate;

import com.minkang.ultimate.commands.*;
import com.minkang.ultimate.listeners.*;
import com.minkang.ultimate.managers.*;
import com.minkang.ultimate.shop.ShopLinkWrapperCommand;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private EconomyManager economy;
    private ShopManager shop;
    private RepairManager repair;
    private TradeManager trade;
    private BanknoteManager bank;
    private LockManager lock;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Managers
        this.economy = new EconomyManager(this);
        this.bank = new BanknoteManager(this);
        this.shop = new ShopManager(this);
        this.repair = new RepairManager(this);
        this.lock = new LockManager(this);
        this.trade = new TradeManager(this);

        // Listeners
        getServer().getPluginManager().registerEvents(bank, this);
        getServer().getPluginManager().registerEvents(shop, this);
        getServer().getPluginManager().registerEvents(lock, this);
        getServer().getPluginManager().registerEvents(trade, this);
        getServer().getPluginManager().registerEvents(new TradeAliasListener(this), this);

        // Commands
        if (getCommand("개체값") != null) getCommand("개체값").setExecutor(new StatsCommand());
        if (getCommand("노력치") != null) getCommand("노력치").setExecutor(new StatsCommand());
        if (getCommand("알걸음") != null) getCommand("알걸음").setExecutor(new EggStepsCommand());
        if (getCommand("잠금") != null) getCommand("잠금").setExecutor(new LockCommand(lock));
        if (getCommand("잠금권") != null) getCommand("잠금권").setExecutor(new LockTokenCommand(this));
        if (getCommand("수표") != null) getCommand("수표").setExecutor(new ChequeCommand(this));
        if (getCommand("수리권") != null) getCommand("수리권").setExecutor(new RepairTicketCommand(this));
        if (getCommand("거래") != null) getCommand("거래").setExecutor(new TradeCommand(this));

        // Night Vision (/야투)
        NamespacedKey YATU_KEY = new NamespacedKey(this, "yatu_enabled");
        if (getCommand("야투") != null) getCommand("야투").setExecutor(new YatuCommand(this, YATU_KEY));
        getServer().getPluginManager().registerEvents(new YatuListener(this, YATU_KEY), this);

        // Citizens wrapper (optional)
        if (getServer().getPluginManager().getPlugin("Citizens") != null) {
            if (getCommand("상점") != null) getCommand("상점").setExecutor(new ShopLinkWrapperCommand(this));
        }

        getLogger().info("UltimateServerPlugin enabled.");
    }

    @Override
    public void onDisable() {
        if (shop != null) shop.save();
        if (lock != null) lock.save();
        getLogger().info("UltimateServerPlugin disabled.");
    }

    // Accessors (other classes use these)
    public EconomyManager eco(){ return economy; }
    public ShopManager shop(){ return shop; }
    public RepairManager repair(){ return repair; }
    public TradeManager trade(){ return trade; }
    public BanknoteManager bank(){ return bank; }
    public LockManager lock(){ return lock; }
}
