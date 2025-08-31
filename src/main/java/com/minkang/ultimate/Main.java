
package com.minkang.ultimate;

import com.minkang.ultimate.commands.*;
import com.minkang.ultimate.listeners.*;
import com.minkang.ultimate.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

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
        getCommand("배틀종료").setExecutor(new BattleEndCommand());
        getCommand("돈").setExecutor(new MoneyCommand(this));
        getCommand("수표").setExecutor(new ChequeCommand(this));
        getCommand("수리권").setExecutor(new RepairTicketCommand(this));
        getCommand("거래").setExecutor(new TradeCommand(this));
        ShopCommand shopCmd = new ShopCommand(this);
        getCommand("상점").setExecutor(shopCmd);
        getCommand("상점리로드").setExecutor(shopCmd);
        getCommand("잠금").setExecutor(new LockCommand(this));
        getCommand("잠금권").setExecutor(new LockTokenCommand(this));
        PixelmonAliasCommand stats = new PixelmonAliasCommand();
        getCommand("개체값").setExecutor(stats);
        getCommand("노력치").setExecutor(stats);
getLogger().info("UltimateServerPlugin enabled.");
    }

    @Override
    public void onDisable() {
        trade.closeAll();
        getLogger().info("UltimateServerPlugin disabled.");
    }
}
