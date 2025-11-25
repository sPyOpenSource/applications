package view;

import javafx.scene.image.ImageView;

public class ImageViewClone extends ImageView {
    public ImageViewClone(ImageView imageView) {
        this.setImage(imageView.getImage());
        this.setX(imageView.getX());
        this.setY(imageView.getY());
        this.setFitWidth(imageView.getFitWidth());
        this.setFitHeight(imageView.getFitHeight());
    }
}
