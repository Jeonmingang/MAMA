
package com.minkang.usp2;

import com.minkang.usp2.commands.*;
import com.minkang.usp2.listeners.*;
import com.minkang.usp2.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.CommandExecutor;

public class Main extends JavaPlugin {
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
        saveDefaultConfig();
        economy = new EconomyManager(this);
        banknote = new BanknoteManager(this);
        repair = new RepairManager(this);
        trade = new TradeManager(this);
        shop = new ShopManager(this);
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

        
        // commands
        bindCmd("배틀종료", new BattleEndCommand());
        bindCmd("돈", new MoneyCommand(this));
        bindCmd("수표", new ChequeCommand(this));
        bindCmd("수리권", new RepairTicketCommand(this));
        bindCmd("거래", new TradeCommand(this));
        ShopCommand shopCmd = new ShopCommand(this);
        bindCmd("상점", shopCmd);
        bindCmd("상점리로드", shopCmd);
        bindCmd("잠금", new LockCommand(this));
        bindCmd("잠금권", new LockTokenCommand(this));
        bindCmd("야투", new NightVisionCommand());
getLogger().info("UltimateServerPlugin enabled.");
    }

    @Override
    public void onDisable() {
        trade.closeAll();
        getLogger().info("UltimateServerPlugin disabled.");
    }
}
