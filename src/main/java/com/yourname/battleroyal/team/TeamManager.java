package com.yourname.battleroyal.team;

import java.util.*;

public class TeamManager {

    private final Map<UUID, Team> playerTeam = new HashMap<>();
    private final Map<UUID, Set<UUID>> pendingInvites = new HashMap<>();

    public Team getTeam(UUID player) {
        return playerTeam.get(player);
    }

    public boolean hasTeam(UUID player) {
        return playerTeam.containsKey(player);
    }

    public void createTeam(UUID leader) {
        if (hasTeam(leader)) return;
        Team team = new Team(leader);
        playerTeam.put(leader, team);
        team.addMember(leader);
    }

    public boolean invitePlayer(UUID inviter, UUID target) {
        if (!hasTeam(inviter)) {
            createTeam(inviter);
        }
        Team team = playerTeam.get(inviter);
        if (team.getMembers().contains(target)) {
            return false;
        }
        pendingInvites.computeIfAbsent(target, k -> new HashSet<>()).add(inviter);
        return true;
    }

    public boolean acceptInvite(UUID player, UUID inviter) {
        Set<UUID> invites = pendingInvites.get(player);
        if (invites == null || !invites.contains(inviter)) return false;
        invites.remove(inviter);
        if (invites.isEmpty()) pendingInvites.remove(player);
        Team team = playerTeam.get(inviter);
        if (team == null) return false;
        if (hasTeam(player)) {
            leaveTeam(player);
        }
        team.addMember(player);
        playerTeam.put(player, team);
        return true;
    }

    public boolean leaveTeam(UUID player) {
        if (!hasTeam(player)) return false;
        Team team = playerTeam.get(player);
        team.removeMember(player);
        playerTeam.remove(player);
        if (team.getMembers().isEmpty()) {
            // team sẽ bị garbage collected
        } else {
            if (team.getLeader().equals(player)) {
                UUID newLeader = team.getMembers().iterator().next();
                team.setLeader(newLeader);
            }
        }
        return true;
    }

    public void disbandTeam(UUID player) {
        if (!hasTeam(player)) return;
        Team team = playerTeam.get(player);
        if (!team.getLeader().equals(player)) return;
        for (UUID member : new ArrayList<>(team.getMembers())) {
            playerTeam.remove(member);
        }
    }

    public boolean isSameTeam(UUID p1, UUID p2) {
        if (!hasTeam(p1) || !hasTeam(p2)) return false;
        return playerTeam.get(p1).equals(playerTeam.get(p2));
    }

    public Set<UUID> getTeamMembers(UUID player) {
        if (!hasTeam(player)) return Collections.emptySet();
        return playerTeam.get(player).getMembers();
    }

    // ===== PHƯƠNG THỨC BỔ SUNG =====
    public UUID getTeamLeader(UUID player) {
        if (!hasTeam(player)) return null;
        return playerTeam.get(player).getLeader();
    }

    public void setTeamLeader(UUID player, UUID newLeader) {
        if (!hasTeam(player)) return;
        Team team = playerTeam.get(player);
        if (team.getMembers().contains(newLeader)) {
            team.setLeader(newLeader);
        }
    }

    public boolean hasPendingInvites(UUID player) {
        return pendingInvites.containsKey(player) && !pendingInvites.get(player).isEmpty();
    }

    public Set<UUID> getPendingInvites(UUID player) {
        return pendingInvites.getOrDefault(player, Collections.emptySet());
    }

    // ===== LỚP TEAM NỘI BỘ =====
    private static class Team {
        private UUID leader;
        private final Set<UUID> members = new HashSet<>();

        public Team(UUID leader) {
            this.leader = leader;
        }

        public UUID getLeader() { return leader; }
        public void setLeader(UUID leader) { this.leader = leader; }
        public Set<UUID> getMembers() { return members; }
        public void addMember(UUID p) { members.add(p); }
        public void removeMember(UUID p) { members.remove(p); }
        public boolean hasMember(UUID p) { return members.contains(p); }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Team)) return false;
            return leader.equals(((Team) o).leader);
        }
        @Override
        public int hashCode() { return leader.hashCode(); }
    }
}
