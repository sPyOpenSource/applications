package cr0s.javara.entity.infantry;

import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.render.Sequence;

import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.util.Pos;

public class EntityGlaWorker extends EntityInfantry implements ISelectable, IHaveCost {

    private final int BUILD_COST = 8;

    public EntityGlaWorker(Pos pos) {
        this(pos, SubCell.CENTER);
    }

    public EntityGlaWorker(Pos pos, SubCell sub) {
        super(pos, sub);

        this.texture = ResourceManager.getInstance().getInfantryTexture("e1.shp");

        this.setMaxHp(40);
        this.setHp(40);

        this.currentFrame = 0;

        this.standSequence = new Sequence(texture, 0, 8, 0, 0, null);
        this.runSequence = new Sequence(texture, 16, 8, 6, 2, null);
        this.runSequence.setIsLoop(true);

        this.attackingSequence = new Sequence(texture, 64, 8, 8, 2, null);

        this.idleSequences.add(new Sequence(texture, 256, 0, 16, 2, null));
        this.idleSequences.add(new Sequence(texture, 272, 0, 16, 2, null));

        this.deathSequences.add(new Sequence(texture, 288, 0, 8, 2, null));
        this.deathSequences.add(new Sequence(texture, 296, 0, 8, 2, null));
        this.deathSequences.add(new Sequence(texture, 304, 0, 8, 2, null));
        this.deathSequences.add(new Sequence(texture, 312, 0, 12, 2, null));
        this.deathSequences.add(new Sequence(texture, 324, 0, 18, 2, null));
        this.deathSequences.add(new Sequence(electro, 0, 14, 0, 1, null));

        // Worker has no weapon - it cannot attack
        this.attack = null;

        this.setName("glaWorker");
    }

    @Override
    public void updateEntity(long delta) {
        super.updateEntity(delta);
    }

    @Override
    public int getMinimumEnoughRange() {
        return 0;
    }

    @Override
    public int getWaitAverageTime() {
        return 50;
    }

    @Override
    public int getWaitSpreadTime() {
        return 10;
    }

    @Override
    public int getRevealingRange() {
        return 4;
    }

    @Override
    public void select() {
        this.isSelected = true;
    }

    @Override
    public void cancelSelect() {
        this.isSelected = false;
    }

    @Override
    public boolean isSelected() {
        return this.isSelected;
    }

    @Override
    public int getBuildingCost() {
        return BUILD_COST;
    }
}
