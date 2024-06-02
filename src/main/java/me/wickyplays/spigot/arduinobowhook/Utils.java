package me.wickyplays.spigot.arduinobowhook;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class Utils {

    public static String sendPlayerMessage(Player player, String message) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        return message;
    }

    public static String sendPlayerTitle(Player player, String title, String subtitle) {
        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle), 0, 10, 10);
        return title;
    }
}
