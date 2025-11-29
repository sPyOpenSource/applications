package cr0s.javara.entity.turreted;

import java.util.List;
import javafx.scene.Scene;

public interface IHaveTurret {
    public void drawTurrets(Scene g);
    public void updateTurrets(long delta);
    
    public List<Turret> getTurrets();
}
