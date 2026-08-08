package com.yourname.battleroyal;

import com.yourname.battleroyal.team.TeamManager;
import com.yourname.battleroyal.utils.LocationUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.WorldBorder;

import java.util.*;

public class GameManager {

    private final BattleRoyalPlugin plugin;
    private GameState state = GameState.WAITING;

    private Location lobbyLocation;
    private Location waitingLocation;

    private int matchIndex = 0;
    private static final int CENTER_START = 5500;
    private static final int CENTER_STEP = 11000;

    private final Set<UUID> alivePlayers = new HashSet<>();
    private final Set<UUID> allPlayers = new HashSet<>();

    private BukkitTask countdownTask;
    private BukkitTask pvpTask;
    private BukkitTask scoreboardTask;

    private int gameTimeSeconds = 0;
    private int nextShrinkIndex = 0;
    private final List<Integer> borderSizes = Arrays.asList(10000, 8500, 7000, 5500, 4000, 2500, 1000);
    private static final int SHRINK_INTERVAL = 15 * 60; // 15 phút
    private boolean pvpEnabled = false;
    private static final int PVP_DELAY = 15 * 60; // 15 phút

    private Location borderCenter;
    private World currentWorld;

    // --- Team manager ---
    private final TeamManager teamManager = new TeamManager();

    public GameManager(BattleRoyalPlugin plugin) {
        this.plugin = plugin;
        this.currentWorld = Bukkit.getWorlds().get(0);
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    // ==================== CONFIG ====================

    public void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        String lobbyStr = config.getString("lobby");
        String waitingStr = config.getString("waiting");
        lobbyLocation = (lobbyStr != null && !lobbyStr.isEmpty()) ? LocationUtil.deserialize(lobbyStr) : null;
        waitingLocation = (waitingStr != null && !waitingStr.isEmpty()) ? LocationUtil.deserialize(waitingStr) : null;
        matchIndex = config.getInt("matchIndex", 0);
    }

    public void saveConfig() {
        FileConfiguration config = plugin.getConfig();
        config.set("lobby", LocationUtil.serialize(lobbyLocation));
        config.set("waiting", LocationUtil.serialize(waitingLocation));
        config.set("matchIndex", matchIndex);
        plugin.saveConfig();
    }

    // ==================== COMMANDS ====================

    public boolean joinGame(Player player) {
        if (state == GameState.WAITING || state == GameState.COUNTDOWN) {
            if (allPlayers.size() >= 30) {
                player.sendMessage("§cTrận đấu đã đủ 30 người!");
                return false;
            }
            if (allPlayers.contains(player.getUniqueId())) {
                player.sendMessage("§eBạn đã tham gia rồi!");
                return false;
            }
            if (waitingLocation == null) {
                player.sendMessage("§cKhu vực chờ chưa được thiết lập! Liên hệ admin.");
                return false;
            }
            player.teleport(waitingLocation);
            allPlayers.add(player.getUniqueId());
            alivePlayers.add(player.getUniqueId());
            player.sendMessage("§aBạn đã tham gia trận đấu! (" + allPlayers.size() + "/30)");

            if (allPlayers.size() >= 15 && state == GameState.WAITING) {
                startCountdown();
            }
            updateScoreboard();
            return true;
        } else {
            player.sendMessage("§cTrận đấu đã bắt đầu, không thể tham gia!");
            return false;
        }
    }

    public void leaveGame(Player player) {
        UUID uuid = player.getUniqueId();
        if (!allPlayers.contains(uuid)) {
            // Nếu chưa tham gia, teleport về lobby nếu có
            if (lobbyLocation != null) player.teleport(lobbyLocation);
            return;
        }

        // Xử lý team trước khi rời game
        if (teamManager.hasTeam(uuid)) {
            // Nếu là leader và team còn thành viên, chuyển leader
            if (teamManager.getTeam(uuid).getLeader().equals(uuid)) {
                Set<UUID> members = teamManager.getTeamMembers(uuid);
                if (members.size() > 1) {
                    // Tìm member khác để làm leader
                    for (UUID m : members) {
                        if (!m.equals(uuid)) {
                            teamManager.getTeam(uuid).setLeader(m);
                            break;
                        }
                    }
                } else {
                    // Không còn ai, giải tán team
                    teamManager.disbandTeam(uuid);
                }
            }
            // Rời khỏi team (nếu vẫn còn trong team)
            if (teamManager.hasTeam(uuid)) {
                teamManager.leaveTeam(uuid);
            }
        }

        // Xóa khỏi danh sách trận đấu
        allPlayers.remove(uuid);
        alivePlayers.remove(uuid);

        // Teleport về lobby và reset
        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
            player.setGameMode(GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(20);
            player.setFoodLevel(20);
            player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
            player.setWalkSpeed(0.2f);
        }
        player.sendMessage("§aBạn đã rời trận đấu và trở về Lobby.");

        // Cập nhật trạng thái nếu cần
        if (state == GameState.STARTED) {
            if (alivePlayers.isEmpty()) {
                endGame(null);
            } else if (alivePlayers.size() == 1) {
                Player winner = Bukkit.getPlayer(alivePlayers.iterator().next());
                endGame(winner);
            }
        } else {
            if (state == GameState.COUNTDOWN && allPlayers.size() < 15) {
                cancelCountdown();
            }
        }
        updateScoreboard();
    }

    public void setWaitingLocation(Location loc) {
        this.waitingLocation = loc;
        saveConfig();
    }

    public void setLobbyLocation(Location loc) {
        this.lobbyLocation = loc;
        saveConfig();
    }

    public void forceStart(Player admin) {
        if (state != GameState.WAITING && state != GameState.COUNTDOWN) {
            admin.sendMessage("§cTrận đấu đã bắt đầu rồi!");
            return;
        }
        if (allPlayers.size() < 2) {
            admin.sendMessage("§cCần ít nhất 2 người để bắt đầu!");
            return;
        }
        if (countdownTask != null) countdownTask.cancel();
        startGame();
        admin.sendMessage("§aTrận đấu đã được bắt đầu bởi admin!");
    }

    // ==================== INTERNAL LOGIC ====================

    private void startCountdown() {
        if (countdownTask != null) return;
        state = GameState.COUNTDOWN;
        Bukkit.broadcastMessage("§e§lĐã đủ 15 người! Trận đấu sẽ bắt đầu sau 10 phút.");
        countdownTask = new BukkitRunnable() {
            int seconds = 600;
            @Override
            public void run() {
                if (state != GameState.COUNTDOWN) {
                    this.cancel();
                    return;
                }
                if (seconds <= 0) {
                    startGame();
                    this.cancel();
                    return;
                }
                if (seconds % 60 == 0 || seconds <= 10) {
                    Bukkit.broadcastMessage("§eTrận đấu bắt đầu sau " + seconds + " giây...");
                }
                seconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        state = GameState.WAITING;
        Bukkit.broadcastMessage("§cĐếm ngược bị hủy do thiếu người chơi (cần 15).");
    }

    private void startGame() {
        state = GameState.STARTED;
        gameTimeSeconds = 0;
        nextShrinkIndex = 0;
        pvpEnabled = false;

        // Chọn tọa độ border mới
        int x = CENTER_START + matchIndex * CENTER_STEP;
        int z = CENTER_START + matchIndex * CENTER_STEP;
        borderCenter = new Location(currentWorld, x, 0, z);
        WorldBorder wb = currentWorld.getWorldBorder();
        wb.setCenter(x, z);
        wb.setSize(borderSizes.get(0));

        spawnPlayers();

        // Bật PVP sau 15 phút
        pvpTask = new BukkitRunnable() {
            @Override
            public void run() {
                pvpEnabled = true;
                Bukkit.broadcastMessage("§c§lPVP ĐÃ BẬT! Hãy chiến đấu!");
                updateScoreboard();
            }
        }.runTaskLater(plugin, PVP_DELAY * 20L);

        // Cập nhật scoreboard và thu hẹp border mỗi 15 phút
        scoreboardTask = new BukkitRunnable() {
            @Override
            public void run() {
                gameTimeSeconds++;
                updateScoreboard();

                if (gameTimeSeconds % SHRINK_INTERVAL == 0 && gameTimeSeconds > 0) {
                    shrinkBorder();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        Bukkit.broadcastMessage("§a§lTRẬN ĐẤU BẮT ĐẦU! Chúc các bạn may mắn!");
        updateScoreboard();
    }

    private void spawnPlayers() {
        if (borderCenter == null) return;
        World world = borderCenter.getWorld();
        int radius = 300;
        List<UUID> players = new ArrayList<>(alivePlayers);
        int count = players.size();
        if (count == 0) return;

        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double xOffset = radius * Math.cos(angle);
            double zOffset = radius * Math.sin(angle);
            int x = (int) (borderCenter.getX() + xOffset);
            int z = (int) (borderCenter.getZ() + zOffset);
            Location spawnLoc = new Location(world, x + 0.5, 100, z + 0.5);
            spawnLoc.setY(world.getHighestBlockYAt(spawnLoc) + 1);

            Player player = Bukkit.getPlayer(players.get(i));
            if (player != null && player.isOnline()) {
                player.teleport(spawnLoc);
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(20);
                player.setFoodLevel(20);
                player.getInventory().clear();
                player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
                player.setWalkSpeed(0f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) player.setWalkSpeed(0.2f);
                }, 30 * 20L);
            }
        }
    }

    private void shrinkBorder() {
        if (nextShrinkIndex >= borderSizes.size() - 1) return;
        nextShrinkIndex++;
        int newSize = borderSizes.get(nextShrinkIndex);
        WorldBorder wb = currentWorld.getWorldBorder();
        wb.setSize(newSize);
        Bukkit.broadcastMessage("§6Worldborder thu nhỏ xuống " + newSize + "x" + newSize);
        updateScoreboard();
    }

    public void handlePlayerDeath(Player player) {
        if (state != GameState.STARTED) return;
        UUID uuid = player.getUniqueId();
        if (!alivePlayers.contains(uuid)) return;

        alivePlayers.remove(uuid);
        player.setGameMode(GameMode.SPECTATOR);
        Bukkit.broadcastMessage("§c" + player.getName() + " đã chết! (" + alivePlayers.size() + " người sống)");
        updateScoreboard();

        if (alivePlayers.size() == 1) {
            Player winner = Bukkit.getPlayer(alivePlayers.iterator().next());
            endGame(winner);
        } else if (alivePlayers.isEmpty()) {
            endGame(null);
        }
    }

    private void endGame(Player winner) {
        if (state != GameState.STARTED) return;
        state = GameState.WAITING;

        if (pvpTask != null) { pvpTask.cancel(); pvpTask = null; }
        if (scoreboardTask != null) { scoreboardTask.cancel(); scoreboardTask = null; }
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }

        if (winner != null) {
            Bukkit.broadcastMessage("§6§l🏆 " + winner.getName() + " là người chiến thắng! 🏆");
            // Bắn pháo hoa 10 giây
            for (int i = 0; i < 10; i++) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (winner.isOnline()) {
                        winner.getWorld().spawn(winner.getLocation().add(0, 2, 0), Firework.class);
                    }
                }, i * 20L);
            }
        } else {
            Bukkit.broadcastMessage("§cTrận đấu kết thúc mà không có người thắng!");
        }

        // Reset tất cả người chơi
        for (UUID uuid : allPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.setGameMode(GameMode.SURVIVAL);
                p.getInventory().clear();
                p.setHealth(20);
                p.setFoodLevel(20);
                p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
                p.setWalkSpeed(0.2f);
                if (lobbyLocation != null) p.teleport(lobbyLocation);
                p.sendMessage("§aTrận đấu đã kết thúc! Bạn được đưa về Lobby.");
            }
        }

        matchIndex++;
        saveConfig();
        allPlayers.clear();
        alivePlayers.clear();

        // Không xóa team - giữ team cho trận sau

        state = GameState.WAITING;
        Bukkit.broadcastMessage("§eTrận đấu mới đã sẵn sàng! Sử dụng /join để tham gia.");
        updateScoreboard();
    }

    private void updateScoreboard() {
        plugin.getScoreboardManager().updateScoreboard(this);
    }

    // ==================== GETTERS ====================

    public GameState getState() { return state; }
    public int getAliveCount() { return alivePlayers.size(); }
    public int getTotalPlayers() { return allPlayers.size(); }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public int getGameTimeSeconds() { return gameTimeSeconds; }
    public Set<UUID> getAlivePlayers() { return alivePlayers; }
    public Set<UUID> getAllPlayers() { return allPlayers; }

    public int getCurrentBorderSize() {
        if (currentWorld == null) return 0;
        return (int) currentWorld.getWorldBorder().getSize();
    }

    public int getSecondsUntilNextShrink() {
        if (state != GameState.STARTED) return 0;
        int elapsed = gameTimeSeconds % SHRINK_INTERVAL;
        return SHRINK_INTERVAL - elapsed;
    }

    public void cleanup() {
        if (countdownTask != null) countdownTask.cancel();
        if (pvpTask != null) pvpTask.cancel();
        if (scoreboardTask != null) scoreboardTask.cancel();
    }
}
