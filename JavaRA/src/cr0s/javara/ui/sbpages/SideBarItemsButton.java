package cr0s.javara.ui.sbpages;

import cr0s.javara.main.GUI;
import cr0s.javara.resources.ResourceManager;
import cr0s.javara.resources.ShpTexture;
import cr0s.javara.util.Pos;
import java.awt.image.BufferedImage;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

public abstract class SideBarItemsButton {
    private ShpTexture buttonTexture;
    private String textureName;
    private BufferedImage buttonImg;
    
    private Pos position;
    private boolean isVisible;
    private Color disabledColor = Color.rgb(0, 0, 0, 200f/255);
    
    public int posX, posY;
    
    private String description;
    
    public SideBarItemsButton(String aDescription, String aTextureName, Pos pagePos, int aPosX, int aPosY, boolean aIsVisible) {
	this.position = new Pos(pagePos.getX() + aPosX * 64, pagePos.getY() + aPosY * 48);
	this.posX = aPosX;
	this.posY = aPosY;
	
	this.description = aDescription;
	this.isVisible = aIsVisible;
	
	if (!aTextureName.isEmpty()) {
	    this.textureName = aTextureName;
	    this.buttonTexture = ResourceManager.getInstance().getSidebarTexture(aTextureName);
	    this.buttonImg = buttonTexture.getAsImage(0, Color.RED);//GUI.getInstance().getSideBar().getPlayer().playerColor);
	}
    }
    
    public void render(Scene g, Color filterColor) {
	//this.buttonImg.draw(position.getX(), position.getY(), filterColor);
    }

    public void renderDisabled(Scene g, Color filterColor) {
	/*this.buttonImg.draw(position.getX(), position.getY(), filterColor);
	Color pColor = g.getColor();
	g.setColor(disabledColor.multiply(filterColor));
	g.fillRect(position.getX(), position.getY(), 64, 48);
	g.setColor(pColor);*/
    }
        
    
    public Pos getPosition() {
	return this.position;
    }
    
    public boolean isVisible() {
	return this.isVisible;
    }

    public String getDescription() {
	return this.description;
    }
    
    /*public Image getTexture() {
	return this.buttonImg;
    }*/

    public void setVisible(boolean b) {
	this.isVisible = b;
    }

    public String getTextureName() {
	return this.textureName;
    }
}
