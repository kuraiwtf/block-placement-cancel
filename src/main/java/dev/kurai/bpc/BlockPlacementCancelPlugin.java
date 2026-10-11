package dev.kurai.bpc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import dev.kurai.bpc.command.BlockPlacementCancelCommand;
import dev.kurai.bpc.fake.FakeBlockManager;
import dev.kurai.bpc.listener.BlockChangeSyncListener;
import dev.kurai.bpc.listener.PlaceGuardListener;
import dev.kurai.bpc.listener.RayCastListener;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockPlacementCancelPlugin extends JavaPlugin {

  private BlockPlacementCancelService service;
  private FakeBlockManager fakeBlockManager;

  @Override
  public void onEnable() {
    this.service = new BlockPlacementCancelService();
    this.fakeBlockManager = new FakeBlockManager();

    final RayCastListener rayCastListener =
        new RayCastListener(this.service, this.fakeBlockManager);

    final PluginManager pluginManager = this.getServer().getPluginManager();
    pluginManager.registerEvents(rayCastListener, this);
    pluginManager.registerEvents(new PlaceGuardListener(this.service), this);

    final EventManager eventManager = PacketEvents.getAPI().getEventManager();
    eventManager.registerListener(rayCastListener, PacketListenerPriority.NORMAL);
    eventManager.registerListener(
        new BlockChangeSyncListener(this.fakeBlockManager), PacketListenerPriority.NORMAL);

    final BlockPlacementCancelCommand executor =
        new BlockPlacementCancelCommand(this.service, rayCastListener);
    final PluginCommand pluginCommand = this.getCommand("blockplacementcancel");
    pluginCommand.setExecutor(executor);
    pluginCommand.setTabCompleter(executor);
  }

  @Override
  public void onDisable() {
    if (this.fakeBlockManager != null) {
      for (final Player player : Bukkit.getOnlinePlayers()) {
        this.fakeBlockManager.clear(player);
      }
    }
  }
}
