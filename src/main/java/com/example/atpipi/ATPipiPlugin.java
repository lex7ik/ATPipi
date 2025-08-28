package com.example.atpipi;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class ATPipiPlugin extends JavaPlugin implements CommandExecutor {

    private boolean isLegacyVersion;
    private boolean enabled = true;

    @Override
    public void onEnable() {
        // Определяем, является ли версия "устаревшей" (до 1.13)
        isLegacyVersion = !Bukkit.getVersion().contains("1.13")
                      && !Bukkit.getVersion().contains("1.14")
                      && !Bukkit.getVersion().contains("1.15")
                      && !Bukkit.getVersion().contains("1.16")
                      && !Bukkit.getVersion().contains("1.17")
                      && !Bukkit.getVersion().contains("1.18")
                      && !Bukkit.getVersion().contains("1.19")
                      && !Bukkit.getVersion().contains("1.20")
                      && !Bukkit.getVersion().contains("1.21");

        // Регистрируем команды
        getCommand("atpipi").setExecutor(this);
        getCommand("atpipi-toggle").setExecutor(this);
        getLogger().info("ATPipiPlugin включён");
    }

    @Override
    public void onDisable() {
        getLogger().info("ATPipiPlugin выключен");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Переключение плагина
        if (command.getName().equalsIgnoreCase("atpipi-toggle")) {
            if (!sender.hasPermission("atpipi.toggle")) {
                sender.sendMessage("§cУ тебя нет прав на эту команду");
                return true;
            }
            enabled = !enabled;
            sender.sendMessage("§eATPipi теперь " + (enabled ? "§aвключён" : "§cвыключен"));
            return true;
        }

        // Команда писания
        if (command.getName().equalsIgnoreCase("atpipi")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cТолько игрок может использовать эту команду");
                return true;
            }
            Player player = (Player) sender;
            if (!enabled) {
                player.sendMessage("§cПлагин выключен админом");
                return true;
            }
            if (!player.hasPermission("atpipi.use")) {
                player.sendMessage("§cУ тебя нет прав на эту команду");
                return true;
            }
            startPipiAction(player);
            return true;
        }

        return false;
    }

    private void startPipiAction(Player player) {
        Set<Player> hitPlayers = new HashSet<>();
        final boolean[] hasNotified = {false};

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks++ >= 100) { // 100 тиков = 5 секунд
                    cancel();
                    return;
                }
                dropYellowConcrete(player, hitPlayers, hasNotified);
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void dropYellowConcrete(Player player, Set<Player> hitPlayers, boolean[] hasNotified) {
        // Выбираем материал бетона в зависимости от версии
        Material concreteMaterial = isLegacyVersion
            ? Material.valueOf("CONCRETE")
            : Material.YELLOW_CONCRETE;

        // Спавним падающий блок вместо предмета, чтобы избежать крашей
        FallingBlock block = player.getWorld().spawnFallingBlock(
            player.getLocation(),
            concreteMaterial.createBlockData()
        );
        block.setDropItem(false); // Чтобы при исчезновении не дропался блок

        // Задаём направление и скорость полёта
        Vector direction = player.getLocation().getDirection().normalize().multiply(0.5);
        block.setVelocity(direction);

        // Через 20 тиков (1 секунда) удаляем сущность
        new BukkitRunnable() {
            @Override
            public void run() {
                block.remove();
            }
        }.runTaskLater(this, 20L);

        // Проверяем попадание на игроков в радиусе 1 блока
        for (Entity entity : player.getNearbyEntities(1, 1, 1)) {
            if (entity instanceof Player target && !hitPlayers.contains(target)) {
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
