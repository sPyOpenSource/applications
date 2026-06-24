package cr0s.javara.resources;

import cr0s.javara.main.GUI;
import cr0s.javara.util.Pos;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import soundly.XSound;

public class SoundManager {
    private static SoundManager instance;
    
    // Audio layers (lower number = higher priority)
    private static final int LAYER_MUSIC = 0;
    private static final int LAYER_SFX = 1;
    private static final int LAYER_UNIT = 2;
    private static final int LAYER_SPEECH = 3;
    
    private double listenerX, listenerY;
    private MediaPlayer currentMusic;
    private final Map<String, MediaPlayer> activeSounds = new HashMap<>();
    
    private SoundManager() {}
    
    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }
    
    public void playSoundGlobal(XSound snd) {
        if (snd != null && snd.getClip() != null) {
            MediaPlayer clip = snd.getClip();
            clip.play();
            activeSounds.put(snd.getDescription(), clip);
        }
    }
    
    public void playMusic(String sound) {
        stopMusic();
        
        //String filePath = getSoundPath("mix/scores.mix", sound + ".wav");
        String filePath = getSoundPath("sounds", "success.wav");

        Media media = new Media(new File(filePath).toURI().toString().replace("JavaRA","assets/src"));
        currentMusic = new MediaPlayer(media);
        currentMusic.setVolume(30);
        currentMusic.setCycleCount(MediaPlayer.INDEFINITE);
        currentMusic.play();
        
        activeSounds.put("music", currentMusic);
    }
    
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }
    
    public void playSfxAt(String sound, Pos pos) {
        if (sound == null || sound.isEmpty()) {
            return;
        }
        
        String filePath = getSoundPath("sounds.mix", sound + ".wav");
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Sound file not found: " + filePath);
            return;
        }
        
        MediaPlayer player = new MediaPlayer(new Media(file.toURI().toString()));
        player.setVolume(100);
        
        // Basic 2D positional audio
        double distance = Math.sqrt(pos.getX() * pos.getX() + pos.getY() * pos.getY());
        double volume = Math.max(0, Math.min(1, 1 - distance / 300));
        player.setVolume((float)(volume * 100));
        
        player.play();
        activeSounds.put(sound, player);
    }
    
    public void update(int delta) {
        listenerX = -GUI.getInstance().getCamera().getTranslateX() + 
                   GUI.getInstance().getContainer().getWidth() / 2;
        listenerY = -GUI.getInstance().getCamera().getTranslateY() + 
                   GUI.getInstance().getContainer().getHeight() / 2;
    }
    
    public void stopAllSounds() {
        for (MediaPlayer player : activeSounds.values()) {
            player.stop();
            player.dispose();
        }
        activeSounds.clear();
    }
    
    private String getSoundPath(String mixname, String name) {
        String currentDir = System.getProperty("user.dir");
        
        String filePath = currentDir + "/assets/" + mixname + "/" + name;
        
        File file = new File(filePath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        
        filePath = currentDir + "/assets/" + mixname + "/" + name;
        file = new File(filePath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        
        return filePath;
    }
}