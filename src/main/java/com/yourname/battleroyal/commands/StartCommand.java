package com.yourname.battleroyal.commands;

import com.yourname.battleroyal.BattleRoyalPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StartCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cChỉ admin mới dùng được.");
            return true;
        }
        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage("§cBạn không có quyền!");
            return true;
        }
        BattleRoyalPlugin.getInstance().getGameManager().forceStart(player);
        return true;
    }
}
