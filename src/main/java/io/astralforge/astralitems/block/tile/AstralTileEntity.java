package io.astralforge.astralitems.block.tile;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;

public abstract class AstralTileEntity {

  protected Location location;
  public Location getLocation() {return location;}
  public void setLocation(Location location) {this.location = location;}

  public void tick() {}

  public void onUnload(PersistentDataContainer container) {}
  public void onLoad(PersistentDataContainer container) {}

  public void onDestroy() {}
  public void onPlace(Player player) {}

  public void onInteract(PlayerInteractEvent event) {}

  public interface Builder {
    AstralTileEntity build();
  }

}
