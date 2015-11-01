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
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HorseSaver extends JavaPlugin implements Listener {

	//essential variables
	HashMap<Horse, UUID> horses = new HashMap<Horse, UUID>();
	HashMap<Player, String> playerRegisterHorseMode = new HashMap<Player, String>();
	List<Player> playerStayHorseMode = new ArrayList<Player>();
	List<Horse> stayingHorses = new ArrayList<Horse>();
	Logger log = this.getServer().getLogger();
	
	//functional variables; partially used
	
	@Override
	public void onEnable() {
		reloadCfg();
		this.getServer().getPluginManager().registerEvents(new HorseSaverEventListener(this), this);
	}

	
	
	
	//Helper methods
	
	//reload the configuration
	@SuppressWarnings("unchecked")
	private void reloadCfg() {
		
		this.reloadConfig();
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
		
		
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
		List<String> list = new ArrayList<String>();
		if(!horses.containsValue(p.getUniqueId())) return list;
		horses.keySet().stream().filter(e -> horses.get(e).equals(p.getUniqueId())).collect(Collectors.toList()).forEach(h -> list.add(h.getCustomName()));
		return list;
	}
	
	//refresh the config.yml with new information
	void refreshConfig(){
		//horse-player-relationship
		HashMap<String, String> horsesToSave = new HashMap<String, String>();
		for (Entry<Horse, UUID> entry : horses.entrySet()) {
			horsesToSave.put(entry.getKey().getUniqueId().toString(), entry.getValue().toString());
		}
		this.getConfig().set("savedhorses", horsesToSave);
		
		//staying horses
		List<String> list = new ArrayList<String>();
		for (Horse h : stayingHorses) {
			list.add(h.getUniqueId().toString());
		}
		this.getConfig().set("stayingHorses", list);
		
		
		this.saveConfig();
	}
	
	//toggle horse staying
	void toggleHorseStaying(Horse h, Player p, boolean event){
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
		List<String> list = new ArrayList<String>();
		
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
