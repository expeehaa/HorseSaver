package de.expeehaa.spigot.horsesaver;

import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class HorseSaverEventListener implements Listener {
	
	HorseSaver main;
	
	public HorseSaverEventListener(HorseSaver main) {
		this.main = main;
	}
	
	
	@EventHandler
	public void onEntityDamaged(EntityDamageEvent e){
		if(e.getEntity() instanceof Horse 
				&& main.horses.containsKey((Horse)e.getEntity())){
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent e){
		if(e.getEntity() instanceof Horse
				&& main.horses.containsKey((Horse)e.getEntity())){
			Horse h = (Horse)e.getEntity();
			if(main.getServer().getPlayer(main.horses.get(h)) != null){
				main.getServer().getPlayer(main.getConfig().getString("msg.horse.die").replaceAll("\\{horsename\\}", h.getCustomName()).replaceAll("\\{cause\\}", h.getLastDamageCause().getCause().toString()));
			}
			main.horses.remove(h);
			if(main.stayingHorses.contains(h)) main.stayingHorses.remove(h);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerInteractsWithEntity(PlayerInteractEntityEvent e){
		//log.info("Player " + e.getPlayer().getName() + " is rightclicking on entity");
		Player p = e.getPlayer();
		Entity entity = e.getRightClicked();
		
		/*log.info("horse? " + (entity instanceof Horse ? "true" : "false") 
				+ " registered? " + (horses.containsKey((Horse)entity) ? "true" : "false")
				+ " sneaking? " + (p.isSneaking() ? "true" : "false")
				+ " staymode? " + (playerStayHorseMode.contains(p) ? "true" : "false")
				+ " savemode? " + (playerSaveHorseMode.containsKey(p) ? "true" : "false"));*/
		
		if(entity instanceof Horse 
				&& main.horses.containsKey((Horse)entity)){
			if(!main.horses.get((Horse)entity).equals(p.getUniqueId())){
				String horseownername = null;
				if(main.getServer().getOfflinePlayer(main.horses.get((Horse)entity)) != null) horseownername = main.getServer().getOfflinePlayer(main.horses.get((Horse)entity)).getName();
				else horseownername = main.getServer().getPlayer(main.horses.get((Horse)entity)).getName();
				main.log.info(main.getConfig().getString("msg.horse.unauthorizedAccess").replaceAll("\\{playername\\}", p.getName()).replaceAll("\\{horseowner\\}", horseownername).replaceAll("\\{horsename\\}", ((Horse)entity).getCustomName()));
				e.setCancelled(true);
				return;
			}
		}
		//registering and releasing horses
		if(entity instanceof Horse
				&& main.playerRegisterHorseMode.containsKey(p)
				&& p.isSneaking()){
			//log.info("(Un-)Registering horse for player " + p.getName());
			//register horse
			if(!main.horses.containsKey((Horse)entity)){
				main.log.info("");
				entity.setCustomName(main.playerRegisterHorseMode.get(p));
				entity.setCustomNameVisible(true);
				main.horses.put((Horse)entity, p.getUniqueId());
				main.playerRegisterHorseMode.remove(p);
				int horsecount = 0;
				for (Entry<Horse, UUID> entry : main.horses.entrySet()) {
					if(entry.getValue().equals(p.getUniqueId())) horsecount++;
				}
				e.getPlayer().sendMessage(main.getConfig().getString("msg.horse.registerMode.registered").replaceAll("\\{horsenumber\\}", String.valueOf(horsecount)));
				e.setCancelled(true);
			}
			
			
			//release horse
			else {
				entity.setCustomName("former used by HorseSaver");
				entity.setCustomNameVisible(false);
				
				main.horses.remove((Horse)entity);
				main.playerRegisterHorseMode.remove(p);
				int horsecount = 0;
				for (Entry<Horse, UUID> entry : main.horses.entrySet()) {
					if(entry.getValue().equals(p.getUniqueId())) horsecount++;
				}
				e.getPlayer().sendMessage(main.getConfig().getString("msg.horse.registerMode.unregistered").replaceAll("\\{horsenumber\\}", String.valueOf(horsecount)));
				e.setCancelled(true);
			}
			
			//refresh config.yml
			main.refreshConfig();
			return;
		}
		//make horses stay or not stay
		if(entity instanceof Horse 
				&& main.playerStayHorseMode.contains(p)
				&& p.isSneaking()){
			
			
			main.toggleHorseStaying((Horse)entity, p, true);
			
			e.setCancelled(true);
			//refresh config.yml
			main.refreshConfig();
			return;
		}
	}
}
