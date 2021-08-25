package com.jaoow.damageableblocks;

import com.google.common.collect.Lists;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class BlockUtils {

    public static @NotNull List<Material> getNearbyMaterials(@NotNull Location location, int radius) {
        return getNearbyBlocks(location, radius, radius, radius).stream().map(Block::getType).collect(Collectors.toList());
    }

    public static @NotNull List<Block> getNearbyBlocks(@NotNull Location location, int i, int j, int k) {
        List<Block> blocks = Lists.newArrayList();
        for (int x = location.getBlockX() - i; x <= location.getBlockX() + i; x++) {
            for (int y = location.getBlockY() - j; y <= location.getBlockY() + j; y++) {
                for (int z = location.getBlockZ() - k; z <= location.getBlockZ() + k; z++) {
                    blocks.add(location.getWorld().getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

}
