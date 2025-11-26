package cr0s.javara.main;

import java.util.ArrayList;
import assets.Assets;

import cr0s.javara.ui.cursor.CursorManager;
import cr0s.javara.ui.cursor.CursorType;
import cr0s.javara.util.SpriteSheet;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class StateMainMenu extends Application {

	public static final int STATE_ID = 0;
	
	private final ArrayList<MainMenuItem> menuItems = new ArrayList<>();
	private final Assets assets = new Assets();
        
	private SpriteSheet ss;
	private ImageView menuBackground;
	private final int menuWidth = 480;
	private final float menuX = 155f;
	private final float menuY = 25f;
	
	private ImageView menuFrameVert;
	private final int MENU_FRAME_VERT_HEIGHT = 192, MENU_FRAME_VERT_WIDTH = 18;

	private ImageView menuFrameHoriz;
	private final int MENU_FRAME_HORIZ_HEIGHT = 18, MENU_FRAME_HORIZ_WIDTH = 191;
	
	private ImageView menuFrameTick;
	private final int MENU_FRAME_TICK_HEIGHT = 20, MENU_FRAME_TICK_WIDTH = 18;
	private final int MENU_BUTTONS_SPACE = 25;
	
	private ImageView menuButton, menuButtonMouseover, menuButtonPressed;
	private final int MENU_BUTTON_SIZE_SHEET = 128; // In sprites sheet
	private final int MENU_BUTTON_HEIGHT = 60;
	private final int MENU_BUTTON_WIDTH = menuWidth - MENU_BUTTONS_SPACE * 2;
	
	private int MENU_HEIGHT;
	private boolean leftMousePressed = false;
	
	Stage c;
	
        public static void main(String[] args){
            launch();
        }
        
	public void mousePressed(int button, int x, int y) {
		if (button == 0) {
			this.leftMousePressed = true;
		}
	}

	public void mouseReleased(int button, int arg1, int arg2) {
		if (button == 0) {
			this.leftMousePressed = false;
			
			for (MainMenuItem m : menuItems) {
				if (m.isSelected) {
					switch (m.id) {
						case 0:
							//Main.getInstance().enterState(3);
							break;
						case 2:
							//c.exit();
							break;
					}
				}
			}
		}
	}
	
	public boolean isAcceptingInput() {
		// TODO Auto-generated method stub
		return true;
	}

	public void enter(Stage c)
			throws Exception {
	    CursorManager.getInstance().setCursorType(CursorType.CURSOR_POINTER);
	    
	    // Place menu on middle of screen
	    //this.menuX = (c.getWidth() / 2) - this.menuWidth / 2;
	}

	public int getID() {
		return StateMainMenu.STATE_ID;
	}

        @Override
	public void start(Stage c) throws InterruptedException {
            BorderPane root = new BorderPane();
            Scene scene = new Scene(root, 800, 400);
            this.c = c;

            menuItems.add(new MainMenuItem(0, "Start test game"));
            menuItems.add(new MainMenuItem(1, "Options"));
            menuItems.add(new MainMenuItem(2, "Quit"));

            this.MENU_HEIGHT = MENU_FRAME_HORIZ_HEIGHT + menuItems.size() * (this.MENU_BUTTON_HEIGHT + MENU_BUTTONS_SPACE);

            try {
                this.ss = new SpriteSheet(assets.get("/assets/png/dialog.png"), 1024, 512);

                this.menuBackground = ss.getSubImage(0, 0, menuWidth, MENU_HEIGHT);
                this.menuFrameVert = ss.getSubImage(480, 0, MENU_FRAME_VERT_WIDTH, MENU_FRAME_VERT_HEIGHT);
                this.menuFrameHoriz = ss.getSubImage(0, 480, MENU_FRAME_HORIZ_WIDTH, MENU_FRAME_HORIZ_HEIGHT);

                this.menuFrameTick = ss.getSubImage(191, 480, MENU_FRAME_TICK_WIDTH, MENU_FRAME_TICK_HEIGHT);

                this.menuButton = ss.getSubImage(512, -1, MENU_BUTTON_SIZE_SHEET, MENU_BUTTON_SIZE_SHEET + 2);
                this.menuButtonMouseover = ss.getSubImage(512, 127, MENU_BUTTON_SIZE_SHEET, MENU_BUTTON_SIZE_SHEET);
                this.menuButtonPressed = ss.getSubImage(512 + 128, 127, MENU_BUTTON_SIZE_SHEET, MENU_BUTTON_SIZE_SHEET);
            } catch (Exception e) {
                e.printStackTrace();
            }

		//g.clear();
		
		//this.menuBackground.draw(menuX, menuY);
		root.getChildren().add(menuBackground);
                //root.getChildren().add(menuButton);
                /*root.getChildren().add(menuFrameVert);
                root.getChildren().add(menuFrameHoriz);
                root.getChildren().add(menuFrameTick);
                root.getChildren().add(menuButtonMouseover);
                root.getChildren().add(menuButtonPressed);*/
		// Draw frames
		//Image scaledFrameVert = this.menuFrameVert.getScaledCopy(MENU_FRAME_VERT_WIDTH, MENU_HEIGHT);
		//scaledFrameVert.draw(menuX - MENU_FRAME_VERT_WIDTH, menuY);
		//scaledFrameVert.draw(menuX + menuWidth, menuY);
		
		//Image scaledFrameHoriz = this.menuFrameHoriz.getScaledCopy(menuWidth + 2 * MENU_FRAME_TICK_WIDTH, MENU_FRAME_HORIZ_HEIGHT);
		//scaledFrameHoriz.draw(menuX - MENU_FRAME_TICK_WIDTH, menuY - MENU_FRAME_HORIZ_HEIGHT);
		//scaledFrameHoriz.draw(menuX - MENU_FRAME_TICK_WIDTH, menuY + MENU_HEIGHT);
		
		// Menu ticks in corners
		/*this.menuFrameTick.draw(menuX - MENU_FRAME_TICK_WIDTH, menuY - MENU_FRAME_TICK_HEIGHT);
		this.menuFrameTick.draw(menuX + menuWidth, menuY - MENU_FRAME_TICK_HEIGHT);
		
		this.menuFrameTick.draw(menuX - MENU_FRAME_TICK_WIDTH, menuY + MENU_HEIGHT);
		this.menuFrameTick.draw(menuX + menuWidth, menuY + MENU_HEIGHT);
		*/
		
		// Draw menu buttons
		int itemsSize = this.menuItems.size();
		//Image menuItem = menuButton.getScaledCopy(MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
		//Image menuItemMouseover = menuButtonMouseover.getScaledCopy(MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
		//Image menuItemPressed = menuButtonPressed.getScaledCopy(MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
		
		int mouseX = 0;//c.getInput().getMouseX();
		int mouseY = 0;//c.getInput().getMouseY();
		
		for (int i = 0; i < itemsSize; i++) {
			MainMenuItem m = this.menuItems.get(i);
			
			float itemX = this.menuX + 25.0f;
			float itemY = this.menuY + 25.0f + (i * (this.MENU_BUTTON_HEIGHT + this.MENU_BUTTONS_SPACE));
			
			//m.boundingBox.setBounds(itemX, itemY, this.MENU_BUTTON_WIDTH, this.MENU_BUTTON_HEIGHT);
			
			if (m.boundingBox.contains(mouseX, mouseY)) {
				if (!this.leftMousePressed) {
					m.isMouseOver = true;
					m.isSelected = false;
					
					//menuItemMouseover.draw(itemX, itemY);
				} else {
					m.isSelected = true;
					//menuItemPressed.draw(itemX, itemY);
				}
			} else {
				m.isMouseOver = false;
				m.isSelected = false;
				
				//menuItem.draw(itemX, itemY);
			}
			
			//c.getDefaultFont().drawString(itemX + (MENU_BUTTON_WIDTH / 2) - (c.getDefaultFont().getWidth(m.text) / 2), itemY + MENU_BUTTON_HEIGHT / 2 - (c.getDefaultFont().getLineHeight() / 2), m.text);
			
		}
		
		//CursorManager.getInstance().drawCursor(g);
                c.setScene(scene);
                c.show();
	}

	public void update(Stage arg0, int arg2)
			throws Exception {
	    CursorManager.getInstance().update();
	}
	
	private class MainMenuItem {
		public int id;
		public String text;
		
		public Rectangle boundingBox = new Rectangle(0, 0, 0, 0);
		public boolean isMouseOver = false;
		public boolean isSelected = false;
		
		public MainMenuItem(int id, String text) {
			this.id = id;
			this.text = text;
		}
	}

}
