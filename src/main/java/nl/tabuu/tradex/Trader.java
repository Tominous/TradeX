package nl.tabuu.tradex;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Trader {

    private UUID _uuid;
    private ItemStack[] _offer;
    private boolean _confirmed;

    public Trader(Player player){
        _uuid = player.getUniqueId();
        _offer = new ItemStack[0];
    }

    public Player getPlayer(){
        return Bukkit.getPlayer(_uuid);
    }

    public ItemStack[] getOffer(){
        return _offer;
    }

    public boolean hasConfirmed(){
        return _confirmed;
    }

    public void setConfirmed(boolean confirmed){
        _confirmed = confirmed;
    }

    public void setOffer(ItemStack... offer){
        _offer = offer;
        _confirmed = false;
    }
}
