package dev.kurai.bpc.raytrace;

import org.bukkit.World;
import org.bukkit.block.Block;

public record TargetResult(World world, int targetX, int targetY, int targetZ, int placeX, int placeY, int placeZ) {

  public Block targetBlock() {
    return world.getBlockAt(targetX, targetY, targetZ);
  }

  public Block placeBlock() {
    return world.getBlockAt(placeX, placeY, placeZ);
  }
}
