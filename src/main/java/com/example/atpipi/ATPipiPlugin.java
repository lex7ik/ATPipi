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

import java.util.HashSet;
import java.util.Set;

public class ATPipiPlugin extends JavaPlugin implements CommandExecutor {

    private boolean isLegacyVersion;

    @Override
    public void onEnable() {
        // Определяем, является ли версия "устаревшей" (до 1.13)
        isLegacyVersion = !Bukkit.getVersion().contains("1.13") &&
                !Bukkit.getVersion().contains("1.14") &&
                !Bukkit.getVersion().contains("1.15") &&
                !Bukkit.getVersion().contains("1.16") &&
                !Bukkit.getVersion().contains("1.17") &&
                !Bukkit.getVersion().contains("1.18") &&
                !Bukkit.getVersion().contains("1.19") &&
                !Bukkit.getVersion().contains("1.20");

        this.getCommand("atpipi").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Проверка наличия разрешения
            if (!player.hasPermission("atpipi.use")) {
                player.sendMessage("У вас нет прав для использования этой команды.");
                return true;
            }

            startPipiAction(player);
            return true;
        }
        return false;
    }

    private void startPipiAction(Player player) {
        Set<Player> hitPlayers = new HashSet<Player>();
        final boolean[] hasNotified = {false};

        // Начинаем действие команды, которое будет длиться 5 секунд (100 тиков)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 100) { // 5 секунд = 100 тиков
                    this.cancel();
                    return;
                }
                dropYellowConcrete(player, hitPlayers, hasNotified);
                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void dropYellowConcrete(Player player, Set<Player> hitPlayers, boolean[] hasNotified) {
        // Создаем желтый бетон, учитывая версию Minecraft
        Material concreteMaterial;
        if (isLegacyVersion) {
            concreteMaterial = Material.valueOf("CONCRETE");
        } else {
            concreteMaterial = Material.YELLOW_CONCRETE;
        }

        final org.bukkit.entity.Item item = player.getWorld().dropItemNaturally(player.getLocation(), new org.bukkit.inventory.ItemStack(concreteMaterial, 1));
        item.setPickupDelay(Integer.MAX_VALUE); // Запрещаем подбирать блоки

        // Устанавливаем направление движения блока
        Vector direction = player.getLocation().getDirection().normalize().multiply(0.5);
        item.setVelocity(direction);

        // Удаление блока через 1 секунду (20 тиков)
        new BukkitRunnable() {
            @Override
            public void run() {
                item.remove();
            }
        }.runTaskLater(this, 20L);

        // Проверка на попадание в других игроков, и вывод сообщения только один раз
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
