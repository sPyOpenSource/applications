package cr0s.javara.entity.building.china;

import cr0s.javara.combat.ArmorType;
import cr0s.javara.combat.TargetType;
import cr0s.javara.entity.IHaveCost;
import cr0s.javara.entity.ISelectable;
import cr0s.javara.entity.building.BibType;
import cr0s.javara.entity.building.EntityBuilding;
import cr0s.javara.gameplay.Team.Alignment;
import cr0s.javara.util.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class GoldMine extends EntityBuilding implements ISelectable, IHaveCost {

    public static final int WIDTH_TILES = 1;
    public static final int HEIGHT_TILES = 1;
    private static final int BUILDING_COST = 200;

    public GoldMine(Pos x) {
        super(x, WIDTH_TILES * 24, HEIGHT_TILES * 24, "x");

        setBibType(BibType.SMALL);
        setProgressValue(-1);

        setMaxHp(1180);
        setHp(getMaxHp());

        this.armorType = ArmorType.WOOD;
        this.targetTypes.add(TargetType.GROUND);
        this.unitProductionAlingment = Alignment.NEUTRAL;

        this.setName("gold");

        Image view = new Image("assets/png/gold_mine.png", true);
        setImageView(new ImageView(view));
        getImageView().setX(x.getX());
        getImageView().setY(x.getY());
        getImages().add(view);
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
