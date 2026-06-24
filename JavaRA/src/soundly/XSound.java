package soundly;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.io.File;

public class XSound {
    private final MediaPlayer mediaPlayer;
    
    public XSound(String filePath) {
        Media media = new Media(new File(filePath).toURI().toString());
        this.mediaPlayer = new MediaPlayer(media);
        this.mediaPlayer.setVolume(100);
    }
    
    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume * 100);
        }
    }
    
    public void setLooping(boolean loop) {
        if (mediaPlayer != null) {
            mediaPlayer.setCycleCount(loop ? MediaPlayer.INDEFINITE : 1);
        }
    }
    
    public void setPosition(double x, double y) {
        if (mediaPlayer != null) {
            double distance = Math.sqrt(x * x + y * y);
            double volume = Math.max(0, Math.min(1, 1 - distance / 500));
            mediaPlayer.setVolume((float)(volume * 100));
        }
    }
    
    public boolean isPlaying() {
        return mediaPlayer != null && 
               mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }
    
    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }
    
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }
    
    public MediaPlayer getClip() {
        return mediaPlayer;
    }
    
    public String getDescription() {
        return "sound_" + System.currentTimeMillis();
    }
}