package cr0s.javara.entity;

import cr0s.javara.combat.Warhead;
import cr0s.javara.entity.actor.EntityActor;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.entity.building.common.EntityWarFactory;
import cr0s.javara.entity.building.soviet.EntityBarracks;
import cr0s.javara.entity.infantry.EntityInfantry;
import cr0s.javara.gameplay.Player;
import cr0s.javara.gameplay.Team;
import cr0s.javara.render.EntityBlockingMap.FillsSpace;
import cr0s.javara.render.World;
import java.util.ArrayList;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public abstract class Entity extends Node {
    public World world;

    public double moveX, moveY;

    private boolean isDead = false;
    private int hp, maxHp;
    public Player owner = new Player(null,null,null,null);
    public boolean isSelected = false;
    public boolean isVisible = false;

    public Rectangle boundingBox;

    public boolean isMouseOver = false;

    protected boolean isInvuln = false;

    private int SELECTION_BOX_ADD = 1;

    public double sizeWidth, sizeHeight;

    public int currentFacing;

    public float updateDelta;

    public FillsSpace fillsSpace;
    private ImageView viewHero;
    private final ArrayList<ImageView> imageViews = new ArrayList<>();
    
    public ImageView getViewHero() {
        return viewHero;
    }

    public void setViewHero(ImageView viewHero) {
        this.viewHero = viewHero;
    }
    
    public ArrayList<ImageView> getImageViews() {
        return imageViews;
    }
    
    public void setImageViews(String... paths){
        for (String path : paths){
            ImageView imageView = new ImageView(new Image(path, true));
            imageView.setFitWidth(sizeWidth);
            imageView.setFitHeight(sizeHeight);
            imageView.setX(boundingBox.getX());
            imageView.setY(boundingBox.getY());
            imageViews.add(imageView);
        }
    }
    
    public Entity (double posX, double posY, double aSizeWidth, double aSizeHeight) {
	this.sizeWidth = aSizeWidth;
	this.sizeHeight = aSizeWidth;

	this.boundingBox = new Rectangle(posX, posY, sizeWidth, sizeHeight);

	this.fillsSpace = FillsSpace.DONT_FILLS;
    }

    public void setWorld(World w) {
	this.world = w;
    }

    public abstract void updateEntity(int delta);
    public abstract void renderEntity(Scene g);
    public abstract boolean shouldRenderedInPass(int passNum);

    public void drawSelectionBox(Scene g) {
	int REDUCE = 8;
	//g.setLineWidth(2);
	//g.setColor(Color.WHITE);

	double minX = this.boundingBox.getX();
	double minY = this.boundingBox.getY();
	double maxX = minX + this.boundingBox.getWidth();
	double maxY = minY + this.boundingBox.getHeight();

	int bbWidth = (int) (this.boundingBox.getWidth() + 2 * SELECTION_BOX_ADD);
	int bbHeight = (int) (this.boundingBox.getHeight() + 2 * SELECTION_BOX_ADD);

	if (this instanceof EntityInfantry) {
	    this.SELECTION_BOX_ADD = 0;
	    REDUCE = 10;
	}

	double cornerXUpLeft = minX - 2 * SELECTION_BOX_ADD;
	double cornerYUpLeft = minY - 2 * SELECTION_BOX_ADD;
	//g.drawLine(cornerXUpLeft, cornerYUpLeft, cornerXUpLeft + bbWidth / REDUCE, cornerYUpLeft);
	//g.drawLine(cornerXUpLeft, cornerYUpLeft, cornerXUpLeft, cornerYUpLeft + bbHeight / REDUCE);

	double cornerXDownLeft = minX - 2 * SELECTION_BOX_ADD;
	double cornerYDownLeft = minY + this.boundingBox.getHeight() + 2 * SELECTION_BOX_ADD;
	//g.drawLine(cornerXDownLeft, cornerYDownLeft, cornerXDownLeft + bbWidth / REDUCE, cornerYDownLeft);
	//g.drawLine(cornerXDownLeft, cornerYDownLeft, cornerXDownLeft, cornerYDownLeft - bbHeight / REDUCE);

	// Right corners
	double cornerXUpRight = maxX + 2 * SELECTION_BOX_ADD;
	double cornerYUpRight = maxY - this.boundingBox.getHeight() - 2*SELECTION_BOX_ADD;
	//g.drawLine(cornerXUpRight, cornerYUpRight, cornerXUpRight - bbWidth / REDUCE, cornerYUpRight);
	//g.drawLine(cornerXUpRight, cornerYUpRight, cornerXUpRight, cornerYUpRight + bbHeight / REDUCE);

	double cornerXDownRight = maxX + 2 * SELECTION_BOX_ADD;
	double cornerYDownRight = maxY + 2 * SELECTION_BOX_ADD;
	//g.drawLine(cornerXDownRight, cornerYDownRight, cornerXDownRight - bbWidth / REDUCE, cornerYDownRight);
	//g.drawLine(cornerXDownRight, cornerYDownRight, cornerXDownRight, cornerYDownRight - bbHeight / REDUCE);

	drawHpBar(g);
	if (this instanceof IPips) {
	    drawPips(g);
	}
    }

    public void drawHpBar(Scene g) {
	final int BAR_COMPRESS = 3; // "Compress" bar by N pixels from left and right

	if (this instanceof EntityInfantry) {
	    this.SELECTION_BOX_ADD = 0;
	}

	double minX = this.boundingBox.getX();
	double minY = this.boundingBox.getY();
	double maxX = minX + this.boundingBox.getWidth();
	double maxY = minY + this.boundingBox.getHeight();

	double cornerXUpLeft = minX - 2 * this.SELECTION_BOX_ADD + BAR_COMPRESS;
	double cornerYUpLeft = minY - 2 * this.SELECTION_BOX_ADD;
	double cornerXDownLeft = minX - 2 * this.SELECTION_BOX_ADD;
	double cornerYDownLeft = minY + this.boundingBox.getHeight() + 2 * this.SELECTION_BOX_ADD;
	double cornerXUpRight = maxX + 2 * this.SELECTION_BOX_ADD - BAR_COMPRESS;
	double cornerYUpRight = maxY - this.boundingBox.getHeight() - 2 * this.SELECTION_BOX_ADD;
	double cornerXDownRight = maxX + 2 * this.SELECTION_BOX_ADD;
	double cornerYDownRight = maxY + 2 * this.SELECTION_BOX_ADD;

	double barWidth = (cornerXUpRight - cornerXUpLeft) - 1;

	// Draw HP bar
	//g.setLineWidth(5);
	//g.setColor(Color.BLACK);
	//g.drawLine(cornerXUpLeft, cornerYUpLeft - 8, cornerXUpRight, cornerYUpRight - 8);

	double hpBarWidth = this.getHp() / (float) Math.max(1, this.getMaxHp()) * barWidth;

	//g.setLineWidth(3);
	//g.setColor(getHpColor());
	//g.drawLine(cornerXUpLeft + 1, cornerYUpLeft - 8, cornerXUpLeft + hpBarWidth + 1, cornerYUpRight - 8);	

	// Draw progress bar
	if (this instanceof EntityBuilding && ((EntityBuilding) this).getProgressValue() != -1) {
	    int total = ((EntityBuilding) this).getMaxProgress();
	    int ready = ((EntityBuilding) this).getProgressValue();

	    double barWidthProgress = (ready / (float) total) * barWidth;

	    //g.setLineWidth(5);
	    //g.setColor(Color.BLACK);
	    //g.drawLine(cornerXUpLeft, cornerYUpLeft - 2, cornerXUpRight, cornerYUpRight - 2);

	    //g.setLineWidth(3);
	    //g.setColor(Color.MAGENTA);
	    //g.drawLine(cornerXUpLeft + 1, cornerYUpLeft - 2, cornerXUpLeft + 1 + barWidthProgress, cornerYUpRight - 2);			
	}

	if (this instanceof EntityWarFactory || this instanceof EntityBarracks) {
	    if (((EntityBuilding) this).isPrimary()) {
		((EntityBuilding) this).drawPrimarySign(g);
	    }
	}
    }	

    private void drawPips(Scene g) {
	final int PIPS_OFFSET_Y = 8;
	final int PIPS_OFFSET_X = 4;
	final int PIPS_SPACING_X = 5;
	final int PIPS_SIZE = 3;

	double cornerYDownLeft = this.boundingBox.getY() + this.boundingBox.getHeight() + 2 * this.SELECTION_BOX_ADD;
	double cornerXDownLeft = this.boundingBox.getX() - 2 * this.SELECTION_BOX_ADD;   

	int pipY = (int) cornerYDownLeft - PIPS_OFFSET_Y; 
	int startPipX = (int) cornerXDownLeft + PIPS_SPACING_X;


	IPips pipedEntity = (IPips) this;

	for (int pip = 0; pip < pipedEntity.getPipCount(); pip++) {
	    Color pipColor = pipedEntity.getPipColorAt(pip);

	    int pipX = startPipX + pip * PIPS_SPACING_X;

	    //g.setColor(Color.GRAY);
	    //g.setLineWidth(1);

	    //g.drawRect(pipX, pipY, PIPS_SIZE, PIPS_SIZE);

	    if (pipColor != null) {
		//g.setColor(pipColor);

		//g.fillRect(pipX + 1, pipY + 1, PIPS_SIZE - 1, PIPS_SIZE - 1);
	    }
	}
    }

    public void drawPrimarySign(Scene g) {
	//g.setColor(Color.WHITE);
	final String s = "Primary";
	//g.drawString(s, this.boundingBox.getCenterX() - (g.getFont().getWidth(s) / 2), this.boundingBox.getMaxY() - g.getFont().getLineHeight() - 2);
    }

    public Color getHpColor() {
	float part = this.getHp() / (float) Math.max(1, this.getMaxHp());

	if (part <= 0.25f) {
	    return Color.RED;
	} else if (part > 0.25f && part < 0.50f) {
	    return Color.ORANGE;
	} else if (part >= 0.50f) {
	    return Color.GREEN;
	}

	return Color.BLACK;
    }

    public boolean isDead() {
	return this.isDead;
    }

    public void setDead() {
	this.isVisible = false;
	this.isDead = true;
    }

    public int getHp() {
	return this.hp;
    }

    public void setHp(int aHp) {
	this.hp = aHp;
    }

    public int getMaxHp() {
	return this.maxHp;
    }

    public void setMaxHp(int aMaxHp) {
	this.maxHp = aMaxHp;
    }

    public void giveDamage(EntityActor firedBy, int amount, Warhead warhead) {
	if (!isInvuln) {
	    this.setHp(this.getHp() - amount);

	    if (this.getHp() <= 0) {
		this.setHp(0);
		setDead();
	    }
	}

	//this.owner.notifyDamaged(this, firedBy, amount, warhead);
    }

    public void setInvuln(boolean invuln) {
	this.isInvuln = invuln;
    }

    public boolean getInvuln() {
	return this.isInvuln;
    }
}
