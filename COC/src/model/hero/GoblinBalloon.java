package model.hero;

import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.aircraft.EntityAircraft;
import cr0s.javara.util.Pos;
import javafx.scene.Scene;
import javafx.scene.shape.Path;
import model.building.BuildingType;

public class GoblinBalloon extends EntityAircraft{
    public GoblinBalloon(double x, double y) {
        super(x, y, 60, 80);
        //super(300, 2, 52, 0, 32, BuildingType.REFERENCES, 60, 80, 3, x, y, 
        setHp(300);
        setDamagePerSecond(52);
        setImageViews("assets/png/goblin_balloon.png", "assets/gif/goblin_attack.gif");
        getImageViews().get(1).setFitHeight(113);
    }
    @Override
    public Path findPathFromTo(cr0s.javara.entity.MobileEntity e, int aGoalX, int aGoalY){
        return null;
    }

    @Override
    public float getMoveSpeed(){ return 32; }    
    @Override
    public int getMinimumEnoughRange(){ return 0; }
    @Override
    public boolean canEnterCell(cr0s.javara.util.Pos cellPos){ return true; }   
    @Override
    public int getWaitAverageTime(){ return 0; }
    @Override
    public int getWaitSpreadTime(){ return 0; }
    @Override
    protected Activity moveToRange(Pos cellPos, int range){ return null; }
    @Override
    public void renderEntity(Scene g){}
}
