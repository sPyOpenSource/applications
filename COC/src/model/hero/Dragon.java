package model.hero;

import cr0s.javara.entity.actor.activity.Activity;
import cr0s.javara.entity.aircraft.EntityAircraft;
import cr0s.javara.util.Pos;
import javafx.scene.shape.Path;
import javafx.scene.layout.StackPane;

public class Dragon extends EntityAircraft{
    public Dragon(double x, double y) {
        super(new Pos(x, y), 120, 120);
        //super(3000, 20, 300, 10, 16, null, 120, 120, 1, x, y, )
        setHp(3000);
        setDamagePerSecond(300);
        setImageViews("assets/gif/dragonL.gif", "assets/gif/dragon_attackL.gif", "assets/gif/dragonR.gif", "assets/gif/dragon_attackR.gif");
        //getImageViews().get(1).setFitWidth(170);
        //getImageViews().get(1).setFitHeight(120);
        //getImageViews().get(3).setFitWidth(170);
        //getImageViews().get(3).setFitHeight(120);
    }
    
    @Override
    public Path findPathFromTo(cr0s.javara.entity.MobileEntity e, Pos aGoal){
        return null;
    }

    @Override
    public float getMoveSpeed(){ return 16; }    
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
