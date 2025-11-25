
package soundly;

/**
 * A simpler version of Slick's Audio class, more suitable for Soundly.
 * 
 * @author Matt
 */
public class AudioData {

    /** The buffer containing the sound */
    private int buffer = 0;
    /** The length of the audio */
    private float length = 0;
    
    private boolean stream = false;
    
    public AudioData(int buffer) {
        this.buffer = buffer;
        stream = false;
        
        int bytes = 0;//AL10.alGetBufferi(buffer, AL10.AL_SIZE);
        int bits = 0;//AL10.alGetBufferi(buffer, AL10.AL_BITS);
        int channels = 0;//AL10.alGetBufferi(buffer, AL10.AL_CHANNELS);
        int freq = 0;//AL10.alGetBufferi(buffer, AL10.AL_FREQUENCY);

        int samples = bytes / (bits / 8);
        this.length = (samples / (float) freq) / channels;
    }
    
    /** Creates an empty AudioData for use with streaming audio. */
    public AudioData() {
        stream = true;
    }
    
    /** Returns true if this data is being streamed. */
    public boolean isStreaming() {
        return stream;
    }
    
    public int getBufferID() {
        return buffer;
    }
    
    public float getLength() {
        return length;
    }
}
