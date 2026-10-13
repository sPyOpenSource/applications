package view;

import java.util.function.BiFunction;
import javafx.scene.image.ImageView;

public class UnitIcon {
    public final ImageView view;
    public final int cost;
    public final int minLevel;
    public final BiFunction<Double, Double, Thread> spawn;

    public UnitIcon(ImageView view, int cost, int minLevel, BiFunction<Double, Double, Thread> spawn) {
        this.view = view;
        this.cost = cost;
        this.minLevel = minLevel;
        this.spawn = spawn;
    }
}
