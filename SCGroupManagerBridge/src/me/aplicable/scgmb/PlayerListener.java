package me.aplicable.scgmb;

import java.util.List;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import com.p000ison.dev.simpleclans2.api.clanplayer.ClanPlayer;

import com.p000ison.dev.simpleclans2.api.SCCore;

public class PlayerListener implements Listener {

	private Server server;
	private SCCore clans;
	private GroupManager groupManager;
	private String defaultGroup;
	private String defaultLeaderGroup;

	public PlayerListener(Server server,SCCore clans, GroupManager groupManager, String defaultGroup, String defaultLeaderGroup) {
		this.server=server;
		this.clans=clans;
		this.groupManager=groupManager;
		this.defaultGroup=defaultGroup;
		this.defaultLeaderGroup=defaultLeaderGroup;
	}
	
	public void playerLoginCheck(PlayerLoginEvent event){
		runChecks();
	}

	public void playerCommandCheck(PlayerCommandPreprocessEvent event){
		runChecks();
		
	}
	
	private void runChecks(){
		List<Player> playerList = server.getWorlds().get(0).getPlayers();
		for(Player player:playerList){
			 ClanPlayer clanPlayer = clans.getClanPlayerManager().getClanPlayer(player.getName());
			 User user = groupManager.getWorldsHolder().getWorldDataByPlayerName(player.getName()).getUser(player.getName());
			 String g = user.getGroup().getName();
			 OverloadedWorldHolder world = groupManager.getWorldsHolder().getWorldDataByPlayerName(player.getName());
			 if(clanPlayer.getClan()==null&&!user.getGroup().equals(defaultGroup)){
				 if(!(g.equalsIgnoreCase("admin")||g.equalsIgnoreCase("owner")||g.equalsIgnoreCase("operator")||g.equalsIgnoreCase("moderator"))){
					 user.setGroup(world.getGroup(defaultGroup));
					 player.sendMessage(ChatColor.AQUA+"You have been reset to the appropriate group.");
				 }
			 }
			 else if(clanPlayer.isLeader()&&!user.getGroup().equals(defaultLeaderGroup)){
				 if(!(g.equalsIgnoreCase("admin")||g.equalsIgnoreCase("owner")||g.equalsIgnoreCase("operator")||g.equalsIgnoreCase("moderator"))){
					 user.setGroup(world.getGroup(defaultLeaderGroup));
					 player.sendMessage(ChatColor.AQUA+"You have been regrouped to the leader position.");
				 }
			 }
		}
	}
	
}
