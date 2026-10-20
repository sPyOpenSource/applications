package controller.attack;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class AttackEffects {
    public static void flashImpact(AnchorPane root, double x, double y, Color color) {
        Circle circle = new Circle(x, y, 14);
        circle.setFill(color);
        circle.setStroke(null);
        root.getChildren().add(circle);

        FadeTransition fade = new FadeTransition(Duration.millis(250), circle);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> root.getChildren().remove(circle));
        fade.play();
    }

    public static void pulse(ImageView view) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(120), view);
        scale.setToX(1.12);
        scale.setToY(1.12);
        scale.setAutoReverse(true);
        scale.setCycleCount(2);
        scale.play();
    }
}
