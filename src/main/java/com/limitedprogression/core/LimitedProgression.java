package com.limitedprogression.core;

import com.google.common.primitives.Doubles;
import com.limitedprogression.commands.AgeSwitcher;
import com.limitedprogression.commands.AgeTabCompleter;
import org.apache.commons.lang3.ObjectUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEnterEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.StructurePiece;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.generator.structure.Structure;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;


public final class LimitedProgression extends JavaPlugin implements Listener{

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(LimitedProgression.class);
    private Logger logger = getLogger();
    private age CurrentAge = age.wood;
    private World overworld;
    private World nether;
    private World end;
    private Collection<BoundingBox> BannedRegions;
    private Collection<BoundingBox> BannedPieces;
    private List<World> worldList;
    private Boolean initialize = true;
    Iterator<Recipe> recipiter = getServer().recipeIterator();

    @Override
    public void onEnable() {
        worldList = new ArrayList<>();
        //overworld = Bukkit.getServer().getWorld("world");
        //nether = Bukkit.getServer().getWorld("world_nether");
        //end = Bukkit.getServer().getWorld("world_the_end");
        saveDefaultConfig();
        BannedRegions = new HashSet<>() {
        };
        BannedPieces = new HashSet<>(){};
        logger.info("Limited Progression Plugin has been Enabled");
        loadCommands();
        String configAge = getConfig().getString("current-age");
        getServer().getPluginManager().registerEvents(this,this);
        age agetosetto = age.wood;
        try {
            agetosetto = age.valueOf(configAge);
            logger.info("successfully retrieved age: "+ configAge);
        }
        catch (IllegalArgumentException e){
            logger.info("config.yml has invalid age set");
            getConfig().set("current-age","wood");
            saveConfig();
        }
        setCurrentAge(agetosetto);
    }
    public void setCurrentAge(age newAge){

        CurrentAge = newAge;
        getConfig().set("current-age",newAge.name());

        saveConfig();
        refreshWorld();

        //reset to defaul recipes, then remove ones not allowed
        if(initialize){
            initialize = false;
        }
        else{
            getServer().resetRecipes();
            recipiter = getServer().recipeIterator();
        }
        while (recipiter.hasNext()){
            Recipe r = recipiter.next();
            if(ageLookup.get(allowBlockDrop.get(r.getResult().getType()))>ageLookup.get(getCurrentAge())){
                //logger.info(r.getResult().getType().name() + " REMOVED");
                recipiter.remove();
            }
        }
        //Trading Age
        if(ageLookup.get(age.trade)<=ageLookup.get(CurrentAge)) {
            for(World world:Bukkit.getWorlds()){
                world.setGameRule(GameRule.DO_TRADER_SPAWNING, true);
            }
        }
        else{
            for(World world:Bukkit.getWorlds()){
                world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
            }
        }
        //PILLAGER AGE -------------------------------
        if(ageLookup.get(age.pillager)<=ageLookup.get(CurrentAge)) {
            for(World world:Bukkit.getWorlds()){
                world.setGameRule(GameRule.DO_PATROL_SPAWNING, true);
                world.setGameRule(GameRule.DISABLE_RAIDS, false);
            }

        }
        else{
            for(World world:Bukkit.getWorlds()){
                world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
                world.setGameRule(GameRule.DISABLE_RAIDS, true);
            }
        }
        //
    }
    private void refreshWorld(){
        BannedRegions.clear();
        BannedPieces.clear();
        for(World world : worldList){
            Chunk[] chunkstorefresh = world.getLoadedChunks();
            for(Chunk chunk: chunkstorefresh){
                checkForBannedRegions(chunk);
            }
        }
    }
    private void checkForBannedRegions (Chunk chunk){
        Collection<GeneratedStructure> ChunkStructs = chunk.getStructures();
        for (GeneratedStructure genStruct : ChunkStructs) {
            if (!BannedRegions.contains(genStruct.getBoundingBox()) && ageLookup.get(structureLookup.get(genStruct.getStructure())) > ageLookup.get(CurrentAge)){

                BannedRegions.add(getBoundingBox(genStruct));
                if(genStruct.getStructure() == Structure.DESERT_PYRAMID || genStruct.getStructure() == Structure.JUNGLE_PYRAMID){
                        BannedPieces.add(genStruct.getBoundingBox());
                        /*
                        double xCorner = genStruct.getBoundingBox().getCenterX();
                        double yCorner = genStruct.getBoundingBox().getMinY();
                        double zCorner = genStruct.getBoundingBox().getCenterZ();
                        BannedPieces.add(new BoundingBox(xCorner-3,yCorner-14,zCorner-3,xCorner+4, yCorner,zCorner+4)); //loot room is not part of structure
                        logger.info((xCorner-3.5)+" "+ (yCorner-14)+" "+(zCorner-3.5)+" "+(xCorner+3.5)+" "+ yCorner+" "+(zCorner+3.5) );
                        logger.info(xCorner+" "+yCorner+" "+zCorner);
                         */
                }
                else{
                    for(StructurePiece piece: genStruct.getPieces()){
                        BannedPieces.add(piece.getBoundingBox());
                    }
                }

                logger.info("adding banned region: " + genStruct.getStructure());

            }

        }
    }

    private static @NotNull BoundingBox getBoundingBox(GeneratedStructure genStruct) {
        return genStruct.getBoundingBox();
    }

    public age getCurrentAge(){
        return CurrentAge;
    }


    @Override
    public void onDisable() {
        logger.info("Limited Progression Plugin has been Disabled");
    }

    private void loadCommands(){
        getCommand("age").setExecutor(new AgeSwitcher(this));
        getCommand("age").setTabCompleter(new AgeTabCompleter());
    }
    @EventHandler
    private void onBlockPlace(BlockPlaceEvent e){
        for(BoundingBox region: BannedPieces){
            if (region.contains(e.getBlock().getBoundingBox())){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onPortalCreate(PortalCreateEvent e){
        if(e.getReason().equals(PortalCreateEvent.CreateReason.FIRE)){
            if(ageLookup.get(age.nether)>ageLookup.get(CurrentAge)){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void EntityPortalEnter(EntityPortalEnterEvent e){
        if(e.getPortalType().equals(PortalType.NETHER)){
            if(ageLookup.get(age.nether)>ageLookup.get(CurrentAge)){
                e.setCancelled(true);
            }
        } else if (e.getPortalType().equals(PortalType.ENDER)) {
            if(ageLookup.get(age.end)>ageLookup.get(CurrentAge)){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void PlayerPortalEnter(PlayerPortalEvent e){
        if(e.getCause().equals(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL)){
            if(ageLookup.get(age.nether)>ageLookup.get(CurrentAge)){
                e.setCancelled(true);
            }
        } else if (e.getCause().equals(PlayerTeleportEvent.TeleportCause.END_PORTAL)) {
            if(ageLookup.get(age.end)>ageLookup.get(CurrentAge)){
                e.setCancelled(true);
            }
        }
        else if(e.getCause().equals(PlayerTeleportEvent.TeleportCause.END_GATEWAY)){
            if(ageLookup.get(age.postend)>ageLookup.get(CurrentAge)){
                e.setCancelled(true);
            }
        }
    }


    @EventHandler(priority = EventPriority.LOW)
    public void onEntitySpawn(EntitySpawnEvent e){
        /** unneeded due to trader spawn game rule
        if(e.getEntityType() == EntityType.WANDERING_TRADER || e.getEntityType() == EntityType.TRADER_LLAMA ){
            if(ageLookup.get(age.trade)>ageLookup.get(CurrentAge)) {
                e.setCancelled(true);
            }
        }
         **/
        Entity entity = e.getEntity();
        EntityType et = entity.getType();
        if(entity instanceof LivingEntity){
            if(et==EntityType.DROWNED){
                Drowned d = (Drowned)entity;
                if(d.getEquipment().getItemInMainHand().getType().equals(Material.TRIDENT)){
                    if(ageLookup.get(age.ocean)>ageLookup.get(CurrentAge)) {
                        d.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                    }
                }
            }
            else if(et== EntityType.SKELETON_HORSE){
                if(ageLookup.get(age.stone)>ageLookup.get(CurrentAge)) {
                    e.setCancelled(true);
                }
            }
            else if(et == EntityType.ZOMBIE){
                Zombie z = (Zombie)entity;
                ItemStack[] armor = z.getEquipment().getArmorContents();
                if(ageLookup.get(allowBlockDrop.get(z.getEquipment().getItemInMainHand().getType()))>ageLookup.get(CurrentAge)){
                    z.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(ageLookup.get(allowBlockDrop.get(piece.getType()))>ageLookup.get(CurrentAge)){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.ZOMBIE_VILLAGER){
                ZombieVillager z = (ZombieVillager) entity;
                ItemStack[] armor = z.getEquipment().getArmorContents();
                if(ageLookup.get(allowBlockDrop.get(z.getEquipment().getItemInMainHand().getType()))>ageLookup.get(CurrentAge)){
                    z.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(ageLookup.get(allowBlockDrop.get(piece.getType()))>ageLookup.get(CurrentAge)){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.HUSK){
                Husk h = (Husk)entity;
                ItemStack[] armor = h.getEquipment().getArmorContents();
                if(ageLookup.get(allowBlockDrop.get(h.getEquipment().getItemInMainHand().getType()))>ageLookup.get(CurrentAge)){
                    h.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(ageLookup.get(allowBlockDrop.get(piece.getType()))>ageLookup.get(CurrentAge)){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.STRAY){
                Stray s = (Stray)entity;
                ItemStack[] armor = s.getEquipment().getArmorContents();
                if(ageLookup.get(allowBlockDrop.get(s.getEquipment().getItemInMainHand().getType()))>ageLookup.get(CurrentAge)){
                    s.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(ageLookup.get(allowBlockDrop.get(piece.getType()))>ageLookup.get(CurrentAge)){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.BOGGED){
                Bogged b = (Bogged) entity;
                ItemStack[] armor = b.getEquipment().getArmorContents();
                if(ageLookup.get(allowBlockDrop.get(b.getEquipment().getItemInMainHand().getType()))>ageLookup.get(CurrentAge)){
                    b.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(ageLookup.get(allowBlockDrop.get(piece.getType()))>ageLookup.get(CurrentAge)){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.SKELETON){
                Skeleton s = (Skeleton) entity;
                ItemStack[] armor = s.getEquipment().getArmorContents();
                if(ageLookup.get(allowBlockDrop.get(s.getEquipment().getItemInMainHand().getType()))>ageLookup.get(CurrentAge)){
                    s.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(ageLookup.get(allowBlockDrop.get(piece.getType()))>ageLookup.get(CurrentAge)){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.FOX){
                Fox f = (Fox) entity;
                if(ageLookup.get(allowBlockDrop.get(f.getEquipment().getItemInMainHand().getType()))>ageLookup.get(CurrentAge)){
                    f.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
            }
            else if(et == EntityType.GUARDIAN){
                if(ageLookup.get(age.ocean)>ageLookup.get(CurrentAge)){
                    e.setCancelled(true);
                }
            }
        }

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        for(BoundingBox region: BannedPieces){
            if (region.contains(e.getBlock().getBoundingBox())){
                e.setCancelled(true);

            }
        }
        if(!droppedFromBreaking(e.getBlock().getType())){
            e.setDropItems(false);
        }
    }
    @EventHandler(priority = EventPriority.LOW)
    private void onPlayerInteract (PlayerInteractEvent e){
        if(e.getAction()== Action.RIGHT_CLICK_BLOCK){
            for(BoundingBox region: BannedPieces){
                try{
                    if (region.contains(e.getClickedBlock().getBoundingBox())){
                        e.setUseInteractedBlock(PlayerInteractEvent.Result.DENY);
                        e.setCancelled(true);
                        logger.info("THIS INTERACTION SHOULD BE DENIED");
                        return;
                    }
                }
                catch(Exception exep){
                    logger.info(exep.toString());
                }

            }
        }

    }
    private void onPlayerInteractEntity(PlayerInteractEntityEvent e){
        logger.info("interact Entity");
    }
    private void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e){
        logger.info("interact at entity");
    }
    @EventHandler
    private void onPlayerEntityInteract (PlayerInteractEntityEvent e){
        for(BoundingBox region: BannedPieces){
            if (region.contains(e.getRightClicked().getBoundingBox())){
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e){
        logger.info("LOADING A WORLD AND IM DETECTING IT");
        worldList.add(e.getWorld());
    }
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent s){

        if(!droppedFromBreaking(s.getEntity().getItemStack().getType())){
            logger.info("stopped"+s.getEntity().getItemStack().getType()+ " from spawning");
            s.setCancelled(true);
        }
    }
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e){
        checkForBannedRegions(e.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e){
        //Collection<GeneratedStructure> structureStarts = e.getChunk().getStructures();
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent move){
        Location oldLoc = move.getFrom().toBlockLocation();
        Location newLoc = move.getTo().toBlockLocation();
        if(oldLoc.x()==newLoc.x()&oldLoc.y()==newLoc.y()&oldLoc.z()==newLoc.z()){
            return;
        }

        for(BoundingBox region: BannedPieces){
            if(region.contains(newLoc.x(), newLoc.y(), newLoc.z())){
                Player p = move.getPlayer();
                Location newLocPercise = move.getTo();
                Location oldLocPercise = move.getFrom();
                //prevent being stuck on top by allowing them to move freely across top region
                if(oldLoc.y() > newLoc.y()){ //oldLoc.x()==newLoc.x() && oldLoc.z()==newLoc.z() &&
                    Vector vel = p.getVelocity();
                    p.teleport(new Location(oldLoc.getWorld(),newLocPercise.x(),oldLoc.y(),newLocPercise.z(), newLoc.getYaw(), newLoc.getPitch()));
                    p.setVelocity(new Vector(vel.getX(),.1,vel.getZ()));
                }
                else{
                    //teleport them to last block they were in.
                    p.teleport(new Location(oldLocPercise.getWorld(),oldLocPercise.x(),oldLocPercise.y(),oldLocPercise.z(), newLoc.getYaw(), newLoc.getPitch()));
                }
                p.sendMessage("This structure is not unlocked yet");

            }

        }

    }
/*
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent move){
        Player player = move.getPlayer();
         BoundingBox box = move.getPlayer().getBoundingBox();
         Boolean playeradjusted = false; //determines whether velocity is set or added to (in multiple hitboxes)
         for(BoundingBox region: BannedPieces){
             if(region.overlaps(box)){
                 logger.info("OVERLAPPING");
                 //BoundingBox moveBox = region.intersection(box);
                 BoundingBox moveBox = new BoundingBox(region.getMinX(),region.getMinY(),region.getMinZ(),region.getMaxX(),region.getMaxY(),region.getMaxZ());
                 moveBox = moveBox.intersection(box);
                 Double boxX = moveBox.getMaxX() - moveBox.getMinX();
                 Double boxY = moveBox.getMaxY() - moveBox.getMinY();
                 Double boxZ = moveBox.getMaxZ() - moveBox.getMinZ();
                 Double VectorStrength = 1.0;
                 Vector playerVel = player.getVelocity();
                 if(boxX < boxY){
                     if(boxX < boxZ){
                         //move X
                         if(box.getCenter().getX() > region.getCenter().getX()){
                             player.setVelocity(new Vector(boxX*VectorStrength,playerVel.getY(),playerVel.getZ()));
                             logger.info("PUSHING POSITIVE X");
                         }
                         else{
                             player.setVelocity(new Vector(-boxX*VectorStrength,playerVel.getY(),playerVel.getZ()));
                             logger.info("PUSHING NEGATIVE X");
                         }
                     }
                     else{
                         //move Z
                         if(box.getCenter().getZ() > region.getCenter().getZ()){
                             player.setVelocity(new Vector(playerVel.getX(),playerVel.getY(),boxZ*VectorStrength));
                             logger.info("PUSHING POSITIVE Z");
                         }
                         else{
                             player.setVelocity(new Vector(playerVel.getX(),playerVel.getY(),-boxZ*VectorStrength));
                             logger.info("PUSHING NEGATIVE Z");
                         }
                     }
                 }
                 else{
                     if(boxY < boxZ){
                         //move Y
                         if(box.getCenter().getY() > region.getCenter().getY()){
                             player.setVelocity(new Vector(playerVel.getX(),boxY*VectorStrength,playerVel.getZ()));
                             logger.info("PUSHING POSITIVE Y");
                         }
                         else{
                             player.setVelocity(new Vector(playerVel.getX(),-boxY*VectorStrength,playerVel.getZ()));
                             logger.info("PUSHING NEGATIVE Y");
                         }
                     }
                     else{
                         //move Z
                         if(box.getCenter().getZ() > region.getCenter().getZ()){
                             player.setVelocity(new Vector(playerVel.getX(),playerVel.getY(),boxZ*VectorStrength));
                             logger.info("PUSHING POSITIVE Z");
                         }
                         else{
                             player.setVelocity(new Vector(playerVel.getX(),playerVel.getY(),-boxZ*VectorStrength));
                             logger.info("PUSHING NEGATIVE Z");
                         }
                     }
                 }
             }

         }
    }
*/
    private boolean droppedFromBreaking(Material m){
        if(ageLookup.get(allowBlockDrop.get(m))<=ageLookup.get(CurrentAge)){
            return true;
        }
        else return false;
    }

    public enum age {
        wood,
        stone,
        iron,
        nether,
        redstone,
        diamond,
        trade,
        ocean,
        trial,
        pillager,
        end,
        postend,
        netherite,
    }
    Dictionary<Structure, age> structureLookup = new Hashtable<>(){{
        put(Structure.ANCIENT_CITY, age.diamond);
        put(Structure.BASTION_REMNANT,age.nether);
        put(Structure.BURIED_TREASURE,age.stone);
        put(Structure.END_CITY,age.postend);
        put(Structure.FORTRESS,age.nether);
        put(Structure.DESERT_PYRAMID,age.stone);//needs modification
        put(Structure.IGLOO,age.stone);
        put(Structure.JUNGLE_PYRAMID,age.stone);//needs modification
        put(Structure.MANSION,age.pillager);
        put(Structure.MINESHAFT,age.wood);
        put(Structure.MINESHAFT_MESA, age.wood);
        put(Structure.MONUMENT,age.ocean);
        put(Structure.NETHER_FOSSIL, age.nether);
        put(Structure.OCEAN_RUIN_COLD, age.stone);
        put(Structure.OCEAN_RUIN_WARM, age.stone);
        put(Structure.PILLAGER_OUTPOST, age.stone);
        put(Structure.RUINED_PORTAL,age.stone);
        put(Structure.RUINED_PORTAL_DESERT,age.stone);
        put(Structure.RUINED_PORTAL_JUNGLE,age.stone);
        put(Structure.RUINED_PORTAL_MOUNTAIN,age.stone);
        put(Structure.RUINED_PORTAL_OCEAN,age.stone);
        put(Structure.RUINED_PORTAL_SWAMP,age.stone);
        put(Structure.RUINED_PORTAL_NETHER,age.nether);
        put(Structure.SHIPWRECK, age.wood);
        put(Structure.SHIPWRECK_BEACHED, age.wood);
        put(Structure.STRONGHOLD, age.end);
        put(Structure.SWAMP_HUT, age.stone);
        put(Structure.TRAIL_RUINS, age.stone);
        put(Structure.TRIAL_CHAMBERS, age.trial);
        put(Structure.VILLAGE_DESERT,age.wood);
        put(Structure.VILLAGE_PLAINS,age.wood);
        put(Structure.VILLAGE_SNOWY,age.wood);
        put(Structure.VILLAGE_TAIGA,age.wood);
        put(Structure.VILLAGE_SAVANNA,age.wood);
    }};


    Dictionary<age,Integer> ageLookup = new Hashtable<age, Integer>(){{
        put(age.wood,0);
        put(age.stone,1);
        put(age.iron,2);
        put(age.nether,3);
        put(age.redstone,4);
        put(age.diamond,5);
        put(age.trade,6);
        put(age.ocean,7);
        put(age.trial,8);
        put(age.pillager,9);
        put(age.end,10);
        put(age.postend,11);
        put(age.netherite,12);
    }};
    Dictionary<Material,age> allowBlockDrop = new Hashtable<Material,age>(){{
        put(Material.ACACIA_BOAT,age.wood);
        put(Material.ACACIA_BUTTON,age.wood);
        put(Material.ACACIA_CHEST_BOAT,age.wood);
        put(Material.ACACIA_DOOR,age.wood);
        put(Material.ACACIA_FENCE,age.wood);
        put(Material.ACACIA_FENCE_GATE,age.wood);
        put(Material.ACACIA_HANGING_SIGN,age.wood);
        put(Material.ACACIA_LEAVES,age.wood);
        put(Material.ACACIA_LOG,age.wood);
        put(Material.ACACIA_PLANKS,age.wood);
        put(Material.ACACIA_PRESSURE_PLATE,age.wood);
        put(Material.ACACIA_SAPLING,age.wood);
        put(Material.ACACIA_SIGN,age.wood);
        put(Material.ACACIA_SLAB,age.wood);
        put(Material.ACACIA_STAIRS,age.wood);
        put(Material.ACACIA_TRAPDOOR,age.wood);
        put(Material.ACACIA_WALL_HANGING_SIGN,age.wood);
        put(Material.ACACIA_WALL_SIGN,age.wood);
        put(Material.ACACIA_WOOD,age.wood);
        put(Material.ACTIVATOR_RAIL,age.iron);
        put(Material.AIR,age.wood);
        put(Material.ALLAY_SPAWN_EGG,age.wood);
        put(Material.ALLIUM,age.wood);
        put(Material.AMETHYST_BLOCK,age.wood);
        put(Material.AMETHYST_CLUSTER,age.wood);
        put(Material.AMETHYST_SHARD,age.wood);
        put(Material.ANCIENT_DEBRIS,age.netherite);
        put(Material.ANDESITE,age.stone);
        put(Material.ANDESITE_SLAB,age.stone);
        put(Material.ANDESITE_STAIRS,age.stone);
        put(Material.ANDESITE_WALL,age.stone);
        put(Material.ANGLER_POTTERY_SHERD,age.stone);
        put(Material.ANVIL,age.iron);
        put(Material.APPLE,age.wood);
        put(Material.ARCHER_POTTERY_SHERD,age.stone);
        put(Material.ARMADILLO_SCUTE,age.stone);
        put(Material.ARMADILLO_SPAWN_EGG,age.wood);
        put(Material.ARMOR_STAND,age.stone);
        put(Material.ARMS_UP_POTTERY_SHERD,age.stone);
        put(Material.ARROW,age.wood);
        put(Material.ATTACHED_MELON_STEM,age.wood);
        put(Material.ATTACHED_PUMPKIN_STEM,age.wood);
        put(Material.AXOLOTL_BUCKET,age.iron);
        put(Material.AXOLOTL_SPAWN_EGG,age.wood);
        put(Material.AZALEA,age.wood);
        put(Material.AZALEA_LEAVES,age.wood);
        put(Material.AZURE_BLUET,age.wood);
        put(Material.BAKED_POTATO,age.wood);
        put(Material.BAMBOO,age.wood);
        put(Material.BAMBOO_BLOCK,age.wood);
        put(Material.BAMBOO_BUTTON,age.wood);
        put(Material.BAMBOO_CHEST_RAFT,age.wood);
        put(Material.BAMBOO_DOOR,age.wood);
        put(Material.BAMBOO_FENCE,age.wood);
        put(Material.BAMBOO_FENCE_GATE,age.wood);
        put(Material.BAMBOO_HANGING_SIGN,age.wood);
        put(Material.BAMBOO_MOSAIC,age.wood);
        put(Material.BAMBOO_MOSAIC_SLAB,age.wood);
        put(Material.BAMBOO_MOSAIC_STAIRS,age.wood);
        put(Material.BAMBOO_PLANKS,age.wood);
        put(Material.BAMBOO_PRESSURE_PLATE,age.wood);
        put(Material.BAMBOO_RAFT,age.wood);
        put(Material.BAMBOO_SAPLING,age.wood);
        put(Material.BAMBOO_SIGN,age.wood);
        put(Material.BAMBOO_SLAB,age.wood);
        put(Material.BAMBOO_STAIRS,age.wood);
        put(Material.BAMBOO_TRAPDOOR,age.wood);
        put(Material.BAMBOO_WALL_HANGING_SIGN,age.wood);
        put(Material.BAMBOO_WALL_SIGN,age.wood);
        put(Material.BARREL,age.wood);
        put(Material.BARRIER,age.wood);
        put(Material.BASALT,age.stone);
        put(Material.BAT_SPAWN_EGG,age.wood);
        put(Material.BEACON,age.netherite);
        put(Material.BEDROCK,age.wood);
        put(Material.BEE_NEST,age.wood);
        put(Material.BEE_SPAWN_EGG,age.wood);
        put(Material.BEEF,age.wood);
        put(Material.BEEHIVE,age.wood);
        put(Material.BEETROOT,age.wood);
        put(Material.BEETROOT_SEEDS,age.wood);
        put(Material.BEETROOT_SOUP,age.wood);
        put(Material.BEETROOTS,age.wood);
        put(Material.BELL,age.wood);
        put(Material.BIG_DRIPLEAF,age.wood);
        put(Material.BIG_DRIPLEAF_STEM,age.wood);
        put(Material.BIRCH_BOAT,age.wood);
        put(Material.BIRCH_BUTTON,age.wood);
        put(Material.BIRCH_CHEST_BOAT,age.wood);
        put(Material.BIRCH_DOOR,age.wood);
        put(Material.BIRCH_FENCE,age.wood);
        put(Material.BIRCH_FENCE_GATE,age.wood);
        put(Material.BIRCH_HANGING_SIGN,age.wood);
        put(Material.BIRCH_LEAVES,age.wood);
        put(Material.BIRCH_LOG,age.wood);
        put(Material.BIRCH_PLANKS,age.wood);
        put(Material.BIRCH_PRESSURE_PLATE,age.wood);
        put(Material.BIRCH_SAPLING,age.wood);
        put(Material.BIRCH_SIGN,age.wood);
        put(Material.BIRCH_SLAB,age.wood);
        put(Material.BIRCH_STAIRS,age.wood);
        put(Material.BIRCH_TRAPDOOR,age.wood);
        put(Material.BIRCH_WALL_HANGING_SIGN,age.wood);
        put(Material.BIRCH_WALL_SIGN,age.wood);
        put(Material.BIRCH_WOOD,age.wood);
        put(Material.BLACK_BANNER,age.wood);
        put(Material.BLACK_BED,age.wood);
        put(Material.BLACK_BUNDLE,age.wood);
        put(Material.BLACK_CANDLE,age.wood);
        put(Material.BLACK_CANDLE_CAKE,age.wood);
        put(Material.BLACK_CARPET,age.wood);
        put(Material.BLACK_CONCRETE,age.wood);
        put(Material.BLACK_CONCRETE_POWDER,age.wood);
        put(Material.BLACK_DYE,age.wood);
        put(Material.BLACK_GLAZED_TERRACOTTA,age.stone);
        put(Material.BLACK_HARNESS,age.wood);
        put(Material.BLACK_SHULKER_BOX,age.postend);
        put(Material.BLACK_STAINED_GLASS,age.wood);
        put(Material.BLACK_STAINED_GLASS_PANE,age.wood);
        put(Material.BLACK_TERRACOTTA,age.stone);
        put(Material.BLACK_WALL_BANNER,age.wood);
        put(Material.BLACK_WOOL,age.wood);
        put(Material.BLACKSTONE,age.stone);
        put(Material.BLACKSTONE_SLAB,age.stone);
        put(Material.BLACKSTONE_STAIRS,age.stone);
        put(Material.BLACKSTONE_WALL,age.stone);
        put(Material.BLADE_POTTERY_SHERD,age.stone);
        put(Material.BLAST_FURNACE,age.iron);
        put(Material.BLAZE_POWDER,age.nether);
        put(Material.BLAZE_ROD,age.nether);
        put(Material.BLAZE_SPAWN_EGG,age.wood);
        put(Material.BLUE_BANNER,age.wood);
        put(Material.BLUE_BED,age.wood);
        put(Material.BLUE_BUNDLE,age.wood);
        put(Material.BLUE_CANDLE,age.wood);
        put(Material.BLUE_CANDLE_CAKE,age.wood);
        put(Material.BLUE_CARPET,age.wood);
        put(Material.BLUE_CONCRETE,age.wood);
        put(Material.BLUE_CONCRETE_POWDER,age.wood);
        put(Material.BLUE_DYE,age.wood);
        put(Material.BLUE_EGG,age.wood);
        put(Material.BLUE_GLAZED_TERRACOTTA,age.stone);
        put(Material.BLUE_HARNESS,age.wood);
        put(Material.BLUE_ICE,age.wood);
        put(Material.BLUE_ORCHID,age.wood);
        put(Material.BLUE_SHULKER_BOX,age.postend);
        put(Material.BLUE_STAINED_GLASS,age.wood);
        put(Material.BLUE_STAINED_GLASS_PANE,age.wood);
        put(Material.BLUE_TERRACOTTA,age.stone);
        put(Material.BLUE_WALL_BANNER,age.wood);
        put(Material.BLUE_WOOL,age.wood);
        put(Material.BOGGED_SPAWN_EGG,age.wood);
        put(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,age.trial);
        put(Material.BONE,age.wood);
        put(Material.BONE_BLOCK,age.wood);
        put(Material.BONE_MEAL,age.wood);
        put(Material.BOOK,age.wood);
        put(Material.BOOKSHELF,age.wood);
        put(Material.BORDURE_INDENTED_BANNER_PATTERN,age.wood);
        put(Material.BOW,age.wood);
        put(Material.BOWL,age.wood);
        put(Material.BRAIN_CORAL,age.wood);
        put(Material.BRAIN_CORAL_BLOCK,age.wood);
        put(Material.BRAIN_CORAL_FAN,age.wood);
        put(Material.BRAIN_CORAL_WALL_FAN,age.wood);
        put(Material.BREAD,age.wood);
        put(Material.BREEZE_ROD,age.trial);
        put(Material.BREEZE_SPAWN_EGG,age.wood);
        put(Material.BREWER_POTTERY_SHERD,age.stone);
        put(Material.BREWING_STAND,age.nether);
        put(Material.BRICK,age.stone);
        put(Material.BRICK_SLAB,age.stone);
        put(Material.BRICK_STAIRS,age.stone);
        put(Material.BRICK_WALL,age.stone);
        put(Material.BRICKS,age.stone);
        put(Material.BROWN_BANNER,age.wood);
        put(Material.BROWN_BED,age.wood);
        put(Material.BROWN_BUNDLE,age.wood);
        put(Material.BROWN_CANDLE,age.wood);
        put(Material.BROWN_CANDLE_CAKE,age.wood);
        put(Material.BROWN_CARPET,age.wood);
        put(Material.BROWN_CONCRETE,age.wood);
        put(Material.BROWN_CONCRETE_POWDER,age.wood);
        put(Material.BROWN_DYE,age.wood);
        put(Material.BROWN_EGG,age.wood);
        put(Material.BROWN_GLAZED_TERRACOTTA,age.stone);
        put(Material.BROWN_HARNESS,age.wood);
        put(Material.BROWN_MUSHROOM,age.wood);
        put(Material.BROWN_MUSHROOM_BLOCK,age.wood);
        put(Material.BROWN_SHULKER_BOX,age.postend);
        put(Material.BROWN_STAINED_GLASS,age.wood);
        put(Material.BROWN_STAINED_GLASS_PANE,age.wood);
        put(Material.BROWN_TERRACOTTA,age.stone);
        put(Material.BROWN_WALL_BANNER,age.wood);
        put(Material.BROWN_WOOL,age.wood);
        put(Material.BRUSH,age.stone);
        put(Material.BUBBLE_COLUMN,age.wood);
        put(Material.BUBBLE_CORAL,age.wood);
        put(Material.BUBBLE_CORAL_BLOCK,age.wood);
        put(Material.BUBBLE_CORAL_FAN,age.wood);
        put(Material.BUBBLE_CORAL_WALL_FAN,age.wood);
        put(Material.BUCKET,age.iron);
        put(Material.BUDDING_AMETHYST,age.wood);
        put(Material.BUNDLE,age.wood);
        put(Material.BURN_POTTERY_SHERD,age.stone);
        put(Material.BUSH,age.wood);
        put(Material.CACTUS,age.wood);
        put(Material.CACTUS_FLOWER,age.wood);
        put(Material.CAKE,age.wood);
        put(Material.CALCITE,age.stone);
        put(Material.CALIBRATED_SCULK_SENSOR,age.stone);
        put(Material.CAMEL_SPAWN_EGG,age.wood);
        put(Material.CAMPFIRE,age.wood);
        put(Material.CANDLE,age.wood);
        put(Material.CANDLE_CAKE,age.wood);
        put(Material.CARROT,age.wood);
        put(Material.CARROT_ON_A_STICK,age.wood);
        put(Material.CARROTS,age.wood);
        put(Material.CARTOGRAPHY_TABLE,age.wood);
        put(Material.CARVED_PUMPKIN,age.wood);
        put(Material.CAT_SPAWN_EGG,age.wood);
        put(Material.CAULDRON,age.iron);
        put(Material.CAVE_AIR,age.wood);
        put(Material.CAVE_SPIDER_SPAWN_EGG,age.wood);
        put(Material.CAVE_VINES,age.wood);
        put(Material.CAVE_VINES_PLANT,age.wood);
        put(Material.CHAIN,age.iron);
        put(Material.CHAIN_COMMAND_BLOCK,age.wood);
        put(Material.CHAINMAIL_BOOTS,age.stone);
        put(Material.CHAINMAIL_CHESTPLATE,age.stone);
        put(Material.CHAINMAIL_HELMET,age.stone);
        put(Material.CHAINMAIL_LEGGINGS,age.stone);
        put(Material.CHARCOAL,age.wood);
        put(Material.CHERRY_BOAT,age.wood);
        put(Material.CHERRY_BUTTON,age.wood);
        put(Material.CHERRY_CHEST_BOAT,age.wood);
        put(Material.CHERRY_DOOR,age.wood);
        put(Material.CHERRY_FENCE,age.wood);
        put(Material.CHERRY_FENCE_GATE,age.wood);
        put(Material.CHERRY_HANGING_SIGN,age.wood);
        put(Material.CHERRY_LEAVES,age.wood);
        put(Material.CHERRY_LOG,age.wood);
        put(Material.CHERRY_PLANKS,age.wood);
        put(Material.CHERRY_PRESSURE_PLATE,age.wood);
        put(Material.CHERRY_SAPLING,age.wood);
        put(Material.CHERRY_SIGN,age.wood);
        put(Material.CHERRY_SLAB,age.wood);
        put(Material.CHERRY_STAIRS,age.wood);
        put(Material.CHERRY_TRAPDOOR,age.wood);
        put(Material.CHERRY_WALL_HANGING_SIGN,age.wood);
        put(Material.CHERRY_WALL_SIGN,age.wood);
        put(Material.CHERRY_WOOD,age.wood);
        put(Material.CHEST,age.wood);
        put(Material.CHEST_MINECART,age.iron);
        put(Material.CHICKEN,age.wood);
        put(Material.CHICKEN_SPAWN_EGG,age.wood);
        put(Material.CHIPPED_ANVIL,age.iron);
        put(Material.CHISELED_BOOKSHELF,age.wood);
        put(Material.CHISELED_COPPER,age.stone);
        put(Material.CHISELED_DEEPSLATE,age.stone);
        put(Material.CHISELED_NETHER_BRICKS,age.stone);
        put(Material.CHISELED_POLISHED_BLACKSTONE,age.stone);
        put(Material.CHISELED_QUARTZ_BLOCK,age.stone);
        put(Material.CHISELED_RED_SANDSTONE,age.stone);
        put(Material.CHISELED_RESIN_BRICKS,age.wood);
        put(Material.CHISELED_SANDSTONE,age.stone);
        put(Material.CHISELED_STONE_BRICKS,age.stone);
        put(Material.CHISELED_TUFF,age.stone);
        put(Material.CHISELED_TUFF_BRICKS,age.stone);
        put(Material.CHORUS_FLOWER,age.end);
        put(Material.CHORUS_FRUIT,age.end);
        put(Material.CHORUS_PLANT,age.end);
        put(Material.CLAY,age.wood);
        put(Material.CLAY_BALL,age.wood);
        put(Material.CLOCK,age.stone);
        put(Material.CLOSED_EYEBLOSSOM,age.wood);
        put(Material.COAL,age.wood);
        put(Material.COAL_BLOCK,age.wood);
        put(Material.COAL_ORE,age.wood);
        put(Material.COARSE_DIRT,age.wood);
        put(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.COBBLED_DEEPSLATE,age.stone);
        put(Material.COBBLED_DEEPSLATE_SLAB,age.stone);
        put(Material.COBBLED_DEEPSLATE_STAIRS,age.stone);
        put(Material.COBBLED_DEEPSLATE_WALL,age.stone);
        put(Material.COBBLESTONE,age.stone);
        put(Material.COBBLESTONE_SLAB,age.stone);
        put(Material.COBBLESTONE_STAIRS,age.stone);
        put(Material.COBBLESTONE_WALL,age.stone);
        put(Material.COBWEB,age.wood);
        put(Material.COCOA,age.wood);
        put(Material.COCOA_BEANS,age.wood);
        put(Material.COD,age.wood);
        put(Material.COD_BUCKET,age.iron);
        put(Material.COD_SPAWN_EGG,age.wood);
        put(Material.COMMAND_BLOCK,age.wood);
        put(Material.COMMAND_BLOCK_MINECART,age.wood);
        put(Material.COMPARATOR,age.iron);
        put(Material.COMPASS,age.iron);
        put(Material.COMPOSTER,age.wood);
        put(Material.CONDUIT,age.ocean);
        put(Material.COOKED_BEEF,age.wood);
        put(Material.COOKED_CHICKEN,age.wood);
        put(Material.COOKED_COD,age.wood);
        put(Material.COOKED_MUTTON,age.wood);
        put(Material.COOKED_PORKCHOP,age.wood);
        put(Material.COOKED_RABBIT,age.wood);
        put(Material.COOKED_SALMON,age.wood);
        put(Material.COOKIE,age.wood);
        put(Material.COPPER_BLOCK,age.stone);
        put(Material.COPPER_BULB,age.redstone);
        put(Material.COPPER_DOOR,age.stone);
        put(Material.COPPER_GRATE,age.stone);
        put(Material.COPPER_INGOT,age.stone);
        put(Material.COPPER_ORE,age.stone);
        put(Material.COPPER_TRAPDOOR,age.stone);
        put(Material.CORNFLOWER,age.wood);
        put(Material.COW_SPAWN_EGG,age.wood);
        put(Material.CRACKED_DEEPSLATE_BRICKS,age.stone);
        put(Material.CRACKED_DEEPSLATE_TILES,age.stone);
        put(Material.CRACKED_NETHER_BRICKS,age.stone);
        put(Material.CRACKED_POLISHED_BLACKSTONE_BRICKS,age.stone);
        put(Material.CRACKED_STONE_BRICKS,age.stone);
        put(Material.CRAFTER,age.redstone);
        put(Material.CRAFTING_TABLE,age.wood);
        put(Material.CREAKING_HEART,age.wood);
        put(Material.CREAKING_SPAWN_EGG,age.wood);
        put(Material.CREEPER_BANNER_PATTERN,age.stone);
        put(Material.CREEPER_HEAD,age.stone);
        put(Material.CREEPER_SPAWN_EGG,age.wood);
        put(Material.CREEPER_WALL_HEAD,age.stone);
        put(Material.CRIMSON_BUTTON,age.wood);
        put(Material.CRIMSON_DOOR,age.wood);
        put(Material.CRIMSON_FENCE,age.wood);
        put(Material.CRIMSON_FENCE_GATE,age.wood);
        put(Material.CRIMSON_FUNGUS,age.wood);
        put(Material.CRIMSON_HANGING_SIGN,age.wood);
        put(Material.CRIMSON_HYPHAE,age.wood);
        put(Material.CRIMSON_NYLIUM,age.wood);
        put(Material.CRIMSON_PLANKS,age.wood);
        put(Material.CRIMSON_PRESSURE_PLATE,age.wood);
        put(Material.CRIMSON_ROOTS,age.wood);
        put(Material.CRIMSON_SIGN,age.wood);
        put(Material.CRIMSON_SLAB,age.wood);
        put(Material.CRIMSON_STAIRS,age.wood);
        put(Material.CRIMSON_STEM,age.wood);
        put(Material.CRIMSON_TRAPDOOR,age.wood);
        put(Material.CRIMSON_WALL_HANGING_SIGN,age.wood);
        put(Material.CRIMSON_WALL_SIGN,age.wood);
        put(Material.CROSSBOW,age.iron);
        put(Material.CRYING_OBSIDIAN,age.stone);
        put(Material.CUT_COPPER,age.stone);
        put(Material.CUT_COPPER_SLAB,age.stone);
        put(Material.CUT_COPPER_STAIRS,age.stone);
        put(Material.CUT_RED_SANDSTONE,age.stone);
        put(Material.CUT_RED_SANDSTONE_SLAB,age.stone);
        put(Material.CUT_SANDSTONE,age.stone);
        put(Material.CUT_SANDSTONE_SLAB,age.stone);
        put(Material.CYAN_BANNER,age.wood);
        put(Material.CYAN_BED,age.wood);
        put(Material.CYAN_BUNDLE,age.wood);
        put(Material.CYAN_CANDLE,age.wood);
        put(Material.CYAN_CANDLE_CAKE,age.wood);
        put(Material.CYAN_CARPET,age.wood);
        put(Material.CYAN_CONCRETE,age.wood);
        put(Material.CYAN_CONCRETE_POWDER,age.wood);
        put(Material.CYAN_DYE,age.wood);
        put(Material.CYAN_GLAZED_TERRACOTTA,age.stone);
        put(Material.CYAN_HARNESS,age.wood);
        put(Material.CYAN_SHULKER_BOX,age.postend);
        put(Material.CYAN_STAINED_GLASS,age.wood);
        put(Material.CYAN_STAINED_GLASS_PANE,age.wood);
        put(Material.CYAN_TERRACOTTA,age.stone);
        put(Material.CYAN_WALL_BANNER,age.wood);
        put(Material.CYAN_WOOL,age.wood);
        put(Material.DAMAGED_ANVIL,age.iron);
        put(Material.DANDELION,age.wood);
        put(Material.DANGER_POTTERY_SHERD,age.stone);
        put(Material.DARK_OAK_BOAT,age.wood);
        put(Material.DARK_OAK_BUTTON,age.wood);
        put(Material.DARK_OAK_CHEST_BOAT,age.wood);
        put(Material.DARK_OAK_DOOR,age.wood);
        put(Material.DARK_OAK_FENCE,age.wood);
        put(Material.DARK_OAK_FENCE_GATE,age.wood);
        put(Material.DARK_OAK_HANGING_SIGN,age.wood);
        put(Material.DARK_OAK_LEAVES,age.wood);
        put(Material.DARK_OAK_LOG,age.wood);
        put(Material.DARK_OAK_PLANKS,age.wood);
        put(Material.DARK_OAK_PRESSURE_PLATE,age.wood);
        put(Material.DARK_OAK_SAPLING,age.wood);
        put(Material.DARK_OAK_SIGN,age.wood);
        put(Material.DARK_OAK_SLAB,age.wood);
        put(Material.DARK_OAK_STAIRS,age.wood);
        put(Material.DARK_OAK_TRAPDOOR,age.wood);
        put(Material.DARK_OAK_WALL_HANGING_SIGN,age.wood);
        put(Material.DARK_OAK_WALL_SIGN,age.wood);
        put(Material.DARK_OAK_WOOD,age.wood);
        put(Material.DARK_PRISMARINE,age.ocean);
        put(Material.DARK_PRISMARINE_SLAB,age.ocean);
        put(Material.DARK_PRISMARINE_STAIRS,age.ocean);
        put(Material.DAYLIGHT_DETECTOR,age.nether);
        put(Material.DEAD_BRAIN_CORAL,age.wood);
        put(Material.DEAD_BRAIN_CORAL_BLOCK,age.wood);
        put(Material.DEAD_BRAIN_CORAL_FAN,age.wood);
        put(Material.DEAD_BRAIN_CORAL_WALL_FAN,age.wood);
        put(Material.DEAD_BUBBLE_CORAL,age.wood);
        put(Material.DEAD_BUBBLE_CORAL_BLOCK,age.wood);
        put(Material.DEAD_BUBBLE_CORAL_FAN,age.wood);
        put(Material.DEAD_BUBBLE_CORAL_WALL_FAN,age.wood);
        put(Material.DEAD_BUSH,age.wood);
        put(Material.DEAD_FIRE_CORAL,age.wood);
        put(Material.DEAD_FIRE_CORAL_BLOCK,age.wood);
        put(Material.DEAD_FIRE_CORAL_FAN,age.wood);
        put(Material.DEAD_FIRE_CORAL_WALL_FAN,age.wood);
        put(Material.DEAD_HORN_CORAL,age.wood);
        put(Material.DEAD_HORN_CORAL_BLOCK,age.wood);
        put(Material.DEAD_HORN_CORAL_FAN,age.wood);
        put(Material.DEAD_HORN_CORAL_WALL_FAN,age.wood);
        put(Material.DEAD_TUBE_CORAL,age.wood);
        put(Material.DEAD_TUBE_CORAL_BLOCK,age.wood);
        put(Material.DEAD_TUBE_CORAL_FAN,age.wood);
        put(Material.DEAD_TUBE_CORAL_WALL_FAN,age.wood);
        put(Material.DEBUG_STICK,age.wood);
        put(Material.DECORATED_POT,age.stone);
        put(Material.DEEPSLATE,age.stone);
        put(Material.DEEPSLATE_BRICK_SLAB,age.stone);
        put(Material.DEEPSLATE_BRICK_STAIRS,age.stone);
        put(Material.DEEPSLATE_BRICK_WALL,age.stone);
        put(Material.DEEPSLATE_BRICKS,age.stone);
        put(Material.DEEPSLATE_COAL_ORE,age.stone);
        put(Material.DEEPSLATE_COPPER_ORE,age.stone);
        put(Material.DEEPSLATE_DIAMOND_ORE,age.diamond);
        put(Material.DEEPSLATE_EMERALD_ORE,age.stone);
        put(Material.DEEPSLATE_GOLD_ORE,age.stone);
        put(Material.DEEPSLATE_IRON_ORE,age.iron);
        put(Material.DEEPSLATE_LAPIS_ORE,age.stone);
        put(Material.DEEPSLATE_REDSTONE_ORE,age.redstone);
        put(Material.DEEPSLATE_TILE_SLAB,age.stone);
        put(Material.DEEPSLATE_TILE_STAIRS,age.stone);
        put(Material.DEEPSLATE_TILE_WALL,age.stone);
        put(Material.DEEPSLATE_TILES,age.stone);
        put(Material.DETECTOR_RAIL,age.iron);
        put(Material.DIAMOND,age.diamond);
        put(Material.DIAMOND_AXE,age.diamond);
        put(Material.DIAMOND_BLOCK,age.diamond);
        put(Material.DIAMOND_BOOTS,age.diamond);
        put(Material.DIAMOND_CHESTPLATE,age.diamond);
        put(Material.DIAMOND_HELMET,age.diamond);
        put(Material.DIAMOND_HOE,age.diamond);
        put(Material.DIAMOND_HORSE_ARMOR,age.stone);
        put(Material.DIAMOND_LEGGINGS,age.diamond);
        put(Material.DIAMOND_ORE,age.diamond);
        put(Material.DIAMOND_PICKAXE,age.diamond);
        put(Material.DIAMOND_SHOVEL,age.diamond);
        put(Material.DIAMOND_SWORD,age.diamond);
        put(Material.DIORITE,age.stone);
        put(Material.DIORITE_SLAB,age.stone);
        put(Material.DIORITE_STAIRS,age.stone);
        put(Material.DIORITE_WALL,age.stone);
        put(Material.DIRT,age.wood);
        put(Material.DIRT_PATH,age.wood);
        put(Material.DISC_FRAGMENT_5,age.diamond);
        put(Material.DISPENSER,age.iron);
        put(Material.DOLPHIN_SPAWN_EGG,age.wood);
        put(Material.DONKEY_SPAWN_EGG,age.wood);
        put(Material.DRAGON_BREATH,age.end);
        put(Material.DRAGON_EGG,age.end);
        put(Material.DRAGON_HEAD,age.postend);
        put(Material.DRAGON_WALL_HEAD,age.postend);
        put(Material.DRIED_GHAST,age.nether);
        put(Material.DRIED_KELP,age.wood);
        put(Material.DRIED_KELP_BLOCK,age.wood);
        put(Material.DRIPSTONE_BLOCK,age.stone);
        put(Material.DROPPER,age.iron);
        put(Material.DROWNED_SPAWN_EGG,age.wood);
        put(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.ECHO_SHARD,age.diamond);
        put(Material.EGG,age.wood);
        put(Material.ELDER_GUARDIAN_SPAWN_EGG,age.wood);
        put(Material.ELYTRA,age.postend);
        put(Material.EMERALD,age.stone);
        put(Material.EMERALD_BLOCK,age.stone);
        put(Material.EMERALD_ORE,age.stone);
        put(Material.ENCHANTED_BOOK,age.wood);
        put(Material.ENCHANTED_GOLDEN_APPLE,age.wood);
        put(Material.ENCHANTING_TABLE,age.diamond);
        put(Material.END_CRYSTAL,age.end);
        put(Material.END_GATEWAY,age.end);
        put(Material.END_PORTAL,age.wood);
        put(Material.END_PORTAL_FRAME,age.wood);
        put(Material.END_ROD,age.end);
        put(Material.END_STONE,age.end);
        put(Material.END_STONE_BRICK_SLAB,age.end);
        put(Material.END_STONE_BRICK_STAIRS,age.end);
        put(Material.END_STONE_BRICK_WALL,age.end);
        put(Material.END_STONE_BRICKS,age.end);
        put(Material.ENDER_CHEST,age.nether);
        put(Material.ENDER_DRAGON_SPAWN_EGG,age.wood);
        put(Material.ENDER_EYE,age.end);
        put(Material.ENDER_PEARL,age.wood);
        put(Material.ENDERMAN_SPAWN_EGG,age.wood);
        put(Material.ENDERMITE_SPAWN_EGG,age.wood);
        put(Material.EVOKER_SPAWN_EGG,age.wood);
        put(Material.EXPERIENCE_BOTTLE,age.wood);
        put(Material.EXPLORER_POTTERY_SHERD,age.stone);
        put(Material.EXPOSED_CHISELED_COPPER,age.stone);
        put(Material.EXPOSED_COPPER,age.stone);
        put(Material.EXPOSED_COPPER_BULB,age.redstone);
        put(Material.EXPOSED_COPPER_DOOR,age.stone);
        put(Material.EXPOSED_COPPER_GRATE,age.stone);
        put(Material.EXPOSED_COPPER_TRAPDOOR,age.stone);
        put(Material.EXPOSED_CUT_COPPER,age.stone);
        put(Material.EXPOSED_CUT_COPPER_SLAB,age.stone);
        put(Material.EXPOSED_CUT_COPPER_STAIRS,age.stone);
        put(Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,age.end);
        put(Material.FARMLAND,age.wood);
        put(Material.FEATHER,age.wood);
        put(Material.FERMENTED_SPIDER_EYE,age.wood);
        put(Material.FERN,age.wood);
        put(Material.FIELD_MASONED_BANNER_PATTERN,age.stone);
        put(Material.FILLED_MAP,age.stone);
        put(Material.FIRE,age.wood);
        put(Material.FIRE_CHARGE,age.wood);
        put(Material.FIRE_CORAL,age.wood);
        put(Material.FIRE_CORAL_BLOCK,age.wood);
        put(Material.FIRE_CORAL_FAN,age.wood);
        put(Material.FIRE_CORAL_WALL_FAN,age.wood);
        put(Material.FIREFLY_BUSH,age.wood);
        put(Material.FIREWORK_ROCKET,age.wood);
        put(Material.FIREWORK_STAR,age.wood);
        put(Material.FISHING_ROD,age.wood);
        put(Material.FLETCHING_TABLE,age.wood);
        put(Material.FLINT,age.wood);
        put(Material.FLINT_AND_STEEL,age.iron);
        put(Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,age.trial);
        put(Material.FLOW_BANNER_PATTERN,age.trial);
        put(Material.FLOW_POTTERY_SHERD,age.trial);
        put(Material.FLOWER_BANNER_PATTERN,age.wood);
        put(Material.FLOWER_POT,age.wood);
        put(Material.FLOWERING_AZALEA,age.wood);
        put(Material.FLOWERING_AZALEA_LEAVES,age.wood);
        put(Material.FOX_SPAWN_EGG,age.wood);
        put(Material.FRIEND_POTTERY_SHERD,age.stone);
        put(Material.FROG_SPAWN_EGG,age.wood);
        put(Material.FROGSPAWN,age.wood);
        put(Material.FROSTED_ICE,age.wood);
        put(Material.FURNACE,age.stone);
        put(Material.FURNACE_MINECART,age.iron);
        put(Material.GHAST_SPAWN_EGG,age.wood);
        put(Material.GHAST_TEAR,age.nether);
        put(Material.GILDED_BLACKSTONE,age.nether);
        put(Material.GLASS,age.wood);
        put(Material.GLASS_BOTTLE,age.wood);
        put(Material.GLASS_PANE,age.wood);
        put(Material.GLISTERING_MELON_SLICE,age.nether);
        put(Material.GLOBE_BANNER_PATTERN,age.wood);
        put(Material.GLOW_BERRIES,age.wood);
        put(Material.GLOW_INK_SAC,age.wood);
        put(Material.GLOW_ITEM_FRAME,age.wood);
        put(Material.GLOW_LICHEN,age.wood);
        put(Material.GLOW_SQUID_SPAWN_EGG,age.wood);
        put(Material.GLOWSTONE,age.nether);
        put(Material.GLOWSTONE_DUST,age.nether);
        put(Material.GOAT_HORN,age.wood);
        put(Material.GOAT_SPAWN_EGG,age.wood);
        put(Material.GOLD_BLOCK,age.stone);
        put(Material.GOLD_INGOT,age.stone);
        put(Material.GOLD_NUGGET,age.stone);
        put(Material.GOLD_ORE,age.stone);
        put(Material.GOLDEN_APPLE,age.wood);
        put(Material.GOLDEN_AXE,age.stone);
        put(Material.GOLDEN_BOOTS,age.stone);
        put(Material.GOLDEN_CARROT,age.wood);
        put(Material.GOLDEN_CHESTPLATE,age.stone);
        put(Material.GOLDEN_HELMET,age.stone);
        put(Material.GOLDEN_HOE,age.stone);
        put(Material.GOLDEN_HORSE_ARMOR,age.stone);
        put(Material.GOLDEN_LEGGINGS,age.stone);
        put(Material.GOLDEN_PICKAXE,age.stone);
        put(Material.GOLDEN_SHOVEL,age.stone);
        put(Material.GOLDEN_SWORD,age.stone);
        put(Material.GRANITE,age.stone);
        put(Material.GRANITE_SLAB,age.stone);
        put(Material.GRANITE_STAIRS,age.stone);
        put(Material.GRANITE_WALL,age.stone);
        put(Material.GRASS_BLOCK,age.wood);
        put(Material.GRAVEL,age.wood);
        put(Material.GRAY_BANNER,age.wood);
        put(Material.GRAY_BED,age.wood);
        put(Material.GRAY_BUNDLE,age.wood);
        put(Material.GRAY_CANDLE,age.wood);
        put(Material.GRAY_CANDLE_CAKE,age.wood);
        put(Material.GRAY_CARPET,age.wood);
        put(Material.GRAY_CONCRETE,age.wood);
        put(Material.GRAY_CONCRETE_POWDER,age.wood);
        put(Material.GRAY_DYE,age.wood);
        put(Material.GRAY_GLAZED_TERRACOTTA,age.stone);
        put(Material.GRAY_HARNESS,age.wood);
        put(Material.GRAY_SHULKER_BOX,age.postend);
        put(Material.GRAY_STAINED_GLASS,age.wood);
        put(Material.GRAY_STAINED_GLASS_PANE,age.wood);
        put(Material.GRAY_TERRACOTTA,age.stone);
        put(Material.GRAY_WALL_BANNER,age.wood);
        put(Material.GRAY_WOOL,age.wood);
        put(Material.GREEN_BANNER,age.wood);
        put(Material.GREEN_BED,age.wood);
        put(Material.GREEN_BUNDLE,age.wood);
        put(Material.GREEN_CANDLE,age.wood);
        put(Material.GREEN_CANDLE_CAKE,age.wood);
        put(Material.GREEN_CARPET,age.wood);
        put(Material.GREEN_CONCRETE,age.wood);
        put(Material.GREEN_CONCRETE_POWDER,age.wood);
        put(Material.GREEN_DYE,age.wood);
        put(Material.GREEN_GLAZED_TERRACOTTA,age.stone);
        put(Material.GREEN_HARNESS,age.wood);
        put(Material.GREEN_SHULKER_BOX,age.postend);
        put(Material.GREEN_STAINED_GLASS,age.wood);
        put(Material.GREEN_STAINED_GLASS_PANE,age.wood);
        put(Material.GREEN_TERRACOTTA,age.stone);
        put(Material.GREEN_WALL_BANNER,age.wood);
        put(Material.GREEN_WOOL,age.wood);
        put(Material.GRINDSTONE,age.stone);
        put(Material.GUARDIAN_SPAWN_EGG,age.wood);
        put(Material.GUNPOWDER,age.wood);
        put(Material.GUSTER_BANNER_PATTERN,age.trial);
        put(Material.GUSTER_POTTERY_SHERD,age.trial);
        put(Material.HANGING_ROOTS,age.wood);
        put(Material.HAPPY_GHAST_SPAWN_EGG,age.wood);
        put(Material.HAY_BLOCK,age.wood);
        put(Material.HEART_OF_THE_SEA,age.ocean);
        put(Material.HEART_POTTERY_SHERD,age.wood);
        put(Material.HEARTBREAK_POTTERY_SHERD,age.wood);
        put(Material.HEAVY_CORE,age.trial);
        put(Material.HEAVY_WEIGHTED_PRESSURE_PLATE,age.iron);
        put(Material.HOGLIN_SPAWN_EGG,age.wood);
        put(Material.HONEY_BLOCK,age.wood);
        put(Material.HONEY_BOTTLE,age.wood);
        put(Material.HONEYCOMB,age.wood);
        put(Material.HONEYCOMB_BLOCK,age.wood);
        put(Material.HOPPER,age.iron);
        put(Material.HOPPER_MINECART,age.iron);
        put(Material.HORN_CORAL,age.wood);
        put(Material.HORN_CORAL_BLOCK,age.wood);
        put(Material.HORN_CORAL_FAN,age.wood);
        put(Material.HORN_CORAL_WALL_FAN,age.wood);
        put(Material.HORSE_SPAWN_EGG,age.wood);
        put(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.HOWL_POTTERY_SHERD,age.wood);
        put(Material.HUSK_SPAWN_EGG,age.wood);
        put(Material.ICE,age.wood);
        put(Material.INFESTED_CHISELED_STONE_BRICKS,age.stone);
        put(Material.INFESTED_COBBLESTONE,age.stone);
        put(Material.INFESTED_CRACKED_STONE_BRICKS,age.stone);
        put(Material.INFESTED_DEEPSLATE,age.stone);
        put(Material.INFESTED_MOSSY_STONE_BRICKS,age.stone);
        put(Material.INFESTED_STONE,age.stone);
        put(Material.INFESTED_STONE_BRICKS,age.stone);
        put(Material.INK_SAC,age.wood);
        put(Material.IRON_AXE,age.iron);
        put(Material.IRON_BARS,age.iron);
        put(Material.IRON_BLOCK,age.iron);
        put(Material.IRON_BOOTS,age.iron);
        put(Material.IRON_CHESTPLATE,age.iron);
        put(Material.IRON_DOOR,age.iron);
        put(Material.IRON_GOLEM_SPAWN_EGG,age.iron);
        put(Material.IRON_HELMET,age.iron);
        put(Material.IRON_HOE,age.iron);
        put(Material.IRON_HORSE_ARMOR,age.stone);
        put(Material.IRON_INGOT,age.iron);
        put(Material.IRON_LEGGINGS,age.iron);
        put(Material.IRON_NUGGET,age.iron);
        put(Material.IRON_ORE,age.iron);
        put(Material.IRON_PICKAXE,age.iron);
        put(Material.IRON_SHOVEL,age.iron);
        put(Material.IRON_SWORD,age.iron);
        put(Material.IRON_TRAPDOOR,age.iron);
        put(Material.ITEM_FRAME,age.wood);
        put(Material.JACK_O_LANTERN,age.wood);
        put(Material.JIGSAW,age.wood);
        put(Material.JUKEBOX,age.diamond);
        put(Material.JUNGLE_BOAT,age.wood);
        put(Material.JUNGLE_BUTTON,age.wood);
        put(Material.JUNGLE_CHEST_BOAT,age.wood);
        put(Material.JUNGLE_DOOR,age.wood);
        put(Material.JUNGLE_FENCE,age.wood);
        put(Material.JUNGLE_FENCE_GATE,age.wood);
        put(Material.JUNGLE_HANGING_SIGN,age.wood);
        put(Material.JUNGLE_LEAVES,age.wood);
        put(Material.JUNGLE_LOG,age.wood);
        put(Material.JUNGLE_PLANKS,age.wood);
        put(Material.JUNGLE_PRESSURE_PLATE,age.wood);
        put(Material.JUNGLE_SAPLING,age.wood);
        put(Material.JUNGLE_SIGN,age.wood);
        put(Material.JUNGLE_SLAB,age.wood);
        put(Material.JUNGLE_STAIRS,age.wood);
        put(Material.JUNGLE_TRAPDOOR,age.wood);
        put(Material.JUNGLE_WALL_HANGING_SIGN,age.wood);
        put(Material.JUNGLE_WALL_SIGN,age.wood);
        put(Material.JUNGLE_WOOD,age.wood);
        put(Material.KELP,age.wood);
        put(Material.KELP_PLANT,age.wood);
        put(Material.KNOWLEDGE_BOOK,age.wood);
        put(Material.LADDER,age.wood);
        put(Material.LANTERN,age.iron);
        put(Material.LAPIS_BLOCK,age.stone);
        put(Material.LAPIS_LAZULI,age.stone);
        put(Material.LAPIS_ORE,age.stone);
        put(Material.LARGE_AMETHYST_BUD,age.stone);
        put(Material.LARGE_FERN,age.wood);
        put(Material.LAVA,age.wood);
        put(Material.LAVA_BUCKET,age.iron);
        put(Material.LAVA_CAULDRON,age.iron);
        put(Material.LEAD,age.wood);
        put(Material.LEAF_LITTER,age.wood);
        put(Material.LEATHER,age.wood);
        put(Material.LEATHER_BOOTS,age.wood);
        put(Material.LEATHER_CHESTPLATE,age.wood);
        put(Material.LEATHER_HELMET,age.wood);
        put(Material.LEATHER_HORSE_ARMOR,age.stone);
        put(Material.LEATHER_LEGGINGS,age.wood);
        put(Material.LECTERN,age.wood);
        put(Material.LEVER,age.stone);
        put(Material.LIGHT,age.wood);
        put(Material.LIGHT_BLUE_BANNER,age.wood);
        put(Material.LIGHT_BLUE_BED,age.wood);
        put(Material.LIGHT_BLUE_BUNDLE,age.wood);
        put(Material.LIGHT_BLUE_CANDLE,age.wood);
        put(Material.LIGHT_BLUE_CANDLE_CAKE,age.wood);
        put(Material.LIGHT_BLUE_CARPET,age.wood);
        put(Material.LIGHT_BLUE_CONCRETE,age.wood);
        put(Material.LIGHT_BLUE_CONCRETE_POWDER,age.wood);
        put(Material.LIGHT_BLUE_DYE,age.wood);
        put(Material.LIGHT_BLUE_GLAZED_TERRACOTTA,age.stone);
        put(Material.LIGHT_BLUE_HARNESS,age.wood);
        put(Material.LIGHT_BLUE_SHULKER_BOX,age.postend);
        put(Material.LIGHT_BLUE_STAINED_GLASS,age.wood);
        put(Material.LIGHT_BLUE_STAINED_GLASS_PANE,age.wood);
        put(Material.LIGHT_BLUE_TERRACOTTA,age.stone);
        put(Material.LIGHT_BLUE_WALL_BANNER,age.wood);
        put(Material.LIGHT_BLUE_WOOL,age.wood);
        put(Material.LIGHT_GRAY_BANNER,age.wood);
        put(Material.LIGHT_GRAY_BED,age.wood);
        put(Material.LIGHT_GRAY_BUNDLE,age.wood);
        put(Material.LIGHT_GRAY_CANDLE,age.wood);
        put(Material.LIGHT_GRAY_CANDLE_CAKE,age.wood);
        put(Material.LIGHT_GRAY_CARPET,age.wood);
        put(Material.LIGHT_GRAY_CONCRETE,age.wood);
        put(Material.LIGHT_GRAY_CONCRETE_POWDER,age.wood);
        put(Material.LIGHT_GRAY_DYE,age.wood);
        put(Material.LIGHT_GRAY_GLAZED_TERRACOTTA,age.stone);
        put(Material.LIGHT_GRAY_HARNESS,age.wood);
        put(Material.LIGHT_GRAY_SHULKER_BOX,age.postend);
        put(Material.LIGHT_GRAY_STAINED_GLASS,age.wood);
        put(Material.LIGHT_GRAY_STAINED_GLASS_PANE,age.wood);
        put(Material.LIGHT_GRAY_TERRACOTTA,age.stone);
        put(Material.LIGHT_GRAY_WALL_BANNER,age.wood);
        put(Material.LIGHT_GRAY_WOOL,age.wood);
        put(Material.LIGHT_WEIGHTED_PRESSURE_PLATE,age.stone);
        put(Material.LIGHTNING_ROD,age.stone);
        put(Material.LILAC,age.wood);
        put(Material.LILY_OF_THE_VALLEY,age.wood);
        put(Material.LILY_PAD,age.wood);
        put(Material.LIME_BANNER,age.wood);
        put(Material.LIME_BED,age.wood);
        put(Material.LIME_BUNDLE,age.wood);
        put(Material.LIME_CANDLE,age.wood);
        put(Material.LIME_CANDLE_CAKE,age.wood);
        put(Material.LIME_CARPET,age.wood);
        put(Material.LIME_CONCRETE,age.wood);
        put(Material.LIME_CONCRETE_POWDER,age.wood);
        put(Material.LIME_DYE,age.wood);
        put(Material.LIME_GLAZED_TERRACOTTA,age.stone);
        put(Material.LIME_HARNESS,age.wood);
        put(Material.LIME_SHULKER_BOX,age.postend);
        put(Material.LIME_STAINED_GLASS,age.wood);
        put(Material.LIME_STAINED_GLASS_PANE,age.wood);
        put(Material.LIME_TERRACOTTA,age.stone);
        put(Material.LIME_WALL_BANNER,age.wood);
        put(Material.LIME_WOOL,age.wood);
        put(Material.LINGERING_POTION,age.end);
        put(Material.LLAMA_SPAWN_EGG,age.wood);
        put(Material.LODESTONE,age.iron);
        put(Material.LOOM,age.wood);
        put(Material.MACE,age.trial);
        put(Material.MAGENTA_BANNER,age.wood);
        put(Material.MAGENTA_BED,age.wood);
        put(Material.MAGENTA_BUNDLE,age.wood);
        put(Material.MAGENTA_CANDLE,age.wood);
        put(Material.MAGENTA_CANDLE_CAKE,age.wood);
        put(Material.MAGENTA_CARPET,age.wood);
        put(Material.MAGENTA_CONCRETE,age.wood);
        put(Material.MAGENTA_CONCRETE_POWDER,age.wood);
        put(Material.MAGENTA_DYE,age.wood);
        put(Material.MAGENTA_GLAZED_TERRACOTTA,age.stone);
        put(Material.MAGENTA_HARNESS,age.wood);
        put(Material.MAGENTA_SHULKER_BOX,age.postend);
        put(Material.MAGENTA_STAINED_GLASS,age.wood);
        put(Material.MAGENTA_STAINED_GLASS_PANE,age.wood);
        put(Material.MAGENTA_TERRACOTTA,age.stone);
        put(Material.MAGENTA_WALL_BANNER,age.wood);
        put(Material.MAGENTA_WOOL,age.wood);
        put(Material.MAGMA_BLOCK,age.stone);
        put(Material.MAGMA_CREAM,age.nether);
        put(Material.MAGMA_CUBE_SPAWN_EGG,age.wood);
        put(Material.MANGROVE_BOAT,age.wood);
        put(Material.MANGROVE_BUTTON,age.wood);
        put(Material.MANGROVE_CHEST_BOAT,age.wood);
        put(Material.MANGROVE_DOOR,age.wood);
        put(Material.MANGROVE_FENCE,age.wood);
        put(Material.MANGROVE_FENCE_GATE,age.wood);
        put(Material.MANGROVE_HANGING_SIGN,age.wood);
        put(Material.MANGROVE_LEAVES,age.wood);
        put(Material.MANGROVE_LOG,age.wood);
        put(Material.MANGROVE_PLANKS,age.wood);
        put(Material.MANGROVE_PRESSURE_PLATE,age.wood);
        put(Material.MANGROVE_PROPAGULE,age.wood);
        put(Material.MANGROVE_ROOTS,age.wood);
        put(Material.MANGROVE_SIGN,age.wood);
        put(Material.MANGROVE_SLAB,age.wood);
        put(Material.MANGROVE_STAIRS,age.wood);
        put(Material.MANGROVE_TRAPDOOR,age.wood);
        put(Material.MANGROVE_WALL_HANGING_SIGN,age.wood);
        put(Material.MANGROVE_WALL_SIGN,age.wood);
        put(Material.MANGROVE_WOOD,age.wood);
        put(Material.MAP,age.iron);
        put(Material.MEDIUM_AMETHYST_BUD,age.stone);
        put(Material.MELON,age.wood);
        put(Material.MELON_SEEDS,age.wood);
        put(Material.MELON_SLICE,age.wood);
        put(Material.MELON_STEM,age.wood);
        put(Material.MILK_BUCKET,age.iron);
        put(Material.MINECART,age.iron);
        put(Material.MINER_POTTERY_SHERD,age.wood);
        put(Material.MOJANG_BANNER_PATTERN,age.wood);
        put(Material.MOOSHROOM_SPAWN_EGG,age.wood);
        put(Material.MOSS_BLOCK,age.wood);
        put(Material.MOSS_CARPET,age.wood);
        put(Material.MOSSY_COBBLESTONE,age.stone);
        put(Material.MOSSY_COBBLESTONE_SLAB,age.stone);
        put(Material.MOSSY_COBBLESTONE_STAIRS,age.stone);
        put(Material.MOSSY_COBBLESTONE_WALL,age.stone);
        put(Material.MOSSY_STONE_BRICK_SLAB,age.stone);
        put(Material.MOSSY_STONE_BRICK_STAIRS,age.stone);
        put(Material.MOSSY_STONE_BRICK_WALL,age.stone);
        put(Material.MOSSY_STONE_BRICKS,age.stone);
        put(Material.MOURNER_POTTERY_SHERD,age.wood);
        put(Material.MOVING_PISTON,age.iron);
        put(Material.MUD,age.wood);
        put(Material.MUD_BRICK_SLAB,age.wood);
        put(Material.MUD_BRICK_STAIRS,age.wood);
        put(Material.MUD_BRICK_WALL,age.wood);
        put(Material.MUD_BRICKS,age.wood);
        put(Material.MUDDY_MANGROVE_ROOTS,age.wood);
        put(Material.MULE_SPAWN_EGG,age.wood);
        put(Material.MUSHROOM_STEM,age.wood);
        put(Material.MUSHROOM_STEW,age.wood);
        put(Material.MUSIC_DISC_11,age.wood);
        put(Material.MUSIC_DISC_13,age.wood);
        put(Material.MUSIC_DISC_5,age.wood);
        put(Material.MUSIC_DISC_BLOCKS,age.wood);
        put(Material.MUSIC_DISC_CAT,age.wood);
        put(Material.MUSIC_DISC_CHIRP,age.wood);
        put(Material.MUSIC_DISC_CREATOR,age.wood);
        put(Material.MUSIC_DISC_CREATOR_MUSIC_BOX,age.wood);
        put(Material.MUSIC_DISC_FAR,age.wood);
        put(Material.MUSIC_DISC_LAVA_CHICKEN,age.wood);
        put(Material.MUSIC_DISC_MALL,age.wood);
        put(Material.MUSIC_DISC_MELLOHI,age.wood);
        put(Material.MUSIC_DISC_OTHERSIDE,age.wood);
        put(Material.MUSIC_DISC_PIGSTEP,age.wood);
        put(Material.MUSIC_DISC_PRECIPICE,age.wood);
        put(Material.MUSIC_DISC_RELIC,age.wood);
        put(Material.MUSIC_DISC_STAL,age.wood);
        put(Material.MUSIC_DISC_STRAD,age.wood);
        put(Material.MUSIC_DISC_TEARS,age.wood);
        put(Material.MUSIC_DISC_WAIT,age.wood);
        put(Material.MUSIC_DISC_WARD,age.wood);
        put(Material.MUTTON,age.wood);
        put(Material.MYCELIUM,age.wood);
        put(Material.NAME_TAG,age.wood);
        put(Material.NAUTILUS_SHELL,age.ocean);
        put(Material.NETHER_BRICK,age.stone);
        put(Material.NETHER_BRICK_FENCE,age.stone);
        put(Material.NETHER_BRICK_SLAB,age.stone);
        put(Material.NETHER_BRICK_STAIRS,age.stone);
        put(Material.NETHER_BRICK_WALL,age.stone);
        put(Material.NETHER_BRICKS,age.stone);
        put(Material.NETHER_GOLD_ORE,age.nether);
        put(Material.NETHER_PORTAL,age.nether);
        put(Material.NETHER_QUARTZ_ORE,age.nether);
        put(Material.NETHER_SPROUTS,age.nether);
        put(Material.NETHER_STAR,age.netherite);
        put(Material.NETHER_WART,age.nether);
        put(Material.NETHER_WART_BLOCK,age.nether);
        put(Material.NETHERITE_AXE,age.netherite);
        put(Material.NETHERITE_BLOCK,age.netherite);
        put(Material.NETHERITE_BOOTS,age.netherite);
        put(Material.NETHERITE_CHESTPLATE,age.netherite);
        put(Material.NETHERITE_HELMET,age.netherite);
        put(Material.NETHERITE_HOE,age.netherite);
        put(Material.NETHERITE_INGOT,age.netherite);
        put(Material.NETHERITE_LEGGINGS,age.netherite);
        put(Material.NETHERITE_PICKAXE,age.netherite);
        put(Material.NETHERITE_SCRAP,age.netherite);
        put(Material.NETHERITE_SHOVEL,age.netherite);
        put(Material.NETHERITE_SWORD,age.netherite);
        put(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,age.netherite);
        put(Material.NETHERRACK,age.nether);
        put(Material.NOTE_BLOCK,age.redstone);
        put(Material.OAK_BOAT,age.wood);
        put(Material.OAK_BUTTON,age.wood);
        put(Material.OAK_CHEST_BOAT,age.wood);
        put(Material.OAK_DOOR,age.wood);
        put(Material.OAK_FENCE,age.wood);
        put(Material.OAK_FENCE_GATE,age.wood);
        put(Material.OAK_HANGING_SIGN,age.wood);
        put(Material.OAK_LEAVES,age.wood);
        put(Material.OAK_LOG,age.wood);
        put(Material.OAK_PLANKS,age.wood);
        put(Material.OAK_PRESSURE_PLATE,age.wood);
        put(Material.OAK_SAPLING,age.wood);
        put(Material.OAK_SIGN,age.wood);
        put(Material.OAK_SLAB,age.wood);
        put(Material.OAK_STAIRS,age.wood);
        put(Material.OAK_TRAPDOOR,age.wood);
        put(Material.OAK_WALL_HANGING_SIGN,age.wood);
        put(Material.OAK_WALL_SIGN,age.wood);
        put(Material.OAK_WOOD,age.wood);
        put(Material.OBSERVER,age.redstone);
        put(Material.OBSIDIAN,age.stone);
        put(Material.OCELOT_SPAWN_EGG,age.wood);
        put(Material.OCHRE_FROGLIGHT,age.nether);
        put(Material.OMINOUS_BOTTLE,age.trial);
        put(Material.OMINOUS_TRIAL_KEY,age.trial);
        put(Material.OPEN_EYEBLOSSOM,age.wood);
        put(Material.ORANGE_BANNER,age.wood);
        put(Material.ORANGE_BED,age.wood);
        put(Material.ORANGE_BUNDLE,age.wood);
        put(Material.ORANGE_CANDLE,age.wood);
        put(Material.ORANGE_CANDLE_CAKE,age.wood);
        put(Material.ORANGE_CARPET,age.wood);
        put(Material.ORANGE_CONCRETE,age.wood);
        put(Material.ORANGE_CONCRETE_POWDER,age.wood);
        put(Material.ORANGE_DYE,age.wood);
        put(Material.ORANGE_GLAZED_TERRACOTTA,age.stone);
        put(Material.ORANGE_HARNESS,age.wood);
        put(Material.ORANGE_SHULKER_BOX,age.postend);
        put(Material.ORANGE_STAINED_GLASS,age.wood);
        put(Material.ORANGE_STAINED_GLASS_PANE,age.wood);
        put(Material.ORANGE_TERRACOTTA,age.stone);
        put(Material.ORANGE_TULIP,age.wood);
        put(Material.ORANGE_WALL_BANNER,age.wood);
        put(Material.ORANGE_WOOL,age.wood);
        put(Material.OXEYE_DAISY,age.wood);
        put(Material.OXIDIZED_CHISELED_COPPER,age.stone);
        put(Material.OXIDIZED_COPPER,age.stone);
        put(Material.OXIDIZED_COPPER_BULB,age.redstone);
        put(Material.OXIDIZED_COPPER_DOOR,age.stone);
        put(Material.OXIDIZED_COPPER_GRATE,age.stone);
        put(Material.OXIDIZED_COPPER_TRAPDOOR,age.stone);
        put(Material.OXIDIZED_CUT_COPPER,age.stone);
        put(Material.OXIDIZED_CUT_COPPER_SLAB,age.stone);
        put(Material.OXIDIZED_CUT_COPPER_STAIRS,age.stone);
        put(Material.PACKED_ICE,age.wood);
        put(Material.PACKED_MUD,age.wood);
        put(Material.PAINTING,age.wood);
        put(Material.PALE_HANGING_MOSS,age.wood);
        put(Material.PALE_MOSS_BLOCK,age.wood);
        put(Material.PALE_MOSS_CARPET,age.wood);
        put(Material.PALE_OAK_BOAT,age.wood);
        put(Material.PALE_OAK_BUTTON,age.wood);
        put(Material.PALE_OAK_CHEST_BOAT,age.wood);
        put(Material.PALE_OAK_DOOR,age.wood);
        put(Material.PALE_OAK_FENCE,age.wood);
        put(Material.PALE_OAK_FENCE_GATE,age.wood);
        put(Material.PALE_OAK_HANGING_SIGN,age.wood);
        put(Material.PALE_OAK_LEAVES,age.wood);
        put(Material.PALE_OAK_LOG,age.wood);
        put(Material.PALE_OAK_PLANKS,age.wood);
        put(Material.PALE_OAK_PRESSURE_PLATE,age.wood);
        put(Material.PALE_OAK_SAPLING,age.wood);
        put(Material.PALE_OAK_SIGN,age.wood);
        put(Material.PALE_OAK_SLAB,age.wood);
        put(Material.PALE_OAK_STAIRS,age.wood);
        put(Material.PALE_OAK_TRAPDOOR,age.wood);
        put(Material.PALE_OAK_WALL_HANGING_SIGN,age.wood);
        put(Material.PALE_OAK_WALL_SIGN,age.wood);
        put(Material.PALE_OAK_WOOD,age.wood);
        put(Material.PANDA_SPAWN_EGG,age.wood);
        put(Material.PAPER,age.wood);
        put(Material.PARROT_SPAWN_EGG,age.wood);
        put(Material.PEARLESCENT_FROGLIGHT,age.nether);
        put(Material.PEONY,age.wood);
        put(Material.PETRIFIED_OAK_SLAB,age.wood);
        put(Material.PHANTOM_MEMBRANE,age.wood);
        put(Material.PHANTOM_SPAWN_EGG,age.wood);
        put(Material.PIG_SPAWN_EGG,age.wood);
        put(Material.PIGLIN_BANNER_PATTERN,age.nether);
        put(Material.PIGLIN_BRUTE_SPAWN_EGG,age.nether);
        put(Material.PIGLIN_HEAD,age.stone);
        put(Material.PIGLIN_SPAWN_EGG,age.wood);
        put(Material.PIGLIN_WALL_HEAD,age.stone);
        put(Material.PILLAGER_SPAWN_EGG,age.wood);
        put(Material.PINK_BANNER,age.wood);
        put(Material.PINK_BED,age.wood);
        put(Material.PINK_BUNDLE,age.wood);
        put(Material.PINK_CANDLE,age.wood);
        put(Material.PINK_CANDLE_CAKE,age.wood);
        put(Material.PINK_CARPET,age.wood);
        put(Material.PINK_CONCRETE,age.wood);
        put(Material.PINK_CONCRETE_POWDER,age.wood);
        put(Material.PINK_DYE,age.wood);
        put(Material.PINK_GLAZED_TERRACOTTA,age.stone);
        put(Material.PINK_HARNESS,age.wood);
        put(Material.PINK_PETALS,age.wood);
        put(Material.PINK_SHULKER_BOX,age.postend);
        put(Material.PINK_STAINED_GLASS,age.wood);
        put(Material.PINK_STAINED_GLASS_PANE,age.wood);
        put(Material.PINK_TERRACOTTA,age.stone);
        put(Material.PINK_TULIP,age.wood);
        put(Material.PINK_WALL_BANNER,age.wood);
        put(Material.PINK_WOOL,age.wood);
        put(Material.PISTON,age.iron);
        put(Material.PISTON_HEAD,age.iron);
        put(Material.PITCHER_CROP,age.stone);
        put(Material.PITCHER_PLANT,age.stone);
        put(Material.PITCHER_POD,age.stone);
        put(Material.PLAYER_HEAD,age.wood);
        put(Material.PLAYER_WALL_HEAD,age.wood);
        put(Material.PLENTY_POTTERY_SHERD,age.wood);
        put(Material.PODZOL,age.wood);
        put(Material.POINTED_DRIPSTONE,age.stone);
        put(Material.POISONOUS_POTATO,age.wood);
        put(Material.POLAR_BEAR_SPAWN_EGG,age.wood);
        put(Material.POLISHED_ANDESITE,age.stone);
        put(Material.POLISHED_ANDESITE_SLAB,age.stone);
        put(Material.POLISHED_ANDESITE_STAIRS,age.stone);
        put(Material.POLISHED_BASALT,age.stone);
        put(Material.POLISHED_BLACKSTONE,age.stone);
        put(Material.POLISHED_BLACKSTONE_BRICK_SLAB,age.stone);
        put(Material.POLISHED_BLACKSTONE_BRICK_STAIRS,age.stone);
        put(Material.POLISHED_BLACKSTONE_BRICK_WALL,age.stone);
        put(Material.POLISHED_BLACKSTONE_BRICKS,age.stone);
        put(Material.POLISHED_BLACKSTONE_BUTTON,age.stone);
        put(Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,age.stone);
        put(Material.POLISHED_BLACKSTONE_SLAB,age.stone);
        put(Material.POLISHED_BLACKSTONE_STAIRS,age.stone);
        put(Material.POLISHED_BLACKSTONE_WALL,age.stone);
        put(Material.POLISHED_DEEPSLATE,age.stone);
        put(Material.POLISHED_DEEPSLATE_SLAB,age.stone);
        put(Material.POLISHED_DEEPSLATE_STAIRS,age.stone);
        put(Material.POLISHED_DEEPSLATE_WALL,age.stone);
        put(Material.POLISHED_DIORITE,age.stone);
        put(Material.POLISHED_DIORITE_SLAB,age.stone);
        put(Material.POLISHED_DIORITE_STAIRS,age.stone);
        put(Material.POLISHED_GRANITE,age.stone);
        put(Material.POLISHED_GRANITE_SLAB,age.stone);
        put(Material.POLISHED_GRANITE_STAIRS,age.stone);
        put(Material.POLISHED_TUFF,age.stone);
        put(Material.POLISHED_TUFF_SLAB,age.stone);
        put(Material.POLISHED_TUFF_STAIRS,age.stone);
        put(Material.POLISHED_TUFF_WALL,age.stone);
        put(Material.POPPED_CHORUS_FRUIT,age.end);
        put(Material.POPPY,age.wood);
        put(Material.PORKCHOP,age.wood);
        put(Material.POTATO,age.wood);
        put(Material.POTATOES,age.wood);
        put(Material.POTION,age.nether);
        put(Material.POTTED_ACACIA_SAPLING,age.wood);
        put(Material.POTTED_ALLIUM,age.wood);
        put(Material.POTTED_AZALEA_BUSH,age.wood);
        put(Material.POTTED_AZURE_BLUET,age.wood);
        put(Material.POTTED_BAMBOO,age.wood);
        put(Material.POTTED_BIRCH_SAPLING,age.wood);
        put(Material.POTTED_BLUE_ORCHID,age.wood);
        put(Material.POTTED_BROWN_MUSHROOM,age.wood);
        put(Material.POTTED_CACTUS,age.wood);
        put(Material.POTTED_CHERRY_SAPLING,age.wood);
        put(Material.POTTED_CLOSED_EYEBLOSSOM,age.wood);
        put(Material.POTTED_CORNFLOWER,age.wood);
        put(Material.POTTED_CRIMSON_FUNGUS,age.wood);
        put(Material.POTTED_CRIMSON_ROOTS,age.wood);
        put(Material.POTTED_DANDELION,age.wood);
        put(Material.POTTED_DARK_OAK_SAPLING,age.wood);
        put(Material.POTTED_DEAD_BUSH,age.wood);
        put(Material.POTTED_FERN,age.wood);
        put(Material.POTTED_FLOWERING_AZALEA_BUSH,age.wood);
        put(Material.POTTED_JUNGLE_SAPLING,age.wood);
        put(Material.POTTED_LILY_OF_THE_VALLEY,age.wood);
        put(Material.POTTED_MANGROVE_PROPAGULE,age.wood);
        put(Material.POTTED_OAK_SAPLING,age.wood);
        put(Material.POTTED_OPEN_EYEBLOSSOM,age.wood);
        put(Material.POTTED_ORANGE_TULIP,age.wood);
        put(Material.POTTED_OXEYE_DAISY,age.wood);
        put(Material.POTTED_PALE_OAK_SAPLING,age.wood);
        put(Material.POTTED_PINK_TULIP,age.wood);
        put(Material.POTTED_POPPY,age.wood);
        put(Material.POTTED_RED_MUSHROOM,age.wood);
        put(Material.POTTED_RED_TULIP,age.wood);
        put(Material.POTTED_SPRUCE_SAPLING,age.wood);
        put(Material.POTTED_TORCHFLOWER,age.wood);
        put(Material.POTTED_WARPED_FUNGUS,age.wood);
        put(Material.POTTED_WARPED_ROOTS,age.wood);
        put(Material.POTTED_WHITE_TULIP,age.wood);
        put(Material.POTTED_WITHER_ROSE,age.wood);
        put(Material.POWDER_SNOW,age.wood);
        put(Material.POWDER_SNOW_BUCKET,age.iron);
        put(Material.POWDER_SNOW_CAULDRON,age.iron);
        put(Material.POWERED_RAIL,age.iron);
        put(Material.PRISMARINE,age.ocean);
        put(Material.PRISMARINE_BRICK_SLAB,age.ocean);
        put(Material.PRISMARINE_BRICK_STAIRS,age.ocean);
        put(Material.PRISMARINE_BRICKS,age.ocean);
        put(Material.PRISMARINE_CRYSTALS,age.ocean);
        put(Material.PRISMARINE_SHARD,age.ocean);
        put(Material.PRISMARINE_SLAB,age.ocean);
        put(Material.PRISMARINE_STAIRS,age.ocean);
        put(Material.PRISMARINE_WALL,age.ocean);
        put(Material.PRIZE_POTTERY_SHERD,age.wood);
        put(Material.PUFFERFISH,age.wood);
        put(Material.PUFFERFISH_BUCKET,age.iron);
        put(Material.PUFFERFISH_SPAWN_EGG,age.wood);
        put(Material.PUMPKIN,age.wood);
        put(Material.PUMPKIN_PIE,age.wood);
        put(Material.PUMPKIN_SEEDS,age.wood);
        put(Material.PUMPKIN_STEM,age.wood);
        put(Material.PURPLE_BANNER,age.wood);
        put(Material.PURPLE_BED,age.wood);
        put(Material.PURPLE_BUNDLE,age.wood);
        put(Material.PURPLE_CANDLE,age.wood);
        put(Material.PURPLE_CANDLE_CAKE,age.wood);
        put(Material.PURPLE_CARPET,age.wood);
        put(Material.PURPLE_CONCRETE,age.wood);
        put(Material.PURPLE_CONCRETE_POWDER,age.wood);
        put(Material.PURPLE_DYE,age.wood);
        put(Material.PURPLE_GLAZED_TERRACOTTA,age.stone);
        put(Material.PURPLE_HARNESS,age.wood);
        put(Material.PURPLE_SHULKER_BOX,age.postend);
        put(Material.PURPLE_STAINED_GLASS,age.wood);
        put(Material.PURPLE_STAINED_GLASS_PANE,age.wood);
        put(Material.PURPLE_TERRACOTTA,age.stone);
        put(Material.PURPLE_WALL_BANNER,age.wood);
        put(Material.PURPLE_WOOL,age.wood);
        put(Material.PURPUR_BLOCK,age.postend);
        put(Material.PURPUR_PILLAR,age.postend);
        put(Material.PURPUR_SLAB,age.postend);
        put(Material.PURPUR_STAIRS,age.postend);
        put(Material.QUARTZ,age.nether);
        put(Material.QUARTZ_BLOCK,age.nether);
        put(Material.QUARTZ_BRICKS,age.nether);
        put(Material.QUARTZ_PILLAR,age.nether);
        put(Material.QUARTZ_SLAB,age.nether);
        put(Material.QUARTZ_STAIRS,age.nether);
        put(Material.RABBIT,age.wood);
        put(Material.RABBIT_FOOT,age.wood);
        put(Material.RABBIT_HIDE,age.wood);
        put(Material.RABBIT_SPAWN_EGG,age.wood);
        put(Material.RABBIT_STEW,age.wood);
        put(Material.RAIL,age.iron);
        put(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.RAVAGER_SPAWN_EGG,age.wood);
        put(Material.RAW_COPPER,age.stone);
        put(Material.RAW_COPPER_BLOCK,age.stone);
        put(Material.RAW_GOLD,age.stone);
        put(Material.RAW_GOLD_BLOCK,age.stone);
        put(Material.RAW_IRON,age.stone);
        put(Material.RAW_IRON_BLOCK,age.stone);
        put(Material.RECOVERY_COMPASS,age.diamond);
        put(Material.RED_BANNER,age.wood);
        put(Material.RED_BED,age.wood);
        put(Material.RED_BUNDLE,age.wood);
        put(Material.RED_CANDLE,age.wood);
        put(Material.RED_CANDLE_CAKE,age.wood);
        put(Material.RED_CARPET,age.wood);
        put(Material.RED_CONCRETE,age.wood);
        put(Material.RED_CONCRETE_POWDER,age.wood);
        put(Material.RED_DYE,age.wood);
        put(Material.RED_GLAZED_TERRACOTTA,age.stone);
        put(Material.RED_HARNESS,age.wood);
        put(Material.RED_MUSHROOM,age.wood);
        put(Material.RED_MUSHROOM_BLOCK,age.wood);
        put(Material.RED_NETHER_BRICK_SLAB,age.stone);
        put(Material.RED_NETHER_BRICK_STAIRS,age.stone);
        put(Material.RED_NETHER_BRICK_WALL,age.stone);
        put(Material.RED_NETHER_BRICKS,age.stone);
        put(Material.RED_SAND,age.wood);
        put(Material.RED_SANDSTONE,age.stone);
        put(Material.RED_SANDSTONE_SLAB,age.stone);
        put(Material.RED_SANDSTONE_STAIRS,age.stone);
        put(Material.RED_SANDSTONE_WALL,age.stone);
        put(Material.RED_SHULKER_BOX,age.postend);
        put(Material.RED_STAINED_GLASS,age.wood);
        put(Material.RED_STAINED_GLASS_PANE,age.wood);
        put(Material.RED_TERRACOTTA,age.stone);
        put(Material.RED_TULIP,age.wood);
        put(Material.RED_WALL_BANNER,age.wood);
        put(Material.RED_WOOL,age.wood);
        put(Material.REDSTONE,age.redstone);
        put(Material.REDSTONE_BLOCK,age.redstone);
        put(Material.REDSTONE_LAMP,age.redstone);
        put(Material.REDSTONE_ORE,age.redstone);
        put(Material.REDSTONE_TORCH,age.redstone);
        put(Material.REDSTONE_WALL_TORCH,age.redstone);
        put(Material.REDSTONE_WIRE,age.redstone);
        put(Material.REINFORCED_DEEPSLATE,age.diamond);
        put(Material.REPEATER,age.redstone);
        put(Material.REPEATING_COMMAND_BLOCK,age.wood);
        put(Material.RESIN_BLOCK,age.wood);
        put(Material.RESIN_BRICK,age.wood);
        put(Material.RESIN_BRICK_SLAB,age.wood);
        put(Material.RESIN_BRICK_STAIRS,age.wood);
        put(Material.RESIN_BRICK_WALL,age.wood);
        put(Material.RESIN_BRICKS,age.wood);
        put(Material.RESIN_CLUMP,age.wood);
        put(Material.RESPAWN_ANCHOR,age.nether);
        put(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.ROOTED_DIRT,age.wood);
        put(Material.ROSE_BUSH,age.wood);
        put(Material.ROTTEN_FLESH,age.wood);
        put(Material.SADDLE,age.stone);
        put(Material.SALMON,age.wood);
        put(Material.SALMON_BUCKET,age.iron);
        put(Material.SALMON_SPAWN_EGG,age.wood);
        put(Material.SAND,age.wood);
        put(Material.SANDSTONE,age.stone);
        put(Material.SANDSTONE_SLAB,age.stone);
        put(Material.SANDSTONE_STAIRS,age.stone);
        put(Material.SANDSTONE_WALL,age.stone);
        put(Material.SCAFFOLDING,age.wood);
        put(Material.SCRAPE_POTTERY_SHERD,age.wood);
        put(Material.SCULK,age.stone);
        put(Material.SCULK_CATALYST,age.stone);
        put(Material.SCULK_SENSOR,age.stone);
        put(Material.SCULK_SHRIEKER,age.stone);
        put(Material.SCULK_VEIN,age.stone);
        put(Material.SEA_LANTERN,age.ocean);
        put(Material.SEA_PICKLE,age.wood);
        put(Material.SEAGRASS,age.wood);
        put(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.SHEAF_POTTERY_SHERD,age.wood);
        put(Material.SHEARS,age.iron);
        put(Material.SHEEP_SPAWN_EGG,age.wood);
        put(Material.SHELTER_POTTERY_SHERD,age.wood);
        put(Material.SHIELD,age.iron);
        put(Material.SHORT_DRY_GRASS,age.wood);
        put(Material.SHORT_GRASS,age.wood);
        put(Material.SHROOMLIGHT,age.nether);
        put(Material.SHULKER_BOX,age.postend);
        put(Material.SHULKER_SHELL,age.postend);
        put(Material.SHULKER_SPAWN_EGG,age.wood);
        put(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.SILVERFISH_SPAWN_EGG,age.wood);
        put(Material.SKELETON_HORSE_SPAWN_EGG,age.wood);
        put(Material.SKELETON_SKULL,age.stone);
        put(Material.SKELETON_SPAWN_EGG,age.wood);
        put(Material.SKELETON_WALL_SKULL,age.stone);
        put(Material.SKULL_BANNER_PATTERN,age.wood);
        put(Material.SKULL_POTTERY_SHERD,age.wood);
        put(Material.SLIME_BALL,age.wood);
        put(Material.SLIME_BLOCK,age.wood);
        put(Material.SLIME_SPAWN_EGG,age.wood);
        put(Material.SMALL_AMETHYST_BUD,age.stone);
        put(Material.SMALL_DRIPLEAF,age.wood);
        put(Material.SMITHING_TABLE,age.iron);
        put(Material.SMOKER,age.stone);
        put(Material.SMOOTH_BASALT,age.stone);
        put(Material.SMOOTH_QUARTZ,age.stone);
        put(Material.SMOOTH_QUARTZ_SLAB,age.stone);
        put(Material.SMOOTH_QUARTZ_STAIRS,age.stone);
        put(Material.SMOOTH_RED_SANDSTONE,age.stone);
        put(Material.SMOOTH_RED_SANDSTONE_SLAB,age.stone);
        put(Material.SMOOTH_RED_SANDSTONE_STAIRS,age.stone);
        put(Material.SMOOTH_SANDSTONE,age.stone);
        put(Material.SMOOTH_SANDSTONE_SLAB,age.stone);
        put(Material.SMOOTH_SANDSTONE_STAIRS,age.stone);
        put(Material.SMOOTH_STONE,age.stone);
        put(Material.SMOOTH_STONE_SLAB,age.stone);
        put(Material.SNIFFER_EGG,age.stone);
        put(Material.SNIFFER_SPAWN_EGG,age.wood);
        put(Material.SNORT_POTTERY_SHERD,age.wood);
        put(Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.SNOW,age.wood);
        put(Material.SNOW_BLOCK,age.wood);
        put(Material.SNOW_GOLEM_SPAWN_EGG,age.wood);
        put(Material.SNOWBALL,age.wood);
        put(Material.SOUL_CAMPFIRE,age.nether);
        put(Material.SOUL_FIRE,age.nether);
        put(Material.SOUL_LANTERN,age.nether);
        put(Material.SOUL_SAND,age.nether);
        put(Material.SOUL_SOIL,age.nether);
        put(Material.SOUL_TORCH,age.nether);
        put(Material.SOUL_WALL_TORCH,age.nether);
        put(Material.SPAWNER,age.wood);
        put(Material.SPECTRAL_ARROW,age.nether);
        put(Material.SPIDER_EYE,age.wood);
        put(Material.SPIDER_SPAWN_EGG,age.wood);
        put(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.SPLASH_POTION,age.nether);
        put(Material.SPONGE,age.ocean);
        put(Material.SPORE_BLOSSOM,age.wood);
        put(Material.SPRUCE_BOAT,age.wood);
        put(Material.SPRUCE_BUTTON,age.wood);
        put(Material.SPRUCE_CHEST_BOAT,age.wood);
        put(Material.SPRUCE_DOOR,age.wood);
        put(Material.SPRUCE_FENCE,age.wood);
        put(Material.SPRUCE_FENCE_GATE,age.wood);
        put(Material.SPRUCE_HANGING_SIGN,age.wood);
        put(Material.SPRUCE_LEAVES,age.wood);
        put(Material.SPRUCE_LOG,age.wood);
        put(Material.SPRUCE_PLANKS,age.wood);
        put(Material.SPRUCE_PRESSURE_PLATE,age.wood);
        put(Material.SPRUCE_SAPLING,age.wood);
        put(Material.SPRUCE_SIGN,age.wood);
        put(Material.SPRUCE_SLAB,age.wood);
        put(Material.SPRUCE_STAIRS,age.wood);
        put(Material.SPRUCE_TRAPDOOR,age.wood);
        put(Material.SPRUCE_WALL_HANGING_SIGN,age.wood);
        put(Material.SPRUCE_WALL_SIGN,age.wood);
        put(Material.SPRUCE_WOOD,age.wood);
        put(Material.SPYGLASS,age.stone);
        put(Material.SQUID_SPAWN_EGG,age.wood);
        put(Material.STICK,age.wood);
        put(Material.STICKY_PISTON,age.redstone);
        put(Material.STONE,age.stone);
        put(Material.STONE_AXE,age.stone);
        put(Material.STONE_BRICK_SLAB,age.stone);
        put(Material.STONE_BRICK_STAIRS,age.stone);
        put(Material.STONE_BRICK_WALL,age.stone);
        put(Material.STONE_BRICKS,age.stone);
        put(Material.STONE_BUTTON,age.stone);
        put(Material.STONE_HOE,age.stone);
        put(Material.STONE_PICKAXE,age.stone);
        put(Material.STONE_PRESSURE_PLATE,age.stone);
        put(Material.STONE_SHOVEL,age.stone);
        put(Material.STONE_SLAB,age.stone);
        put(Material.STONE_STAIRS,age.stone);
        put(Material.STONE_SWORD,age.stone);
        put(Material.STONECUTTER,age.iron);
        put(Material.STRAY_SPAWN_EGG,age.wood);
        put(Material.STRIDER_SPAWN_EGG,age.wood);
        put(Material.STRING,age.wood);
        put(Material.STRIPPED_ACACIA_LOG,age.wood);
        put(Material.STRIPPED_ACACIA_WOOD,age.wood);
        put(Material.STRIPPED_BAMBOO_BLOCK,age.wood);
        put(Material.STRIPPED_BIRCH_LOG,age.wood);
        put(Material.STRIPPED_BIRCH_WOOD,age.wood);
        put(Material.STRIPPED_CHERRY_LOG,age.wood);
        put(Material.STRIPPED_CHERRY_WOOD,age.wood);
        put(Material.STRIPPED_CRIMSON_HYPHAE,age.wood);
        put(Material.STRIPPED_CRIMSON_STEM,age.wood);
        put(Material.STRIPPED_DARK_OAK_LOG,age.wood);
        put(Material.STRIPPED_DARK_OAK_WOOD,age.wood);
        put(Material.STRIPPED_JUNGLE_LOG,age.wood);
        put(Material.STRIPPED_JUNGLE_WOOD,age.wood);
        put(Material.STRIPPED_MANGROVE_LOG,age.wood);
        put(Material.STRIPPED_MANGROVE_WOOD,age.wood);
        put(Material.STRIPPED_OAK_LOG,age.wood);
        put(Material.STRIPPED_OAK_WOOD,age.wood);
        put(Material.STRIPPED_PALE_OAK_LOG,age.wood);
        put(Material.STRIPPED_PALE_OAK_WOOD,age.wood);
        put(Material.STRIPPED_SPRUCE_LOG,age.wood);
        put(Material.STRIPPED_SPRUCE_WOOD,age.wood);
        put(Material.STRIPPED_WARPED_HYPHAE,age.wood);
        put(Material.STRIPPED_WARPED_STEM,age.wood);
        put(Material.STRUCTURE_BLOCK,age.wood);
        put(Material.STRUCTURE_VOID,age.wood);
        put(Material.SUGAR,age.wood);
        put(Material.SUGAR_CANE,age.wood);
        put(Material.SUNFLOWER,age.wood);
        put(Material.SUSPICIOUS_GRAVEL,age.wood);
        put(Material.SUSPICIOUS_SAND,age.wood);
        put(Material.SUSPICIOUS_STEW,age.wood);
        put(Material.SWEET_BERRIES,age.wood);
        put(Material.SWEET_BERRY_BUSH,age.wood);
        put(Material.TADPOLE_BUCKET,age.iron);
        put(Material.TADPOLE_SPAWN_EGG,age.wood);
        put(Material.TALL_DRY_GRASS,age.wood);
        put(Material.TALL_GRASS,age.wood);
        put(Material.TALL_SEAGRASS,age.wood);
        put(Material.TARGET,age.redstone);
        put(Material.TERRACOTTA,age.stone);
        put(Material.TEST_BLOCK,age.wood);
        put(Material.TEST_INSTANCE_BLOCK,age.wood);
        put(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,age.ocean);
        put(Material.TINTED_GLASS,age.stone);
        put(Material.TIPPED_ARROW,age.nether);
        put(Material.TNT,age.wood);
        put(Material.TNT_MINECART,age.iron);
        put(Material.TORCH,age.wood);
        put(Material.TORCHFLOWER,age.wood);
        put(Material.TORCHFLOWER_CROP,age.wood);
        put(Material.TORCHFLOWER_SEEDS,age.wood);
        put(Material.TOTEM_OF_UNDYING,age.pillager);
        put(Material.TRADER_LLAMA_SPAWN_EGG,age.wood);
        put(Material.TRAPPED_CHEST,age.redstone);
        put(Material.TRIAL_KEY,age.trial);
        put(Material.TRIAL_SPAWNER,age.trial);
        put(Material.TRIDENT,age.ocean);
        put(Material.TRIPWIRE,age.wood);
        put(Material.TRIPWIRE_HOOK,age.stone);
        put(Material.TROPICAL_FISH,age.wood);
        put(Material.TROPICAL_FISH_BUCKET,age.iron);
        put(Material.TROPICAL_FISH_SPAWN_EGG,age.wood);
        put(Material.TUBE_CORAL,age.wood);
        put(Material.TUBE_CORAL_BLOCK,age.wood);
        put(Material.TUBE_CORAL_FAN,age.wood);
        put(Material.TUBE_CORAL_WALL_FAN,age.wood);
        put(Material.TUFF,age.stone);
        put(Material.TUFF_BRICK_SLAB,age.stone);
        put(Material.TUFF_BRICK_STAIRS,age.stone);
        put(Material.TUFF_BRICK_WALL,age.stone);
        put(Material.TUFF_BRICKS,age.stone);
        put(Material.TUFF_SLAB,age.stone);
        put(Material.TUFF_STAIRS,age.stone);
        put(Material.TUFF_WALL,age.stone);
        put(Material.TURTLE_EGG,age.wood);
        put(Material.TURTLE_HELMET,age.ocean);
        put(Material.TURTLE_SCUTE,age.nether);
        put(Material.TURTLE_SPAWN_EGG,age.wood);
        put(Material.TWISTING_VINES,age.wood);
        put(Material.TWISTING_VINES_PLANT,age.wood);
        put(Material.VAULT,age.wood);
        put(Material.VERDANT_FROGLIGHT,age.nether);
        put(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.VEX_SPAWN_EGG,age.wood);
        put(Material.VILLAGER_SPAWN_EGG,age.wood);
        put(Material.VINDICATOR_SPAWN_EGG,age.wood);
        put(Material.VINE,age.wood);
        put(Material.VOID_AIR,age.wood);
        put(Material.WALL_TORCH,age.wood);
        put(Material.WANDERING_TRADER_SPAWN_EGG,age.wood);
        put(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.WARDEN_SPAWN_EGG,age.wood);
        put(Material.WARPED_BUTTON,age.wood);
        put(Material.WARPED_DOOR,age.wood);
        put(Material.WARPED_FENCE,age.wood);
        put(Material.WARPED_FENCE_GATE,age.wood);
        put(Material.WARPED_FUNGUS,age.wood);
        put(Material.WARPED_FUNGUS_ON_A_STICK,age.wood);
        put(Material.WARPED_HANGING_SIGN,age.wood);
        put(Material.WARPED_HYPHAE,age.wood);
        put(Material.WARPED_NYLIUM,age.wood);
        put(Material.WARPED_PLANKS,age.wood);
        put(Material.WARPED_PRESSURE_PLATE,age.wood);
        put(Material.WARPED_ROOTS,age.wood);
        put(Material.WARPED_SIGN,age.wood);
        put(Material.WARPED_SLAB,age.wood);
        put(Material.WARPED_STAIRS,age.wood);
        put(Material.WARPED_STEM,age.wood);
        put(Material.WARPED_TRAPDOOR,age.wood);
        put(Material.WARPED_WALL_HANGING_SIGN,age.wood);
        put(Material.WARPED_WALL_SIGN,age.wood);
        put(Material.WARPED_WART_BLOCK,age.wood);
        put(Material.WATER,age.wood);
        put(Material.WATER_BUCKET,age.iron);
        put(Material.WATER_CAULDRON,age.iron);
        put(Material.WAXED_CHISELED_COPPER,age.stone);
        put(Material.WAXED_COPPER_BLOCK,age.stone);
        put(Material.WAXED_COPPER_BULB,age.redstone);
        put(Material.WAXED_COPPER_DOOR,age.stone);
        put(Material.WAXED_COPPER_GRATE,age.stone);
        put(Material.WAXED_COPPER_TRAPDOOR,age.stone);
        put(Material.WAXED_CUT_COPPER,age.stone);
        put(Material.WAXED_CUT_COPPER_SLAB,age.stone);
        put(Material.WAXED_CUT_COPPER_STAIRS,age.stone);
        put(Material.WAXED_EXPOSED_CHISELED_COPPER,age.stone);
        put(Material.WAXED_EXPOSED_COPPER,age.stone);
        put(Material.WAXED_EXPOSED_COPPER_BULB,age.redstone);
        put(Material.WAXED_EXPOSED_COPPER_DOOR,age.stone);
        put(Material.WAXED_EXPOSED_COPPER_GRATE,age.stone);
        put(Material.WAXED_EXPOSED_COPPER_TRAPDOOR,age.stone);
        put(Material.WAXED_EXPOSED_CUT_COPPER,age.stone);
        put(Material.WAXED_EXPOSED_CUT_COPPER_SLAB,age.stone);
        put(Material.WAXED_EXPOSED_CUT_COPPER_STAIRS,age.stone);
        put(Material.WAXED_OXIDIZED_CHISELED_COPPER,age.stone);
        put(Material.WAXED_OXIDIZED_COPPER,age.stone);
        put(Material.WAXED_OXIDIZED_COPPER_BULB,age.redstone);
        put(Material.WAXED_OXIDIZED_COPPER_DOOR,age.stone);
        put(Material.WAXED_OXIDIZED_COPPER_GRATE,age.stone);
        put(Material.WAXED_OXIDIZED_COPPER_TRAPDOOR,age.stone);
        put(Material.WAXED_OXIDIZED_CUT_COPPER,age.stone);
        put(Material.WAXED_OXIDIZED_CUT_COPPER_SLAB,age.stone);
        put(Material.WAXED_OXIDIZED_CUT_COPPER_STAIRS,age.stone);
        put(Material.WAXED_WEATHERED_CHISELED_COPPER,age.stone);
        put(Material.WAXED_WEATHERED_COPPER,age.stone);
        put(Material.WAXED_WEATHERED_COPPER_BULB,age.redstone);
        put(Material.WAXED_WEATHERED_COPPER_DOOR,age.stone);
        put(Material.WAXED_WEATHERED_COPPER_GRATE,age.stone);
        put(Material.WAXED_WEATHERED_COPPER_TRAPDOOR,age.stone);
        put(Material.WAXED_WEATHERED_CUT_COPPER,age.stone);
        put(Material.WAXED_WEATHERED_CUT_COPPER_SLAB,age.stone);
        put(Material.WAXED_WEATHERED_CUT_COPPER_STAIRS,age.stone);
        put(Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.WEATHERED_CHISELED_COPPER,age.stone);
        put(Material.WEATHERED_COPPER,age.stone);
        put(Material.WEATHERED_COPPER_BULB,age.redstone);
        put(Material.WEATHERED_COPPER_DOOR,age.stone);
        put(Material.WEATHERED_COPPER_GRATE,age.stone);
        put(Material.WEATHERED_COPPER_TRAPDOOR,age.stone);
        put(Material.WEATHERED_CUT_COPPER,age.stone);
        put(Material.WEATHERED_CUT_COPPER_SLAB,age.stone);
        put(Material.WEATHERED_CUT_COPPER_STAIRS,age.stone);
        put(Material.WEEPING_VINES,age.wood);
        put(Material.WEEPING_VINES_PLANT,age.wood);
        put(Material.WET_SPONGE,age.ocean);
        put(Material.WHEAT,age.wood);
        put(Material.WHEAT_SEEDS,age.wood);
        put(Material.WHITE_BANNER,age.wood);
        put(Material.WHITE_BED,age.wood);
        put(Material.WHITE_BUNDLE,age.wood);
        put(Material.WHITE_CANDLE,age.wood);
        put(Material.WHITE_CANDLE_CAKE,age.wood);
        put(Material.WHITE_CARPET,age.wood);
        put(Material.WHITE_CONCRETE,age.wood);
        put(Material.WHITE_CONCRETE_POWDER,age.wood);
        put(Material.WHITE_DYE,age.wood);
        put(Material.WHITE_GLAZED_TERRACOTTA,age.stone);
        put(Material.WHITE_HARNESS,age.wood);
        put(Material.WHITE_SHULKER_BOX,age.postend);
        put(Material.WHITE_STAINED_GLASS,age.wood);
        put(Material.WHITE_STAINED_GLASS_PANE,age.wood);
        put(Material.WHITE_TERRACOTTA,age.stone);
        put(Material.WHITE_TULIP,age.wood);
        put(Material.WHITE_WALL_BANNER,age.wood);
        put(Material.WHITE_WOOL,age.wood);
        put(Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,age.wood);
        put(Material.WILDFLOWERS,age.wood);
        put(Material.WIND_CHARGE,age.trial);
        put(Material.WITCH_SPAWN_EGG,age.wood);
        put(Material.WITHER_ROSE,age.netherite);
        put(Material.WITHER_SKELETON_SKULL,age.netherite);
        put(Material.WITHER_SKELETON_SPAWN_EGG,age.wood);
        put(Material.WITHER_SKELETON_WALL_SKULL,age.netherite);
        put(Material.WITHER_SPAWN_EGG,age.wood);
        put(Material.WOLF_ARMOR,age.stone);
        put(Material.WOLF_SPAWN_EGG,age.wood);
        put(Material.WOODEN_AXE,age.wood);
        put(Material.WOODEN_HOE,age.wood);
        put(Material.WOODEN_PICKAXE,age.wood);
        put(Material.WOODEN_SHOVEL,age.wood);
        put(Material.WOODEN_SWORD,age.wood);
        put(Material.WRITABLE_BOOK,age.wood);
        put(Material.WRITTEN_BOOK,age.wood);
        put(Material.YELLOW_BANNER,age.wood);
        put(Material.YELLOW_BED,age.wood);
        put(Material.YELLOW_BUNDLE,age.wood);
        put(Material.YELLOW_CANDLE,age.wood);
        put(Material.YELLOW_CANDLE_CAKE,age.wood);
        put(Material.YELLOW_CARPET,age.wood);
        put(Material.YELLOW_CONCRETE,age.wood);
        put(Material.YELLOW_CONCRETE_POWDER,age.wood);
        put(Material.YELLOW_DYE,age.wood);
        put(Material.YELLOW_GLAZED_TERRACOTTA,age.stone);
        put(Material.YELLOW_HARNESS,age.wood);
        put(Material.YELLOW_SHULKER_BOX,age.postend);
        put(Material.YELLOW_STAINED_GLASS,age.wood);
        put(Material.YELLOW_STAINED_GLASS_PANE,age.wood);
        put(Material.YELLOW_TERRACOTTA,age.stone);
        put(Material.YELLOW_WALL_BANNER,age.wood);
        put(Material.YELLOW_WOOL,age.wood);
        put(Material.ZOGLIN_SPAWN_EGG,age.wood);
        put(Material.ZOMBIE_HEAD,age.stone);
        put(Material.ZOMBIE_HORSE_SPAWN_EGG,age.wood);
        put(Material.ZOMBIE_SPAWN_EGG,age.wood);
        put(Material.ZOMBIE_VILLAGER_SPAWN_EGG,age.wood);
        put(Material.ZOMBIE_WALL_HEAD,age.stone);
        put(Material.ZOMBIFIED_PIGLIN_SPAWN_EGG,age.wood);

    }};
}
