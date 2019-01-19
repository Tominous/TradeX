package nl.tabuu.tradex.trade;

import nl.tabuu.tabuucore.configuration.IConfiguration;
import nl.tabuu.tabuucore.util.Dictionary;
import nl.tabuu.tabuucore.util.ItemList;
import nl.tabuu.tempstoragez.api.TempStorageAPI;
import nl.tabuu.tempstoragez.api.storage.IStorage;
import nl.tabuu.tradex.TradeX;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Consumer;

public class Trade {

    private HashMap<UUID, Trader> _traders;
    private List<Consumer<TradeUpdateReason>> _listeners;
    private int _timerID, _confirmTimer, _confirmTime;
    private boolean _completed;
    private Dictionary _local;
    private IConfiguration _config;

    public Trade(Player playerA, Player playerB){
        _traders = new HashMap<>();
        _listeners = new ArrayList<>();

        _confirmTime = 6;
        _confirmTimer = _confirmTime;
        _timerID = -1;
        _completed = false;

        _local = TradeX.getInstance().getConfigurationManager().getConfiguration("lang").getDictionary("");
        _config = TradeX.getInstance().getConfigurationManager().getConfiguration("config");

        _traders.put(playerA.getUniqueId(), new Trader(playerA));
        _traders.put(playerB.getUniqueId(), new Trader(playerB));
    }

    public void cancel(){
        if(complete())
            return;

        sendUpdate(TradeUpdateReason.TRADE_CANCELED);
        TradeManager.getInstance().removeTrade(this);

        _traders.values().forEach(trader -> {
            giveItems(trader, trader.getOffer());

            Player player = trader.getPlayer();
            player.sendMessage(_local.translate("TRADE_CANCELED"));
        });
    }

    public void confirm(Trader trader){
        trader.setConfirmed(true);
        sendUpdate(TradeUpdateReason.OFFER_CONFIRMED);

        if(_traders.values().stream().allMatch(Trader::hasConfirmed)){
            sendUpdate(TradeUpdateReason.TRADE_CONFIRMED);
            _timerID = Bukkit.getScheduler().scheduleSyncRepeatingTask(TradeX.getInstance(), this::timerTick, 0, 20L);
        }
    }

    public void stopConfirm(){
        resetTimer();
        _traders.values().forEach((t) -> t.setConfirmed(false));
        sendUpdate(TradeUpdateReason.TRADE_CONFIRM_STOP);
    }

    private void trade(){
        if(complete())
            return;

        List<Trader> traders = new ArrayList<>(_traders.values());
        Trader traderA = traders.get(0);
        Trader traderB = traders.get(1);

        if(traderA.hasConfirmed() && traderB.hasConfirmed() && traderA.getPlayer().isOnline() && traderB.getPlayer().isOnline()){
            sendUpdate(TradeUpdateReason.TRADE_COMPLETED);
            TradeManager.getInstance().removeTrade(this);
            resetTimer();

            giveItems(traderA, traderB.getOffer());
            giveItems(traderB, traderA.getOffer());

            getTraders().forEach(trader -> {
                Player player = trader.getPlayer();
                player.sendMessage(_local.translate("TRADE_COMPLETED"));
            });
        }
        else
            cancel();
    }

    private void giveItems(Trader trader, ItemStack... items){
        Player player = trader.getPlayer();

        ItemList compressed = new ItemList();
        compressed.stackAll(items);

        Collection<ItemStack> nonFittingItems = player.getInventory().addItem(compressed.stream().toArray(ItemStack[]::new)).values();

        if(TradeX.getInstance().useTempStorageZ()){
            Trader other = getTraders().stream().filter(t -> !t.equals(trader)).findFirst().orElse(trader);
            String description = _local.translate("TEMPSTORAGEZ_DESCRIPTION", "{PLAYER}", other.getPlayer().getDisplayName());
            long time = _config.getTime("TempStorageZTime");

            TempStorageAPI api = TempStorageAPI.getInstance();
            IStorage storage = api.getStorage(player);

            nonFittingItems.forEach(item -> storage.addItem(item, time, description));
        }
        else
            nonFittingItems.forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private void timerTick(){
        if(_traders.values().stream().allMatch(Trader::hasConfirmed) && _traders.values().stream().allMatch(this::canTraderAccept)){
            _confirmTimer--;
            sendUpdate(TradeUpdateReason.TIMER_UPDATE);

            if(_confirmTimer <= 0)
                trade();
        }
        else
            stopConfirm();
    }

    private void resetTimer(){
        if(_timerID != -1){
            Bukkit.getScheduler().cancelTask(_timerID);
            _timerID = -1;
        }

        _confirmTimer = _confirmTime;

        sendUpdate(TradeUpdateReason.TIMER_UPDATE);
    }

    public void onUpdate(Consumer<TradeUpdateReason> consumer){
        _listeners.add(consumer);
    }

    private void sendUpdate(TradeUpdateReason updateReason){
        _listeners.forEach(c -> c.accept(updateReason));
    }

    public void setOffer(Trader trader, ItemStack[] offer){
        trader.setOffer(offer);
        sendUpdate(TradeUpdateReason.OFFER_CHANGED);

        if(_traders.values().stream().allMatch(Trader::hasConfirmed))
            stopConfirm();
    }

    public int getConfirmTimer(){
        return _confirmTimer;
    }

    public Trader getTrader(Player player){
        return _traders.get(player.getUniqueId());
    }

    public boolean isTrader(Player player) {
        return getTrader(player) != null;
    }

    public boolean canTraderAccept(Trader trader){
        if(!_config.getBoolean("CanAcceptOverloadTrade")){
            Trader other = getTraders().stream().filter(t -> t != trader).findAny().get();

            ItemList allItems = new ItemList();
            allItems.addAll(trader.getPlayer().getInventory().getStorageContents());
            allItems = allItems.clone();
            return allItems.stackAll(other.getOffer().clone()).isEmpty();
        }

        return true;
    }

    public Collection<Trader> getTraders(){
        return _traders.values();
    }

    private boolean complete(){
        if(!_completed){
            _completed = true;
            return false;
        }
        return true;
    }
}
