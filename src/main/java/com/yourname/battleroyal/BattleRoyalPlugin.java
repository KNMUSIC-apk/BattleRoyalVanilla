package com.yourname.battleroyal;

import com.yourname.battleroyal.commands.*;
import com.yourname.battleroyal.listeners.PlayerListener;
import com.yourname.battleroyal.listeners.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BattleRoyalPlugin extends JavaPlugin {

    private static BattleRoyalPlugin instance;
    private GameManager gameManager;
    private ScoreboardManager scoreboardManager;

    @Override
    public void onEnable() {
        instance = this;

        // ---- Tự động tạo config nếu chưa có ----
        getConfig().options().copyDefaults(true);
        getConfig().addDefault("lobby", null);
        getConfig().addDefault("waiting", null);
        getConfig().addDefault("matchIndex", 0);
        saveConfig(); // Lưu file config.yml ngay lập tức

        gameManager = new GameManager(this);
        scoreboardManager = new ScoreboardManager(this);

        // Đăng ký lệnh
        getCommand("join").setExecutor(new JoinCommand());
        getCommand("setjoins").setExecutor(new SetJoinsCommand());
        getCommand("setlobby").setExecutor(new SetLobbyCommand());
        getCommand("start").setExecutor(new StartCommand());

        // Đăng ký sự kiện
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);

        // Tải cấu hình
        gameManager.loadConfig();
        getLogger().info("BattleRoyalPlugin enabled!");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.cleanup();
        getLogger().info("BattleRoyalPlugin disabled.");
    }

    public static BattleRoyalPlugin getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
