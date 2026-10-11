package dev.kurai.bpc.raytrace;

import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public final class BlockRayTracer {

  private static final double STEP = 0.1;

  private BlockRayTracer() {}

  public static Optional<TargetResult> trace(final Location eye, final double maxDistance) {
    final World world = eye.getWorld();
    final Vector direction = eye.getDirection().normalize();

    int prevX = Integer.MIN_VALUE;
    int prevY = Integer.MIN_VALUE;
    int prevZ = Integer.MIN_VALUE;

    final int steps = (int) Math.ceil(maxDistance / STEP);
    for (int i = 0; i <= steps; i++) {
      final double distance = i * STEP;
      final double x = eye.getX() + direction.getX() * distance;
      final double y = eye.getY() + direction.getY() * distance;
      final double z = eye.getZ() + direction.getZ() * distance;

      final int blockX = (int) Math.floor(x);
      final int blockY = (int) Math.floor(y);
      final int blockZ = (int) Math.floor(z);

      if (world.getBlockAt(blockX, blockY, blockZ).getType().isSolid()) {
        if (prevX == Integer.MIN_VALUE || (prevX == blockX && prevY == blockY && prevZ == blockZ)) {
          return Optional.empty();
        }

        return Optional.of(new TargetResult(world, blockX, blockY, blockZ, prevX, prevY, prevZ));
      }

      prevX = blockX;
      prevY = blockY;
      prevZ = blockZ;
    }

    return Optional.empty();
  }
}
