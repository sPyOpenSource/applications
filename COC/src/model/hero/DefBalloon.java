package model.hero;

import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.aircraft.EntityAircraft;
import cr0s.javara.util.Pos;
import javafx.scene.shape.Path;
import javafx.scene.layout.StackPane;

public class DefBalloon extends EntityAircraft{
    public DefBalloon(double x, double y) {
        super(new Pos(x, y), 60, 70);
        //super(690, 10, 198, 0, 10, BuildingType.DEFENSIVE, 60, 70, 2, x, y,
        setHp(690);
        setDamagePerSecond(198);
        setImageViews("assets/png/def_balloon.png","assets/gif/balloon_attack.gif");
        //getImageViews().get(1).setFitHeight(102);
    }
    @Override
    public Path findPathFromTo(cr0s.javara.entity.MobileEntity e, Pos aGoal){
        return null;
    }

    @Override
    public float getMoveSpeed(){ return 10; }    
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
    public StackPane renderEntity(){ return null; }
}
