package me.wickyplays.spigot.arduinobowhook;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.firmata4j.IODevice;
import org.firmata4j.Pin;
import org.firmata4j.firmata.FirmataDevice;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class ArduinoBowHook extends JavaPlugin implements Listener {

    private IODevice device = null;
    private BukkitTask runnable = null;

    private final HashMap<UUID, Long> rightClickStartTimes = new HashMap<>();

    private final int[][] pinsNum = {
            {34, 43}, {32, 41}, {30, 39}, {28, 37}, {26, 35}, {24, 33}, {22, 31}
    };

    @Override
    public void onEnable() {
        if (device == null) {
            device = new FirmataDevice("COM10");
            try {
                device.start();
                device.ensureInitializationIsDone();

                for (int i = 0; i < 7; i++) {
                    for (int j = 0; j < 2; j++) {
                        System.out.println("Pin " + pinsNum[i][j] + " has been initialized!");
                        Pin pin = device.getPin(pinsNum[i][j]);
                        pin.setMode(Pin.Mode.OUTPUT);
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        //Register event (in my case, in the Main class)
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
    }

    @Override
    public void onDisable() {

        if (runnable != null) {
            runnable.cancel();
        }
        if (device != null) {
            try {
                device.stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void enablePinsFromStage(int stage) throws IOException {
        if (stage <= 0 || stage > 8) {
            return;
        }

        for (int i = 0; i < stage; i++) {
            if (stage == 7) {
                device.getPin(pinsNum[i][0]).setValue(0);
                device.getPin(pinsNum[i][1]).setValue(1);
            } else {
                device.getPin(pinsNum[i][0]).setValue(1);
                device.getPin(pinsNum[i][1]).setValue(0);
            }
        }
    }

    public void disablePins() throws IOException {
        for (int i = 0; i < 7; i++) {
            device.getPin(pinsNum[i][0]).setValue(0);
            device.getPin(pinsNum[i][1]).setValue(0);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() != Material.BOW) {
            return;
        }

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            long newTime = System.currentTimeMillis();
            rightClickStartTimes.putIfAbsent(playerId, newTime);

            if (runnable != null) {
                runnable.cancel();
            }

            runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    if (newTime == 0) {
                        return;
                    }

                    int stage = (int) ((System.currentTimeMillis() - newTime) / 142);
                    stage = Math.min(stage, 7);

                    try {
                        if (stage < 3) {
                            Utils.sendPlayerTitle(player, "&dWickyPlays - &cBow stage: " + stage, "&ePlease check your Arduino");
                        } else if (stage < 5) {
                            Utils.sendPlayerTitle(player, "&dWickyPlays - &eBow stage: " + stage, "&ePlease check your Arduino");
                        } else if (stage < 7) {
                            Utils.sendPlayerTitle(player, "&dWickyPlays - &aBow stage: " + stage, "&ePlease check your Arduino");
                        }
                        enablePinsFromStage(stage);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }.runTaskTimer(this, 0, 1);

        } else if (rightClickStartTimes.get(playerId) != null) {
            rightClickStartTimes.remove(playerId);
            try {
                enablePinsFromStage(0);
                disablePins();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player) {
            if (runnable != null) {
                runnable.cancel();
            }
            rightClickStartTimes.remove(event.getEntity().getUniqueId());
            try {
                enablePinsFromStage(0);
                disablePins();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        rightClickStartTimes.remove(event.getPlayer().getUniqueId());
        if (runnable != null) {
            runnable.cancel();
        }

        try {
            disablePins();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}