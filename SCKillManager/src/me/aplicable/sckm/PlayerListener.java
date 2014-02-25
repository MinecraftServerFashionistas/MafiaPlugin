package me.aplicable.sckm;

import java.util.logging.Logger;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener{
	
	@SuppressWarnings("unused")
	private SCKillManager killManager;
	private SQLManager sqlManager;
	
	public PlayerListener(SCKillManager killManager){
		this.killManager=killManager;
		this.sqlManager=killManager.getSQLManager();
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event){
		Player player = event.getPlayer();
		if(!sqlManager.playerPresent(player.getName())){
			Logger.getLogger("Minecraft.SCKillManager").info(player.getName() + " is new!");
			sqlManager.preparePlayer(player.getName());
		}
	}
	
}
