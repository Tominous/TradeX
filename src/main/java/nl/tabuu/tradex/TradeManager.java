package nl.tabuu.tradex;

import nl.tabuu.tabuucore.configuration.IConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class TradeManager {

    private static TradeManager INSTANCE;

    public static TradeManager getInstance(){
        if(INSTANCE == null)
            INSTANCE = new TradeManager();

        return INSTANCE;
    }

    private LinkedHashMap<UUID, TradeRequest> _tradeRequests;
    private List<Trade> _trades;
    private IConfiguration _config;

    public TradeManager(){
        _tradeRequests = new LinkedHashMap<>();
        _trades = new ArrayList<>();

        _config = TradeX.getInstance().getConfigurationManager().getConfiguration("config");
    }

    public void sendRequest(Player sender, Player receiver){
        TradeRequest tradeRequest = new TradeRequest(sender, receiver);
        _tradeRequests.put(sender.getUniqueId(), tradeRequest);
        tradeRequest.send();

        long timeout = _config.getTime("RequestTimeOut");
        if(timeout > 0){
            Bukkit.getScheduler().runTaskLater(TradeX.getInstance(), () -> {
                if(tradeRequest.getStatus().equals(TradeRequestStatus.PENDING))
                    tradeRequest.timeOut();
            }, timeout / 50L);
        }
    }

    public void registerTrade(Trade trade){
        _trades.add(trade);
    }

    public List<TradeRequest> getTradeRequests(Player receiver){
        return _tradeRequests.values()
                .stream()
                .filter((request) -> request.getReceiver().getUniqueId().equals(receiver.getUniqueId()))
                .collect(Collectors.toList());
    }

    public TradeRequest getTradeRequest(Player sender, Player receiver){
        List<TradeRequest> trades = getTradeRequests(receiver);
        return trades.stream()
                .filter((request) -> request.getSender().getUniqueId().equals(sender.getUniqueId()))
                .findAny()
                .orElse(null);
    }

    public TradeRequest getLastReceivedRequest(Player receiver){
        List<TradeRequest> requests = getTradeRequests(receiver);
        if(!requests.isEmpty())
            return requests.get(requests.size() - 1);

        return null;
    }

    public TradeRequest getSendRequest(Player sender){
        return _tradeRequests.get(sender.getUniqueId());
    }

    public void removeRequest(TradeRequest request){
        _tradeRequests.remove(request.getSender().getUniqueId(), request);
    }

    public void removeRequest(UUID senderID){
        _tradeRequests.remove(senderID);
    }

    public void removeTrade(Trade trade){
        _trades.remove(trade);
    }

    public Collection<TradeRequest> getRequests(){
        return _tradeRequests.values();
    }

    public Collection<Trade> getTrades(){
        return _trades;
    }


}
