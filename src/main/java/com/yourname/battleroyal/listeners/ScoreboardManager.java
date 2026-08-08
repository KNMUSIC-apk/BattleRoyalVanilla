package com.yourname.battleroyal.listeners;

import com.yourname.battleroyal.BattleRoyalPlugin;
import com.yourname.battleroyal.GameManager;
import com.yourname.battleroyal.GameState;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.UUID;

public class ScoreboardManager {

    private final BattleRoyalPlugin plugin;

    public ScoreboardManager(BattleRoyalPlugin plugin) {
        this.plugin = plugin;
    }

    public void updateScoreboard(GameManager gm) {
        for (UUID uuid : gm.getAllPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            updatePlayerScoreboard(p, gm);
        }
    }

    private void updatePlayerScoreboard(Player player, GameManager gm) {
        Scoreboard board = player.getScoreboard();
        if (board == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
        }
        Objective obj = board.getObjective("battle");
        if (obj == null) {
            obj = board.registerNewObjective("battle", "dummy", ChatColor.GOLD + "BATTLE ROYAL VANILLA");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.unregister();
            obj = board.registerNewObjective("battle", "dummy", ChatColor.GOLD + "BATTLE ROYAL VANILLA");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        GameState state = gm.getState();

        // Border
        int borderSize = gm.getCurrentBorderSize();
        obj.getScore("§7Border: §b" + borderSize + "×" + borderSize).setScore(6);

        if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
            obj.getScore("§7Trạng thái: §eĐang chờ").setScore(5);
            obj.getScore("§7Số người: §b" + gm.getTotalPlayers() + "/30").setScore(4);
            if (state == GameState.COUNTDOWN) {
                obj.getScore("§7Bắt đầu sau: §a10 phút").setScore(3);
            } else {
                obj.getScore("§7Cần 15 người để bắt đầu").setScore(3);
            }
        } else if (state == GameState.STARTED) {
            int timeUntilPvp = gm.getTimeUntilPvp();
            if (timeUntilPvp > 0) {
                String time = formatTime(timeUntilPvp);
                obj.getScore("§7PVP: §e" + time).setScore(5);
            } else {
                int remainingShrink = gm.getSecondsUntilNextShrink();
                String time = formatTime(remainingShrink);
                obj.getScore("§7Bo thu: §e" + time).setScore(5);
            }
        }

        obj.getScore(ChatColor.GRAY + "═══════════════").setScore(1);
        player.setScoreboard(board);
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
