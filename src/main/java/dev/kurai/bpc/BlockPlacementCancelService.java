package dev.kurai.bpc;

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;

@Getter
public final class BlockPlacementCancelService {

  @Getter private static BlockPlacementCancelService instance;

  private final Set<UUID> affectedPlayers;

  public BlockPlacementCancelService() {
    instance = this;

    this.affectedPlayers = Sets.newHashSet();
  }

  public boolean addPlayer(UUID player) {
    return this.affectedPlayers.add(player);
  }

  public boolean removePlayer(UUID player) {
    return this.affectedPlayers.remove(player);
  }

  public boolean isAffected(UUID player) {
    return this.affectedPlayers.contains(player);
  }
}
