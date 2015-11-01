package de.expeehaa.spigot.horsesaver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HorseSaver extends JavaPlugin implements Listener {

	//essential variables
	HashMap<Horse, UUID> horses = new HashMap<>();
	HashMap<Player, String> playerRegisterHorseMode = new HashMap<>();
	List<Player> playerStayHorseMode = new ArrayList<>();
	List<Horse> stayingHorses = new ArrayList<>();
	Logger log = this.getServer().getLogger();
	
	//functional variables; partially used
	
	@Override
	public void onEnable() {
		reloadCfg();
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	
	
	
	//Helper methods
	
	//reload the configuration
	@SuppressWarnings("unchecked")
	private void reloadCfg() {
		
		this.reloadConfig();
		if(this.getConfig().get("savedhorses") != null){
			HashMap<String, Object> savedhorses = (HashMap<String, Object>) this.getConfig().getConfigurationSection("savedhorses").getValues(false);
			for (Entry<String, Object> entry : savedhorses.entrySet()) {
				if(entry.getValue() == null) continue;
				Horse horse = getHorseByUUID(entry.getKey());
				if(horse == null) continue;
				horses.put(horse, UUID.fromString(entry.getValue().toString()));
			}
		}
		log.info("Loaded " + horses.keySet().size() + " horses!");
		if(this.getConfig().getList("stayingHorses") != null){
			for (Object o : (List<Object>) this.getConfig().getList("stayingHorses")) {
				stayingHorses.add(getHorseByUUID(String.valueOf(o)));
			}
		}
		
		
		this.getConfig().addDefault("msg.horse.move.true", "§bYour horse {horsename} can now move!");
		this.getConfig().addDefault("msg.horse.move.false", "§bYour horse {horsename} is now staying!");
		this.getConfig().addDefault("msg.horse.die", "§4Your horse {horsename} was killed by {cause}");
		this.getConfig().addDefault("msg.horse.wrongName", "§4You do not own a horse named {horsename}");
		this.getConfig().addDefault("msg.horse.unauthorizedAccess", "Player {playername} tried to get access to {horseowner}'s horse {horsename}!");
		
		this.getConfig().addDefault("msg.horse.registerMode.add", "§bRight click a horse to claim it as your own or release it!");
		this.getConfig().addDefault("msg.horse.registerMode.remove", "§bYou were §4removed §bfrom horse register mode!");
		this.getConfig().addDefault("msg.horse.registerMode.noName", "§4Please specify a name for your horse!");
		this.getConfig().addDefault("msg.horse.registerMode.assignedName", "§4The given name is already assigned to another horse!");
		this.getConfig().addDefault("msg.horse.registerMode.registered", "§bYou registered your {horsenumber} horse!");
		this.getConfig().addDefault("msg.horse.registerMode.unregistered", "§bYou only have {horsenumber} horse(s) now!");
		
		this.getConfig().addDefault("msg.horse.stayMode.remove", "§bYou were §4removed §bfrom horse stay mode!");
		this.getConfig().addDefault("msg.horse.stayMode.add", "§bRight click a horse to toggle staying!");
		
		this.getConfig().addDefault("msg.horse.tpto.argument", "§4Please specify the horse's name you wish to teleport to!");
		this.getConfig().addDefault("msg.horse.tpto.success", "§bYou were teleported to your horse {horsename}");
		
		this.getConfig().addDefault("msg.horse.tphere.argument", "§4Please specify the horse's name you wish to teleport to you!");
		this.getConfig().addDefault("msg.horse.tphere.success", "§bYour horse {horsename} was teleported to you!");
		
		this.getConfig().addDefault("msg.command.noPlayer", "§4You need to be a player to execute this command!");
		this.getConfig().addDefault("msg.command.arguments", "§4Choose some arguments ('tab' for auto completion)");
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
		this.reloadConfig();
	}

	//get a horse by stringified uuid
	private Horse getHorseByUUID(String uuid) {
		Horse horse = null;
		for (World w : this.getServer().getWorlds()) {
			for (Entity entity : w.getEntities()) {
				if(entity instanceof Horse && entity.getUniqueId().toString().equals(uuid)){
					horse = (Horse)entity;
				}
			}
		}
		return horse;
	}
	
	//get a horse by name
	private Horse getPlayersHorseByName(String name, Player p) {
		Horse horse = null;
		if(!horses.containsValue(p.getUniqueId())) return null;
		List<Horse> horselist = horses.keySet().stream().filter(e -> horses.get(e).equals(p.getUniqueId())).collect(Collectors.toList());
		for (Horse h : horselist) {
			if(h.getCustomName().equals(name)){
				horse = h;
				continue;
			}
		}
		return horse;
	}
	
	private List<String> getPlayersOwnedHorses(Player p){
		List<String> list = new ArrayList<>();
		if(!horses.containsValue(p.getUniqueId())) return list;
		horses.keySet().stream().filter(e -> horses.get(e).equals(p.getUniqueId())).collect(Collectors.toList()).forEach(h -> list.add(h.getCustomName()));
		return list;
	}
	
	//refresh the config.yml with new information
	private void refreshConfig(){
		//horse-player-relationship
		HashMap<String, String> horsesToSave = new HashMap<>();
		for (Entry<Horse, UUID> entry : horses.entrySet()) {
			horsesToSave.put(entry.getKey().getUniqueId().toString(), entry.getValue().toString());
		}
		this.getConfig().set("savedhorses", horsesToSave);
		
		//staying horses
		List<String> list = new ArrayList<>();
		for (Horse h : stayingHorses) {
			list.add(h.getUniqueId().toString());
		}
		this.getConfig().set("stayingHorses", list);
		
		
		this.saveConfig();
	}
	
	//toggle horse staying
	private void toggleHorseStaying(Horse h, Player p, boolean event){
		//free horse
		if(stayingHorses.contains(h)) {
			stayingHorses.remove(h);
			h.removePotionEffect(PotionEffectType.SLOW);
			h.removePotionEffect(PotionEffectType.JUMP);
			p.sendMessage(this.getConfig().getString("msg.horse.move.true").replaceAll("\\{horsename\\}", h.getCustomName()));
		}
		//staying horse
		else {
			stayingHorses.add(h);
			h.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1000000, 32768, false, false));
			h.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 1000000, -32768, false, false));
			p.sendMessage(this.getConfig().getString("msg.horse.move.false").replaceAll("\\{horsename\\}", h.getCustomName()));
		}
		if(event) playerStayHorseMode.remove(p);
		refreshConfig();
	}
	
	
	
	
	//Event Handlers
	
	@EventHandler
	public void onEntityDamaged(EntityDamageEvent e){
		if(e.getEntity() instanceof Horse 
				&& horses.containsKey((Horse)e.getEntity())){
			e.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent e){
		if(e.getEntity() instanceof Horse
				&& horses.containsKey((Horse)e.getEntity())){
			Horse h = (Horse)e.getEntity();
			if(this.getServer().getPlayer(horses.get(h)) != null){
				this.getServer().getPlayer(this.getConfig().getString("msg.horse.die").replaceAll("\\{horsename\\}", h.getCustomName()).replaceAll("\\{cause\\}", h.getLastDamageCause().getCause().toString()));
			}
			horses.remove(h);
			if(stayingHorses.contains(h)) stayingHorses.remove(h);
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
				&& horses.containsKey((Horse)entity)){
			if(!horses.get((Horse)entity).equals(p.getUniqueId())){
				String horseownername = null;
				if(this.getServer().getOfflinePlayer(horses.get((Horse)entity)) != null) horseownername = this.getServer().getOfflinePlayer(horses.get((Horse)entity)).getName();
				else horseownername = this.getServer().getPlayer(horses.get((Horse)entity)).getName();
				log.info(this.getConfig().getString("msg.horse.unauthorizedAccess").replaceAll("\\{playername\\}", p.getName()).replaceAll("\\{horseowner\\}", horseownername).replaceAll("\\{horsename\\}", ((Horse)entity).getCustomName()));
				e.setCancelled(true);
				return;
			}
		}
		//registering and releasing horses
		if(entity instanceof Horse
				&& playerRegisterHorseMode.containsKey(p)
				&& p.isSneaking()){
			//log.info("(Un-)Registering horse for player " + p.getName());
			//register horse
			if(!horses.containsKey((Horse)entity)){
				log.info("");
				entity.setCustomName(playerRegisterHorseMode.get(p));
				entity.setCustomNameVisible(true);
				horses.put((Horse)entity, p.getUniqueId());
				playerRegisterHorseMode.remove(p);
				int horsecount = 0;
				for (Entry<Horse, UUID> entry : horses.entrySet()) {
					if(entry.getValue().equals(p.getUniqueId())) horsecount++;
				}
				e.getPlayer().sendMessage(this.getConfig().getString("msg.horse.registerMode.registered").replaceAll("\\{horsenumber\\}", String.valueOf(horsecount)));
				e.setCancelled(true);
			}
			
			
			//release horse
			else {
				entity.setCustomName("former used by HorseSaver");
				entity.setCustomNameVisible(false);
				
				horses.remove((Horse)entity);
				playerRegisterHorseMode.remove(p);
				int horsecount = 0;
				for (Entry<Horse, UUID> entry : horses.entrySet()) {
					if(entry.getValue().equals(p.getUniqueId())) horsecount++;
				}
				e.getPlayer().sendMessage(this.getConfig().getString("msg.horse.registerMode.unregistered").replaceAll("\\{horsenumber\\}", String.valueOf(horsecount)));
				e.setCancelled(true);
			}
			
			//refresh config.yml
			refreshConfig();
			return;
		}
		//make horses stay or not stay
		if(entity instanceof Horse 
				&& playerStayHorseMode.contains(p)
				&& p.isSneaking()){
			
			
			toggleHorseStaying((Horse)entity, p, true);
			
			e.setCancelled(true);
			//refresh config.yml
			refreshConfig();
			return;
		}
	}
	
	
	
	//Commands
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		final Player p;
		if(sender instanceof Player) p = (Player)sender;
		else p = null;
		
		if(cmd.getName().equalsIgnoreCase("horse") && sender.hasPermission("horsesaver.register")){
			if(args.length == 0) {
				sender.sendMessage(this.getConfig().getString("msg.command.arguments"));
				return true;
			}
			
			if(p == null){
				sender.sendMessage(this.getConfig().getString("msg.command.noPlayer"));
				return true;
			}
			
			//save horses
			if(args[0].equalsIgnoreCase("register")){
				
				if(playerRegisterHorseMode.containsKey(p)){
					p.sendMessage(this.getConfig().getString("msg.horse.registerMode.remove"));
					playerRegisterHorseMode.remove(p);
				}
				else {
					if(args.length < 2){
						p.sendMessage(this.getConfig().getString("msg.horse.registerMode.noName"));
					}
					
					if(horses.containsValue(p)){
						if(horses.keySet().stream().filter(e -> horses.get(e).equals(p.getUniqueId()))
							.anyMatch(h -> args[1].equalsIgnoreCase(h.getCustomName()))){
							
							p.sendMessage(this.getConfig().getString("msg.horse.registerMode.assignedName"));
							return true;
						}
					}
							
					
					p.sendMessage(this.getConfig().getString("msg.horse.registerMode.add"));
					playerRegisterHorseMode.put(p, args[1]);
				}
				
			}
			
			//make horses staying at their current point
			else if(args[0].equalsIgnoreCase("stay") && p.hasPermission("horsesaver.stay")){
				
				boolean contained = playerStayHorseMode.contains(p);
				
				//remove player from stayhorsemode if contained
				if(contained){
					p.sendMessage(this.getConfig().getString("msg.horse.stayMode.remove"));
					playerStayHorseMode.remove(p);
				}
				
				if(args.length == 1){
					if(!contained) {
						p.sendMessage(this.getConfig().getString("msg.horse.stayMode.add"));
						playerStayHorseMode.add(p);
					}
				}
				//if name is given, toggle staying for given horse
				else if(args.length > 1){
					if(horses.containsValue(p.getUniqueId())){
						Horse horse = getPlayersHorseByName(args[1], p);
						if(horse == null){
							p.sendMessage(this.getConfig().getString("msg.horse.wrongName").replaceAll("\\{horsename\\}", args[1]));
							return true;
						}
						toggleHorseStaying(horse, p, false);
					}
				}
				
			}
			
			//teleport horse to the player
			else if(args[0].equalsIgnoreCase("tphere") && p.hasPermission("horsesaver.tphere")){
				if(args.length <= 1){
					p.sendMessage(this.getConfig().getString("msg.horse.tphere.argument"));
					return true;
				}
				Horse h = getPlayersHorseByName(args[1], p);
				if(h == null){
					p.sendMessage(this.getConfig().getString("msg.horse.wrongName").replaceAll("\\{horsename\\}", args[1]));
					return true;
				}
				h.teleport(p);
				p.sendMessage(this.getConfig().getString("msg.horse.tphere.success").replaceAll("\\{horsename\\}", args[1]));
			}
			
			//teleport player to a horse
			else if(args[0].equalsIgnoreCase("tpto") && p.hasPermission("horsesaver.tpto")){
				if(args.length <= 1){
					p.sendMessage(this.getConfig().getString("msg.horse.tpto.success"));
					return true;
				}
				Horse h = getPlayersHorseByName(args[1], p);
				if(h == null){
					p.sendMessage(this.getConfig().getString("msg.horse.wrongName").replaceAll("\\{horsename\\}", args[1]));
					return true;
				}
				p.teleport(h);
				p.sendMessage(this.getConfig().getString("msg.horse.tpto.success").replaceAll("\\{horsename\\}", args[1]));
			}
			return true;
		}
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		List<String> list = new ArrayList<>();
		
		if(!(sender instanceof Player)) return null;
		
		if(cmd.getName().equalsIgnoreCase("horse")){
			if(args.length == 1){
				if(sender.hasPermission("horsesaver.register")) list.add("register");
				if(sender.hasPermission("horsesaver.stay")) list.add("stay");
				if(sender.hasPermission("horsesaver.tpto")) list.add("tpto");
				if(sender.hasPermission("horsesaver.tphere")) list.add("tphere");
			}
			if(args.length == 2){
				if((args[0].equalsIgnoreCase("stay") && sender.hasPermission("horsesaver.stay"))
						|| (args[0].equalsIgnoreCase("tpto") && sender.hasPermission("horsesaver.tpto")) 
						|| (args[0].equalsIgnoreCase("tphere") && sender.hasPermission("horsesaver.tphere"))){
					list.addAll(getPlayersOwnedHorses((Player) sender));
				}
			}
		}
		
		return list;
	}
}
