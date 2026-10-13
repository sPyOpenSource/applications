package controller.attack;

import cr0s.javara.entity.infantry.Wizard;
import cr0s.javara.render.map.Map;
import cr0s.javara.util.Pos;
import javafx.scene.layout.AnchorPane;

public class WizardAttack extends RangedUnitAttack {
    public WizardAttack(double x, double y, AnchorPane root, Map map) {
        super(new Wizard(new Pos(x, y)), root, map);
    }

    @Override
    protected String deployAudio() {
        return "/assets/audio/balloonDeploy.mp3";
    }

    @Override
    protected String attackAudio() {
        return "/assets/audio/fire.mp3";
    }

    @Override
    protected String dieAudio() {
        return "/assets/audio/balloonDie.mp3";
    }

    @Override
    protected double attackRange() {
        return 150;
    }
}
