package com.yourname.battleroyal.commands;

import com.yourname.battleroyal.BattleRoyalPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaveCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng được.");
            return true;
        }
        Player player = (Player) sender;
        BattleRoyalPlugin.getInstance().getGameManager().leaveGame(player);
        return true;
    }
}
