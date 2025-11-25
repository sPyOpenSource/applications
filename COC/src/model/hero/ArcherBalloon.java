package model.hero;

import cr0s.javara.entity.aircraft.EntityAircraft;
import cr0s.javara.util.Pos;
import javafx.scene.shape.Path;
import cr0s.javara.entity.actor.activity.Activity;
import javafx.scene.Scene;

public class ArcherBalloon extends EntityAircraft {
    
    public ArcherBalloon(double x, double y) {
        super(x, y, 60, 80);
        //super(500, 30, 800, 70, 13, null, 60, 80, 4, x, y, "assets/png/archer_balloon.png");
        setHp(500);
        setDamagePerSecond(800);
        setImageViews("assets/png/archer_balloon.png");
    }
    
    @Override
    public Path findPathFromTo(cr0s.javara.entity.MobileEntity e, int aGoalX, int aGoalY){
        return null;
    }

    @Override
    public float getMoveSpeed(){ return 13; }    
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
