package cr0s.javara.main;

import cr0s.javara.util.Pos;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class StateTestScreen extends Application {

    public static final int STATE_ID = 4;
    private static final Color BACKGROUND_COLOR = Color.rgb(32, 0, 0, 1);

    Stage c;

    private float displace;
    private int ticks;

    public static void main(String[] args){
        launch();
    }
    
    public boolean isAcceptingInput() {
	// TODO Auto-generated method stub
	return true;
    }

    public int getID() {
	return StateTestScreen.STATE_ID;
    }

    public void init(Stage c)
	    throws Exception {
	this.c = c;
    }
    
    public static float getRandom(float from, float to) {
	return (float) (from + Math.random() * (to - from));
    }
    
    public static void drawLine(Scene g, double _x1, double _y1, double _x2, double _y2, float thickness)
    {
	//g.setLineWidth(thickness);
	//g.drawLine(_x1, _y1, _x2, _y2);
    }
    
    public static void drawChainLightning(Scene g, Pos[] points, float thickness, int numberOfBolts)
    {
       for (int i = 0;i<points.length-1;i++)
       {
          drawP2PLightning(g, points[i].getX(), points[i].getY(), points[i + 1].getX(), points[i+1].getY(), getRandom(60f, 140f), getRandom(0.8f, 3.8f), thickness, numberOfBolts);
       }
    }
    
    public static void drawP2ALightning(Scene g, float x1, float y1, float x2, float y2, float displace, float detail, float thickness, float noise, int numberOfBolts)
    {
       for (int i=0;i<numberOfBolts;i++)
       {
          //g.setColor(new Color((int) getRandom(14f, 54f), (int) getRandom(100f, 210f), (int) getRandom(200f, 239f), 255));
          drawSingleP2PLightning(g, x1, y1, x2 + getRandom(-noise, noise), y2 + getRandom(-noise, noise), 117, 1.8f, thickness);
       }
    }
    
    public static void drawP2PLightning(Scene g, double x1, double y1, double x2, double y2, float displace, float detail, float thickness, int numberOfBolts)
    {
       for (int i=0;i<numberOfBolts;i++)
       {
	  // g.setColor(new Color((int) getRandom(14f, 54f), (int) getRandom(100f, 210f), (int) getRandom(200f, 239f), 255));
          drawSingleP2PLightning(g, x1, y1, x2, y2, 117, 1.8f, thickness);
       }
    }
    
    public static void drawSingleP2PLightning(Scene g, double x1, double y1, double x2, double y2, float displace, float detail, float thickness)
    {
      if (displace < detail)
      {
         drawLine(g, x1, y1, x2, y2, thickness);
      } else {
        double mid_x = (x2+x1)*0.5f;
        double mid_y = (y2+y1)*0.5f;
        mid_x += (Math.random()-0.5f)*displace;
        mid_y += (Math.random()-0.5f)*displace;
        drawSingleP2PLightning(g, x1,y1,mid_x,mid_y,displace*0.5f, detail, thickness);
        drawSingleP2PLightning(g, x2,y2,mid_x,mid_y,displace*0.5f, detail, thickness);
      }
    }    
    
    @Override
    public void start(Stage c) throws InterruptedException {
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 800, 400);
        scene.setFill(BACKGROUND_COLOR);
	//g.setColor(Color.BLACK);
	//g.clear();
	
	//Graphics g, float x1, float y1, float x2, float y2, float displace, float detail, float thickness, int numberOfBolt
	final int LENGTH = 400;
	//drawP2PLightning(g, c.getWidth() / 2 - LENGTH / 2, c.getHeight() / 2, c.getWidth() / 2 + LENGTH / 2, c.getHeight() / 2, this.displace, 2.0f, 1.0f, 1);
        c.setScene(scene);
        c.show();
    }

    public void update(Stage arg0, int arg2)
	    throws Exception {
	if (++this.ticks % 2 == 0) {
	    this.displace++;
	}
    }
    
}
