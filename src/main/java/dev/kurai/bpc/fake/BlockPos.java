package dev.kurai.bpc.fake;

import org.bukkit.World;

record BlockPos(World world, int x, int y, int z) {}
