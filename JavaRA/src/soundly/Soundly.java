package soundly;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.HashMap;
import java.util.Map;
import java.io.File;

public class Soundly {
    private static final Soundly instance = new Soundly();
    
    // Active media players indexed by file path
    private final Map<String, MediaPlayer> activePlayers = new HashMap<>();
    // Cache loaded media files
    private final Map<String, Media> mediaCache = new HashMap<>();
    
    private Soundly() {
        // JavaFX MediaPlayer and related classes handle audio initialization automatically
        // No need for explicit initialization
    }
    
    public static Soundly get() {
        return instance;
    }
    
    
    public Media getMedia(String filePath) {
        if (!mediaCache.containsKey(filePath)) {
            Media media = new Media(new File(filePath).toURI().toString());
            mediaCache.put(filePath, media);
        }
        return mediaCache.get(filePath);
    }
    
    
    public MediaPlayer playSound(String filePath, float volume, boolean loop) {
        // Stop if already playing
        stopSound(filePath);
        
        // Get or load media
        Media media = getMedia(filePath);
        if (media == null) {
            return null;
        }
        
        // Create and configure player
        MediaPlayer player = new MediaPlayer(media);
        player.setVolume(volume * 100); // Convert 0-1 to 0-100 for JavaFX
        player.setCycleCount(loop ? MediaPlayer.INDEFINITE : 1);
        
        // Auto-cleanup when sound finishes
        player.setOnEndOfMedia(() -> {
            player.dispose();
            activePlayers.remove(filePath);
        });
        
        // Start playback
        player.play();
        activePlayers.put(filePath, player);
        return player;
    }
    
    
    public void stopSound(String filePath) {
        MediaPlayer player = activePlayers.get(filePath);
        if (player != null) {
            player.stop();
            player.dispose();
            activePlayers.remove(filePath);
        }
    }
    
    
    public void setSoundPosition(String filePath, double x, double y) {
        MediaPlayer player = activePlayers.get(filePath);
        if (player != null) {
            // Simple distance-based volume attenuation
            // Volume decreases with distance from listener
            double distance = Math.sqrt(x * x + y * y);
            double volume = Math.max(0, Math.min(1, 1 - distance / 500));
            player.setVolume((float)(volume * 100));
        }
    }
    
    
    public void stopAllSounds() {
        for (MediaPlayer player : activePlayers.values()) {
            player.stop();
            player.dispose();
        }
        activePlayers.clear();
    }
    
    
    public boolean isPlaying(String filePath) {
        MediaPlayer player = activePlayers.get(filePath);
        return player != null && 
               player.getStatus() == MediaPlayer.Status.PLAYING;
    }
}