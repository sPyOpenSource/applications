package jx.keyboard;

import jx.zero.*;
import jx.wm.WindowManager;

public class Main
{
    public static void main(String[] args) {
	KeyboardImpl keyboard;
	
	keyboard = new KeyboardImpl();

	if (keyboard.keyboardHardwareAvailable()) {
	    Debug.out.println ("    init keyboard");
	    keyboard.init();
	    Debug.out.println ("    keyboard initialized");

	    Naming naming = InitialNaming.getInitialNaming();
	    String wmName = args.length > 0 ? args[0] : "WindowManager";
	    WindowManager m_cWindowManager =
		(WindowManager)LookupHelper.waitUntilPortalAvailable(naming, wmName);
	    new KeyboardListener (m_cWindowManager, keyboard);
	    new MouseListener (m_cWindowManager, keyboard);
	} else {
	    Debug.out.println ("************ NO KEYBOARD FOUND!! **************");
	}
    }
}
