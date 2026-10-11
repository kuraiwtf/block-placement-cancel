package dev.kurai.bpc.fake;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import dev.kurai.bpc.raytrace.TargetResult;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public final class FakeBlockManager {

  private static final int RADIUS = 2;
  private static final int RADIUS_SQUARED = RADIUS * RADIUS;

  private final Map<UUID, Map<BlockPos, WrappedBlockState>> active = new ConcurrentHashMap<>();

  public void update(final Player player, final Optional<TargetResult> target) {
    if (target.isEmpty()) {
      this.clear(player);
      return;
    }

    final TargetResult result = target.get();
    final Block centerBlock = result.placeBlock();

    if (centerBlock.getType().isSolid()) {
      this.clear(player);
      return;
    }

    final Map<BlockPos, WrappedBlockState> current =
        this.active.computeIfAbsent(player.getUniqueId(), _ -> new HashMap<>());
    final Set<BlockPos> desired = this.sphere(result.world(), result.placeX(), result.placeY(), result.placeZ());

    final Iterator<Map.Entry<BlockPos, WrappedBlockState>> it = current.entrySet().iterator();
    while (it.hasNext()) {
      final Map.Entry<BlockPos, WrappedBlockState> entry = it.next();
      if (!desired.contains(entry.getKey())) {
        this.sendBlock(player, entry.getKey(), entry.getValue());
        it.remove();
      }
    }

    for (final BlockPos pos : desired) {
      if (current.containsKey(pos)) {
        continue;
      }

      final Block block = pos.world().getBlockAt(pos.x(), pos.y(), pos.z());
      final WrappedBlockState realState = SpigotConversionUtil.fromBukkitMaterialData(block.getState().getData());
      current.put(pos, realState);
      this.sendBlock(player, pos, StateTypes.MOVING_PISTON.createBlockState());
    }
  }

  public boolean isTracking(final UUID uuid) {
    final Map<BlockPos, WrappedBlockState> current = this.active.get(uuid);
    return current != null && !current.isEmpty();
  }

  public boolean syncRealState(
      final UUID uuid, final World world, final int x, final int y, final int z, final WrappedBlockState newState) {
    final Map<BlockPos, WrappedBlockState> current = this.active.get(uuid);
    if (current == null) {
      return false;
    }

    final BlockPos pos = new BlockPos(world, x, y, z);
    if (!current.containsKey(pos)) {
      return false;
    }

    current.put(pos, newState);
    return true;
  }

  public void clear(final Player player) {
    final Map<BlockPos, WrappedBlockState> current = this.active.remove(player.getUniqueId());
    if (current == null) {
      return;
    }

    for (final Map.Entry<BlockPos, WrappedBlockState> entry : current.entrySet()) {
      this.sendBlock(player, entry.getKey(), entry.getValue());
    }
  }

  private Set<BlockPos> sphere(final World world, final int centerX, final int centerY, final int centerZ) {
    final Set<BlockPos> positions = new HashSet<>();

    for (int dx = -RADIUS; dx <= RADIUS; dx++) {
      for (int dy = -RADIUS; dy <= RADIUS; dy++) {
        for (int dz = -RADIUS; dz <= RADIUS; dz++) {
          if (dx * dx + dy * dy + dz * dz > RADIUS_SQUARED) {
            continue;
          }

          final int x = centerX + dx;
          final int y = centerY + dy;
          final int z = centerZ + dz;

          if (!world.getBlockAt(x, y, z).getType().isSolid()) {
            positions.add(new BlockPos(world, x, y, z));
          }
        }
      }
    }

    return positions;
  }

  private void sendBlock(final Player player, final BlockPos pos, final WrappedBlockState state) {
    if (!pos.world().equals(player.getWorld()) || !pos.world().isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
      return;
    }

    PacketEvents.getAPI()
        .getPlayerManager()
        .sendPacketSilently(player, new WrapperPlayServerBlockChange(new Vector3i(pos.x(), pos.y(), pos.z()), state));
  }
}
