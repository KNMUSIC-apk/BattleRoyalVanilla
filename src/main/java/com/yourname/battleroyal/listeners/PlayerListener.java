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
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }

        Player victim = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();
        GameManager gm = plugin.getGameManager();

        if (gm.getState() == GameState.STARTED) {
            // Kiểm tra cùng team
            if (gm.getTeamManager().isSameTeam(damager.getUniqueId(), victim.getUniqueId())) {
                event.setCancelled(true);
                damager.sendMessage("§cBạn không thể tấn công đồng đội!");
                return;
            }

            if (!gm.isPvpEnabled()) {
                event.setCancelled(true);
                int remaining = gm.getTimeUntilPvp();
                damager.sendMessage("§cPVP chưa được bật (còn " + (remaining/60) + " phút)");
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();
        if (gm.getState() == GameState.STARTED && !gm.getAlivePlayers().contains(player.getUniqueId())) {
            event.setFormat("§7[Spectator] " + event.getFormat());
        }
    }
}
