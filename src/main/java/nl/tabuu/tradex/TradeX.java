package nl.tabuu.tradex;

import nl.tabuu.tabuucore.plugin.TabuuCorePlugin;
import nl.tabuu.tradex.command.TradeCommand;
import nl.tabuu.tradex.listener.PlayerListener;
import org.bukkit.Bukkit;

import java.util.Iterator;

public class TradeX extends TabuuCorePlugin {

    private static TradeX _instance;

    @Override
    public void onEnable(){
        _instance = this;

        getConfigurationManager().addConfiguration("config");
        getConfigurationManager().addConfiguration("lang");

        Bukkit.getPluginManager().registerEvents(new PlayerListener(), getInstance());
        this.getCommand("trade").setExecutor(new TradeCommand());

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

    public static TradeX getInstance(){
        return _instance;
    }
}
