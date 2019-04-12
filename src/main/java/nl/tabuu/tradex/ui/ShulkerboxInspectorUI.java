package nl.tabuu.tradex.ui;

import nl.tabuu.tabuucore.inventory.InventorySize;
import nl.tabuu.tabuucore.inventory.ui.InventoryFormUI;
import nl.tabuu.tabuucore.inventory.ui.InventoryUI;
import nl.tabuu.tabuucore.inventory.ui.element.Button;
import nl.tabuu.tabuucore.inventory.ui.element.style.Style;
import nl.tabuu.tabuucore.inventory.ui.graphics.brush.Brush;
import nl.tabuu.tabuucore.inventory.ui.graphics.brush.IBrush;
import nl.tabuu.tabuucore.item.ItemBuilder;
import nl.tabuu.tabuucore.util.Dictionary;
import nl.tabuu.tabuucore.util.vector.Vector2f;
import nl.tabuu.tradex.TradeX;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.IllegalPluginAccessException;

public class ShulkerboxInspectorUI extends InventoryFormUI {

    private Inventory _shulkerInventory;
    private InventoryUI _returnUI;
    private Dictionary _local;

    public ShulkerboxInspectorUI(Inventory shulkerInventory, InventoryUI returnUI) {
        super("Shulker Box", InventorySize.FOUR_ROWS);

        _shulkerInventory = shulkerInventory;
        _returnUI = returnUI;

        _local = TradeX.getInstance().getConfigurationManager().getConfiguration("lang").getDictionary("");

        setTile(_local.translate("UI_SHULKER_TITLE"));
        reload();
    }

    @Override
    protected void draw() {
        ItemBuilder
                black = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).setDisplayName(" "),
                barrier = new ItemBuilder(Material.BARRIER).setDisplayName(" ");

        Style returnButtonStyle = new Style(barrier.setDisplayName(_local.translate("UI_SHULKER_EXIT")).build(), barrier.build());

        IBrush blackBrush = new Brush(black.build());

        for(int i = 0; i < 27; i++){
            ItemStack item = _shulkerInventory.getStorageContents()[i];
            getInventory().setItem(i, item);
        }

        setBrush(blackBrush);
        drawRectangle(new Vector2f(0, 3), new Vector2f(8, 3));

        Button returnButton = new Button(returnButtonStyle, this::onReturnButtonClick);
        setElement(new Vector2f(4, 3), returnButton);

        super.draw();
    }

    private void onReturnButtonClick(Player player){
        this.close(player);
    }

    @Override
    public void onClose(Player player) {
        try{
            Bukkit.getScheduler().runTask(TradeX.getInstance(), () -> _returnUI.open(player));
        }
        catch (IllegalPluginAccessException ignore) {}
    }
}
