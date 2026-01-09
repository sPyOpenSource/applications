package cr0s.javara.ai;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import cr0s.javara.ai.Squad.SquadType;
import cr0s.javara.combat.Warhead;
import cr0s.javara.combat.attack.ICanAttack;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.actor.activity.activities.Move;
import cr0s.javara.entity.actor.activity.activities.Wait;
import cr0s.javara.entity.actor.activity.activities.harvester.FindResources;
import cr0s.javara.entity.building.BuildingType;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.EntityBuildingProgress;
import cr0s.javara.entity.building.common.EntityConstructionYard;
import cr0s.javara.entity.vehicle.common.EntityHarvester;
import cr0s.javara.entity.vehicle.common.EntityMcv;

import cr0s.javara.gameplay.Player;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.main.GUI;
import cr0s.javara.order.Order;
import cr0s.javara.perfomance.Profiler;
import cr0s.javara.render.World;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.CellChooser;
import cr0s.javara.util.Pos;

import org.yaml.snakeyaml.Yaml;
import javafx.scene.paint.Color;

public class AIPlayer extends Player {
    private class Enemy {
	public float aggro;

	public Enemy(float aggro) {
	    this.aggro = aggro;
	}
    }

    private boolean enabled;
    private int ticks;
    private final int feedbackTime = 30;
    private int squadSize = 8;

    private ArrayList<Squad> squads = new ArrayList<>();
    private ArrayList<EntityActor> unitsHangingAroundTheBase = new ArrayList<>();
    private ArrayList<EntityActor> activeUnits = new ArrayList<>();
    ArrayList<Player> enemies;

    private final int assignRolesInterval = 20;
    private final int rushInterval = 600;
    private final int attackForceInterval = 30;

    public int structureProductionInactiveDelay = 125;
    public int structureProductionActiveDelay = 10;
    public int minimumDefenseRadius = 5;
    public int maximumDefenseRadius = 20;

    public int newProductionCashThreshold = 5000;
    public int idleBaseUnitsMaximum = 12;
    public int rushAttackScanRadius = 15;
    public int protectUnitScanRadius = 15;

    public int maxBaseRadius = 20;
    public boolean shoudlRepairBuildings = true;

    // Tables
    HashMap<String, Float> unitsToBuild;
    HashMap<String, Float> buildingFractions;
    HashMap<String, String[]> unitCommonNames;
    HashMap<String, String[]> buildingCommonNames;
    HashMap<String, Integer> buildingLimits;

    private Pos defenseCenter;
    private final BaseBuilder bb;
    private final HashMap<Player, Enemy> aggro = new HashMap<>();

    int assignRolesTicks = 0;
    int rushTicks = 0;
    int attackForceTicks = 0;

    public AIPlayer(World w, String name, Alignment side, Color color) {
	super(w, name, side, color);

	loadAIRules(name);

	this.bb = new BaseBuilder(this);
    }

    private void loadAIRules(String name) {
	this.buildingCommonNames = new HashMap<>();
	this.unitCommonNames = new HashMap<>();
	this.buildingLimits = new HashMap<>();
	this.buildingFractions = new HashMap<>();
	this.unitsToBuild = new HashMap<>();

	InputStream input = null;
	try {
	    input = new FileInputStream(new File(ResourceManager.AI_FOLDER + name + ".yaml"));

	    Yaml yaml = new Yaml();
	    Map<String, Object> map = (Map) yaml.load(input);	   

	    // Load buildings common names
	    Map<String, Object> buildingsCommonNamesMap = (Map) map.get("BuildingCommonNames");
	    for (Entry e : buildingsCommonNamesMap.entrySet()) {
		String[] names = ((String) e.getValue()).split(",");

		this.buildingCommonNames.put((String) e.getKey(), names);
	    }

	    // Load units common names
	    Map<String, Object> unitsCommonNamesMap = (Map) map.get("UnitsCommonNames");
	    for (Entry e : unitsCommonNamesMap.entrySet()) {
		String[] names = ((String) e.getValue()).split(",");

		this.buildingCommonNames.put((String) e.getKey(), names);
	    }	 

	    // Load building limits
	    Map<String, Object> buildingLimitsMap = (Map) map.get("BuildingLimits");
	    for (Entry e : buildingLimitsMap.entrySet()) {
		Integer limit = (Integer) e.getValue();

		this.buildingLimits.put((String) e.getKey(), limit);
	    }		    

	    // Load building fractions
	    Map<String, Object> buildingFractionsMap = (Map) map.get("BuildingFractions");
	    for (Entry e : buildingFractionsMap.entrySet()) {
		String percentageStr = (String) e.getValue();
		Float percentageValue = Float.valueOf(percentageStr.replace("%", ""));

		this.buildingFractions.put((String) e.getKey(), percentageValue / 100.0f);
	    }	

	    // Load units building fractions
	    Map<String, Object> unitsToBuildMap = (Map) map.get("UnitsToBuild");
	    for (Entry e : unitsToBuildMap .entrySet()) {
		String percentageStr = (String) e.getValue();
		Float percentageValue = Float.valueOf(percentageStr.replace("%", ""));

		this.unitsToBuild.put((String) e.getKey(), percentageValue / 100.0f);
	    }		    

	    // Load squad size
	    this.squadSize = (Integer) map.get("SquadSize");
	} catch (IOException e) {
	    e.printStackTrace();
	} finally {
	    if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
	}
    }

    @Override
    public void spawn() {
	super.spawn();

	this.defenseCenter = this.getPlayerSpawnPoint();
	this.enabled = true;
    }

    @Override
    public void update() {
	super.update();

	Profiler.getInstance().startForSection("AI");
	if (!this.enabled) {
	    return;
	}

	this.ticks++;

	if (this.ticks == 1) {
	    this.findAndDeployAllMcv();
	}

	if (this.ticks % this.feedbackTime == 0) {
	    this.productUnits();
	}

	this.bb.update();
        assignRolesToIdleUnits();
	Profiler.getInstance().stopForSection("AI");
    }

    public void findAndDeployAllMcv() {
	for (Entity e : entities) {
	    if (e.isDead() || !(e instanceof EntityMcv)) {
		continue;
	    }

	    EntityMcv mcv = (EntityMcv) e;

	    // It's our, deploy
            if (!mcv.canDeploy()) { // cant deploy in current location, choose another
                Pos desiredLocation = this.chooseBuildLocation("fact", false, BuildingType.NORMAL);
                mcv.resolveOrder(new Order("Move", mcv, desiredLocation, true));
            }

            mcv.resolveOrder(new Order("Deploy", mcv, null, true));
	}	
    }

    public int countBuildings(String frac, Player owner) {
	int result = 0;

	for (EntityBuilding b : owner.getBase().getBuildings()) {
	    if (b.owner == owner 
		    && b.getName().equals(frac)) {
		result++;
	    }
	}

	// Also check current building progress
	for (EntityBuildingProgress b : owner.getBase().getCurrentlyBuilding()) {
	    if (b.getTargetBuilding().getName().equals(frac)) {
		result++;
	    }
	}

	return result;
    }

    private int countUnits(String unit, Player owner) {
	int result = 0;

	for (Entity e : GUI.getInstance().getWorld().getEntitiesList()) {
	    if (!(e instanceof EntityActor)) {
		continue;
	    }

	    EntityActor a = (EntityActor) e;

	    if (a.owner == owner && a.getName().equals(unit)) {
		result++;
	    }
	}

	return result;
    }    

    private Integer countBuildingByCommonName(String commonName, Player owner) {
	if (!this.buildingCommonNames.containsKey(commonName)) {
	    return null;
	}

	int result = 0;

	for (EntityBuilding b : owner.getBase().getBuildings()) {
	    if (b.owner == owner) {
		for (String name : this.buildingCommonNames.get(commonName)) {
		    if (name.equals(b.getName())) {
			result++;
		    }
		}
	    }
	}

	return result;
    }

    private boolean hasAdequateFact() {
	return (this.getBase().isAlliedCYPresent || this.getBase().isSovietCYPresent) 
		|| (!this.getBase().isAlliedWarFactoryPresent && !this.getBase().isSovietWarFactoryPresent);
    }

    public boolean hasAdequateProc() {
	return this.countBuildingByCommonName("Refinery", this) > 0 ||
		this.countBuildingByCommonName("Power", this) == 0;
    }

    public boolean hasMinimumProc() {
	return this.countBuildingByCommonName("Refinery", this) >= 2 ||
		this.countBuildingByCommonName("Power", this) == 0 ||
		this.countBuildingByCommonName("Barracks", this) == 0;
    }

    public boolean hasAdequateAirUnits(EntityActor a) {
	// TODO: finish it after air units being added
	return true;
    }

    private Pos findPosForBuilding(String actorType, final Pos center, final Pos target, final int minRange, final int maxRange, final boolean distanceToBaseIsImportant) {
	ArrayList<Pos> cells = world.chooseTilesInAnnulus(center, minRange, maxRange);

	if (!center.equals(target)) {
	    Collections.sort(cells, new Comparator<>() {

		@Override
		public int compare(Pos p1, Pos p2) {
		    return p1.distanceToSq(target) - p2.distanceToSq(target);
		}

	    });
	} else {
	    // Shuffling is needed to make sure bot place building in random location at the base
	    Collections.shuffle(cells, World.random);
	}

	EntityActor b = this.getBase().getProductionQueue().getBuildableActorByName(actorType);
	if (b == null || !(b instanceof EntityBuilding)) {
	    return null;
	}

	for (Pos cell : cells) {
	    if (!this.getBase().isPossibleToBuildHere(cell, (EntityBuilding) b)) {
		continue;
	    }

	    if (distanceToBaseIsImportant && !this.getBase().checkBuildingDistance(cell, false)) {
		continue;
	    }

	    return cell;
	}

	return null;
    }

    public EntityActor findClosestEnemy(final Pos center) {
        enemies = new ArrayList<>();
	for (Player p : world.getPlayers()) {
	    if (p.isEnemyFor(this)) {
		enemies.add(p);
	    }
	}
	return this.findClosestEnemy(center, enemies.get(0));
    }

    private EntityActor findClosestEnemy(final Pos center, Player p) {
	EntityActor closest = null;
        for(Player player : world.getPlayers()){
            // If we looking for specified owner
	    if (player != p) {
		continue;
	    }
            
            for (Entity e : player.entities) { 
                if (e.isDead() || !(e instanceof EntityActor) /*|| (!e.owner.isEnemyFor(this))*/) {
                    continue;
                }

                EntityActor a = (EntityActor) e;

                if (closest == null 
                        || a.getPosition().Clone().distanceToSq(center) < closest.getPosition().Clone().distanceToSq(center)) {
                    closest = a;
                }
            }
        }

	return closest;
    }

    public Pos chooseBuildLocation(String actorType, boolean distanceToBaseIsImportant, BuildingType type) {
	switch (type) {
            case DEFENSIVE:
                if (World.random.nextInt(100) >= 30) { // In ~70% of cases we build defensive structures as close as possible to the enemy
                    EntityActor closestEnemy = this.findClosestEnemy(this.defenseCenter);
                    Pos targetCell = (closestEnemy != null) ? closestEnemy.getCellPosition() : this.getPlayerSpawnPoint();

                    /*if (closestEnemy != null) {
                        System.out.println("[AI] Closest enemy: " + closestEnemy.getClass().getSimpleName() + " | owner: " + closestEnemy.owner.name);
                    }*/
                    return this.findPosForBuilding(actorType, this.defenseCenter, targetCell, this.minimumDefenseRadius, this.maximumDefenseRadius, distanceToBaseIsImportant);
                } else { // In other cases place build somewhere in base
                    return this.findPosForBuilding(actorType, this.getPlayerSpawnPoint(), this.getPlayerSpawnPoint(), 0, this.maxBaseRadius, false);
                }

            case NORMAL:
                return this.findPosForBuilding(actorType, this.getPlayerSpawnPoint(), this.getPlayerSpawnPoint(), 0, distanceToBaseIsImportant ? this.maxBaseRadius : world.MAX_RANGE, distanceToBaseIsImportant);

            case REFERENCES:
                // Choose set of cells with resources nearby the base inside buildable area
                ArrayList<Pos> resourceTiles = world.chooseTilesInCircle(this.getPlayerSpawnPoint(), this.maxBaseRadius, new CellChooser() {
                    @Override
                    public boolean isCellChoosable(Pos cellPos) {
                        return !world.getMap().getResourcesLayer().isCellEmpty(cellPos);
                    }
                });

                for (Pos c : resourceTiles) {
                    Pos found = this.findPosForBuilding(actorType, c, this.getPlayerSpawnPoint(), 0, this.maxBaseRadius, false);

                    if (found != null) {
                        return found;
                    }
                }

                // Try to find a free spot somewhere else in the base
                return this.findPosForBuilding(actorType, this.getPlayerSpawnPoint(), this.getPlayerSpawnPoint(), 0, this.maxBaseRadius, false);

            default:
                break;
	}

	return null;
    }

    @Override
    public void notifyDamaged(Entity who, EntityActor by, int amount, Warhead warhead) {
	if (!who.isDead() && who.owner == this && by != null && amount > 0 && warhead != null) {
	    if (who instanceof EntityBuilding entityBuilding) { // Defense our buildings
		if (who instanceof EntityBuildingProgress entityBuildingProgress) {
		    who = entityBuildingProgress.getTargetBuilding();
		}

		this.defenseCenter = entityBuilding.getCellPosition();

		// Try to repair building
		if (this.shoudlRepairBuildings) {
		    this.getBase().repairBuilding(entityBuilding);
		}
	    }

	    if (amount > 0) {
		if (this.aggro.containsKey(by.owner)) {
		    Enemy e = new Enemy(this.aggro.get(by.owner).aggro + amount);
		    this.aggro.remove(by.owner);
		    this.aggro.put(by.owner, e);
		} else {
		    this.aggro.put(by.owner, new Enemy(amount));
		}
	    }
	    
	    // Protect our harvesters and buildings
	    if ((who instanceof EntityHarvester) || (who instanceof EntityBuilding) 
		    && by.owner.isEnemyFor(this)) {
		this.defenseCenter = ((EntityActor) who).getCellPosition();
		this.protectOwn(by);
	    }
	}
    }

    private EntityActor chooseEnemyTarget() {
	//if (this.winState != WinState.UNDEFINED) {
	//	return null;
	//}

	ArrayList<Player> players = world.getPlayers();
	enemies = new ArrayList<>();
	for (Player p : players) {
	    if (p.isEnemyFor(this)) {
		enemies.add(p);
	    }
	}

	Collections.sort(enemies, new Comparator<>() {
	    @Override
	    public int compare(Player p1, Player p2) {
		if (AIPlayer.this.aggro.containsKey(p1) && AIPlayer.this.aggro.containsKey(p2)) {
		    return (int) (AIPlayer.this.aggro.get(p1).aggro - AIPlayer.this.aggro.get(p2).aggro);
		}

		return 0;
	    }
	});

	// Pick someting to attack owned by that player
	Player enemy = enemies.get(0);
	EntityActor target = this.findClosestEnemy(this.getPlayerSpawnPoint(), enemy);

	if (target == null) {
	    // Assume that enemy has nothing, cool off on attacks
	    if (this.aggro.containsKey(enemy)) {
		float aggroVal = this.aggro.get(enemy).aggro;
		this.aggro.remove(enemy);

		aggroVal = aggroVal / 2 - 1;
		this.aggro.put(enemy, new Enemy(aggroVal));
	    }
	}

	// Bump that aggro to avoid changing our mind
	if (enemies.size() > 1) {
	    if (this.aggro.containsKey(enemy)) {
		float aggroVal = this.aggro.get(enemy).aggro;
		this.aggro.remove(enemy);
		this.aggro.put(enemy, new Enemy(++aggroVal));
	    }	    
	}

	return target;
    }

    private ArrayList<EntityBuilding> findEnemyConstructionYards() {
	ArrayList<EntityBuilding> result = new ArrayList<>();

	for (Entity e : GUI.getInstance().getWorld().getEntitiesList()) { 
	    if (e.isDead() || !(e instanceof EntityConstructionYard) || (e.owner.isEnemyFor(this))) {
		continue;
	    }

	    result.add((EntityBuilding) e);
	}	

	return result;
    }

    void cleanSquads() {
	ArrayList<Squad> newSquads = new ArrayList<>();
	for (Squad s : this.squads) {
	    if (s.isValid()) {
		newSquads.add(s);
	    }
	}

	this.squads = newSquads;
	for (Squad s : this.squads) {
	    s.removeDeadAndNotOwnUnits();
	}
    }

    Squad getSquadOfType(SquadType type) {
	for (Squad s : this.squads) {
	    if (s.getType() == type) {
		return s;
	    }
	}

	return null;
    }

    Squad registerNewSquad(SquadType type) {
	return this.registerNewSquad(type, null);
    }

    Squad registerNewSquad(SquadType type, EntityActor target) {
	Squad s = new Squad(this, type, target);
	squads.add(s);

	return s;
    }

    private ArrayList<EntityActor> cleanFromDeadAndNotOwn(ArrayList<EntityActor> list) {
	ArrayList<EntityActor> newList = new ArrayList<>();
	for (EntityActor a : list) {
	    if (!a.isDead() && a.owner == this) {
		newList.add(a);
	    }
	}

	return newList;
    }

    void assignRolesToIdleUnits() {
	//this.cleanSquads();
	this.activeUnits = cleanFromDeadAndNotOwn(this.activeUnits);
	this.unitsHangingAroundTheBase = cleanFromDeadAndNotOwn(this.unitsHangingAroundTheBase);

	if (--this.rushTicks <= 0) {
	    this.rushTicks = this.rushInterval;

	    this.tryToRushAttack();
	}

	if (--this.attackForceTicks <= 0) {
	    this.attackForceTicks = this.attackForceInterval;

	    for (Squad s : this.squads) {
		s.update();
	    }
	}

	if (--this.assignRolesTicks > 0) {
	    return;
	}

	this.assignRolesTicks = this.assignRolesInterval;

	this.giveOrdersToIdleHarvesters();
	this.findNewUnits();
	this.createAttackForce();
	this.findAndDeployAllMcv();
    }

    private void createAttackForce() {
	int randomSquadSize = this.squadSize + World.random.nextInt(30);

	if (this.unitsHangingAroundTheBase.size() >= randomSquadSize) {
	    Squad attackForce = this.registerNewSquad(SquadType.ASSAULT);

	    for (EntityActor a : this.unitsHangingAroundTheBase) {
		if (a instanceof ICanAttack) { // TODO: check is not aircraft
		    attackForce.addUnit(a);
		}
	    }

	    this.unitsHangingAroundTheBase.clear();
	}
    }

    private void findNewUnits() {
	ArrayList<EntityActor> newUnits = new ArrayList<>();

	for (Entity e : entities) {
	    if (e.isDead() || !(e instanceof MobileEntity)) {
		continue;
	    }

	    EntityActor a = (EntityActor) e;
	    if (!this.activeUnits.contains(a)) {
		newUnits.add(a);
	    }
	}

	for (EntityActor a : newUnits) {
	    if (a instanceof EntityHarvester) {
		a.resolveOrder(new Order("Harvest", a, null, false));
	    } else {
		this.unitsHangingAroundTheBase.add(a);
	    }

	    // TODO: register squad for aircraft

	    this.activeUnits.add(a);
	}
    }

    private void giveOrdersToIdleHarvesters() {
	for (EntityActor a : this.activeUnits) {
	    if (!(a instanceof EntityHarvester)) {
		continue;
	    }

	    EntityHarvester harv = (EntityHarvester) a;

	    if (!harv.isIdle()) {
		Activity act = a.getCurrentActivity();

		// Skip working harvester
		if (!(act instanceof Wait)
			&& (act.nextActivity == null || !(act.nextActivity instanceof FindResources))) {
		    continue;
		}
	    }

	    if (!harv.isEmpty()) {
		continue;
	    }

	    harv.resolveOrder(new Order("Harvest", harv, null, false));
	}
    }

    private void tryToRushAttack() {
        EntityActor a = chooseEnemyTarget();
        if (squads.isEmpty()) return;
        Squad s = squads.get(0);
        s.setTarget(a);
        for(EntityActor b:s.getUnits()) {
            if(World.random.nextBoolean()) continue;
            if(World.random.nextBoolean()) continue;
            b.queueActivity(new Move((MobileEntity)b, a.getCellPosition()));
        }
    }

    void protectOwn(EntityActor attacker) {
	Squad protectSquad = this.getSquadOfType(SquadType.PROTECTION);

	if (protectSquad == null) {
	    protectSquad = this.registerNewSquad(SquadType.PROTECTION, attacker);
	}

	if (!protectSquad.targetIsValid()) {
	    protectSquad.setTarget(attacker);
	}

	// Call to arms for units around
	if (!protectSquad.isValid()) {
	    ArrayList<EntityActor> actors = GUI.getInstance().getWorld().getActorsInCircle(this.getPlayerSpawnPoint(), this.protectUnitScanRadius);

	    for (EntityActor a : actors) {
		if (!a.isDead() 
			&& a.owner == this 
			&& !(a instanceof EntityBuilding)
			&& (a instanceof ICanAttack)) {
		    protectSquad.addUnit(a);
		}
	    }
	}
    }

    void productUnits() {
	// Stop building until economy is restored
	if (!this.hasAdequateProc()) {
	    return;
	}

	// If we have no Construction Yards, then build a new MCV
	if (!this.hasAdequateFact() && thereIsNoMcv()) {
	    this.buildVehicle("mcv");
	}

	this.buidUnits(this.unitsHangingAroundTheBase.size() < this.idleBaseUnitsMaximum);
    }

    private void buidUnits(boolean buildRandom) {
	// Randomly choose units category
	int i = World.random.nextInt(2);
	
	ArrayList<String> buildables = new ArrayList<>();

	switch (i) {
	case 0: // Infantry
	    if (this.getBase().getProductionQueue().getCurrentInfantryProduction().isBuilding() && !this.getBase().getProductionQueue().getCurrentInfantryProduction().isReady()) {
		return;
	    }
	    
	    for (String name : this.getBase().getProductionQueue().getBuildables().keySet()) {
		if (this.getBase().isTentPresent && this.getBase().getProductionQueue().alliedInfantry.containsKey(name)) {
		    buildables.add(name);
		} else if (this.getBase().isBarracksPresent && this.getBase().getProductionQueue().sovietInfantry.containsKey(name)) {
		    buildables.add(name);
		}
	    }

	    break;

	case 1: // Vehicles
	    if (this.getBase().getProductionQueue().getCurrentVehicleProduction().isBuilding() && !this.getBase().getProductionQueue().getCurrentVehicleProduction().isReady()) {
		return;
	    }
	    
	    for (String name : this.getBase().getProductionQueue().getBuildables().keySet()) {
		if (this.getBase().isAlliedWarFactoryPresent && this.getBase().getProductionQueue().alliedVehicles.containsKey(name)) {
		    buildables.add(name);
		} else if (this.getBase().isSovietWarFactoryPresent && this.getBase().getProductionQueue().sovietVehicles.containsKey(name)) {
		    buildables.add(name);
		} else if ((this.getBase().isAlliedWarFactoryPresent || this.getBase().isSovietWarFactoryPresent)
			&& this.getBase().getProductionQueue().neutralVehicles.containsKey(name)) {
		    
		}
	    }

	    break;	    

	case 2: // TODO: Naval units
	    break;

	case 3: // TODO: Air units
	    break;
	}

	// TODO: add non-random build
	if (!buildables.isEmpty()) {
	    String randomName = buildables.get(World.random.nextInt(buildables.size()));
	    System.out.println(base.getCash());
	    //System.out.println("[AI] Building unit: " + randomName);
	    this.getBase().getProductionQueue().startBuildingActor(this.getBase().getProductionQueue().getBuildables().get(randomName), null);
	}
    }

    private void buildVehicle(String name) {
	if (this.getBase().getProductionQueue().isBuildable(name)
		&& (!this.getBase().getProductionQueue().getCurrentVehicleProduction().isBuilding()
			|| this.getBase().getProductionQueue().getCurrentVehicleProduction().isReady())) {

	    this.getBase().getProductionQueue().startBuildingActor(this.getBase().getProductionQueue().getBuildableActorByName(name), null);
	}
    }

    public void buildInfantry(String name) {
	if (this.getBase().getProductionQueue().isBuildable(name)
		&& (!this.getBase().getProductionQueue().getCurrentInfantryProduction().isBuilding()
			|| this.getBase().getProductionQueue().getCurrentInfantryProduction().isReady())) {

	    this.getBase().getProductionQueue().startBuildingActor(this.getBase().getProductionQueue().getBuildableActorByName(name), null);
	}	
    }

    private boolean thereIsNoMcv() {
	for (Entity e : GUI.getInstance().getWorld().getEntitiesList()) {
	    if (e.isDead() || !(e instanceof EntityMcv)) {
		continue;
	    }

	    EntityMcv mcv = (EntityMcv) e;

	    // It's our
	    if (mcv.owner == this) {
		return false;
	    }
	}

	// No any our MCV is found
	return true;
    }
}
