package controller.attack;

import cr0s.javara.entity.infantry.Barbarian;
import cr0s.javara.render.map.Map;
import cr0s.javara.util.Pos;
import javafx.scene.layout.AnchorPane;

public class BarbarianAttack extends MeleeUnitAttack {
    public BarbarianAttack(double x, double y, AnchorPane root, Map map) {
        super(new Barbarian(new Pos(x, y)), root, map);
    }

    @Override
    protected String deployAudio() {
        return "/assets/audio/enter_panda.mp3";
    }

    @Override
    protected String attackAudio() {
        return "/assets/audio/exp1.mp3";
    }

    @Override
    protected String dieAudio() {
        return "/assets/audio/exp.mp3";
    }
}
