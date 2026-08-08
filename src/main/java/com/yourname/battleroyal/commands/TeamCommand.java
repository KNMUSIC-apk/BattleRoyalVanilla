package com.yourname.battleroyal.commands;

import com.yourname.battleroyal.BattleRoyalPlugin;
import com.yourname.battleroyal.team.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class TeamCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cChỉ người chơi mới dùng được lệnh này.");
            return true;
        }

        Player player = (Player) sender;
        TeamManager teamManager = BattleRoyalPlugin.getInstance().getGameManager().getTeamManager();

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "invite":
                if (args.length < 2) {
                    player.sendMessage("§cDùng: /team invite <tên_người_chơi>");
                    return true;
                }
                handleInvite(player, args[1], teamManager);
                break;

            case "accept":
                handleAccept(player, teamManager);
                break;

            case "leave":
                handleLeave(player, teamManager);
                break;

            default:
                sendHelp(player);
                break;
        }
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§6===== HƯỚNG DẪN TEAM =====");
        player.sendMessage("§e/team invite <player> §7- Mời người chơi vào team");
        player.sendMessage("§e/team accept §7- Chấp nhận lời mời");
        player.sendMessage("§e/team leave §7- Rời team hiện tại");
    }

    private void handleInvite(Player inviter, String targetName, TeamManager teamManager) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            inviter.sendMessage("§cNgười chơi không tồn tại hoặc không online.");
            return;
        }
        if (target.equals(inviter)) {
            inviter.sendMessage("§cBạn không thể mời chính mình.");
            return;
        }

        boolean success = teamManager.invitePlayer(inviter.getUniqueId(), target.getUniqueId());
        if (success) {
            inviter.sendMessage("§aĐã gửi lời mời đến §e" + target.getName());
            target.sendMessage("§eBạn được §6" + inviter.getName() + " §emời vào team. Gõ §a/team accept §eđể chấp nhận.");
        } else {
            inviter.sendMessage("§cKhông thể mời người này (có thể họ đã ở trong team hoặc đã có lời mời).");
        }
    }

    private void handleAccept(Player player, TeamManager teamManager) {
        if (!teamManager.hasPendingInvites(player.getUniqueId())) {
            player.sendMessage("§cBạn không có lời mời nào.");
            return;
        }

        Set<UUID> inviters = teamManager.getPendingInvites(player.getUniqueId());
        UUID inviterUuid = inviters.iterator().next();

        boolean accepted = teamManager.acceptInvite(player.getUniqueId(), inviterUuid);
        if (accepted) {
            Player inviter = Bukkit.getPlayer(inviterUuid);
            String inviterName = (inviter != null) ? inviter.getName() : "???";
            player.sendMessage("§aBạn đã gia nhập team của §e" + inviterName + "§a.");
            if (inviter != null) {
                inviter.sendMessage("§a" + player.getName() + " đã gia nhập team của bạn.");
            }
        } else {
            player.sendMessage("§cKhông thể chấp nhận lời mời (có thể team đã giải tán).");
        }
    }

    private void handleLeave(Player player, TeamManager teamManager) {
        boolean left = teamManager.leaveTeam(player.getUniqueId());
        if (left) {
            player.sendMessage("§aBạn đã rời khỏi team.");
        } else {
            player.sendMessage("§cBạn không ở trong team nào.");
        }
    }
}
