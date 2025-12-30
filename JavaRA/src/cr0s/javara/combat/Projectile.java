package cr0s.javara.combat;

import cr0s.javara.entity.Entity;
import cr0s.javara.entity.IEffect;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.render.Sequence;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import javafx.scene.image.ImageView;

public abstract class Projectile extends Entity implements IEffect {    
    public Weapon weapon;
    public float firepowerModifier = 1.0f;

    public Pos pos, sourcePos;
    public EntityActor sourceActor;

    public Pos passiveTargetPos;
    public EntityActor guidedTarget;

    protected ShpTexture tex;
    protected Sequence projectileSq;

    public int numFacings = 32;
    protected int seqLength = 0;

    private Projectile(Pos pos,
	    float aSizeWidth, float aSizeHeight) {
	super(pos, aSizeWidth, aSizeHeight);
    }

    public Projectile(Pos srcPos, Pos passivePos, EntityActor targetActor, int width, int height) {
	super(srcPos, width, height);

	this.sourcePos = srcPos;
	this.passiveTargetPos = passivePos;
	this.guidedTarget = targetActor;
    }

    public Projectile(EntityActor srcActor, Pos srcPos, Pos passivePos, EntityActor targetActor, int width, int height) {
	super(srcPos, width, height);

	this.sourceActor = srcActor;
	this.sourcePos = srcPos;
	this.passiveTargetPos = passivePos;
    }

    public void initTexture(String textureName, int facings, int len) {
	this.tex = ResourceManager.getInstance().getConquerTexture(textureName);

	if (this.tex != null) {
	    this.numFacings = facings;
	    this.seqLength = len;
	    this.projectileSq = new Sequence(tex, 0, facings, len, 1, null);
	}
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
	return passNum == 1;
    }    


    @Override
    public void updateEntity(long delta) {
	if (this.projectileSq != null) {
	    this.projectileSq.update(this.currentFacing);
	}

	//this.posX = this.pos.getX();
	//this.posY = this.pos.getY() - this.pos.getZ(); // Z is height above ground
    }

    @Override
    public ImageView renderEntity() {
	if (this.projectileSq != null) {
	    ImageView view = this.projectileSq.render();
            view.setX(this.pos.getX() - this.sizeWidth / 2);
            view.setY(this.pos.getY() - this.pos.getZ() - this.sizeHeight / 2);
            return view;
	}
        return null;
    }    
}
