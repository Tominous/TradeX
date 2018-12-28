package nl.tabuu.tradex.command;

import nl.tabuu.tabuucore.command.*;
import nl.tabuu.tabuucore.configuration.IConfiguration;
import nl.tabuu.tabuucore.util.Dictionary;
import nl.tabuu.tradex.TradeManager;
import nl.tabuu.tradex.TradeRequest;
import nl.tabuu.tradex.TradeX;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TradeCommand extends CCommand {

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

        addArgument(CommandArgumentType.PLAYER, true);
        setRequiredCommandSenderType(CommandSenderType.PLAYER);

        addSubCommand("accept", new TradeRequestAcceptCommand(this));
        addSubCommand("deny", new TradeRequestDenyCommand(this));
        addSubCommand("cancel", new TradeRequestCancelCommand(this));
    }

    public CommandResult onCommand(CommandSender commandSender, List<CommandArgument<?>> list) {
        Player receiver = list.get(0).getValue();
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

    public abstract class TradeRequestInteractionCommand extends CCommand{

        protected String _notFoundError;

        public TradeRequestInteractionCommand(String commandName, CCommand parentCommand) {
            super(commandName, parentCommand);
            _notFoundError = _local.translate("ERROR_NO_REQUEST_FOUND");
        }

        abstract void doInteraction(TradeRequest request);

        abstract String getNotFoundError();
    }

    public abstract class TradeRequestReceivedInteractionCommand extends TradeRequestInteractionCommand{

        private TradeRequestReceivedInteractionCommand(String commandName, CCommand parentCommand) {
            super(commandName, parentCommand);

            addArgument(CommandArgumentType.PLAYER, false);
            setRequiredCommandSenderType(CommandSenderType.PLAYER);
        }

        @Override
        public CommandResult onCommand(CommandSender commandSender, List<CommandArgument<?>> list) {
            Player sender = list.get(0).getValue();
            Player receiver = (Player) commandSender;

            TradeRequest request = sender == null ? _tradeManager.getLastReceivedRequest(receiver) : _tradeManager.getSendRequest(sender);

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

        private TradeRequestSendInteractionCommand(String commandName, CCommand parentCommand) {
            super(commandName, parentCommand);

            setRequiredCommandSenderType(CommandSenderType.PLAYER);
        }

        @Override
        public CommandResult onCommand(CommandSender commandSender, List<CommandArgument<?>> list) {
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

        private TradeRequestAcceptCommand(CCommand parentCommand) {
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

        private TradeRequestDenyCommand(CCommand parentCommand) {
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

        private TradeRequestCancelCommand(CCommand parentCommand) {
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
