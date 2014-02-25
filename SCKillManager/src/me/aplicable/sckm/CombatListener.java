package me.aplicable.sckm;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class CombatListener implements Listener{
	
	private KillProcessor processor;
	
	public CombatListener(KillProcessor processor){
		this.processor=processor;
	}
	
	@EventHandler
	public void onPlayerKill(EntityDeathEvent event){
		Player attacker;
		Player target;
		if(event.getEntity() instanceof Player)
			target = (Player)(event.getEntity());
		else
			return;
		if(target.getLastDamageCause().getEntity() instanceof Player)
			attacker = (Player)(target.getKiller());
		else
			return;
		processor.processKill(attacker,target);
	}
}
