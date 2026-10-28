package cr0s.javara.resources;

import cr0s.javara.main.GUI;
import cr0s.javara.util.Log;
import cr0s.javara.util.Pos;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import redhorizon.filetypes.aud.AudFile;
import redhorizon.filetypes.mix.MixFile;
import redhorizon.filetypes.mix.MixRecord;

import soundly.XSound;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Central sound playback for JavaRA.
 * <p>
 * Sound effects and music are stored as {@code .aud} (Westwood ADPCM) entries
 * inside {@code .mix} archives (e.g. {@code assets/sounds.mix}).  JavaFX
 * {@link MediaPlayer} cannot play {@code .aud} directly, so entries are extracted
 * from the archive, decoded to PCM, written to a temporary {@code .wav} file and
 * then played.
 */
public class SoundManager {
    private static final Log LOG = Log.of(SoundManager.class);

    private static SoundManager instance;

    // Audio layers (lower number = higher priority)
    private static final int LAYER_MUSIC = 0;
    private static final int LAYER_SFX = 1;
    private static final int LAYER_UNIT = 2;
    private static final int LAYER_SPEECH = 3;

    private double listenerX, listenerY;
    private MediaPlayer currentMusic;
    private File currentMusicTemp;
    private final Map<String, MediaPlayer> activeSounds = new HashMap<>();
    // Temp wav files created for each playing MediaPlayer, cleaned up on end of media.
    private final Map<MediaPlayer, File> tempFiles = new HashMap<>();
    // Cached mix archives, keyed by mix file name (e.g. "sounds.mix").
    private final Map<String, MixFile> mixCache = new HashMap<>();

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

        MediaPlayer player = loadSoundFromMix("sounds.mix", sound + ".aud");
        if (player == null) {
            player = loadSoundFromMix("scores.mix", sound + ".aud");
        }
        if (player == null) {
            // Fallback to a loose file (legacy deployments).
            String filePath = getSoundPath("sounds", sound + ".wav");
            File file = new File(filePath);
            if (file.exists()) {
                player = new MediaPlayer(new Media(file.toURI().toString()));
            }
        }

        if (player == null) {
            LOG.error("Music not found: " + sound);
            return;
        }

        currentMusic = player;
        currentMusicTemp = tempFiles.get(player);
        currentMusic.setVolume(0.5); // JavaFX volume is 0.0 - 1.0
        currentMusic.setCycleCount(MediaPlayer.INDEFINITE);
        currentMusic.play();
        activeSounds.put("music", currentMusic);
    }

    public void stopMusic() {
        MediaPlayer toStop = currentMusic;
        currentMusic = null;
        currentMusicTemp = null;
        if (toStop != null) {
            toStop.stop();
            toStop.dispose();
        }
        File temp = tempFiles.remove(toStop);
        if (temp != null) {
            try {
                Files.deleteIfExists(temp.toPath());
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Play a sound effect at a world position, with distance-based volume
     * attenuation.
     *
     * @param sound the sound effect name (without extension), looked up in
     *              {@code assets/sounds.mix} as {@code <sound>.aud}
     * @param pos   the world position (may be {@code null} for full volume)
     */
    public void playSfxAt(String sound, Pos pos) {
        if (sound == null || sound.isEmpty()) {
            return;
        }

        MediaPlayer player = loadSoundFromMix("sounds.mix", sound + ".aud");
        if (player == null) {
            LOG.debug("Sound effect not found: " + sound);
            return;
        }

        double distance = 0.0;
        if (pos != null) {
            distance = Math.sqrt(pos.getX() * pos.getX() + pos.getY() * pos.getY());
        }
        double volume = Math.max(0, Math.min(1, 1 - distance / 3000.0));
        player.setVolume(volume);

        // Use a unique key per playback so rapid repeated sounds (weapon fire,
        // impacts, explosions) overlap instead of cutting each other off.
        String key = sound + "_" + System.nanoTime();
        File temp = tempFiles.remove(player);
        activeSounds.put(key, player);

        player.setOnEndOfMedia(() -> {
            player.stop();
            player.dispose();
            activeSounds.remove(key);
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp.toPath());
                } catch (IOException ignored) {
                }
            }
        });

        player.play();
    }

    public void update(int delta) {
        listenerX = -GUI.getInstance().getCamera().getTranslateX() +
                GUI.getInstance().getContainer().getWidth() / 2;
        listenerY = -GUI.getInstance().getCamera().getTranslateY() +
                GUI.getInstance().getContainer().getHeight() / 2;
    }

    public void stopAllSounds() {
        stopMusic();
        for (MediaPlayer player : activeSounds.values()) {
            player.stop();
            player.dispose();
        }
        activeSounds.clear();
        for (File temp : tempFiles.values()) {
            try {
                Files.deleteIfExists(temp.toPath());
            } catch (IOException ignored) {
            }
        }
        tempFiles.clear();
    }

    private String getSoundPath(String mixname, String name) {
        String currentDir = System.getProperty("user.dir");

        String filePath = currentDir + "/assets/" + mixname + "/" + name;

        File file = new File(filePath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }

        return filePath;
    }

    /**
     * Load a sound entry from a {@code .mix} archive, decode the {@code .aud}
     * data to PCM, write a temporary {@code .wav} file and return a
     * {@link MediaPlayer} for it.  The temporary file is tracked in
     * {@link #tempFiles} so it can be deleted when playback finishes.
     *
     * @param mixName   the mix archive name (e.g. {@code "sounds.mix"})
     * @param entryName the entry name inside the archive (e.g. {@code "cannon1.aud"})
     * @return a ready-to-play {@link MediaPlayer}, or {@code null} if the entry
     *         could not be loaded or decoded
     */
    private MediaPlayer loadSoundFromMix(String mixName, String entryName) {
        try {
            MixFile mix = mixCache.get(mixName);
            if (mix == null) {
                Path mixPath = Paths.get(System.getProperty("user.dir") + "/assets/" + mixName);
                if (!Files.exists(mixPath)) {
                    LOG.error("Mix file not found: " + mixPath);
                    return null;
                }
                mix = new MixFile(mixName, FileChannel.open(mixPath));
                mixCache.put(mixName, mix);
            }

            MixRecord rec = mix.getEntry(entryName);
            if (rec == null) {
                return null;
            }

            AudFile aud = new AudFile(entryName, mix.getEntryData(rec));
            byte[] pcm = decodeAudToPcm(aud);

            File wav = File.createTempFile("jr-snd-", ".wav");
            wav.deleteOnExit();
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(wav))) {
                writeWav(out, aud, pcm);
            }

            MediaPlayer player = new MediaPlayer(new Media(wav.toURI().toString()));
            tempFiles.put(player, wav);
            return player;
        } catch (IOException e) {
            LOG.error("Failed to load sound " + entryName + " from " + mixName, e);
            return null;
        }
    }

    private byte[] decodeAudToPcm(AudFile aud) throws IOException {
        ReadableByteChannel pcm = aud.getSoundData();
        try (InputStream is = Channels.newInputStream(pcm)) {
            return is.readAllBytes();
        }
    }

    private void writeWav(OutputStream out, AudFile aud, byte[] pcm) throws IOException {
        int channels = aud.channels().size;
        int bits = aud.bitrate().size;
        int rate = aud.frequency();
        int byteRate = rate * channels * bits / 8;
        int blockAlign = channels * bits / 8;
        int dataSize = pcm.length;
        int chunkSize = 36 + dataSize;

        ByteBuffer hdr = ByteBuffer.allocate(44);
        hdr.order(ByteOrder.LITTLE_ENDIAN); // WAV is little-endian
        hdr.put(new byte[] {'R', 'I', 'F', 'F'});
        hdr.putInt(chunkSize);
        hdr.put(new byte[] {'W', 'A', 'V', 'E'});
        hdr.put(new byte[] {'f', 'm', 't', ' '});
        hdr.putInt(16);
        hdr.putShort((short) 1); // PCM
        hdr.putShort((short) channels);
        hdr.putInt(rate);
        hdr.putInt(byteRate);
        hdr.putShort((short) blockAlign);
        hdr.putShort((short) bits);
        hdr.put(new byte[] {'d', 'a', 't', 'a'});
        hdr.putInt(dataSize);
        hdr.flip();

        out.write(hdr.array(), 0, hdr.limit());
        out.write(pcm);
        out.flush();
    }
}
