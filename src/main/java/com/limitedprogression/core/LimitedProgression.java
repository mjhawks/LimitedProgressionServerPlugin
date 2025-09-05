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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
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
import com.limitedprogression.core.LimitedDictionaries;

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
            if(!canCraftItem(r.getResult().getType())){
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
                if(!canAcquireItem(d.getEquipment().getItemInMainHand().getType())){
                    d.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
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
                if(!canAcquireItem(z.getEquipment().getItemInMainHand().getType())){
                    z.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(!canAcquireItem(piece.getType())){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.ZOMBIE_VILLAGER){
                ZombieVillager z = (ZombieVillager) entity;
                ItemStack[] armor = z.getEquipment().getArmorContents();
                if(!canAcquireItem(z.getEquipment().getItemInMainHand().getType())){
                    z.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(!canAcquireItem(piece.getType())){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.HUSK){
                Husk h = (Husk)entity;
                ItemStack[] armor = h.getEquipment().getArmorContents();
                if(!canAcquireItem(h.getEquipment().getItemInMainHand().getType())){
                    h.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(!canAcquireItem(piece.getType())){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.STRAY){
                Stray s = (Stray)entity;
                ItemStack[] armor = s.getEquipment().getArmorContents();
                if(!canAcquireItem(s.getEquipment().getItemInMainHand().getType())){
                    s.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(!canAcquireItem(piece.getType())){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.BOGGED){
                Bogged b = (Bogged) entity;
                ItemStack[] armor = b.getEquipment().getArmorContents();
                if(!canAcquireItem(b.getEquipment().getItemInMainHand().getType())){
                    b.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(!canAcquireItem(piece.getType())){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.SKELETON){
                Skeleton s = (Skeleton) entity;
                ItemStack[] armor = s.getEquipment().getArmorContents();
                if(!canAcquireItem(s.getEquipment().getItemInMainHand().getType())){
                    s.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                }
                for(ItemStack piece:armor){
                    if(!canAcquireItem(piece.getType())){
                        logger.info(piece.getType().name()+" is not allowed... cancelled");
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            else if(et == EntityType.FOX){
                Fox f = (Fox) entity;
                if(!canAcquireItem(f.getEquipment().getItemInMainHand().getType())){
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
        if(!canMineBlock(e.getBlock().getType())){
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
    @EventHandler(priority = EventPriority.LOW)
    private void onPlayerEntityInteract (PlayerInteractEntityEvent e){
        if(e.getRightClicked() instanceof NPC & (ageLookup.get(age.trade)>ageLookup.get(CurrentAge))){
            e.setCancelled(true);
        }
        if(e.getRightClicked() instanceof Tameable & (ageLookup.get(age.stone)>ageLookup.get(CurrentAge))){
            e.setCancelled(true);
        }
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
        if(!canAcquireItem(s.getEntity().getItemStack().getType())){
            logger.info("stopped"+s.getEntity().getItemStack().getType()+ " from spawning");
            s.setCancelled(true);
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        Material invitem = e.getCurrentItem().getType();
        if(!canAcquireItem(invitem)){
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void onItemMoveInInventory(InventoryMoveItemEvent e){
        Material invitem = e.getItem().getType();
        if(!canAcquireItem(invitem)){
            e.setCancelled(true);
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
    private boolean canMineBlock(Material m){
        if(ageLookup.get(LimitedDictionaries.miningDict.get(m))<=ageLookup.get(CurrentAge)){
            return true;
        }
        else return false;
    }

    private boolean canAcquireItem(Material m){
        if(ageLookup.get(LimitedDictionaries.lootingDict.get(m))<=ageLookup.get(CurrentAge)){
            return true;
        }
        else return false;
    }

    private boolean canCraftItem(Material m){
        if(ageLookup.get(LimitedDictionaries.craftingDict.get(m))<=ageLookup.get(CurrentAge)){
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
}
