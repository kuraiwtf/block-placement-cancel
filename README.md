# Block-Placement-Cancel

Plugin Spigot (1.8.8) qui empêche des joueurs ciblés de placer des blocs, **sans le « ghost block » côté client** : le placement est annulé côté serveur, et le client est en plus empêché de prédire le bloc posé, donc rien n'apparaît puis disparaît.

> ## ⚠️ Avertissement : proof of concept généré avec l'IA
>
> Ce projet est un **proof of concept (PoC)**. Une grande partie du code et de cette documentation a été **produite avec l'aide d'une IA** (Claude). Il n'a **pas été audité ni testé de manière approfondie**.
>
> - Ne l'utilisez **pas en production** sans relecture et tests approfondis.
> - Des bugs, des cas limites non gérés et des problèmes de performance sont probables.
> - Il est fourni **« tel quel », sans aucune garantie**. L'auteur décline toute responsabilité en cas de dommage.

## Le problème

Quand un joueur fait un clic droit pour poser un bloc, le client Minecraft **affiche immédiatement le bloc** (prédiction côté client) avant d'attendre la réponse du serveur. Si le serveur annule simplement le `BlockPlaceEvent`, le joueur voit le bloc apparaître puis disparaître : c'est le ghost block.

## Fonctionnement

Le plugin combine un blocage serveur et un « overlay » de faux blocs envoyés au client avec [PacketEvents](https://github.com/retrooper/packetevents).

1. **Blocage serveur** (`PlaceGuardListener`) : le `BlockPlaceEvent` des joueurs concernés est annulé.
2. **Ray-casting par paquet** (`RayCastListener`, `BlockRayTracer`) : à chaque paquet de mouvement/rotation du joueur (`PLAYER_POSITION`, `PLAYER_ROTATION`, `PLAYER_POSITION_AND_ROTATION`), le plugin reconstitue la position et le regard du joueur, puis lance un rayon (pas de 0,1 bloc) jusqu'à la portée maximale (4,5 blocs en survie, 6 en créatif). Il en déduit le bloc visé et la case où le bloc serait posé (`TargetResult`).
3. **Faux blocs** (`FakeBlockManager`) : autour de la case de placement, une sphère de rayon 2 de blocs non solides est remplacée, **uniquement côté client de ce joueur**, par un bloc `MOVING_PISTON` via des paquets `BlockChange`. Le client ne peut alors pas prédire de placement dans cette zone. Quand le joueur regarde ailleurs, les vrais états des blocs lui sont renvoyés.
4. **Synchronisation** (`BlockChangeSyncListener`) : si un vrai bloc change (commande, piston, autre plugin) dans la zone d'overlay, l'état réel mémorisé est mis à jour et le paquet est intercepté. Ainsi, l'overlay ne restaurera pas un état périmé.
5. **Nettoyage** : l'overlay est retiré à la déconnexion, à la désactivation du plugin ou quand le joueur est retiré de la liste. Le ray-cast est relancé à la connexion, à la téléportation et au respawn.

## Prérequis

- Serveur **Spigot / Paper 1.8.8** (API compilée contre `spigot-api 1.8.8-R0.1-SNAPSHOT`)
- Plugin **[PacketEvents](https://github.com/retrooper/packetevents)** 2.x installé (dépendance obligatoire, déclarée `depend` dans `plugin.yml`)
- **JDK récent pour compiler** : le code utilise les variables inutilisées `_` (Java 22+)

## Compilation

```bash
./gradlew build
```

Le jar est généré dans `build/libs/block-placement-cancel-0.0.1.jar`. Placez-le dans le dossier `plugins/` du serveur, à côté de PacketEvents, puis redémarrez.

## Utilisation

Commande : `/blockplacementcancel` (alias `/bpc`), permission `bpc.command` (OP par défaut).

| Commande | Description |
|---|---|
| `/bpc add <joueur>` | Empêche le joueur (en ligne) de placer des blocs |
| `/bpc remove <joueur>` | Lui rend la possibilité de placer des blocs |
| `/bpc list` | Liste les joueurs concernés |

La complétion automatique est disponible pour les sous-commandes et les noms de joueurs.

## Limites connues

- La liste des joueurs concernés est **en mémoire uniquement** : elle est perdue au redémarrage ou au rechargement du serveur.
- Le ray-cast est une approximation par pas de 0,1 bloc : il peut ne pas correspondre exactement à celui du client (hitboxes partielles, blocs non solides ciblables, etc.).
- Calcul effectué à chaque paquet de mouvement pour les joueurs concernés : impact possible sur les performances s'ils sont nombreux.
- Accès à l'API Bukkit (monde, blocs) depuis le thread réseau de PacketEvents, ce qui n'est pas thread-safe : des problèmes de concurrence sont possibles.
- Ne gère que le placement de blocs classique, pas les seaux, entités ou autres interactions.
- Conçu et ciblé pour la 1.8.8 ; le comportement sur d'autres versions n'est pas garanti.

## Structure du projet

```
src/main/java/dev/kurai/bpc/
├── BlockPlacementCancelPlugin.java   # point d'entrée, enregistrement des listeners
├── BlockPlacementCancelService.java  # liste des joueurs concernés
├── command/                          # commande /bpc
├── listener/                         # PlaceGuard, RayCast, BlockChangeSync
├── raytrace/                         # BlockRayTracer, TargetResult
└── fake/                             # FakeBlockManager, BlockPos
```
