package cr0s.javara.gameplay;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import cr0s.javara.entity.IDefense;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.EntityBuildingProgress;
import cr0s.javara.entity.building.IOreCapacitor;
import cr0s.javara.entity.building.IPowerConsumer;
import cr0s.javara.entity.building.IPowerProducer;
import cr0s.javara.entity.building.common.EntityConstructionYard;
import cr0s.javara.entity.building.common.EntityRadarDome;
import cr0s.javara.entity.building.common.EntityWall;
import cr0s.javara.entity.building.common.EntityWarFactory;
import cr0s.javara.entity.building.soviet.EntityBarracks;
import cr0s.javara.entity.infantry.EntityInfantry;
import cr0s.javara.entity.vehicle.EntityVehicle;

import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.main.GUI;
import cr0s.javara.render.map.TileSet;
import cr0s.javara.resources.SoundManager;
import cr0s.javara.ui.sbpages.SideBarItemsButton;
import cr0s.javara.util.Pos;

/**
 * Describes player's base.
 * @author Cr0s
 */
public class Base {

    public static final int BUILDING_CY_RANGE = 20;
    public static final int BUILDING_NEAREST_BUILDING_DISTANCE = 5;

    public boolean isSovietCYPresent = false;
    public boolean isAlliedCYPresent = false;

    public boolean isBarracksPresent = false;
    public boolean isTentPresent = false;

    public boolean isSovietWarFactoryPresent = false;
    public boolean isAlliedWarFactoryPresent = false;

    public boolean isSubPenPresent = false;
    public boolean isShipYardPresent = false;
    public boolean isHelipadPresent = false;
    public boolean isAirLinePresent = false;

    public boolean isChronoSpherePresent = false;
    public boolean isNukeSiloPresent = false;
    public boolean isIronCurtainPresent = false;

    public boolean isSovietTechPresent = false;
    public boolean isAlliedTechPresent = false;

    public boolean isRadarDomePresent = false;
    public boolean isPowerPlantPresent = false;
    public boolean isFlameTowerPresent = false;

    public boolean isProcPresent = false;
    public boolean isAnySuperPowerPresent;    
    public int oreCapacity, ore, displayOre;

    private boolean isLowPower = false;
    private int powerLevel = 0;
    private int powerConsumptionLevel = 0;

    private final ProductionQueue queue;
    private int cash;
    private int displayCash;

    private final int TICKS_WAIT_CASH = 2;
    private int ticksWaitCash = 0;

    private final int TICKS_WAIT_ORE = 2;
    private int ticksWaitOre = 0;    
    private final Player owner;

    private final float DISPLAY_FRAC_CASH_PER_TICK = 0.07f;
    private final int DISPLAY_CASH_DELTA_PER_TICK = 37;

    private final HashSet<Class> buildingClasses = new HashSet<>();
    private final ArrayList<EntityBuildingProgress> currentlyBuilding = new ArrayList<>();
    private final ArrayList<EntityBuilding> buildings = new ArrayList<>();

    public Base(Team team, Player aOwner) {
	this.owner = aOwner;

	this.queue = new ProductionQueue(this.owner);
    }

    public void update() {
	updateBuildings();
	this.queue.update();

	this.updateDisplayedCash();
    }

    public void updateDisplayedCash() {
	if (this.ticksWaitCash > 0) {
	    this.ticksWaitCash--;
	}

	if (this.ticksWaitOre > 0) {
	    this.ticksWaitOre--;
	}	

	// For cash
	int diff = Math.abs(this.cash - this.displayCash);
	int move = Math.min(Math.max((int)(diff * DISPLAY_FRAC_CASH_PER_TICK), DISPLAY_CASH_DELTA_PER_TICK), diff);


	if (this.displayCash < this.cash)
	{
	    this.displayCash += move;

	    if (this.owner == GUI.getInstance().getPlayer()) {
		SoundManager.getInstance().playSfxGlobal("cashup1", 0.8f);
	    }
	}
	else if (this.displayCash > this.cash)
	{
	    this.displayCash -= move;
	    if (this.ticksWaitCash == 0) { 
		if (this.owner == GUI.getInstance().getPlayer()) {
		    SoundManager.getInstance().playSfxGlobal("cashdn1", 0.8f);
		}

		this.ticksWaitCash = TICKS_WAIT_CASH;

		this.displayCash = this.cash;
	    }
	}

	// For ore
	diff = Math.abs(this.ore - this.displayOre);
	move = Math.min(Math.max((int) (diff * DISPLAY_FRAC_CASH_PER_TICK), DISPLAY_CASH_DELTA_PER_TICK), diff);

	if (this.displayOre < this.ore)
	{
	    this.displayOre += move;

	    if (this.owner == GUI.getInstance().getPlayer()) {
		SoundManager.getInstance().playSfxGlobal("cashup1", 0.8f);
	    }
	}
	else if (this.displayOre > this.ore)
	{
	    this.displayOre -= move;

	    if (this.ticksWaitOre == 0) { 
		if (this.owner == GUI.getInstance().getPlayer()) {
		    SoundManager.getInstance().playSfxGlobal("cashdn1", 0.8f);
		}

		this.ticksWaitOre = TICKS_WAIT_CASH;

		this.displayOre = this.ore;
	    }
	}	
    }

    private void updateBuildings() {
	isSovietCYPresent = isAlliedCYPresent = false;
	isRadarDomePresent = false;
	isProcPresent = false;

	this.oreCapacity = 0;
	this.powerConsumptionLevel = this.powerLevel = 0;

	this.buildingClasses.clear();

	for (EntityBuilding b : this.buildings) {
	    if (!this.buildingClasses.contains(b.getClass())) {
		this.buildingClasses.add(b.getClass());
	    }

	    // Update power levels
	    if (b instanceof IPowerConsumer iPowerConsumer) {
		this.powerConsumptionLevel += iPowerConsumer.getConsumptionLevel();
	    } else if (b instanceof IPowerProducer iPowerProducer) {
		this.powerLevel += iPowerProducer.getPowerProductionLevel();
	    }	    

	    if (b instanceof EntityConstructionYard entityConstructionYard) {
		if (entityConstructionYard.getAlignment() == Alignment.ALLIED) {
		    this.isAlliedCYPresent = true;
		} else if (entityConstructionYard.getAlignment() == Alignment.SOVIET) {
		    this.isSovietCYPresent = true;
		}
	    } else if (b instanceof EntityBarracks) {
		this.isBarracksPresent = true;
		//} else if (b instanceof EntityTent) {
		//this.isTentPresent = true;
	    } else if (b instanceof EntityWarFactory entityWarFactory) {
		if (entityWarFactory.getAlignment() == Alignment.ALLIED) {
		    this.isAlliedWarFactoryPresent = true;
		} else if (entityWarFactory.getAlignment() == Alignment.SOVIET) {
		    this.isSovietWarFactoryPresent = true;
		}
	    } else if (b instanceof EntityRadarDome) {
		this.isRadarDomePresent = true;
	    }

	    if (b instanceof IOreCapacitor iOreCapacitor) {
		this.oreCapacity += iOreCapacitor.getOreCapacityValue();
	    }
	}

	this.isLowPower = this.powerConsumptionLevel > this.powerLevel;
    }

    public boolean isLowPower() {
	return this.isLowPower;
    }

    public int getPowerLevel() {
	return this.powerLevel;
    }

    public int getConsumptionLevel() {
	return this.powerConsumptionLevel;
    }

    public void addBuilding(EntityBuilding building) {
	this.buildings.add(building);

	if (building instanceof EntityWarFactory) {
	    building.setPrimary(!isMoreThanOneWarFactory());
	} else if (building instanceof EntityBarracks /*|| building instanceof EntityTent*/) {
	    building.setPrimary(!isMoreThanOneTentOrBarrack());
	}
    }

    public void removeBuilding(EntityBuilding building) {
	this.buildings.remove(building);
    }

    public boolean isPossibleToBuildHere(Pos cell, EntityBuilding targetBuilding) {
	for (int bX = 0; bX < targetBuilding.getWidthInTiles(); bX++) {
	    for (int bY = 0; bY < targetBuilding.getHeightInTiles(); bY++) {
		if (targetBuilding.getBlockingCells()[bX][bY] != TileSet.SURFACE_CLEAR_ID) {
		    if (!owner.world.isCellBuildable(new Pos(cell.getCellX() + bX , cell.getCellY() + bY))) {
			return false;
		    }
		}
	    }
	}

	return true;
    }

    public boolean tryToBuild(Pos cell,
	    EntityBuilding targetBuilding) {
	if (!isPossibleToBuildHere(cell, targetBuilding)) {
	    return false;
	}

	if (!this.queue.canBuild(targetBuilding)) {
	    return false;
	}

	if (checkBuildingDistance(cell, targetBuilding instanceof EntityWall)) {
	    EntityBuilding b = (EntityBuilding) targetBuilding.newInstance();
	    b.changeCellPos(cell);

	    EntityBuilding ebp = owner.world.addBuildingTo(b);
            ebp.owner = owner;
            owner.entities.add(ebp);
	    queue.getProductionForBuilding(targetBuilding).deployCurrentActor();

	    return true;
	} else {
	    return false;
	}
    }

    public boolean checkBuildingDistance(Pos cell, boolean isWall) {
	// Find minimal distance to all construction yards
	double minDistanceToCYSq = 0;
	double minDistanceToOtherBuildingsSq = 0;

	for (EntityBuilding eb : this.buildings) {
	    // Defense structures don't gives buildable area, so skip it
	    if (eb instanceof IDefense) {
		continue;
	    }

	    double dx = eb.boundingBox.getX() / 24 - cell.getCellX();
	    double dy = eb.boundingBox.getY() / 24 - cell.getCellY();
	    double distanceSq = dx * dx + dy * dy;

	    if (eb instanceof EntityConstructionYard) {
		if (minDistanceToCYSq == 0 || distanceSq < minDistanceToCYSq) {
		    minDistanceToCYSq = distanceSq;
		}
	    }

	    if (minDistanceToOtherBuildingsSq == 0 || distanceSq < minDistanceToOtherBuildingsSq) {
		minDistanceToOtherBuildingsSq = distanceSq;
	    }
	}

	return minDistanceToCYSq <= (Base.BUILDING_CY_RANGE * Base.BUILDING_CY_RANGE) 
		&& (isWall || minDistanceToOtherBuildingsSq <= (Base.BUILDING_NEAREST_BUILDING_DISTANCE * Base.BUILDING_NEAREST_BUILDING_DISTANCE));
    }

    public ArrayList<EntityBuilding> getBuildings() {
	return this.buildings;
    }

    public void deployBuildedVehicle(EntityVehicle v) {
	EntityWarFactory ewf = getPrimaryWarFactory();
	
	if (ewf != null) {
	    ewf.deployEntity(EntityVehicle.newInstance(v));
	}
    }

    public EntityBuilding getPrimaryBarrackOrTent() {
	for (EntityBuilding b : this.buildings) {
	    if (b instanceof EntityBarracks /*|| b instanceof EntityTent*/) {
		if (b.isPrimary()) {
		    return b;
		}
	    }
	}	

	return null;
    }

    public void setPrimaryTentOrBarrack(EntityBuilding b) {
	for (EntityBuilding eb : this.buildings) {
	    if (eb instanceof EntityBarracks/* || eb instanceof EntityTent*/) {
		eb.setPrimary(eb == b);
	    }
	}
    }

    public boolean isMoreThanOneTentOrBarrack() {
	int count = 0;

	for (EntityBuilding eb : this.buildings) {
	    if (eb instanceof EntityBarracks/* || eb instanceof EntityTent*/) {
		count++;
		if (count > 1) {
		    return true;
		}
	    }
	}	

	return false;
    }

    public void deployTrainedInfantry(EntityInfantry i) {
	EntityBuilding b = getPrimaryBarrackOrTent();
	if (b != null) {
	    if (b instanceof EntityBarracks entityBarracks) {
                EntityActor x = i.newInstance();
		entityBarracks.deployEntity(x);
                owner.entities.add(x);
	    }/* else if (b instanceof EntityTent) {
		((EntityTent) b).deployEntity(i.newInstance());
	    }*/
	}
    }

    public EntityWarFactory getPrimaryWarFactory() {
	for (EntityBuilding b : this.buildings) {
	    if (b instanceof EntityWarFactory entityWarFactory) {
		if (b.isPrimary()) {
		    return entityWarFactory;
		}
	    }
	}	

	return null;
    }

    public void setPrimaryWarFactory(EntityWarFactory entityWarFactory) {
	for (EntityBuilding eb : this.buildings) {
	    if (eb instanceof EntityWarFactory) {
		eb.setPrimary(eb == entityWarFactory);
	    }
	}
    }

    public boolean isMoreThanOneWarFactory() {
	int count = 0;

	for (EntityBuilding eb : this.buildings) {
	    if (eb instanceof EntityWarFactory) {
		count++;
		if (count > 1) {
		    return true;
		}
	    }
	}	

	return false;
    }


    public void giveOre(int aCapacity) {
	if (this.ore + aCapacity > 0.8f * this.oreCapacity) {
	    if (this.owner == GUI.getInstance().getPlayer()) {
		// "Silos needed"
		SoundManager.getInstance().playSpeechSoundGlobal("silond1");
	    }
	}

	if (this.ore + aCapacity > this.oreCapacity) {
	    return; // don't accept exceeding ore
	}

	int overflow = 0;
	if (this.ore + aCapacity > this.oreCapacity) {
	    overflow = this.ore + aCapacity - this.oreCapacity;
	}

	this.ore += aCapacity - overflow;
    }

    public void takeOre(int value) {
	this.ore -= value;

	if (this.ore < 0) {
	    this.ore = 0;
	}
    }

    public void gainCash(int amount) {
	this.cash += amount;
    }

    public boolean takeCash(int amount) {
	if (this.cash + this.ore < amount) {
	    return false;
	}

	// Spent ore first
	this.ore -= amount;
	if (this.ore < 0) { // we spent all ore
	    this.cash += this.ore; // spent cash
	    this.ore = 0;
	}

	return true;
    }

    public ProductionQueue getProductionQueue() {
	return this.queue;
    }

    public void productButtonItem(SideBarItemsButton texture) {
	EntityActor target = queue.getBuildableActor(texture);
	queue.startBuildingActor(target, texture);
    }

    public int getCash() {
	return this.cash;
    }

    public int getDisplayCash() {
	return this.displayCash;
    }

    public int getDisplayOre() {
	return this.displayOre;
    }    

    public HashSet<Class> getBuildingClasses() {
	return this.buildingClasses;
    }

    public boolean tryToBuildWalls(LinkedList<Pos> currentWallsList, EntityBuilding targetBuilding) {
	for (Pos cell : currentWallsList) {
	    if (!isPossibleToBuildHere(cell, targetBuilding)) {
		return false;
	    }

	    if (!this.queue.canBuild(targetBuilding)) {
		return false;
	    }

	    if (checkBuildingDistance(cell, true)) {
		EntityBuilding b = (EntityBuilding) targetBuilding.newInstance();
		b.changeCellPos(cell);

		GUI.getInstance().getWorld().addBuildingTo(b);

		queue.getProductionForBuilding(targetBuilding).deployCurrentActor();
	    } else {
		return false;
	    }
	}

	return true;
    }

    public boolean isSilosNeeded() {
	return this.ore > 0.9f * this.oreCapacity;
    }

    public void addToCurrentlyBuilding(
	    EntityBuildingProgress entityBuildingProgress) {
	this.currentlyBuilding.add(entityBuildingProgress);
    }

    public ArrayList<EntityBuildingProgress> getCurrentlyBuilding() {
	return this.currentlyBuilding;
    }

    public void repairBuilding(EntityBuilding entityBuilding) {
	entityBuilding.setRepairing(true);
    }
}
