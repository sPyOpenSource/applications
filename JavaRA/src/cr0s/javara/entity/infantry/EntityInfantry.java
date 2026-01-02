package cr0s.javara.entity.infantry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import mazesolver.Director;
import mazesolver.Maze;

import cr0s.javara.combat.ArmorType;
import cr0s.javara.combat.TargetType;
import cr0s.javara.combat.Warhead;
import cr0s.javara.combat.attack.AttackBase;
import cr0s.javara.combat.attack.AutoTarget;
import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IShroudRevealer;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.actor.activity.activities.Attack;
import cr0s.javara.entity.actor.activity.activities.MoveInfantry;
import cr0s.javara.entity.building.EntityBuilding;

import cr0s.javara.order.InputAttributes;
import cr0s.javara.order.Order;
import cr0s.javara.order.OrderTargeter;
import cr0s.javara.order.Target;
import cr0s.javara.render.EntityBlockingMap.FillsSpace;
import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.render.Sequence;
import cr0s.javara.render.World;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.resources.SoundManager;
import cr0s.javara.util.Pos;

import javafx.scene.shape.Path;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.MoveTo;

public abstract class EntityInfantry extends MobileEntity implements IShroudRevealer {
    private static final float DEFAULT_MOVE_SPEED = 1.5f;
    private static final int WIDTH = 50;
    private static final int HEIGHT = 39;

    public static final int MAX_FACING = 8;
    public static Pos subcellOffsets[] = new Pos[6];
    public enum AnimationState { IDLE, ATTACKING, MOVING, IDLE_ANIMATING, WAITING, DEATH };

    static {
	subcellOffsets[SubCell.TOP_LEFT.ordinal()] = new Pos(-21d, -12d);
	subcellOffsets[SubCell.TOP_RIGHT.ordinal()] = new Pos(-5d, -12d);

	subcellOffsets[SubCell.CENTER.ordinal()] = new Pos(-13d, -6d);

	subcellOffsets[SubCell.BOTTOM_LEFT.ordinal()] = new Pos(-21d, 2d);
	subcellOffsets[SubCell.BOTTOM_RIGHT.ordinal()] = new Pos(-5d, 2d);
    }

    protected ShpTexture texture;
    protected int currentFrame;

    protected Sequence currentSequence;
    protected Sequence standSequence;
    protected Sequence runSequence;    
    protected Sequence attackingSequence;
    protected ArrayList<Sequence> idleSequences = new ArrayList<>();
    protected ArrayList<Sequence> deathSequences = new ArrayList<>();

    private int randomTicksBeforeIdleSeq = 0;
    private AnimationState currentAnimationState;

    protected static ShpTexture electro;
    static {
	electro = ResourceManager.getInstance().getTemplateShpTexture("temperat", "electro.tem");
    }
    
    private final static int MIN_IDLE_DELAY_TICKS = 350;
    private final static int MAX_IDLE_DELAY_TICKS = 900;
    private Sequence currentDeathSequence;

    private final String SELECTED_SOUND = "ready";
    private HashMap<String, Integer[]> orderSounds;
    private final int MAX_VERSIONS = 4;    

    protected AttackBase attack;
    protected AutoTarget autoTarget;
    protected ArrayList<String> deathSounds = new ArrayList<>();
    
    Maze maze;
    ArrayList<Pos> router;
    Director director;
    Random random = new Random();
    
    public EntityInfantry(Pos pos) {
	this(pos, SubCell.CENTER);
    }

    public EntityInfantry(Pos pos, SubCell sub) {
	super(pos, WIDTH, HEIGHT);

	this.currentSubcell = sub;

	//this.posX += subcellOffsets[sub.ordinal()].getX();
	//this.posY += subcellOffsets[sub.ordinal()].getY();

	this.fillsSpace = FillsSpace.ONE_SUBCELL;

	this.setCurrentAnimationState(AnimationState.IDLE);

	this.selectedSounds.put(SELECTED_SOUND, new Integer[] { 1, 3 } );
	this.selectedSounds.put("report1", new Integer[] { 0, 1, 2, 3 } );
	this.selectedSounds.put("yessir1", new Integer[] { 0, 1, 2, 3 } );

	this.orderSounds = new HashMap<>();
	this.orderSounds.put("ackno", new Integer[] { 0, 1, 2, 3 });
	this.orderSounds.put("affirm1", new Integer[] { 0, 1, 2, 3 });
	this.orderSounds.put("noprob", new Integer[] { 1, 3 });
	this.orderSounds.put("overout", new Integer[] { 1, 3 });
	this.orderSounds.put("ritaway", new Integer[] { 1, 3 });
	this.orderSounds.put("roger", new Integer[] { 1, 3 });
	this.orderSounds.put("ugotit", new Integer[] { 1, 3 });

	//this.unitVersion = SoundManager.getInstance().r.nextInt(4); // from 0 to 3	

	this.deathSounds.add("dedman1");
	this.deathSounds.add("dedman2");
	this.deathSounds.add("dedman3");
	this.deathSounds.add("dedman4");
	this.deathSounds.add("dedman5");
	this.deathSounds.add("dedman6");
	this.deathSounds.add("dedman7");
	this.deathSounds.add("dedman8");
	this.deathSounds.add("dedman10"); // last sound is for fire and shock death
	
	this.randomTicksBeforeIdleSeq = (int) (EntityInfantry.MIN_IDLE_DELAY_TICKS + Math.random() * (EntityInfantry.MAX_IDLE_DELAY_TICKS - EntityInfantry.MIN_IDLE_DELAY_TICKS));
	this.armorType = ArmorType.NONE;

	this.maxFacings = EntityInfantry.MAX_FACING;
	this.targetTypes.add(TargetType.GROUND);
    }

    @Override
    public Path findPathFromTo(MobileEntity e, Pos aGoal) {
	return world.getInfantryPathfinder().findPathFromTo((EntityInfantry) e, aGoal);
    }

    @Override
    public boolean canEnterCell(Pos cellPos) {
	return world.blockingEntityMap.isEntityInCell(cellPos, this) || world.isCellPassable(cellPos, (this.desiredSubcell == null) ? this.currentSubcell : this.desiredSubcell);
    }

    @Override
    public float getMoveSpeed() {
	return EntityInfantry.DEFAULT_MOVE_SPEED;// * 10;
    }

    @Override
    public void updateEntity(long delta) {
	super.updateEntity(delta);
        /*if(getImageView()!=null){
            if(router == null) {
                maze = new Maze(
                    EntityBlockingMap.blockingMap, 
                    new Point(
                            (int)getImageView().getX() / 24, 
                            (int)getImageView().getY() / 24
                    ),
                    new Point(
                            (int)getImageView().getX() / 24 + random.nextInt(10) - 5, 
                            (int)getImageView().getY() / 24 + random.nextInt(10) - 5),
                    null
                );
                director = new Director(maze, null);
                director.run();
            }
            if(!director.getBestRoute().isEmpty() && router == null){
                router = director.getBestRoute();
            }
            if(router != null){
                if(!router.isEmpty()){
                    Path path = new Path();
                    MoveTo mv = new MoveTo(
                            router.getFirst().getX() * 24 + random.nextInt(24), 
                            router.getFirst().getY() * 24 + random.nextInt(24));
                    path.getElements().add(mv);
                    for(int i = 1; i < router.size(); i++){
                        LineTo line = new LineTo(
                                router.get(i).getX() * 24 + random.nextInt(24), 
                                router.get(i).getY() * 24 + random.nextInt(24));
                        path.getElements().add(line);
                    }
                    PathTransition transition = new PathTransition();
                    transition.setDuration(Duration.millis(500 * router.size()));
                    router.removeAll(router);

                    transition.setCycleCount(1);
                    transition.setNode(getImageView());
                    transition.setAutoReverse(false);
                    transition.setPath(path);
                    Platform.runLater(transition::play);
                }
            }
        }*/
	if (this.attack != null) {
	    this.attack.update(delta);
	    
	    if (this.autoTarget != null) {
		this.autoTarget.update(delta);
	    }
	}

	if (this.currentSequence != null) { 
	    this.currentSequence.update(this.currentFacing);
            getImageView().setImage(currentSequence.render().getImage());
	}

	// TODO: refactor this crap
	//this.boundingBox.setBounds(this.posX + this.texture.width / 3 + 2, this.posY + this.texture.height / 2 - 15, 13, 18);
	if ((this.currentActivity instanceof MoveInfantry || this.currentActivity instanceof MoveInfantry.MovePart) && this.getCurrentAnimationState() != AnimationState.WAITING) {
	    this.setCurrentAnimationState(AnimationState.MOVING);
	    this.currentSequence = this.runSequence;
	} else if (currentActivity instanceof Attack) {
	    if (this.currentSequence == this.runSequence) {
		this.currentSequence = this.attackingSequence;
	    }
	    
	    if (this.attack.isAttacking && !this.attack.isReloading()) {
		//System.out.println("Attacking");
		this.setCurrentAnimationState(AnimationState.ATTACKING);
		this.currentSequence = this.attackingSequence;
	    } else {
		if (this.attackingSequence.isFinished()) {
		    //System.out.println("Reloading");
		    this.setCurrentAnimationState(AnimationState.WAITING);
		    this.currentSequence = this.standSequence;
		    this.attackingSequence.reset();
		}
	    }
	} else if (this.isIdle()) {
	    if (this.getCurrentAnimationState() != AnimationState.IDLE && this.getCurrentAnimationState() != AnimationState.IDLE_ANIMATING) {
		this.setCurrentAnimationState(AnimationState.IDLE);
		this.currentSequence = this.standSequence;
	    } else if (this.getCurrentAnimationState() == AnimationState.IDLE) {
		if (--this.randomTicksBeforeIdleSeq <= 0) {
		    this.randomTicksBeforeIdleSeq = World.getRandomInt(EntityInfantry.MIN_IDLE_DELAY_TICKS, EntityInfantry.MAX_IDLE_DELAY_TICKS);

		    if (!this.idleSequences.isEmpty()) {
			this.currentSequence = this.idleSequences.get(World.getRandomInt(0, this.idleSequences.size()));
			this.setCurrentAnimationState(AnimationState.IDLE_ANIMATING);
		    }
		} else { // Waiting for idle animation in stand state
		    this.currentSequence = this.standSequence;
		}
	    } else if (this.getCurrentAnimationState() == AnimationState.IDLE_ANIMATING) {
		if (this.currentSequence.isFinished()) {
		    this.setCurrentAnimationState(AnimationState.IDLE);
		    this.currentSequence.reset();
		}
	    }
	} else if (this.getCurrentAnimationState() == AnimationState.WAITING) {
	    this.currentSequence = this.standSequence;
	}
    }

    @Override
    public StackPane renderEntity() {
        updateEntity(0);
	//drawPath(g);

	//if (this.sheet != null) {
            setImageView(this.currentSequence.render());//this.getTranslateX(), this.getTranslateY());
            getImageView().setX(boundingBox.getX());
            getImageView().setY(boundingBox.getY());
	//}
        StackPane combined = new StackPane();
combined.getChildren().add(getImageView());
        return combined;
    }

    @Override
    public void moveTo(Pos destCell, EntityBuilding ignoreBuilding) {
	this.goal.setX(destCell.getX());
	this.goal.setY(destCell.getY());

	MoveInfantry move = new MoveInfantry(this, destCell, getMinimumEnoughRange(), ignoreBuilding);

	// If we already moving
	if (this.currentActivity instanceof MoveInfantry) {
	    this.currentActivity.cancel();
	} else if (this.currentActivity instanceof MoveInfantry.MovePart) {
	    this.currentActivity.queueActivity(move);
	    return;
	}

	queueActivity(move);
    }

    @Override
    public void startMovingByPath(Path p, EntityBuilding ignoreBuilding) {
	this.goal = new Pos(
                ((MoveTo) p.getElements().get(p.getElements().size() - 1)).getX(),
                ((MoveTo) p.getElements().get(p.getElements().size() - 1)).getY()
        );

	queueActivity(new MoveInfantry(this, p, goal, ignoreBuilding));
    }     

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }

    @Override
    public double getCenterPosX() {
	return this.boundingBox.getX() + this.boundingBox.getWidth() / 2;
    }

    @Override
    public double getCenterPosY() {
	return this.boundingBox.getY() + this.boundingBox.getHeight() / 2;
    }	

    @Override
    public String getSelectSound() {
	return SELECTED_SOUND;
    }    

    @Override
    public void playSelectedSound() {
	for (String s : this.selectedSounds.keySet()) {
	    Integer[] versions = this.selectedSounds.get(s);

	    boolean canPlay = false;
	    for (int i = 0; i < Math.min(MAX_VERSIONS, versions.length); i++) {
		if (versions[i] == this.unitVersion) {
		    canPlay = true;
		    break;
		}
	    }

	    if (SoundManager.getInstance().r.nextBoolean() && canPlay) {
		SoundManager.getInstance().playUnitSoundGlobal(this, s, this.unitVersion);
		return;
	    }
	}

	SoundManager.getInstance().playUnitSoundGlobal(this, SELECTED_SOUND, 1);
    }    

    @Override
    public void playOrderSound() {
	// Play order sound
	for (String s : this.orderSounds.keySet()) {
	    Integer[] versions = this.orderSounds.get(s);

	    if (this.unitVersion >= versions.length) {
		continue;
	    }

	    boolean canPlay = false;
	    for (int i = 0; i < Math.min(MAX_VERSIONS, versions.length); i++) {
		if (versions[i] == this.unitVersion) {
		    canPlay = true;
		    break;
		}
	    }

	    if (SoundManager.getInstance().r.nextBoolean() && canPlay) {
		SoundManager.getInstance().playUnitSoundGlobal(this, s, this.unitVersion);
		return;
	    }
	}

	if (SoundManager.getInstance().r.nextBoolean()) {
	    SoundManager.getInstance().playUnitSoundGlobal(this, "ackno", this.unitVersion);
	} else {
	    SoundManager.getInstance().playUnitSoundGlobal(this, "affirm1", this.unitVersion);
	}	
    }

    public AnimationState getCurrentAnimationState() {
	return currentAnimationState;
    }

    public void setCurrentAnimationState(AnimationState currentAnimationState) {
	this.currentAnimationState = currentAnimationState;
    }    

    @Override
    public Order issueOrder(Entity self, OrderTargeter targeter, Target target, InputAttributes ia) {
	if (super.issueOrder(self, targeter, target, ia) == null && this.attack != null) {
	    return this.attack.issueOrder(self, targeter, target, ia);
	}

	return super.issueOrder(self, targeter, target, ia);
    }

    @Override
    public void resolveOrder(Order order) {
	if (this.attack != null) {
	    this.attack.resolveOrder(order);
	}
	
	super.resolveOrder(order);
    }         

    @Override
    public Activity moveToRange(Pos cellPos, int range) {
	MoveInfantry move = new MoveInfantry(this, cellPos, range);
	move.forceRange = true;

	return move;
    }      

    @Override
    public int getMaxFacings() {
	return this.maxFacings;
    }
    
    @Override
    public void giveDamage(EntityActor firedBy, int amount, Warhead warhead) {	
	if (this.isInvuln) {
	    return;
	}
	
	if (this.isDead() || this.getHp() <= 0) {
	    return;
	}
	
	if (this.getHp() > 0 && this.getHp() - amount <= 0) { // prevent overkill and spawn unit explosion
	    Sequence deathSeq = this.getDeathSequence(warhead);
	    if (deathSeq != null) {
		this.world.playSequenceAt(deathSeq, new Pos(this.boundingBox.getX(), this.boundingBox.getY()));
	    }
	    
	    // Play random death sound
	    //SoundManager.getInstance().playSfxAt(getRandomDeathSound(warhead), this.getPosition());
	    
	    this.setHp(0);
	    this.setDead();
	    
	    return;
	}
	
	this.setHp(this.getHp() - amount);	
	
	this.owner.notifyDamaged(this, firedBy, amount, warhead);
    }

    private String getRandomDeathSound(Warhead wh) {
	if (wh.infDeath != 5) {
	    // Get random sound from list excluding last one
	    return this.deathSounds.get(this.world.getRandomInt(0, this.deathSounds.size() - 1));
	} else { // death by fire/electro shock
	    return this.deathSounds.get(this.deathSounds.size() - 1);
	}
    }

    private Sequence getDeathSequence(Warhead warhead) {
	Sequence seq = this.deathSequences.get(warhead.infDeath - 1);
	
	return seq;
    }     
}
