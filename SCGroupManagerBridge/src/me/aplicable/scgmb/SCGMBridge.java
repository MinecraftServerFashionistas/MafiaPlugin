package me.aplicable.scgmb;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.Group;
import org.anjocaido.groupmanager.data.User;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.p000ison.dev.simpleclans2.api.SCCore;
import com.p000ison.dev.simpleclans2.api.clanplayer.ClanPlayer;

public class SCGMBridge extends JavaPlugin {

	private String name;
	private String version;
	private Logger log;
	private SCCore clans;
	private GroupManager groupManager;
	private PluginManager pluginManager;
	private PlayerListener playerListener;
	private List<String> permissableGroupList;
	private List<String> operatorGroupList;
	private String defaultGroup;
	private String defaultLeaderGroup;

	@Override
	public void onEnable(){
		name = this.getDescription().getName();
		version = this.getDescription().getVersion();
		log = Logger.getLogger("Minecraft.SCKillManager");
		log.info(name + " " + version + " enabled.");
		clans = hookSimpleClans();
		groupManager = hookGroupManager();
		establishConfigurations();
		establishListeners();
	}

	@Override
	public void onDisable(){
		log.info(name + " " + version + " disabled.");
		log = null;
	}

	private void establishConfigurations() {
		getConfig().options().copyDefaults(true);
		saveConfig();
		FileConfiguration config = getConfig();
		permissableGroupList = obtainGroups(config);
		operatorGroupList = obtainOperators(config);
		defaultGroup = config.getString("default_group");
		defaultLeaderGroup = config.getString("default_leader_group");
	}

	private void establishListeners(){
		pluginManager = getServer().getPluginManager();
		playerListener = new PlayerListener(getServer(),clans,groupManager,defaultGroup,defaultLeaderGroup);
		pluginManager.registerEvents(playerListener,this);
	}

	private List<String> obtainGroups(FileConfiguration config){
		List<String> groupList = new ArrayList<String>();
		int index = 1;
		while(true){
			if(config.contains("group_"+index)){
				groupList.add(config.getString("group_"+index));
				index++;
			}else{
				break;
			}
		}
		return groupList;
	}

	private List<String> obtainOperators(FileConfiguration config){
		List<String> operatorList = new ArrayList<String>();
		int index = 1;
		while(true){
			if(config.contains("operator_"+index)){
				operatorList.add(config.getString("operator_"+index));
				index++;
			}
			else{
				break;
			}
		}
		return operatorList;
	}

	public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
		if(label.equalsIgnoreCase("mafia")){
			ClanPlayer leader = clans.getClanPlayerManager().getClanPlayer(sender.getName());
			User user = groupManager.getWorldsHolder().getWorldData(sender.getName()).getUser(sender.getName());
			if(operatorGroupList.contains(user.getGroup().getName())){
				if(args.length==3){
					if(args[0].equalsIgnoreCase("setrank")){
						if(sender.getServer().getPlayer(args[1]).isValid()){
							ClanPlayer target = clans.getClanPlayerManager().getClanPlayer(args[1]);
							if(leader.getClan().isMember(target)){
								Group group = groupManager.getWorldsHolder().getWorldData((Player)sender).getGroup(args[2]);
								if(group!=null){
									if(permissableGroupList.contains(group.getName())){
										Server server = sender.getServer();
										CommandSender console = server.getConsoleSender();
										server.dispatchCommand(console, "manuadd "+target.getName()+" "+ group.getName());
										sender.sendMessage(ChatColor.AQUA+"You have set "+target.getName()+"'s group to "+group.getName()+".");
										target.sendMessage(ChatColor.AQUA+sender.getName()+" has set your group to "+group.getName()+".");
									}else{
										sender.sendMessage(ChatColor.YELLOW+"You are not permitted to assign "+target.getName()+" to that group.");
									}
								}else{
									sender.sendMessage(ChatColor.YELLOW+"The group you specified does not exist");
								}
							}else{
								sender.sendMessage(ChatColor.YELLOW+"The player you specified is not in your clan.");
							}
						}else{
							sender.sendMessage(ChatColor.YELLOW+"The player you specified does not exist");
						}
					}else{
						sender.sendMessage(ChatColor.YELLOW+"/mafia setrank <player> <group>");
					}
				}else{
					sender.sendMessage(ChatColor.YELLOW+"/mafia setrank <player> <group>");
				}
			}else{
				sender.sendMessage(ChatColor.YELLOW+"You do not have permission to use this command.");
			}
		}
		return false;	
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

	private GroupManager hookGroupManager() {
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
}
