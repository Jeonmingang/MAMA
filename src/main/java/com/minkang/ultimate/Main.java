
package com.minkang.ultimate;

import com.minkang.ultimate.commands.*;
import com.minkang.ultimate.listeners.*;
import com.minkang.ultimate.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private TradeManager trade;
    private ShopManager shops;

    @Override
    public void onEnable() {
        // init managers
        trade = new TradeManager(this);
        shops = new ShopManager(this);

        // register listeners
        getServer().getPluginManager().registerEvents(trade, this);
        getServer().getPluginManager().registerEvents(new CitizensBridge(shops), this);

        // register commands (remove Pixelmon/EggSteps)
        getCommand("거래").setExecutor(new TradeCommand(trade));
        getCommand("상점").setExecutor(new ShopCommand(shops));
        getCommand("상점리로드").setExecutor(new ShopCommand(shops));
        getCommand("잠금").setExecutor(new LockCommand(this));
        getCommand("잠금권").setExecutor(new LockTokenCommand(this));

        // Optional commands
        try { getCommand("배틀종료").setExecutor(new BattleEndCommand(this)); } catch (Throwable ignored) {}
        try { getCommand("야투").setExecutor(new NightVisionCommand(this)); } catch (Throwable ignored) {}
    }

    @Override
    public void onDisable() {
        if (trade != null) trade.closeAll();
    }
}
