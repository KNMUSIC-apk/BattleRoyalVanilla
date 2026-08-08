package com.yourname.battleroyal.listeners;

import com.yourname.battleroyal.BattleRoyalPlugin;
import com.yourname.battleroyal.GameManager;
import com.yourname.battleroyal.GameState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final BattleRoyalPlugin plugin;

    public PlayerListener(BattleRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        GameManager gm = plugin.getGameManager();
        gm.leaveGame(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        GameManager gm = plugin.getGameManager();
        if (gm.getState() == GameState.STARTED && gm.getAlivePlayers().contains(player.getUniqueId())) {
            gm.handlePlayerDeath(player);
            event.setDeathMessage(null);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Player damager = (Player) event.getDamager();
            GameManager gm = plugin.getGameManager();
            if (gm.getState() == GameState.STARTED) {
                if (!gm.isPvpEnabled()) {
                    event.setCancelled(true);
                    int remaining = 15 - (gm.getGameTimeSeconds() / 60);
                    if (remaining > 0) {
                        damager.sendMessage("§cPVP chưa được bật (còn " + remaining + " phút)");
                    } else {
                        damager.sendMessage("§cPVP chưa được bật (đợi thêm vài giây)");
                    }
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        // Nếu trận đang diễn ra và người chơi không còn sống -> thêm prefix [Spectator]
        if (gm.getState() == GameState.STARTED && !gm.getAlivePlayers().contains(player.getUniqueId())) {
            event.setFormat("§7[Spectator] " + event.getFormat());
        }
    }
}
