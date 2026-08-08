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
        // Cập nhật cho tất cả người chơi đang trong game (bao gồm spectator)
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
            obj = board.registerNewObjective("battle", "dummy", ChatColor.GOLD + "⚔ Battle Royale");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            obj.unregister();
            obj = board.registerNewObjective("battle", "dummy", ChatColor.GOLD + "⚔ Battle Royale");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        GameState state = gm.getState();
        obj.getScore(ChatColor.GRAY + "═══════════════").setScore(8);

        if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
            obj.getScore("§7Trạng thái: §eĐang chờ").setScore(7);
            obj.getScore("§7Số người: §b" + gm.getTotalPlayers() + "/30").setScore(6);
            if (state == GameState.COUNTDOWN) {
                obj.getScore("§7Đếm ngược: §a10 phút").setScore(5);
            } else {
                obj.getScore("§7Cần 15 người để bắt đầu").setScore(5);
            }
        } else if (state == GameState.STARTED) {
            obj.getScore("§7Alive: §a" + gm.getAliveCount() + " người").setScore(7);
            obj.getScore("§7PVP: " + (gm.isPvpEnabled() ? "§aON" : "§cOFF")).setScore(6);
            int sec = gm.getSecondsUntilNextShrink();
            String time = String.format("%02d:%02d", sec / 60, sec % 60);
            obj.getScore("§7Thu nhỏ sau: §e" + time).setScore(5);
            int size = gm.getCurrentBorderSize();
            obj.getScore("§7Border: §b" + size + "x" + size).setScore(4);
        }

        obj.getScore(ChatColor.GRAY + "═══════════════").setScore(3);
        obj.getScore("§7Minecraft Battle").setScore(2);

        player.setScoreboard(board);
    }
}
