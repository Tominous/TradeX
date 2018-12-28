package nl.tabuu.tradex.listener;

import nl.tabuu.tradex.TradeManager;
import nl.tabuu.tradex.TradeRequest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener{

    TradeManager _tradeManager;

    public PlayerListener(){
        _tradeManager = TradeManager.getInstance();
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event){
        TradeRequest request = _tradeManager.getSendRequest(event.getPlayer());

        if(request != null)
            request.cancel();
    }

}
