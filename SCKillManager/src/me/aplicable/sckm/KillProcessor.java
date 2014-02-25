package me.aplicable.sckm;

import java.util.Map;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import com.p000ison.dev.simpleclans2.api.SCCore;
import com.p000ison.dev.simpleclans2.api.clanplayer.ClanPlayerManager;

public class KillProcessor {

	private SCKillManager killManager;
	@SuppressWarnings("unused")
	private SCCore clans;
	private GroupManager groupManager;

	private ClanPlayerManager playerManager;

	private int baseKillReward;
	private int baseDeathPenalty;
	private Map<Integer,Integer> killstreakBonusMap;
	private Map<Group,Integer> groupRewardMap;

	public KillProcessor(SCKillManager killManager, SCCore clans, GroupManager groupManager) {
		this.clans=clans;
		this.killManager=killManager;
		this.groupManager=groupManager;

		baseKillReward=killManager.getBaseKillReward();
		baseDeathPenalty=killManager.getBaseDeathPenalty();
		killstreakBonusMap=killManager.getKillstreakBonusMap();
		groupRewardMap=killManager.getGroupRewardMap();

		playerManager = clans.getClanPlayerManager();
	}

	public void processKill(Player attacker, Player target) {
		double reward = fetchReward(attacker,target);
		Server server = killManager.getServer();
		server.dispatchCommand(server.getConsoleSender(), "money give "+attacker.getName()+" "+reward);
		String strRep = reward + "";
		if(strRep.length()<5)
			strRep += "0";
		else
			strRep = strRep.substring(0,5);
		attacker.sendMessage(ChatColor.YELLOW+" You have earned $"+strRep+" by killing "+target.getName());
		if(!killManager.getSwitches()[5])
			if(getGroup(attacker).equals(getGroup(target)))
				return;
		SQLManager sql = killManager.getSQLManager();
		sql.incrementKillstreak(attacker.getName());
		int attackerKillstreak = sql.fetchKillstreak(attacker.getName());//killstreak rewards
		if(killManager.getSwitches()[0]&&attackerKillstreak>=killManager.getMinKillstreak()){
			int killstreakReward;
			if(attackerKillstreak>=killManager.getMaxKillstreak()){
				killstreakReward = killstreakBonusMap.get(killManager.getMaxKillstreak());				
			}
			else{
				killstreakReward = killstreakBonusMap.get(attackerKillstreak);
			}
			server.dispatchCommand(server.getConsoleSender(), "money give "+attacker.getName()+" "+killstreakReward);
			attacker.sendMessage(ChatColor.YELLOW+"You have received a $"+killstreakReward+" reward by consecutively killing "+attackerKillstreak+" players!");
		}
		int targetKillstreak = sql.fetchKillstreak(target.getName());//killstreak stealing
		if(killManager.getSwitches()[0]&&killManager.getSwitches()[4]&&targetKillstreak>=killManager.getMinKillstreak()){
			int killstreakReward;
			if(targetKillstreak>=killManager.getMaxKillstreak()){
				killstreakReward = killstreakBonusMap.get(killManager.getMaxKillstreak());				
			}
			else{
				killstreakReward = killstreakBonusMap.get(targetKillstreak);
			}
			server.dispatchCommand(server.getConsoleSender(), "money give "+attacker.getName()+" "+killstreakReward);
			attacker.sendMessage(ChatColor.YELLOW+"You have stolen a $"+killstreakReward+" reward by terminating "+target.getName()+"'s killstreak of "+targetKillstreak+"!");
		}
		if(killManager.getSwitches()[1]){//death penalty
			server.dispatchCommand(server.getConsoleSender(), "money give "+target.getName()+" "+(-1*baseDeathPenalty));
			target.sendMessage(ChatColor.YELLOW+""+baseDeathPenalty+" has been taken from your account as a penalty from being killed by "+attacker.getName()+".");
		}
		sql.resetKillstreak(target.getName());
	}

	public double fetchReward(Player attacker, Player target){
		if(!killManager.getSwitches()[5])
			if(getGroup(attacker).equals(getGroup(target)))
				return 0;
		double attackerKDR = getKDR(attacker);
		double targetKDR = getKDR(target);
		double reward = baseKillReward;//kill rewards
		double kdrRatio = targetKDR/attackerKDR;
		double rankReward = getRankValue(target);
		if(killManager.getSwitches()[2]){//include ranks
			reward += rankReward;
		}
		if(killManager.getSwitches()[3]){//include kdr
			reward*=kdrRatio;
		}
		return reward;
	}

	private double getKDR(Player player) {
		return playerManager.getClanPlayer(player.getName()).getKDR();
	}

	private Group getGroup(Player player){
		OverloadedWorldHolder world = groupManager.getWorldsHolder().getWorldData(player.getName());
		Group group = world.getUser(player.getName()).getGroup();
		return group;
	}

	private double getRankValue(Player player){
		Group group = getGroup(player);
		return groupRewardMap.get(group);
	}

}
