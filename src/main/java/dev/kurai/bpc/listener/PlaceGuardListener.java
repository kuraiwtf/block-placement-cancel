package dev.kurai.bpc.listener;

import dev.kurai.bpc.BlockPlacementCancelService;
import dev.kurai.bpc.fake.FakeBlockManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

@RequiredArgsConstructor
public final class PlaceGuardListener implements Listener {

  private final BlockPlacementCancelService service;

  @EventHandler
  public void onPlace(final BlockPlaceEvent event) {
    if (this.service.isAffected(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }
}
