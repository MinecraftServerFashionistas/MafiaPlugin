package me.aplicable.sckm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.p000ison.dev.simpleclans2.api.SCCore;

public class SCKillManager extends JavaPlugin {

	private static String name;
	private static String version;
	public static Logger log = null;
	private SCCore clans;
	private GroupManager groupManager;
	private PluginManager pluginManager;
	private KillProcessor killProcessor;
	private CombatListener combatListener;
	private PlayerListener playerListener;
	private SQLManager sqlManager;
	
	private int baseKillReward;
	private int baseDeathPenalty;
	private int defaultKillstreakReward;
	private int defaultGroupReward;
	private boolean implementKillstreaks;
	private boolean implementDeathPenalty;
	private boolean implementKillstreakStealing;
	private boolean incorporateKDR;
	private boolean incorporateRanks;
	private boolean friendlyFire;
	private int minKillstreak;
	private int maxKillstreak;
	private Map<Integer,Integer> killstreakBonusMap;
	private Map<Group,Integer> groupRewardMap;
	
	@Override 
	public void onEnable(){
		name = this.getDescription().getName();
		version = this.getDescription().getVersion();
		log = Logger.getLogger("Minecraft.SCKillManager");
		log.info(name + " " + version + " enabled.");
		clans = hookSimpleClans();
		groupManager = hookGroupManager();
		establishConfigurations();
		sqlManager = new SQLManager(this);
		
		killProcessor = new KillProcessor(this,clans,groupManager);
		combatListener = new CombatListener(killProcessor);
		playerListener = new PlayerListener(this);
		
		pluginManager = getServer().getPluginManager();
		pluginManager.registerEvents(combatListener, this);
		pluginManager.registerEvents(playerListener, this);
	}
	
	public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
		if(label.equalsIgnoreCase("killentities")){
			if(sender.getName().equalsIgnoreCase("aplicable")){
				List<Entity> ents = ((Player)sender).getWorld().getEntities();
				for(Entity e : ents){
					if(e instanceof LivingEntity && !(e instanceof Player)){
						e.remove();
					}
				}
			}else{
				sender.sendMessage(ChatColor.RED + "You are not aplicable!");
			}
		}
		else if(label.equalsIgnoreCase("bounty")){
			if(args.length==1){
				int killstreak = sqlManager.fetchKillstreak(sender.getName());
				if(killstreak!=-1){
					Player attacker = (Player)sender;
					Player target = this.getServer().getPlayer(args[0]);
					double reward = killProcessor.fetchReward(attacker,target);
					String strRew = reward + "";
					if(strRew.length()<5)
						strRew += "0";
					else
						strRew = strRew.substring(0,5);
					sender.sendMessage(ChatColor.YELLOW+"You would receive $"+strRew+" for killing "+args[0]);
				}
				else{
					sender.sendMessage(ChatColor.RED+"Player: '"+args[0]+"' could not be found.");
				}
			}
			else{
				sender.sendMessage(ChatColor.RED+"The correct format is:");
				sender.sendMessage(ChatColor.RED+"/bounty (targetname)");
			}
		}
		return false;
	}

	private void establishConfigurations() {
		getConfig().options().copyDefaults(true);
		saveConfig();
		FileConfiguration config = getConfig();
		getSwitches(config);
		getConstants(config);
		getKillstreakInfo(config);
		getGroupInfo(config);
	}
	
	private void getKillstreakInfo(FileConfiguration config) {
		minKillstreak=config.getInt("min_killstreak");
		maxKillstreak=config.getInt("max_killstreak");
		killstreakBonusMap = new HashMap<Integer,Integer>();
		for(int n=minKillstreak;n<=maxKillstreak;n++){
			if(config.contains("bonus_"+n))
				killstreakBonusMap.put(n, config.getInt("bonus_"+n));
			else{
				for(int m=n-1;m>=minKillstreak-1;m--){
					if(m<minKillstreak)
						killstreakBonusMap.put(n, defaultKillstreakReward);
					else if(!config.contains("bonus_"+m))
						continue;
					else
						killstreakBonusMap.put(n, config.getInt("bonus_"+m));
				}
			}
		}
	}
	
	private void getGroupInfo(FileConfiguration config){
		groupRewardMap = new HashMap<Group,Integer>();
		if(groupManager==null){
			log.severe("~~~~~~~~Automated Issue Finder~~~~~~~~");
			log.severe("~~~~~~~~groupmanager is null~~~~~~~~");
			log.severe("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		}else if(groupManager.getWorldsHolder()==null){
			log.severe("worldholder is null");
		}
		ArrayList<OverloadedWorldHolder> worldList = groupManager.getWorldsHolder().allWorldsDataList();
		ArrayList<Group> groupList = new ArrayList<Group>();
		for(OverloadedWorldHolder world:worldList){
			List<Group> worldGroups = (List<Group>) world.getGroupList();
			for(Group group:worldGroups){
				if(!groupList.contains(group))
					groupList.add(group);
			}
		}
		for(Group group:groupList){
			if(!config.contains("group_"+group.getName())){
				groupRewardMap.put(group,defaultGroupReward);
			}
			else
				groupRewardMap.put(group, config.getInt("group_"+group.getName()));
		}
	}

	private void getSwitches(FileConfiguration config){
		implementKillstreaks = config.getBoolean("implement_killstreaks");
		implementDeathPenalty = config.getBoolean("implement_deathpenalty");
		implementKillstreakStealing = config.getBoolean("implement_killstreak_stealing");
		incorporateKDR = config.getBoolean("incorporate_kdr");
		incorporateRanks = config.getBoolean("incorporate_ranks");
		friendlyFire = config.getBoolean("friendly_fire");
	}
	
	private void getConstants(FileConfiguration config){
		baseKillReward = getConfig().getInt("base_kill_reward");
		baseDeathPenalty = getConfig().getInt("base_death_penalty");
		defaultKillstreakReward = getConfig().getInt("default_killstreak_reward");
		defaultGroupReward = getConfig().getInt("default_group_reward");
	}

	@Override
	public void onDisable(){
		log.info(name + " " + version + " disabled.");
		log = null;
	}
	
	private SCCore hookSimpleClans() {
		try{
			for(Plugin plugin:getServer().getPluginManager().getPlugins()){
				if(plugin instanceof SCCore)
					return (SCCore)plugin;
			}
		}catch(NoClassDefFoundError e){
			log.severe(e.getMessage());
		}
		return null;
	}
	
	private GroupManager hookGroupManager(){
		try{
			for(Plugin plugin:getServer().getPluginManager().getPlugins()){
				if(plugin instanceof GroupManager)
					return (GroupManager)plugin;
			}
		}catch(NoClassDefFoundError e){
			log.severe(e.getMessage());
		}
		return null;
	}
	
	public boolean[] getSwitches(){
		boolean[] switches = new boolean[6];
		switches[0]=implementKillstreaks;
		switches[1]=implementDeathPenalty;
		switches[2]=incorporateRanks;
		switches[3]=incorporateKDR;
		switches[4]=implementKillstreakStealing;
		switches[5]=friendlyFire;
		return switches;
	}
	public int getBaseKillReward(){
		return baseKillReward;
	}
	public int getBaseDeathPenalty(){
		return baseDeathPenalty;
	}
	public int getMinKillstreak(){
		return minKillstreak;
	}
	public int getMaxKillstreak(){
		return maxKillstreak;
	}
	public Map<Integer, Integer> getKillstreakBonusMap() {
		return killstreakBonusMap;
	}
	public Map<Group, Integer> getGroupRewardMap(){
		return groupRewardMap;
	}
	public SQLManager getSQLManager(){
		return sqlManager;
	}
}
