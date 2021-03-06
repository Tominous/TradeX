package nl.tabuu.tradex;

import nl.tabuu.tabuucore.configuration.IConfiguration;
import nl.tabuu.tabuucore.plugin.TabuuCorePlugin;
import nl.tabuu.tempstoragez.TempStorageZ;
import nl.tabuu.tradex.command.TradeCommand;
import nl.tabuu.tradex.listener.PlayerListener;
import nl.tabuu.tradex.trade.Trade;
import nl.tabuu.tradex.trade.TradeManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Iterator;

public class TradeX extends TabuuCorePlugin {

    private static TradeX INSTANCE;

    private boolean _useTempStorageZ = false;
    private IConfiguration _config;

    @Override
    public void onEnable(){
        INSTANCE = this;

        getConfigurationManager().addConfiguration("config");
        getConfigurationManager().addConfiguration("lang");

        _config = getConfigurationManager().getConfiguration("config");

        Bukkit.getPluginManager().registerEvents(new PlayerListener(), getInstance());
        this.getCommand("trade").setExecutor(new TradeCommand());

        // TempStorage hook
        Plugin tempStorageZ = Bukkit.getPluginManager().getPlugin("TempStorageZ");
        if(tempStorageZ instanceof TempStorageZ && _config.getBoolean("UseTempStorageZ"))
            _useTempStorageZ = true;

        getInstance().getLogger().info("TradeX is now enabled.");
    }

    @Override
    public void onDisable(){
        Iterator<Trade> trades = TradeManager.getInstance().getTrades().iterator();
        while(trades.hasNext()){
            Trade trade = trades.next();
            trades.remove();
            trade.cancel();
        }

        getInstance().getLogger().info("TradeX is now disabled.");
    }

    public boolean useTempStorageZ(){
        return _useTempStorageZ;
    }

    public static TradeX getInstance(){
        return INSTANCE;
    }
}
