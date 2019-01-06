package nl.tabuu.tradex.command;

import nl.tabuu.tabuucore.command.*;
import nl.tabuu.tabuucore.configuration.IConfiguration;
import nl.tabuu.tabuucore.util.Dictionary;
import nl.tabuu.tradex.trade.TradeManager;
import nl.tabuu.tradex.trade.TradeRequest;
import nl.tabuu.tradex.TradeX;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

public class TradeCommand extends Command {

    private TradeManager _tradeManager;
    private Dictionary _local;
    private IConfiguration _config;
    private String _outOfRangeError;
    private double _maximumTradeRange;

    public TradeCommand() {
        super(Bukkit.getPluginCommand("trade"));
        _tradeManager = TradeManager.getInstance();
        _local = TradeX.getInstance().getConfigurationManager().getConfiguration("lang").getDictionary("");
        _config = TradeX.getInstance().getConfigurationManager().getConfiguration("config");

        _outOfRangeError = _local.translate("ERROR_PLAYER_OUT_OF_RANGE", "{RANGE_INT}", (int)_config.getDouble("MaximumTradeDistance") + "");
        _maximumTradeRange = _config.getDouble("MaximumTradeDistance");

        setArgumentConverter(new OrderedArgumentConverter().setSequence(ArgumentType.PLAYER));
        setRequiredSenderType(SenderType.PLAYER);

        addSubCommand("accept", new TradeRequestAcceptCommand(this));
        addSubCommand("deny", new TradeRequestDenyCommand(this));
        addSubCommand("cancel", new TradeRequestCancelCommand(this));
    }

    @Override
    protected CommandResult onCommand(CommandSender commandSender, List<Optional<?>> list) {
        if(!list.get(0).isPresent())
            return CommandResult.WRONG_SYNTAX;

        Player receiver = (Player) list.get(0).get();
        Player sender = (Player) commandSender;

        if(!sender.getUniqueId().equals(receiver.getUniqueId())) {
            if(_maximumTradeRange < 0 || (sender.getLocation().distance(receiver.getLocation()) <= _config.getDouble("MaximumTradeDistance")
                    && sender.getWorld().equals(receiver.getWorld()))){

                TradeRequest request = _tradeManager.getTradeRequest(receiver, sender);
                if(request != null)
                    request.accept();
                else
                    _tradeManager.sendRequest(sender, receiver);
            }
            else
                sender.sendMessage(_local.translate("REQUEST_SEND_ERROR", "{ERROR}", _outOfRangeError));
        }
        else
            return CommandResult.WRONG_SYNTAX;

        return CommandResult.SUCCESS;
    }

    abstract class TradeRequestInteractionCommand extends Command{

        protected String _notFoundError;

        public TradeRequestInteractionCommand(String commandName, Command parentCommand) {
            super(commandName, parentCommand);
            _notFoundError = _local.translate("ERROR_NO_REQUEST_FOUND");
        }

        abstract void doInteraction(TradeRequest request);

        abstract String getNotFoundError();
    }

    public abstract class TradeRequestReceivedInteractionCommand extends TradeRequestInteractionCommand{

        private TradeRequestReceivedInteractionCommand(String commandName, Command parentCommand) {
            super(commandName, parentCommand);

            setArgumentConverter(new OrderedArgumentConverter().setSequence(ArgumentType.PLAYER));
            setRequiredSenderType(SenderType.PLAYER);
        }

        @Override
        protected CommandResult onCommand(CommandSender commandSender, List<Optional<?>> list) {
            Optional<Player> sender = (Optional<Player>) list.get(0);
            Player receiver = (Player) commandSender;

            TradeRequest request = sender.isPresent() ? _tradeManager.getSendRequest(sender.get()) : _tradeManager.getLastReceivedRequest(receiver);

            if(request != null){
                if(_maximumTradeRange < 0 || (receiver.getLocation().distance(request.getSender().getLocation()) > _config.getDouble("MaximumTradeDistance")
                        && request.getSender().getWorld().equals(receiver.getWorld())))
                    receiver.sendMessage(getOutOfRangeError());
                else
                    doInteraction(request);
            }
            else
                receiver.sendMessage(getNotFoundError());

            return CommandResult.SUCCESS;
        }

        abstract String getOutOfRangeError();
    }

    public abstract class TradeRequestSendInteractionCommand extends TradeRequestInteractionCommand{

        private TradeRequestSendInteractionCommand(String commandName, Command parentCommand) {
            super(commandName, parentCommand);

            setRequiredSenderType(SenderType.PLAYER);
        }

        @Override
        protected CommandResult onCommand(CommandSender commandSender, List<Optional<?>> list) {
            Player sender = (Player) commandSender;

            TradeRequest request = _tradeManager.getSendRequest(sender);

            if(request != null)
                doInteraction(request);
            else
                sender.sendMessage(getNotFoundError());

            return CommandResult.SUCCESS;
        }
    }

    public class TradeRequestAcceptCommand extends TradeRequestReceivedInteractionCommand {

        private TradeRequestAcceptCommand(Command parentCommand) {
            super("trade accept", parentCommand);
        }

        @Override
        void doInteraction(TradeRequest request) {
            request.accept();
        }

        @Override
        String getNotFoundError() {
            return _local.translate("REQUEST_ACCEPT_ERROR", "{ERROR}", _notFoundError);
        }

        @Override
        String getOutOfRangeError() {
            return _local.translate("REQUEST_ACCEPT_ERROR", "{ERROR}", _outOfRangeError);
        }
    }

    public class TradeRequestDenyCommand extends TradeRequestReceivedInteractionCommand {

        private TradeRequestDenyCommand(Command parentCommand) {
            super("trade deny", parentCommand);
        }

        @Override
        void doInteraction(TradeRequest request) {
            request.deny();
        }

        @Override
        String getNotFoundError() {
            return _local.translate("REQUEST_DENY_ERROR", "{ERROR}", _notFoundError);
        }

        @Override
        String getOutOfRangeError() {
            return _local.translate("REQUEST_DENY_ERROR", "{ERROR}", _outOfRangeError);
        }
    }

    public class TradeRequestCancelCommand extends TradeRequestSendInteractionCommand {

        private TradeRequestCancelCommand(Command parentCommand) {
            super("trade cancel", parentCommand);
        }

        @Override
        void doInteraction(TradeRequest request) {
            request.cancel();
        }

        @Override
        String getNotFoundError() {
            return _local.translate("ERROR_MESSAGE", "{ERROR}", _notFoundError);
        }
    }
}
