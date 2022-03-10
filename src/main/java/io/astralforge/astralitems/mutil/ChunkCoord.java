package io.astralforge.astralitems.mutil;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

public final class ChunkCoord {
  public UUID world;
  public int x;
  public int z;

  public ChunkCoord(UUID world, int x, int z) {
    this.world = world;
    this.x = x;
    this.z = z;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ChunkCoord that = (ChunkCoord) o;
    return x == that.x && z == that.z && Objects.equals(world, that.world);
  }

  @Override
  public int hashCode() {
    return Objects.hash(world, x, z);
  }

  public static ChunkCoord from(Location location) {
    World world = location.getWorld();
    return new ChunkCoord(
        world != null ? world.getUID() : null,
        location.getChunk().getX(),
        location.getChunk().getZ()
    );
  }

  public static ChunkCoord from(Chunk chunk) {
    return new ChunkCoord(
        chunk.getWorld().getUID(),
        chunk.getX(),
        chunk.getZ()
    );
  }
}
