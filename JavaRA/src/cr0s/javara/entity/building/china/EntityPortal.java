package cr0s.javara.entity.building.china;

import cr0s.javara.combat.ArmorType;
import cr0s.javara.combat.TargetType;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.MobileEntity;
import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.render.EntityBlockingMap.SubCell;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.render.World;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class EntityPortal extends EntityBuilding implements ISelectable, IHaveCost {

    public static final int WIDTH_TILES = 1;
    public static final int HEIGHT_TILES = 1;
    private static final int BUILDING_COST = 300;

    private EntityPortal linkedPortal;
    private int teleportCooldown = 0;
    private static final int TELEPORT_COOLDOWN_TICKS = 60; // ~3 seconds at 20 ticks/sec

    public EntityPortal(Pos x) {
        super(x, WIDTH_TILES * 24, HEIGHT_TILES * 24, "x");

        setBibType(BibType.SMALL);
        setProgressValue(-1);

        setMaxHp(800);
        setHp(getMaxHp());

        this.armorType = ArmorType.WOOD;
        this.targetTypes.add(TargetType.GROUND);
        this.unitProductionAlingment = Alignment.NEUTRAL;

        this.setName("portal");

        Image view = new Image("assets/png/gold_mine.png", true);
        setImageView(new ImageView(view));
        getImageView().setX(x.getX());
        getImageView().setY(x.getY());
        getImages().add(view);
    }

    public void setLinkedPortal(EntityPortal portal) {
        this.linkedPortal = portal;
    }

    public EntityPortal getLinkedPortal() {
        return linkedPortal;
    }

    @Override
    public void updateEntity(long delta) {
        super.updateEntity(delta);

        if (teleportCooldown > 0) {
            teleportCooldown--;
        }

        if (linkedPortal == null || linkedPortal.isDead() || teleportCooldown > 0) {
            return;
        }

        // Check for mobile entities on this portal's cell and teleport them
        Pos cellPos = new Pos((int) (boundingBox.getX() / 24), (int) (boundingBox.getY() / 24));
        MobileEntity mobile = owner.world.getMobileEntityInCell(cellPos);

        if (mobile != null && !mobile.isDead() && mobile.owner == this.owner) {
            // Teleport to linked portal position
            double targetX = linkedPortal.boundingBox.getX() + 12;
            double targetY = linkedPortal.boundingBox.getY() + 12;

            mobile.setPos(new Pos(targetX, targetY));
            mobile.currentSubcell = SubCell.CENTER;
            mobile.desiredSubcell = SubCell.CENTER;

            teleportCooldown = TELEPORT_COOLDOWN_TICKS;

            // Spawn teleport effect at both portals
            owner.world.spawnExplosionAt(new Pos(boundingBox.getX(), boundingBox.getY()), "fball1.shp");
            owner.world.spawnExplosionAt(new Pos(linkedPortal.boundingBox.getX(), linkedPortal.boundingBox.getY()), "fball1.shp");
        }
    }

    @Override
    public StackPane renderEntity() {
        if (getImageView() == null) {
            setImageView(new ImageView(getImages().get(0)));
        }
        StackPane combined = new StackPane();
        combined.getChildren().add(getImageView());
        return combined;
    }

    @Override
    public boolean shouldRenderedInPass(int passNum) {
        return passNum == 0;
    }

    @Override
    public int getHeightInTiles() {
        return HEIGHT_TILES;
    }

    @Override
    public int getWidthInTiles() {
        return WIDTH_TILES;
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
        return BUILDING_COST;
    }
}
