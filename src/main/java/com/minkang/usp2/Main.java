
package com.minkang.usp2;

import com.minkang.usp2.commands.*;
import com.minkang.usp2.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    // Managers
    private EconomyManager economy;
    private BanknoteManager banknote;
    private RepairManager repair;
    private TradeManager trade;
    private ShopManager shop;
    private LockManager lock;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Instantiate managers
        economy = new EconomyManager(this);
        banknote = new BanknoteManager(this);
        repair = new RepairManager(this);
        trade = new TradeManager(this);
        shop = new ShopManager(this);
        lock = new LockManager(this);

        // Register listeners
        Bukkit.getPluginManager().registerEvents(banknote, this);
        Bukkit.getPluginManager().registerEvents(repair, this);
        Bukkit.getPluginManager().registerEvents(trade, this);
        Bukkit.getPluginManager().registerEvents(shop, this);
        Bukkit.getPluginManager().registerEvents(lock, this);

        // Bind commands (only if declared in plugin.yml)
        bindCmd("돈", new MoneyCommand(this));
        bindCmd("수표", new ChequeCommand(this));
        bindCmd("거래", new TradeCommand(this));
        bindCmd("상점", new ShopCommand(this));
        bindCmd("배틀종료", new BattleEndCommand(this));
        bindCmd("픽셀몬", new PixelmonAliasCommand(this));
        bindCmd("잠금", new LockCommand(this));
        bindCmd("잠금권", new LockTokenCommand(this));
        bindCmd("야투", new YatuCommand(this));
        bindCmd("알걸음", new EggStepsCommand(this));
        bindCmd("수리권", new RepairTicketCommand(this));
        bindCmd("나야투", new NightVisionCommand(this));
    }

    @Override
    public void onDisable() {
        if (trade != null) {
            try { trade.closeAll(); } catch (Throwable ignored) {}
        }
        if (shop != null) {
            try { shop.save(); } catch (Throwable ignored) {}
        }
    }

    private void bindCmd(String name, CommandExecutor exec) {
        PluginCommand pc = getCommand(name);
        if (pc != null) pc.setExecutor(exec);
    }

    // Accessors used in other classes
    public EconomyManager eco() { return economy; }
    public BanknoteManager bank() { return banknote; }
    public RepairManager repair() { return repair; }
    public TradeManager trade() { return trade; }
    public ShopManager shop() { return shop; }
    public LockManager lock() { return lock; }
}
