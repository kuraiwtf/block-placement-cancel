package dev.kurai.bpc.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange.EncodedBlock;
import dev.kurai.bpc.fake.FakeBlockManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Keeps FakeBlockManager's cached real-block state in sync with actual block changes (commands,
 * pistons, other plugins) that occur at a position currently overlaid with a fake block, so the
 * fake overlay doesn't later get reverted to a stale state.
 */
@RequiredArgsConstructor
public final class BlockChangeSyncListener implements PacketListener {

  private final FakeBlockManager fakeBlockManager;

  @Override
  public void onPacketSend(final PacketSendEvent event) {
    if (event.getPacketType() == PacketType.Play.Server.BLOCK_CHANGE) {
      this.handleBlockChange(event);
    } else if (event.getPacketType() == PacketType.Play.Server.MULTI_BLOCK_CHANGE) {
      this.handleMultiBlockChange(event);
    }
  }

  private void handleBlockChange(final PacketSendEvent event) {
    final UUID uuid = event.getUser().getUUID();
    if (!this.fakeBlockManager.isTracking(uuid)) {
      return;
    }

    final Player player = Bukkit.getPlayer(uuid);
    if (player == null) {
      return;
    }

    final WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(event);
    final Vector3i pos = wrapper.getBlockPosition();
    final World world = player.getWorld();

    if (this.fakeBlockManager.syncRealState(
        uuid, world, pos.getX(), pos.getY(), pos.getZ(), wrapper.getBlockState())) {
      event.setCancelled(true);
    }
  }

  private void handleMultiBlockChange(final PacketSendEvent event) {
    final UUID uuid = event.getUser().getUUID();
    if (!this.fakeBlockManager.isTracking(uuid)) {
      return;
    }

    final Player player = Bukkit.getPlayer(uuid);
    if (player == null) {
      return;
    }

    final WrapperPlayServerMultiBlockChange wrapper = new WrapperPlayServerMultiBlockChange(event);
    final World world = player.getWorld();
    final EncodedBlock[] blocks = wrapper.getBlocks();

    final List<EncodedBlock> remaining = new ArrayList<>(blocks.length);
    for (final EncodedBlock block : blocks) {
      final WrappedBlockState state = block.getBlockState(event.getUser().getClientVersion());
      if (!this.fakeBlockManager.syncRealState(uuid, world, block.getX(), block.getY(), block.getZ(), state)) {
        remaining.add(block);
      }
    }

    if (remaining.size() == blocks.length) {
      return;
    }

    if (remaining.isEmpty()) {
      event.setCancelled(true);
      return;
    }

    wrapper.setBlocks(remaining.toArray(new EncodedBlock[0]));
  }
}
