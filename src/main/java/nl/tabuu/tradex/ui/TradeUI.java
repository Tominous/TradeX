package nl.tabuu.tradex.ui;

import nl.tabuu.tabuucore.configuration.IConfiguration;
import nl.tabuu.tabuucore.inventory.InventorySize;
import nl.tabuu.tabuucore.inventory.ui.InventoryFormUI;
import nl.tabuu.tabuucore.inventory.ui.InventoryUIClick;
import nl.tabuu.tabuucore.inventory.ui.InventoryUIDrag;
import nl.tabuu.tabuucore.inventory.ui.element.Checkbox;
import nl.tabuu.tabuucore.inventory.ui.element.Element;
import nl.tabuu.tabuucore.inventory.ui.element.ElementGroup;
import nl.tabuu.tabuucore.inventory.ui.element.ItemInput;
import nl.tabuu.tabuucore.inventory.ui.element.style.Style;
import nl.tabuu.tabuucore.inventory.ui.element.style.ToggleableStyle;
import nl.tabuu.tabuucore.inventory.ui.graphics.InventoryCanvas;
import nl.tabuu.tabuucore.inventory.ui.graphics.brush.Brush;
import nl.tabuu.tabuucore.inventory.ui.graphics.brush.IBrush;
import nl.tabuu.tabuucore.util.Dictionary;
import nl.tabuu.tabuucore.util.ItemBuilder;
import nl.tabuu.tabuucore.util.vector.Vector2f;
import nl.tabuu.tradex.Trade;
import nl.tabuu.tradex.TradeUpdateReason;
import nl.tabuu.tradex.TradeX;
import nl.tabuu.tradex.Trader;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.ArrayList;
import java.util.List;

public class TradeUI extends InventoryFormUI {

    private Trade _trade;
    private Trader _self, _other;
    private ElementGroup _itemInputElements;
    private Dictionary _local;
    private IConfiguration _config;
    private boolean _excuseNextClose;

    public TradeUI(Trade trade, Player self, Player other) {
        super("", InventorySize.DOUBLE_CHEST);

        _trade = trade;
        _self = trade.getTrader(self);
        _other = trade.getTrader(other);
        _excuseNextClose = false;

        _itemInputElements = new ElementGroup();

        _local = TradeX.getInstance().getConfigurationManager().getConfiguration("lang").getDictionary("");
        _config = TradeX.getInstance().getConfigurationManager().getConfiguration("config");

        _trade.onUpdate(this::onTradeChange);

        Style itemInputStyle = new Style(Material.AIR, Material.AIR);

        for(int x = 0; x < 4; x++){
            for(int y = 0; y < 5; y++){
                Vector2f position = new Vector2f(x, y);
                ItemInput itemInput = new ItemInput(itemInputStyle, false, this::onItemInputChange);

                _itemInputElements.addElements(itemInput);
                setElement(position, itemInput);
            }
        }

        setTile(_local.translate("UI_TITLE", "{PLAYER}", _other.getPlayer().getDisplayName()));
        reload();
    }

    @Override
    protected void draw() {

        //region Items, styles, brushes
        ItemBuilder
                green = new ItemBuilder(Material.GREEN_STAINED_GLASS_PANE).setDisplayName(" "),
                black = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" "),
                red = new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayName(" "),
                blue = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setDisplayName(" "),
                bars = new ItemBuilder(Material.IRON_BARS).setDisplayName(" ");

        ItemStack confirmIndicator = _other.hasConfirmed() ?
                green.setDisplayName(_local.translate("UI_ACCEPTED")).build() :
                blue.setDisplayName(_local.translate("UI_OTHER_WAITING")).build();

        ToggleableStyle confirmCheckboxStyle = new ToggleableStyle(
                green.setDisplayName(_local.translate("UI_WAITING")).build(),
                blue.setDisplayName(_local.translate("UI_ACCEPT")).build(),
                red.setDisplayName(_local.translate("UI_ACCEPT_ERROR", "{ERROR}", _local.translate("ERROR_INVENTORY_OVERLOAD"))).build());

        IBrush barBrush = new Brush(bars.build());
        //endregion

        //region Timer
        Style timerStyle = new Style(green.build(), black.build());

        for(int i = 0; i < 5; i++){
            Vector2f position = new Vector2f(4, i);
            if((5 - _trade.getConfirmTimer()) >= i)
                setItemAt(position, timerStyle.getEnabled());
            else
                setItemAt(position, timerStyle.getDisabled());
        }
        //endregion

        //region Bottom buttons
        setBrush(barBrush);
        drawLine(new Vector2f(0, 5), new Vector2f(8, 5));

        Checkbox confirmCheckbox = new Checkbox(confirmCheckboxStyle, this::onConfirmStatusChange);
        confirmCheckbox.setValue(_self.hasConfirmed());

        confirmCheckbox.setEnabled(_trade.canTraderAccept(_self));

        setItemAt(new Vector2f(5, 5), confirmIndicator);
        setElement(new Vector2f(3, 5), confirmCheckbox);
        //endregion

        //region Other trader's items
        int i = 0;
        for(int x = 5; x < 9; x++){
            for(int y = 0; y < 5; y++){
                Vector2f position = new Vector2f(x, y);
                if(_other.getOffer().length > i){
                    ItemStack item = _other.getOffer()[i];
                    setItemAt(position, item);
                }
                i++;
            }
        }
        //endregion

        super.draw();
    }

    @Override
    public void onClose(Player player) {
        super.onClose(player);

        if(_excuseNextClose)
            _excuseNextClose = false;
        else
            _trade.cancel();
    }

    @Override
    public void onClickUI(Player player, InventoryUIClick click) {
        super.onClickUI(player, click);

        Vector2f clickedPosition = InventoryCanvas.vectorToSlot(click.getSlot());

        Vector2f boxStart = new Vector2f(5, 0);
        Vector2f boxStop = new Vector2f(9, 5);

        if(boxStart.getIntX() <= clickedPosition.getIntX() &&
        clickedPosition.getIntX() <= boxStop.getIntX() &&
        boxStart.getIntY() <= clickedPosition.getIntY() &&
        clickedPosition.getIntY() <= boxStop.getIntY()){
            if(click.getClickedItem().getType().name().endsWith("SHULKER_BOX") && click.getClickedItem().hasItemMeta()){
                BlockStateMeta meta = (BlockStateMeta) click.getClickedItem().getItemMeta();
                ShulkerBox box = (ShulkerBox) meta.getBlockState();
                _excuseNextClose = true;
                new ShulkerboxInspectorUI(box.getInventory(), this).open(player);
            }
        }
    }

    private void onTradeChange(TradeUpdateReason reason) {
        Player player = _self.getPlayer();

        switch (reason){
            case OFFER_CONFIRMED:
                player.playNote(player.getLocation(), Instrument.XYLOPHONE, Note.flat(0, Note.Tone.C));
                break;
            case TRADE_CONFIRMED:
                player.playNote(player.getLocation(), Instrument.CHIME, Note.flat(0, Note.Tone.C));
                break;
            case TRADE_CONFIRM_STOP:
                player.playNote(player.getLocation(), Instrument.STICKS, Note.flat(0, Note.Tone.C));
                break;
            case TRADE_COMPLETED:
                player.playNote(player.getLocation(), Instrument.BELL, Note.flat(0, Note.Tone.C));
                break;
            case TIMER_UPDATE:
                player.playNote(player.getLocation(), Instrument.BASS_GUITAR, Note.sharp(1, Note.Tone.C));
                break;
        }

        switch (reason){
            case TRADE_COMPLETED:
            case TRADE_CANCELED:
                player.closeInventory();
                for(Element element : _itemInputElements.getElements())
                    element.setEnabled(false);
                break;

            default:
                draw();
        }
    }

    private void onConfirmStatusChange(Player player, boolean status){
        if(status && _trade.canTraderAccept(_self))
            _trade.confirm(_self);
        else
            _trade.stopConfirm();
    }

    private void onItemInputChange(Player player, ItemStack itemStack){
        List<ItemStack> offer = new ArrayList<>();

        for(Element element : _itemInputElements.getElements()){
            if(element instanceof ItemInput){
                ItemInput itemInput = (ItemInput) element;
                offer.add(itemInput.getValue());
            }
        }

        _trade.setOffer(_self, offer.stream().toArray(ItemStack[]::new));
    }
}
