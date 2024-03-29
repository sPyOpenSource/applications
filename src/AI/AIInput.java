package AI;

/**
 * This is the input class of AI.
 * 
 * @author X. Wang 
 * @version 1.0
 */

public class AIInput extends AIZeroInput
{
    /**
     * This is the initialization of AIInput class 
     */
    //private BufferedReader in;
    
    /**
     * Constructor for objects of class AIInput
     * @param mem
     */
    public AIInput(AIMemory mem)
    {
        super(mem);
        /*try {
            in = new BufferedReader(new InputStreamReader(mem.getSerialPort().getInputStream()));
        } catch (IOException |NullPointerException ex) {
            Logger.getLogger(AIInput.class.getName()).log(Level.SEVERE, null, ex);
        }
        capColorCamera.open(0);
        capDepthCamera.open(1);
        int fourcc = VideoWriter.fourcc('Z', '1', '6', ' ');
        capDepthCamera.set(Videoio.CAP_PROP_FOURCC, fourcc);
        mem.addInfo(new Info(capColorCamera), "the webcam");
        mem.addInfo(new Info(capDepthCamera), "the webcam");*/
    }
    
    private void ReadMessageFromArduino(){
        /*try {
            mem.addInfo(new Info(in.readLine()), "incomingMessages");         
        } catch (IOException ex) {
            Logger.getLogger(AIInput.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    @Override
    protected void setup() {
        /*Thread ReadMessageFromArduino = new Thread(){
            @Override
            public void run(){
                while(true)
                    ReadMessageFromArduino();
            }
        };*/
        //ReadMessageFromArduino.start();
        /*Thread getImageFromWebcamColorCamera = new Thread(){
            @Override
            public void run(){
                while(true)
                    getImageFromWebcam(capColorCamera, "colorCameraImages");
            }
        };
        //getImageFromWebcamColorCamera.start();
        Thread getImageFromWebcamDepthCamera = new Thread(){
            @Override
            public void run(){
                while(true)
                    getImageFromWebcam(capDepthCamera, "depthCameraImages");
            }
        };*/
        //getImageFromWebcamDepthCamera.start();
    }
}