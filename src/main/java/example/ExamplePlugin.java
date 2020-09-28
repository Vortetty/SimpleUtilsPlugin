package example;

import arc.Events;
import arc.util.CommandHandler;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.UnitTypes;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tiles;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;

public class ExamplePlugin extends mindustry.mod.Plugin{
    
    double ratio = 2d/3d;
    HashSet<Player> votes = new HashSet<>();
    HashSet<Player> novotes = new HashSet<>();
    
    public ExamplePlugin(){
        Events.on(EventType.PlayerLeave.class, e-> {
            Player player = e.player;
            int cur = this.votes.size() - novotes.size();
            int req = (int) Math.ceil(ratio * Groups.player.size());
            if(votes.contains(player)) {
                votes.remove(player);
                Call.sendMessage("Map Skipper: [accent]" + player.name + "[] left, [green]" + cur + "[] votes, [green]" + req + "[] required");
            }
            if(novotes.contains(player)) {
                novotes.remove(player);
                Call.sendMessage("Map Skipper: [accent]" + player.name + "[] left, [green]" + cur + "[] votes, [green]" + req + "[] required");
            }
        });
        
        // clear votes on game over
        Events.on(EventType.GameOverEvent.class, e -> {
            this.votes.clear();
            this.novotes.clear();
        });
    }

    CommandHandler serverHandler;
    HashMap<String, Block> blockList = new HashMap<>();
    HashMap<String, UnitType> unitList = new HashMap<>();
    ArrayList<String> mechs = new ArrayList<String>();
    
    HashMap<String, BiFunction<Player, Boolean, Unit>> units = new HashMap<String, BiFunction<Player, Boolean, Unit>>(){
        {
            put("vortettyAir", (Player p, Boolean c) -> {
                UnitType unit = UnitTypes.mega;
                Unit outUnit = unit.create(p.team());
                outUnit.elevation = 1;
                outUnit.ammo = Integer.MAX_VALUE;
                outUnit.armor = Integer.MAX_VALUE;
                outUnit.spawnedByCore = c;
                
                return outUnit;
            });
        }
    };
    
    public void init() {
        Field[] blockFields = Blocks.class.getDeclaredFields();
        System.out.printf("Loading Blocks");
        for (Field field : blockFields) {
            if(field.getType().isAssignableFrom(Block.class)){
                try {
                    field.setAccessible(true);
                    Object blockbuf = field.get(Block.class);
                    Block block = (Block) blockbuf;
                    System.out.println(
                            "Loaded " + block.localizedName + " " + field.getName()
                    );
                    blockList.put(field.getName(), block);
                    blockList.put(block.localizedName, block);
                } catch(IllegalAccessException e){
                    System.out.println("Cant access " + field.getName());
                    continue;
                }
            }
        }
    
        Field[] unitFields = UnitTypes.class.getDeclaredFields();
        System.out.printf("Loading Units");
        for (Field field : unitFields) {
            if(field.getType().isAssignableFrom(UnitType.class)){
                try {
                    field.setAccessible(true);
                    Object unitbuf = field.get(UnitType.class);
                    UnitType unit = (UnitType) unitbuf;
                    System.out.println(
                            "Loaded " + unit.localizedName + " " + field.getName()
                    );
                    unitList.put(field.getName(), unit);
                    unitList.put(unit.localizedName, unit);
                    mechs.add(unit.localizedName);
                } catch(IllegalAccessException e){
                    System.out.println("Cant access " + field.getName());
                    continue;
                }
            }
        }
    }
    
    public void loadContent() {
    }
    
    public void registerServerCommands(CommandHandler handler) {
        serverHandler = handler;

        handler.register("saytoanon", "[message...]", "Tell specific player something.", args -> {
            if (!Vars.state.is(GameState.State.playing)) {
            } else {
                //find player by name
                Player other = Groups.player.find(p -> p.name.equalsIgnoreCase(args[0]));
            
                //give error message with scarlet-colored text if player isn't found
                if (other == null) {
                    return;
                }
            
                other.sendMessage("[lightgray]" + args[1]);
            }
        });

        handler.register("sayas", "<name+message...>", "Send a message to all players without server message.", arg -> {
            if (!Vars.state.is(GameState.State.playing)) {
                //Log.err("Not hosting. Host a game first.", new Object[0]);
            } else {
                if (arg[0] != "") {
                    ArrayList<String> parsedargs = new ArrayList<>(Arrays.asList(arg[0].split("\\[blue\\]: \\[white\\]")));
                    parsedargs.add("?");
                    parsedargs.add("?");
                    String name = parsedargs.get(0);
                    parsedargs.remove(0);
                    String message = String.join("", parsedargs);
                    Call.sendMessage("[scarlet][" + name + "[scarlet]][]: " + message);
                }
            }
        });
    
        handler.register("sayanon", "[message...]", "Send a message to all players without server message.", arg -> {
            if (!Vars.state.is(GameState.State.playing)) {
                //Log.err("Not hosting. Host a game first.", new Object[0]);
            } else {
                if (arg[0] != "") {
                    Call.sendMessage(arg[0]);
                }
                else {
                }
            }
        });

        handler.register("setblock", "<x> <y> <block-id/name> [team]", "Sets specified block", arg -> {
            Team team = Team.get(Integer.parseInt(arg[3].toLowerCase().replace("grey", "0").replace("derelict", "0").replace("yellow", "1").replace("sharded", "1").replace("red", "2").replace("crux", "2").replace("green", "3").replace("purple", "4").replace("blue", "5")));
            Vars.world.tile(Integer.parseInt(arg[0]), Integer.parseInt(arg[1])).setNet(blockList.get(arg[2]), team, 0);
        });
    }
    
    public void registerClientCommands(CommandHandler handler) {
        handler.register("setblock", "<x> <y> <block-id/name> [team]", "Sets specified block", (String[] arg, Player p) -> {
            if(p.admin){
                if (arg.length < 4) serverHandler.handleMessage("setblock " + arg[0] + " " + arg[1] + " " + blockList.get(arg[2]) + " " + p.team()); //System.out.println("setblock " + arg[0] + " " + arg[1] + " " + blockList.get(arg[2]));
                else serverHandler.handleMessage("setblock " + arg[0] + " " + arg[1] + " " + blockList.get(arg[2]) + " " + arg[3]);
            }
            else {
                p.sendMessage("[#ff5050]You do not have permission to perform this command, if you believe this to be an error please contact Shockelite or Vortetty on out discord https://discord.gg/5nnzbrf");
            }
        });
    
        handler.register("setunit", "<unit> [preservePrevious] [playerName...]", "Sets your unit to specified unit", (String[] arg, Player p) -> {
            if(p.admin){
                ArrayList<String> args = new ArrayList<>(Arrays.asList(arg));
                args.add("?"); // Pad array to avoid errors
                args.add("?");
    
                Player other = Groups.player.find(player -> player.name.equalsIgnoreCase(args.get(2).replaceAll("^((?!\\\\\\[).)*(\\]|\\\\\\])", "")));
                if(other != null){
                    p = other;
                }
                
                Unit playerUnit = p.unit();
                playerUnit.spawnedByCore(!args.get(1).toLowerCase().startsWith("t"));
                Unit unit = unitList.get(arg[0]).create(p.team());
                unit.spawnedByCore(true);
                unit.add();
                unit.x = playerUnit.x;
                unit.y = playerUnit.y;
                p.unit(unit);
                p.unit(unit);
                p.unit(unit);
            }
            else {
                p.sendMessage("[#ff5050]You do not have permission to perform this command, if you believe this to be an error please contact Shockelite or Vortetty on out discord https://discord.gg/5nnzbrf");
            }
        });
    
        handler.register("spawnunit", "<unit> <n>", "Spawns specified unit n times, n defaults to 1", (String[] arg, Player p) -> {
            if(p.admin){
                ArrayList<String> args = new ArrayList<>(Arrays.asList(arg));
                args.add("1"); // Pad array to avoid errors
                
                for(int i = 0; i < Integer.parseInt(args.get(1)); i++) {
                    Unit unit = unitList.get(arg[0]).create(p.team());
                    unit.spawnedByCore(false);
                    unit.add();
                    unit.x = p.x;
                    unit.y = p.y;
                }
            }
            else {
                p.sendMessage("[#ff5050]You do not have permission to perform this command, if you believe this to be an error please contact Shockelite or Vortetty on out discord https://discord.gg/5nnzbrf");
            }
        });
    
        handler.register("vortunit", "[player...]", "Sets the specified player's unit to Vort's personal unit", (String[] arg, Player p) -> {
            if(p.admin){
                ArrayList<String> args = new ArrayList<>(Arrays.asList(arg));
                args.add("?"); // Pad array to avoid errors
    
                Player other = Groups.player.find(player -> player.name.equalsIgnoreCase(args.get(0).replaceAll("^((?!\\\\\\[).)*(\\]|\\\\\\])", "")));
                if(other != null){
                    p = other;
                }
                
                Unit playerUnit = p.unit();
                playerUnit.spawnedByCore(true);
                
                Unit unit;
                
                unit = units.get("vortettyAir").apply(p, true);
                
                unit.spawnedByCore(true);
                unit.add();
                unit.x = playerUnit.x;
                unit.y = playerUnit.y;
                p.unit(unit);
            }
            else {
                p.sendMessage("[#ff5050]You do not have permission to perform this command, if you believe this to be an error please contact Shockelite or Vortetty on out discord https://discord.gg/5nnzbrf");
            }
        });

        handler.register("teams", "", "lists the teams", (String[] arg, Player p) -> {
            p.sendMessage("Teams:");
            p.sendMessage("[#4d4e58]Grey");
            p.sendMessage("[#ffd37f]Yellow");
            p.sendMessage("[#f25555]Red");
            p.sendMessage("[#4dd98b]Green");
            p.sendMessage("[#9a4bdf]Purple");
            p.sendMessage("[#4169FF]Blue");
        });
    
        handler.register("setblock", "<x> <y> <block-id/name> [team]", "Sets specified block", (String[] arg, Player p) -> {
            if(p.admin){
                if (arg.length < 4) serverHandler.handleMessage("setblock " + arg[0] + " " + arg[1] + " " + blockList.get(arg[2]) + " " + p.team()); //System.out.println("setblock " + arg[0] + " " + arg[1] + " " + blockList.get(arg[2]));
                else serverHandler.handleMessage("setblock " + arg[0] + " " + arg[1] + " " + blockList.get(arg[2]) + " " + arg[3]);
            }
            else {
                p.sendMessage("[#ff5050]You do not have permission to perform this command, if you believe this to be an error please contact Shockelite or Vortetty on out discord https://discord.gg/5nnzbrf");
            }
        });
    
        handler.register("killteam", "<team>", "Kills specified team", (String[] arg, Player p) -> {
            if(p.admin){
                Team team = Team.get(Integer.parseInt(arg[0].toLowerCase().replace("grey", "0").replace("derelict", "0").replace("yellow", "1").replace("sharded", "1").replace("red", "2").replace("crux", "2").replace("green", "3").replace("purple", "4").replace("blue", "5")));
                Tiles tiles = Vars.world.tiles;
                for(int x = 0; x < tiles.width; x++){
                    for(int y = 0; y < tiles.height; y++){
                        if(tiles.get(x, y).team() == team){
                            tiles.get(x, y).build.kill();
                        }
                    }
                }
            }
            else {
                p.sendMessage("[#ff5050]You do not have permission to perform this command, if you believe this to be an error please contact Shockelite or Vortetty on out discord https://discord.gg/5nnzbrf");
            }
        });
    
        handler.register("units", "", "lists the units you can be", (String[] arg, Player p) -> mechs.forEach(p::sendMessage));
        
        handler.register("skip", "<Y/N> [force(T/F)]", "Vote to change the map, force parameter requires admin", (String[] arg, Player p) -> {
            ArrayList<String> args = new ArrayList<>(Arrays.asList(arg));
            args.add("?"); // Pad array to avoid errors
            
            if(p.admin && args.get(1).toLowerCase().startsWith("t")){
                this.votes.clear();
                Call.sendMessage("Map Skipper:[gold] Map change forced by an admin.");
                Events.fire(new EventType.GameOverEvent(Team.derelict));
                Events.fire(new EventType.GameOverEvent(Team.sharded));
                Events.fire(new EventType.GameOverEvent(Team.purple));
                Events.fire(new EventType.GameOverEvent(Team.green));
                Events.fire(new EventType.GameOverEvent(Team.crux));
                Events.fire(new EventType.GameOverEvent(Team.blue));
            }
            else {
                if(args.get(0).toLowerCase().startsWith("y")){
                    this.votes.add(p);
                    this.novotes.remove(p);
                }
                else {
                    this.novotes.add(p);
                    this.votes.remove(p);
                }
                int cur = this.votes.size() - novotes.size();
                int req = (int) Math.ceil(ratio * Groups.player.size());
                if(args.get(0).toLowerCase().startsWith("y")){
                    Call.sendMessage("Map Skipper: [accent]" + p.name + "[] wants to change the map, [green]" + cur + "[] votes, [green]" + req + "[] required");
                }
                else {
                    Call.sendMessage("Map Skipper: [accent]" + p.name + "[] does not want to change the map, [green]" + cur + "[] votes, [green]" + req + "[] required");
                }
                
                if (cur < req) {
                    return;
                }
    
                this.votes.clear();
                Call.sendMessage("Map Skipper: [green] vote passed, changing map.");
                Events.fire(new EventType.GameOverEvent(Team.crux));
            }
        });
    }
}