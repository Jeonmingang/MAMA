
package com.minkang.ultimate;

import com.minkang.ultimate.listeners.JoinQuitListener;

import com.minkang.ultimate.listeners.ShopGuiListener;

import com.minkang.ultimate.commands.*;
import com.minkang.ultimate.listeners.*;
import com.minkang.ultimate.managers.*;
import com.minkang.ultimate.kit.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.CommandExecutor;
import com.minkang.ultimate.kit.KitManager;
import com.minkang.ultimate.kit.KitGuiListener;

public class Main extends JavaPlugin {
    private KitManager kitManager;
    private KitGuiListener kitGuiListener;

    private void logSevere(String msg, Throwable t){
        getLogger().severe(msg + (t!=null? (" :: " + t.getMessage()) : ""));
    }

    
    private void bindCmd(String name, CommandExecutor exec){
        PluginCommand cmd = getCommand(name);
        if (cmd != null){ cmd.setExecutor(exec); }
        else { getLogger().warning("command not found: " + name); }
    }
private EconomyManager economy;
    private BanknoteManager banknote;
    private RepairManager repair;
    private TradeManager trade;
    private ShopManager shop;
    private LockManager lock;

    public EconomyManager eco(){ return economy; }
    public BanknoteManager bank(){ return banknote; }
    public RepairManager repair(){ return repair; }
    public TradeManager trade(){ return trade; }
    public ShopManager shop(){ return shop; }
    public LockManager lock(){ return lock; }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ShopGuiListener(this), this);
        saveDefaultConfig();
        economy = new EconomyManager(this);
        banknote = new BanknoteManager(this);
        repair = new RepairManager(this);
        trade = new TradeManager(this);
        shop = new ShopManager(this);



        

        

        // ===== Ensure command executors are registered =====
        try {
            if (getCommand("야투") != null) getCommand("야투").setExecutor(new NightVisionCommand());
            if (getCommand("잠금") != null) getCommand("잠금").setExecutor(new LockCommand(this));
            if (getCommand("잠금권") != null) getCommand("잠금권").setExecutor(new LockTokenCommand(this));
            if (getCommand("수리권") != null) getCommand("수리권").setExecutor(new RepairTicketCommand(this));
            if (getCommand("상점") != null) getCommand("상점").setExecutor(new ShopCommand(this));
            if (getCommand("전체지급") != null) getCommand("전체지급").setExecutor(new GlobalGiveCommand(this));
            if (getCommand("추첨") != null) getCommand("추첨").setExecutor(new RaffleCommand(this));
            if (getCommand("기본템") != null) getCommand("기본템").setExecutor(new KitCommand(this, this.kitManager, this.kitGuiListener));
        } catch (Throwable ignored) {}

// ===== Register command executors (ensured) =====
        try {
            if (getCommand("야투") != null) getCommand("야투").setExecutor(new NightVisionCommand());
            if (getCommand("잠금") != null) getCommand("잠금").setExecutor(new LockCommand(this));
            if (getCommand("잠금권") != null) getCommand("잠금권").setExecutor(new LockTokenCommand(this));
            if (getCommand("수리권") != null) getCommand("수리권").setExecutor(new RepairTicketCommand(this));
            if (getCommand("상점") != null) getCommand("상점").setExecutor(new ShopCommand(this));
            if (getCommand("전체지급") != null) getCommand("전체지급").setExecutor(new GlobalGiveCommand(this));
            if (getCommand("추첨") != null) getCommand("추첨").setExecutor(new RaffleCommand(this));
            if (getCommand("기본템") != null) getCommand("기본템").setExecutor(new KitCommand(this, this.kitManager, this.kitGuiListener));
        } catch (Throwable ignored) {}
// ===== Register commands (core) =====
        if (getCommand("야투") != null) getCommand("야투").setExecutor(new NightVisionCommand());
        if (getCommand("잠금") != null) getCommand("잠금").setExecutor(new LockCommand(this));
        if (getCommand("잠금권") != null) getCommand("잠금권").setExecutor(new LockTokenCommand(this));
        if (getCommand("수리권") != null) getCommand("수리권").setExecutor(new RepairTicketCommand(this));
        if (getCommand("상점") != null) getCommand("상점").setExecutor(new ShopCommand(this));
        if (getCommand("전체지급") != null) getCommand("전체지급").setExecutor(new GlobalGiveCommand(this));
        if (getCommand("추첨") != null) getCommand("추첨").setExecutor(new RaffleCommand(this));
        if (getCommand("기본템") != null) getCommand("기본템").setExecutor(new KitCommand(this, this.kitManager, this.kitGuiListener));

// NPC right-click -> open shop (no permission required; runs console commands from config)
getServer().getPluginManager().registerEvents(new com.minkang.ultimate.listeners.ShopNpcListener(this), this);

        lock = new LockManager(this);

        // listeners
        if (getConfig().getBoolean("hunger-disable", true)) {
            Bukkit.getPluginManager().registerEvents(new HungerListener(), this);
        }
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(), this);
        Bukkit.getPluginManager().registerEvents(banknote, this);
        Bukkit.getPluginManager().registerEvents(repair, this);
        Bukkit.getPluginManager().registerEvents(trade, this);
        Bukkit.getPluginManager().registerEvents(shop, this);
        Bukkit.getPluginManager().registerEvents(lock, this);

        
        
        // Citizens bridge (optional)
        try {
            if (getServer().getPluginManager().getPlugin("Citizens") != null) {
                getServer().getPluginManager().registerEvents(new com.minkang.ultimate.listeners.CitizensBridge(shop), this);
                getLogger().info("Citizens bridge enabled.");
            } else {
                getLogger().info("Citizens not found; NPC shop linking disabled.");
            }
        } catch (Throwable t) {
            getLogger().warning("Citizens bridge init failed: " + t.getMessage());
        }
        // commands
        bindCmd("배틀종료", new BattleEndCommand());
        bindCmd("돈", new MoneyCommand(this));
        bindCmd("수표", new ChequeCommand(this));
        bindCmd("수리권", new RepairTicketCommand(this));
        bindCmd("거래", new TradeCommand(this));
        ShopCommand shopCmd = new ShopCommand(this);
        bindCmd("상점", new ShopCommand(this));
        bindCmd("상점리로드", shopCmd);
        bindCmd("잠금", new LockCommand(this));
        bindCmd("잠금권", new LockTokenCommand(this));
        bindCmd("야투", new NightVisionCommand());
        bindCmd("전체지급", new GlobalGiveCommand(this));
        bindCmd("추첨", new RaffleCommand(this));
KitManager kitManager = new KitManager(this);
        KitGuiListener kitGui = new KitGuiListener(this, kitManager);
        getServer().getPluginManager().registerEvents(kitGui, this);
        bindCmd("기본템", new com.minkang.ultimate.commands.KitCommand(this, kitManager, kitGui));
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);
        getLogger().info("UltimateServerPlugin enabled.");
    }

    @Override
    public void onDisable() {
        trade.closeAll();
        getLogger().info("UltimateServerPlugin disabled.");
    }
}
