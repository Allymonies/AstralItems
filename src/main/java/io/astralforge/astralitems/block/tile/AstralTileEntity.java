package io.astralforge.astralitems.block.tile;

import org.bukkit.Location;

public abstract class AstralTileEntity {

  protected Location location;
  public Location getLocation() {return location;}
  public void setLocation(Location location) {this.location = location;}

  public void tick() {}

  public void onLoad() {}
  public void onUnload() {}

  public interface Builder {
    AstralTileEntity build();
  }

}
