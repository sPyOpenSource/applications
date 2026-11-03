package cr0s.javara.ui.sbpages.infantry;

import cr0s.javara.main.GUI;
import cr0s.javara.resources.SoundManager;
import cr0s.javara.ui.sbpages.SideBarItemsButton;
import cr0s.javara.ui.sbpages.SideBarPage;
import cr0s.javara.util.Pos;

public class PageInfantry extends SideBarPage {

    public PageInfantry(Pos pos) {
	super(pos);

	addButton(new InfantrySidebarButton("Shock Trooper", "shokicon.shp", this.getPosition(), 0, 4, false, null));
	addButton(new InfantrySidebarButton("Jetpack Soldier", "e5icon.shp", this.getPosition(), 1, 4, false, null));

	addButton(new InfantrySidebarButton("GLA Rebel", "glaRebelicon.shp", this.getPosition(), 0, 5, false, null));
	addButton(new InfantrySidebarButton("GLA Rocket Soldier", "glaRocketicon.shp", this.getPosition(), 1, 5, false, null));

	//addButton(new InfantrySidebarButton("", "icon.shp", this.getPosition(), 1, 5, false, null));
	//addButton(new InfantrySidebarButton("", "icon.shp", this.getPosition(), 1, 5, false, null));

	addButton(new InfantrySidebarButton("Flamethrower", "e4icon.shp", this.getPosition(), 0, 6, false, null));
	addButton(new InfantrySidebarButton("GLA Worker", "glaWorkericon.shp", this.getPosition(), 1, 6, false, null));
	//addButton(new InfantrySidebarButton("", "icon.shp", this.getPosition(), 1, 6, false, null));

	addButton(new InfantrySidebarButton("Rocket Soldier", "e3icon.shp", this.getPosition(), 0, 7, false, null));
	addButton(new InfantrySidebarButton("Tanya", "e7icon.shp", this.getPosition(), 1, 7, false, null));

	addButton(new InfantrySidebarButton("Grenade Trooper", "e2icon.shp", this.getPosition(), 0, 8, false, null));
	addButton(new InfantrySidebarButton("GLA Terrorist", "glaTerroristicon.shp", this.getPosition(), 1, 8, false, null));

	addButton(new InfantrySidebarButton("Riffle Trooper", "e1icon.shp", this.getPosition(), 0, 9, false, null));
	addButton(new InfantrySidebarButton("Engineer", "e6icon.shp", this.getPosition(), 1, 9, false, null));

	addButton(new InfantrySidebarButton("Spy", "spyicon.shp", this.getPosition(), 0, 10, false, null));
	addButton(new InfantrySidebarButton("Sniper", "snipericon.shp", this.getPosition(), 1, 10, false, null));
	addButton(new InfantrySidebarButton("Seal Force", "sealicon.shp", this.getPosition(), 0, 11, false, null));
	addButton(new InfantrySidebarButton("Hacker", "hackericon.shp", this.getPosition(), 1, 11, false, null));
	addButton(new InfantrySidebarButton("Dog", "dogicon.shp", this.getPosition(), 0, 12, false, null));
	addButton(new InfantrySidebarButton("Robotic Dog", "robodogicon.shp", this.getPosition(), 1, 12, false, null));
	addButton(new InfantrySidebarButton("Humanoid Robot", "humanoidroboticon.shp", this.getPosition(), 0, 13, false, null));
	addButton(new InfantrySidebarButton("Terror Drone", "terrordroneicon.shp", this.getPosition(), 1, 13, false, null));
    }

    @Override
    public void update(int delta) {
	for (SideBarItemsButton button : this.buttonsHash.values()) {
	    button.setVisible(GUI.getInstance().getPlayer().getBase().getProductionQueue().isBuildable(button.getTextureName()));
	}
    }

    @Override
    public void buttonClicked(SideBarItemsButton button) {
	super.buttonClicked(button);
	//SoundManager.getInstance().playSpeechSoundGlobal("train1");
    }

}
