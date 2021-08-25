package com.jaoow.damageableblocks;

import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DamageableBlocks extends JavaPlugin implements Runnable, CommandExecutor {

    private int blockRadius = this.getConfig().getInt("radius");
    private int searchDelay = this.getConfig().getInt("delay");
    private final Map<String, EnumMap<Material, Double>> worldDamageMap = Maps.newHashMap();

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();

        loadAllBlocks();
        getCommand("blocksreload").setExecutor(this);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this, searchDelay, searchDelay);
    }

    public void loadAllBlocks() {
        for (String worldName : this.getConfig().getConfigurationSection("worlds").getKeys(false)) {
            String sectionKey = "worlds." + worldName;
            worldDamageMap.put(worldName, parseValueMap(Material.class, sectionKey, 0));
            this.getLogger().info(String.format("Loading damage blocks from world '%s'", worldName));
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Bukkit.getScheduler().cancelTasks(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("blocks.admin")) {
            sender.sendMessage("§cYou don't have permission to do this.");
            return true;
        }

        this.reloadConfig();

        // Cancel tasks
        Bukkit.getScheduler().cancelTasks(this);

        // Reload variables
        blockRadius = this.getConfig().getInt("radius");
        searchDelay = this.getConfig().getInt("delay");

        // Reload all blocks.
        worldDamageMap.clear();
        loadAllBlocks();

        // Init new task
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this, searchDelay, searchDelay);

        sender.sendMessage("§aConfiguration successfully reloaded.");
        return super.onCommand(sender, command, label, args);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player.getGameMode() == GameMode.CREATIVE) continue;

            Location location = player.getLocation();
            String worldName = location.getWorld().getName();
            if (worldDamageMap.containsKey(worldName)) {

                EnumMap<Material, Double> damageableBlocks = worldDamageMap.get(worldName);
                List<Material> materials = BlockUtils.getNearbyMaterials(location, blockRadius);

                for (Map.Entry<Material, Double> entry : damageableBlocks.entrySet()) {
                    if (materials.contains(entry.getKey())) {
                        Bukkit.getScheduler().runTask(this, () -> player.damage(entry.getValue()));
                    }
                }
            }
        }
    }


    // Utility Methods
    private <T extends Enum<T>> @NotNull EnumMap<T, Double> parseValueMap(Class<T> type, String key, int def) {

        EnumMap<T, Double> target = new EnumMap<>(type);
        ConfigurationSection section = this.getConfig().getConfigurationSection(key);
        if (section == null) return target;

        for (String name : section.getKeys(false)) {
            // Warn user if unable to parse enum.
            Optional<T> parsed = this.parseEnum(type, name);
            if (!parsed.isPresent()) {
                this.getLogger().warning("Invalid " + type.getSimpleName() + ": " + name);
                continue;
            }

            // Add the parsed enum and value to the target map.
            target.put(parsed.get(), section.getDouble(name, def));
        }
        return target;
    }

    private <T extends Enum<T>> Optional<T> parseEnum(Class<T> type, String name) {
        name = name.toUpperCase().replaceAll("\\s+", "_").replaceAll("\\W", "");
        try {
            return Optional.of(Enum.valueOf(type, name));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
