package nl.tabuu.tradex.trade;

import nl.tabuu.tabuucore.util.Dictionary;
import nl.tabuu.tradex.TradeX;
import nl.tabuu.tradex.ui.TradeUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class TradeRequest {

    private UUID _sender, _receiver;
    private TradeRequestStatus _status;
    private Dictionary _local;

    public TradeRequest(Player sender, Player receiver){
        _sender = sender.getUniqueId();
        _receiver = receiver.getUniqueId();

        _local = TradeX.getInstance().getConfigurationManager().getConfiguration("lang").getDictionary("");
    }

    public void send(){
        setStatus(TradeRequestStatus.PENDING);
    }

    public void accept(){
        setStatus(TradeRequestStatus.ACCEPTED);

        if(isValid()){
            Trade trade = new Trade(getSender(), getReceiver());

            new TradeUI(trade, getSender(), getReceiver()).open(getSender());
            new TradeUI(trade, getReceiver(), getSender()).open(getReceiver());

            TradeManager.getInstance().registerTrade(trade);
        }
        else{
            String error = _local.translate("ERROR_PLAYER_OFFLINE");
            String errorMessage = _local.translate("REQUEST_ACCEPT_ERROR", "{ERROR}", error);
            getReceiver().sendMessage(errorMessage);
        }
    }

    public void deny(){
        setStatus(TradeRequestStatus.DENIED);
    }

    public void cancel(){
        setStatus(TradeRequestStatus.CANCELED);
    }

    public void timeOut(){
        setStatus(TradeRequestStatus.TIMED_OUT);
    }

    private void setStatus(TradeRequestStatus status){
        _status = status;

        switch (_status){
            case ACCEPTED:
            case DENIED:
            case CANCELED:
            case TIMED_OUT:
                TradeManager.getInstance().removeRequest(this);
                break;
        }

        switch (_status){
            case TIMED_OUT:
                getSender().sendMessage(_local.translate("REQUEST_TIMED_OUT"));
                break;

            case DENIED:
                getSender().sendMessage(_local.translate("REQUEST_DENIED", "{PLAYER}", getSender().getDisplayName()));
                getReceiver().sendMessage(_local.translate("REQUEST_DENY", "{PLAYER}", getSender().getDisplayName()));
                break;

            case ACCEPTED:
                getSender().sendMessage(_local.translate("REQUEST_ACCEPTED", "{PLAYER}", getSender().getDisplayName()));
                getReceiver().sendMessage(_local.translate("REQUEST_ACCEPT", "{PLAYER}", getSender().getDisplayName()));
                break;

            case CANCELED:
                getSender().sendMessage(_local.translate("REQUEST_CANCELED"));
                break;

            case PENDING:
                getSender().sendMessage(_local.translate("REQUEST_SEND", "{PLAYER}", getReceiver().getName()));
                getReceiver().sendMessage(_local.translate("REQUEST_RECEIVED", "{PLAYER}", getSender().getName()));
                break;
        }
    }

    public TradeRequestStatus getStatus(){
        return _status;
    }

    private boolean isValid(){
        return getSender() != null && getReceiver() != null;
    }

    public Player getSender(){
        Player player = Bukkit.getPlayer(_sender);

        if(player == null)
            TradeManager.getInstance().removeRequest(_sender);

        return player;
    }

    public Player getReceiver(){
        Player player = Bukkit.getPlayer(_receiver);

        if(player == null)
            TradeManager.getInstance().removeRequest(_sender);

        return player;
    }
}
