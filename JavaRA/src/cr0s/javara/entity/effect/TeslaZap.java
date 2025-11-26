package cr0s.javara.entity.effect;

import cr0s.javara.combat.Combat;
import cr0s.javara.combat.Projectile;
import cr0s.javara.entity.IEffect;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.order.Target;
import cr0s.javara.util.Pos;
import javafx.scene.Scene;

public class TeslaZap extends Projectile implements IEffect {

    private int timeUntilRemove = 10; // in ticks
    private boolean doneDamage = false;
    private final boolean initialized = false;

    private final Target guided;
    private Pos p;

    private final int BRIGHT_ZAPS = 1;
    private final int DIM_ZAPS = 3;
    private boolean isInit = false;

    public TeslaZap(EntityActor srcActor, Pos srcPos, Pos passivePos,
	    EntityActor targetActor, int width, int height) {
	super(srcActor, srcPos, passivePos, targetActor, width, height);

	this.guided = new Target(this.guidedTarget);

	this.p = this.guided.isValidFor(this.sourceActor) ? this.guidedTarget.getPosition() : this.passiveTargetPos;	
    }

    @Override
    public void updateEntity(int delta) {
	//super.updateEntity(delta);

	this.p = this.guided.isValidFor(this.sourceActor) ? this.guidedTarget.getPosition() : this.passiveTargetPos;

	if (!this.isInit) {

	    for (int n = 0; n < this.BRIGHT_ZAPS; n++) {
		TeslaZapRenderer tzr = new TeslaZapRenderer(this.sourceActor, this.sourcePos, this.passiveTargetPos, this.guidedTarget, 24, 24, true);
		tzr.isVisible = true;

		this.world.spawnEntityInWorld(tzr);
	    }

	    for (int n = 0; n < this.DIM_ZAPS; n++) {
		TeslaZapRenderer tzr = new TeslaZapRenderer(this.sourceActor, this.sourcePos, this.passiveTargetPos, this.guidedTarget, 24, 24, false);
		tzr.isVisible = true;
		
		this.world.spawnEntityInWorld(tzr);
	    }

	    this.isInit = true;
	}

	if (--timeUntilRemove <= 0) {
	    this.setDead();

	    return;
	}	
	
	if (!this.doneDamage) {
	    Combat.doImpacts(this.p, this.weapon, this.sourceActor, this.firepowerModifier);
	    this.doneDamage = true;
	}
    }

    @Override
    public void renderEntity(Scene g) {

    }
    
}
