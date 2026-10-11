package dev.kurai.bpc.command;

import dev.kurai.bpc.BlockPlacementCancelService;
import dev.kurai.bpc.listener.RayCastListener;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class BlockPlacementCancelCommand implements CommandExecutor, TabCompleter {

  private final BlockPlacementCancelService service;
  private final RayCastListener rayCastListener;

  @Override
  public boolean onCommand(
      final CommandSender sender, final Command command, final String label, final String[] args) {
    if (args.length == 0) {
      sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <add|remove|list> [player]");
      return true;
    }

    switch (args[0].toLowerCase()) {
      case "add" -> this.handleAdd(sender, label, args);
      case "remove" -> this.handleRemove(sender, label, args);
      case "list" -> this.handleList(sender);
      default -> sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <add|remove|list> [player]");
    }

    return true;
  }

  private void handleAdd(final CommandSender sender, final String label, final String[] args) {
    final Player target = this.resolveTarget(sender, label, args);
    if (target == null) {
      return;
    }

    if (this.service.addPlayer(target.getUniqueId())) {
      sender.sendMessage(ChatColor.GREEN + target.getName() + " can no longer place blocks.");
      this.rayCastListener.retrace(target);
    } else {
      sender.sendMessage(ChatColor.YELLOW + target.getName() + " is already affected.");
    }
  }

  private void handleRemove(final CommandSender sender, final String label, final String[] args) {
    final Player target = this.resolveTarget(sender, label, args);
    if (target == null) {
      return;
    }

    if (this.service.removePlayer(target.getUniqueId())) {
      sender.sendMessage(ChatColor.GREEN + target.getName() + " can place blocks again.");
    } else {
      sender.sendMessage(ChatColor.YELLOW + target.getName() + " was not affected.");
    }
  }

  private void handleList(final CommandSender sender) {
    final String names =
        this.service.affectedPlayers().stream()
            .map(Bukkit::getOfflinePlayer)
            .map(player -> player.getName() == null ? player.getUniqueId().toString() : player.getName())
            .collect(Collectors.joining(", "));

    sender.sendMessage(
        ChatColor.GOLD + "Affected players: " + (names.isEmpty() ? ChatColor.GRAY + "none" : names));
  }

  private Player resolveTarget(final CommandSender sender, final String label, final String[] args) {
    if (args.length < 2) {
      sender.sendMessage(ChatColor.RED + "Usage: /" + label + " " + args[0] + " <player>");
      return null;
    }

    final Player target = Bukkit.getPlayerExact(args[1]);
    if (target == null) {
      sender.sendMessage(ChatColor.RED + "Player " + args[1] + " is not online.");
    }

    return target;
  }

  @Override
  public List<String> onTabComplete(
      final CommandSender sender, final Command command, final String alias, final String[] args) {
    if (args.length == 1) {
      return this.filter(List.of("add", "remove", "list"), args[0]);
    }

    if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
      return this.filter(this.onlinePlayerNames(args[0].equalsIgnoreCase("remove")), args[1]);
    }

    return List.of();
  }

  private List<String> onlinePlayerNames(final boolean onlyAffected) {
    return Bukkit.getOnlinePlayers().stream()
        .filter(player -> !onlyAffected || this.service.isAffected(player.getUniqueId()))
        .map(Player::getName)
        .collect(Collectors.toList());
  }

  private List<String> filter(final List<String> options, final String prefix) {
    return options.stream()
        .filter(option -> option.toLowerCase().startsWith(prefix.toLowerCase()))
        .collect(Collectors.toList());
  }
}
