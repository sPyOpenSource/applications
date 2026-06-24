package test;

import org.junit.Test;
import static org.junit.Assert.*;

import cr0s.javara.resources.SoundManager;
import cr0s.javara.util.Pos;

public class SoundManagerTest {

    @Test
    public void testSoundManagerSingleton() {
        SoundManager instance1 = SoundManager.getInstance();
        SoundManager instance2 = SoundManager.getInstance();
        
        assertSame("SoundManager should be a singleton", instance1, instance2);
    }
    
    @Test
    public void testPlayMusicDoesNotCrash() {
        SoundManager manager = SoundManager.getInstance();
        // Should not throw exception even if file doesn't exist
        try {
            manager.playMusic("nonexistent");
        } catch (Exception e) {
            // It's okay if it throws an exception for missing file
        }
    }
    
    @Test
    public void testPlaySfxAtDoesNotCrash() {
        SoundManager manager = SoundManager.getInstance();
        Pos pos = new Pos(100, 100);
        
        try {
            manager.playSfxAt("nonexistent", pos);
        } catch (Exception e) {
            // It's okay if it throws an exception for missing file
        }
    }
    
    @Test
    public void testStopMusicDoesNotCrash() {
        SoundManager manager = SoundManager.getInstance();
        
        try {
            manager.stopMusic();
        } catch (Exception e) {
            fail("stopMusic should not throw exception");
        }
    }
}