package cr0s.javara.main;

import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.application.Application;
import javafx.scene.text.Text;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class StateLoadingScreen extends Application {

    public static final int STATE_ID = 3;


    private static final Color BACKGROUND_COLOR = Color.rgb(32, 0, 0, 1);


    Stage c;

    private boolean isLoading = false;
    private int ticksBeforeLoading = 10;
    private int ticks, i;


    private String[] strings = {
	    "Filling Crates...", 
	    "Charging Capacitors...", 
	    "Reticulating Splines...", 
	    "Planting Trees...", 
	    "Building Bridges...", 
	    "Aging Empires...", 
	    "Compiling EVA...", 
	    "Constructing Pylons...", 
	    "Activating Skynet...", 
	    "Splitting Atoms...",
    };

    public static void main(String[] args){
        launch();
    }
    
    public int getID() {
	return this.STATE_ID;
    }

    public void init(Stage c)
	    throws Exception {
	this.c = c;
	
	this.i = (int) (Math.random() * this.strings.length);
    }

    @Override
    public void start(Stage c) throws InterruptedException {
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 800, 400);
	scene.setFill(BACKGROUND_COLOR);
	//g.fillRect(0, 0, c.getWidth(), c.getHeight());

	String s = this.strings[this.i];
        Text textFieldID = new Text(s);
        textFieldID.setStyle("-fx-fill: darkcyan");
        VBox box = new VBox(10); // 10 is the spacing between children
        root.setPadding(new Insets(20));
	//float x = c.getWidth() - 15 - g.getFont().getWidth(s);
	//float y = c.getHeight() - 15 - g.getFont().getHeight(s);

	//g.getFont().drawString(x, y, s);
        box.getChildren().add(textFieldID);
        root.getChildren().add(box);
        c.setScene(scene);
        c.show();
    }

    public void update(Stage arg0, int arg2)
	    throws Exception {
	if (!this.isLoading) {		
	    if (--this.ticksBeforeLoading < 0) {
		this.i = (int) (Math.random() * this.strings.length);
		this.isLoading = true;

		//new Thread(new Runnable() {
		//	    @Override
		//	    public void run() {
		//Main.getInstance().startNewGame("");
		//Main.getInstance().enterState(1);
		//	    }
		//	}).start();		
		//  }
		//}
	    }
	}
    }
}
