package io.astralforge.astralitems.block.tile;

import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataContainer;

public abstract class AstralTileEntity {

  protected Location location;
  public Location getLocation() {return location;}
  public void setLocation(Location location) {this.location = location;}

  public void tick() {}

  public void onLoad() {}
  public void onUnload() {}

  // TODO: Call these on loading/unloading tile entities
  public void serialize(PersistentDataContainer container) {}
  public void deserialize(PersistentDataContainer container) {}

  public interface Builder {
    AstralTileEntity build();
  }

}
