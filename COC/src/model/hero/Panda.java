package model.hero;

import cr0s.javara.entity.infantry.EntityInfantry;
import cr0s.javara.util.Pos;
import javafx.scene.shape.Path;
import javafx.scene.layout.StackPane;

public class Panda extends EntityInfantry{
    public Panda(double x, double y) {
        super(new Pos(x, y));
        //super(1500, 10, 150, 0, 24, null, 50, 50, 1, x, y, 
        setHp(1500);
        setDamagePerSecond(150);
        setImageViews(
                "assets/gif/pandaL.gif", 
                "assets/gif/panda_attackL.gif", 
                "assets/gif/pandaR.gif", 
                "assets/gif/panda_attackR.gif", 
                "assets/png/horse2.png", 
                "assets/png/test.png", 
                "assets/png/olifant.png", 
                "assets/png/rubuska.png",
                "assets/png/rubuska1.png"
        );
        //getImageViews().get(1).setFitWidth(55);
        //getImageViews().get(1).setFitHeight(45);
        //getImageViews().get(3).setFitWidth(55);
        //getImageViews().get(3).setFitHeight(45);
    }
    
    @Override
    public Path findPathFromTo(cr0s.javara.entity.MobileEntity e, Pos aGoal){
        return null;
    }

    @Override
    public float getMoveSpeed(){ return 24; }    
    @Override
    public int getMinimumEnoughRange(){ return 0; }
    @Override
    public boolean canEnterCell(Pos cellPos){ return true; }   
    @Override
    public int getWaitAverageTime(){ return 0; }
    @Override
    public int getWaitSpreadTime(){ return 0; }
    @Override
    public int getRevealingRange(){ return 0; }
    @Override
    public StackPane renderEntity(){ return null; }
}
