package cr0s.javara.ui.sbpages.infantry;

import cr0s.javara.entity.infantry.EntityInfantry;
import cr0s.javara.ui.sbpages.SideBarItemsButton;
import cr0s.javara.util.Pos;

public class InfantrySidebarButton extends SideBarItemsButton {

    private EntityInfantry targetInfantry;
    
    public InfantrySidebarButton(String aDescription, String textureName,
	    Pos pagePos, int aPosX, int aPosY, boolean aIsVisible, EntityInfantry aTargetInfantry) {
	super(aDescription, textureName, pagePos, aPosX, aPosY, aIsVisible);
	
	this.targetInfantry = aTargetInfantry;
    }

    public EntityInfantry getTargetInfantry() {
	return this.targetInfantry;
    }
}
