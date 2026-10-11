package dev.kurai.bpc.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.Location;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import dev.kurai.bpc.BlockPlacementCancelService;
import dev.kurai.bpc.fake.FakeBlockManager;
import dev.kurai.bpc.raytrace.BlockRayTracer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

@RequiredArgsConstructor
public final class RayCastListener implements Listener, PacketListener {

  private static final double SURVIVAL_REACH = 4.5;
  private static final double CREATIVE_REACH = 6.0;

  private final BlockPlacementCancelService service;
  private final FakeBlockManager fakeBlockManager;
  private final Map<UUID, Cursor> cursors = new ConcurrentHashMap<>();

  @Override
  public void onPacketReceive(final PacketReceiveEvent event) {
    if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())
        || event.getPacketType() == PacketType.Play.Client.PLAYER_FLYING) {
      return;
    }

    final UUID uuid = event.getUser().getUUID();
    if (!this.service.isAffected(uuid)) {
      return;
    }

    final Player player = Bukkit.getPlayer(uuid);
    if (player == null) {
      return;
    }

    final WrapperPlayClientPlayerFlying wrapper = new WrapperPlayClientPlayerFlying(event);
    final Location packetLocation = wrapper.getLocation();
    final org.bukkit.Location fallback = player.getLocation();
    final Cursor previous =
        this.cursors.getOrDefault(
            uuid, new Cursor(fallback.getX(), fallback.getY(), fallback.getZ(), fallback.getYaw(), fallback.getPitch()));

    final double x = wrapper.hasPositionChanged() ? packetLocation.getPosition().getX() : previous.x();
    final double y = wrapper.hasPositionChanged() ? packetLocation.getPosition().getY() : previous.y();
    final double z = wrapper.hasPositionChanged() ? packetLocation.getPosition().getZ() : previous.z();
    final float yaw = wrapper.hasRotationChanged() ? packetLocation.getYaw() : previous.yaw();
    final float pitch = wrapper.hasRotationChanged() ? packetLocation.getPitch() : previous.pitch();

    final Cursor cursor = new Cursor(x, y, z, yaw, pitch);
    this.cursors.put(uuid, cursor);
    this.trace(player, cursor);
  }

  @EventHandler
  public void onJoin(final PlayerJoinEvent event) {
    this.seedAndTrace(event.getPlayer());
  }

  @EventHandler
  public void onTeleport(final PlayerTeleportEvent event) {
    this.seedAndTrace(event.getPlayer());
  }

  @EventHandler
  public void onRespawn(final PlayerRespawnEvent event) {
    this.seedAndTrace(event.getPlayer());
  }

  @EventHandler
  public void onQuit(final PlayerQuitEvent event) {
    final Player player = event.getPlayer();
    this.cursors.remove(player.getUniqueId());
    if (this.service.isAffected(player.getUniqueId())) {
      this.fakeBlockManager.clear(player);
    }
  }

  public void retrace(final Player player) {
    this.seedAndTrace(player);
  }

  private void seedAndTrace(final Player player) {
    if (!this.service.isAffected(player.getUniqueId())) {
      return;
    }

    final org.bukkit.Location location = player.getLocation();
    final Cursor cursor = new Cursor(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    this.cursors.put(player.getUniqueId(), cursor);
    this.trace(player, cursor);
  }

  private void trace(final Player player, final Cursor cursor) {
    final double eyeHeight = player.getEyeHeight();
    final org.bukkit.Location eye =
        new org.bukkit.Location(player.getWorld(), cursor.x(), cursor.y() + eyeHeight, cursor.z(), cursor.yaw(), cursor.pitch());

    final double reach = player.getGameMode() == GameMode.CREATIVE ? CREATIVE_REACH : SURVIVAL_REACH;
    this.fakeBlockManager.update(player, BlockRayTracer.trace(eye, reach));
  }

  private record Cursor(double x, double y, double z, float yaw, float pitch) {}
}
