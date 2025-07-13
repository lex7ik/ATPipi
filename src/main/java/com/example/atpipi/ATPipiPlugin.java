package com.example.atpipi;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class ATPipiPlugin extends JavaPlugin implements CommandExecutor {

    private boolean isLegacyVersion;
    private final Set<UUID> activePlayers = new HashSet<>();
    private final Map<UUID, BukkitRunnable> activeTasks = new HashMap<>();

    @Override
    public void onEnable() {
        isLegacyVersion = !Bukkit.getVersion().contains("1.13") &&
                !Bukkit.getVersion().contains("1.14") &&
                !Bukkit.getVersion().contains("1.15") &&
                !Bukkit.getVersion().contains("1.16") &&
                !Bukkit.getVersion().contains("1.17") &&
                !Bukkit.getVersion().contains("1.18") &&
                !Bukkit.getVersion().contains("1.19") &&
                !Bukkit.getVersion().contains("1.20") &&
                !Bukkit.getVersion().contains("1.21");

        this.getCommand("atpipi").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return false;

        Player player = (Player) sender;

        if (!player.hasPermission("atpipi.use")) {
            player.sendMessage("У вас нет прав для использования этой команды.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
            stopPipiAction(player);
            return true;
        }

        if (activePlayers.contains(player.getUniqueId())) {
            player.sendMessage("Вы уже используете Pipi! Используйте /atpipi off для остановки.");
            return true;
        }

        startPipiAction(player);
        return true;
    }

    private void startPipiAction(Player player) {
        UUID uuid = player.getUniqueId();
        activePlayers.add(uuid);
        Set<Player> hitPlayers = new HashSet<>();
        final boolean[] hasNotified = {false};

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!activePlayers.contains(uuid)) {
                    this.cancel();
                    return;
                }

                if (ticks >= 100) {
                    stopPipiAction(player);
                    return;
                }

                dropYellowConcrete(player, hitPlayers, hasNotified);
                ticks++;
            }
        };

        task.runTaskTimer(this, 0L, 1L);
        activeTasks.put(uuid, task);
        player.sendMessage("💛 Pipi началось! Используй /atpipi off для остановки.");
    }

    private void stopPipiAction(Player player) {
        UUID uuid = player.getUniqueId();
        if (activePlayers.contains(uuid)) {
            activePlayers.remove(uuid);
            BukkitRunnable task = activeTasks.remove(uuid);
            if (task != null) task.cancel();
            player.sendMessage("⛔️ Pipi остановлено.");
        } else {
            player.sendMessage("❌ У вас не было активного Pipi.");
        }
    }

    private void dropYellowConcrete(Player player, Set<Player> hitPlayers, boolean[] hasNotified) {
        Material concreteMaterial = isLegacyVersion ? Material.valueOf("CONCRETE") : Material.YELLOW_CONCRETE;

        final org.bukkit.entity.Item item = player.getWorld().dropItemNaturally(player.getLocation(), new org.bukkit.inventory.ItemStack(concreteMaterial, 1));
        item.setPickupDelay(Integer.MAX_VALUE);
        Vector direction = player.getLocation().getDirection().normalize().multiply(0.5);
        item.setVelocity(direction);

        new BukkitRunnable() {
            @Override
            public void run() {
                item.remove();
            }
        }.runTaskLater(this, 20L);

        for (Entity entity : player.getNearbyEntities(1, 1, 1)) {
            if (entity instanceof Player) {
                Player target = (Player) entity;
                if (!hitPlayers.contains(target)) {
                    hitPlayers.add(target);
                    player.sendMessage("Я попал на " + target.getName());
                    if (!hasNotified[0]) {
                        target.sendMessage("На вас попал " + player.getName());
                        hasNotified[0] = true;
                    }
                }
            }
        }
    }
}
