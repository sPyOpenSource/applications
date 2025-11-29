package cr0s.javara.render;

import cr0s.javara.gameplay.Player;
import javafx.scene.input.KeyCode;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.HOME;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.UP;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Stage;
import javafx.scene.Camera;
import javafx.scene.Scene;

public class Controller {

    private final Player player;
    private final Camera camera;
    private double offsetX, offsetY;
    private int boarderX = 700, boarderY = 560;

    private final int VIEWPORT_SCROLL_TRESHOLD = 35; // TODO: config this
    private final int ALLOWED_BORDER_TILES = 0;
    private final float SCROLL_SPEED = 0.5f; // TODO: config this
    private int zTrans = -800;

    public Controller(Player player, Camera camera, Scene scene) {
        this.player = player;
        this.camera = camera;
        camera.setFarClip(Integer.MAX_VALUE);
        camera.setNearClip(0.1);
        scene.setCamera(camera);
        offsetX = player.getPlayerSpawnPoint().getX() * 24;
        offsetY = player.getPlayerSpawnPoint().getY() * 24;
        scene.getCamera().setTranslateX(offsetX);
        scene.getCamera().setTranslateY(offsetY);
        scene.setOnScroll((ScrollEvent event) -> {
            //zTrans += event.getDeltaY() * (zTrans / -50);
            offsetX -= event.getDeltaX() * SCROLL_SPEED;
            offsetY -= event.getDeltaY() * SCROLL_SPEED;
        });
        scene.setOnKeyPressed((KeyEvent e) -> {
            KeyCode code = e.getCode();
            switch (code) {
                case LEFT:
                    offsetX = scene.getCamera().getTranslateX() - 40;
                    break;
                case RIGHT:
                    offsetX = scene.getCamera().getTranslateX() + 40;
                    break;
                case UP:
                    offsetY = scene.getCamera().getTranslateY() - 40;
                    break;
                case DOWN:
                    offsetY = scene.getCamera().getTranslateY() + 40;
                    break;
                case HOME:
                    //g.xRotate.setAngle(-90);
                    //g.yRotate.setAngle(0);
                    //g.zRotate.setAngle(0);
                    offsetX = player.getPlayerSpawnPoint().getX();
                    offsetY = player.getPlayerSpawnPoint().getY();
                    zTrans = -800;
                    break;
                default:
                    break;
            }
        });
        new javafx.animation.AnimationTimer() {
            @Override
            public void handle(long now) {
                if(zTrans < -2000) zTrans = -2000;
                if(zTrans > -400) zTrans = -400;
                if(offsetX < boarderX) offsetX = boarderX;
                if(offsetX > 128 * 24 - boarderX) offsetX = 128 * 24 - boarderX;
                if(offsetY < boarderY) offsetY = boarderY;
                if(offsetY > 128 * 24 - boarderY) offsetY = 128 * 24 - boarderY;
                scene.getCamera().setTranslateX(offsetX);
                scene.getCamera().setTranslateY(offsetY);
                scene.getCamera().setTranslateZ(zTrans);
            }
        }.start();
    }

	public void update(final Stage gc, final int delta)
			throws Exception {
		/*if (gc.isPaused()) {
			return;
		}*/

		float dx = 0f, dy = 0;

		int mouseX = 0;//gc.getInput().getMouseX();
		int mouseY = 0;//gc.getInput().getMouseY();

		if (mouseX <= VIEWPORT_SCROLL_TRESHOLD) {
			dx = 1;
		}
		if (mouseX >= gc.getWidth() - VIEWPORT_SCROLL_TRESHOLD) {
			dx = -1;
		}

		if (mouseY <= VIEWPORT_SCROLL_TRESHOLD) {
			dy = 1;
		}
		if (mouseY >= gc.getHeight() - VIEWPORT_SCROLL_TRESHOLD) {
			dy = -1;
		}
		
		/*if (camera.map != null) {
			//int mapWidthPixels = (int) camera.map.getBounds().getWidth() + (TileMap.MAP_OFFSET_TILES * 24) + (TileMap.ALLOWED_DARKNESS_SHIFT_XMAX * 24);
			//int mapHeightPixels = (int) camera.map.getBounds().getHeight() + (TileMap.MAP_OFFSET_TILES * 24) + (TileMap.ALLOWED_DARKNESS_SHIFT * 24);
			
			//newOffsetX = camera.getOffsetX() + (float) Math.floor(dx * SCROLL_SPEED * delta);
			//newOffsetY = camera.getOffsetY() + (float) Math.floor(dy * SCROLL_SPEED * delta);
			
			float lowerBoundX = 0;//camera.map.getBounds().getMinX() - (2 * TileMap.MAP_OFFSET_TILES * 24) + (TileMap.ALLOWED_DARKNESS_SHIFT * 24);
			float lowerBoundY = 0;//camera.map.getBounds().getMinY() - (2 * TileMap.MAP_OFFSET_TILES * 24) + (TileMap.ALLOWED_DARKNESS_SHIFT * 24);
			
			// Clamp offset by map size
			if (newOffsetX > lowerBoundX) {
				newOffsetX = lowerBoundX;
			} else if (mapWidthPixels + newOffsetX < gc.getWidth()) {
				//newOffsetX = -(mapWidthPixels - gc.getWidth());
			}
			
			if (newOffsetY > lowerBoundY) {
				newOffsetY = lowerBoundY;
			} else if (mapHeightPixels + newOffsetY < gc.getHeight()) {
				//newOffsetY = -(mapHeightPixels - gc.getHeight());
			}
		//}

		//camera.setOffset(newOffsetX, newOffsetY);*/
	}

}
