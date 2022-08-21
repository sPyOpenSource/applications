package AI;

import AI.Models.Info;

/**
 * This is the logic class of AI.
 * 
 * @author X. Wang 
 * @version 1.0
 */
public class AILogic extends AIZeroLogic
{
    // instance variables
    private final double threshold = 1000000, filter = 0.99;
    private static long state = System.currentTimeMillis();
    //private final MotionDetection colorCamera;
    //private final PointCloud depthCamera;
    //private final VectorFilter accFilter, magFilter;

    /**
     * Constructor for objects of class AILogic
     * @param mem
     */
    public AILogic(AIMemory mem)
    {
        // Initialize instance variables        
	super(mem);
        //StartTimer.main(new String[]{"TimerManager"});
        //colorCamera  = new MotionDetection(filter, threshold);
        //depthCamera = new PointCloud();
        /*accFilter = new VectorFilter(10, 10, 0.0001, 0.1, 0.1 / 3);
        magFilter = new VectorFilter(10, 10, 0.0001, 0.1, 0.1 / 3);
        accFilter.init(new Vector3D(0d, 0d, 0d), new Vector3D(0d, 0d, 0d));
        magFilter.init(new Vector3D(0d, 0d, 0d), new Vector3D(0d, 0d, 0d));*/
    }

    public void ProcessImages() {    
        /*if (index % 60 == 0){
            colorCamera.saveBack("/home/spy/color.jpg");
        }
        index++;
        colorCamera.UpdatePosition(mem.dequeFirst("colorCameraImages"));
        depthCamera.Calculate(mem.dequeFirst("depthCameraImages"), colorCamera.getX(), colorCamera.getY());*/
    }

    @Override
    protected void loop() {
        ProcessImages();
    }

    protected void MessagesLogic(Info info) {
        /*String[] result = info.getPayload().split(",");
        if (result.length == 9){
            Vector3D gyr = new Vector3D(Double.parseDouble(result[3]), Double.parseDouble(result[4]), Double.parseDouble(result[5]));
            //VectorMat answerAcc = accFilter.Filter(new Vector3D(Double.parseDouble(result[0]), Double.parseDouble(result[1]), Double.parseDouble(result[2])), gyr);
            //mem.addInfo(new Info(answerAcc.getX(0).Display()), "outgoingMessages");
        }*/
    }
    
    private static byte LSFR4(){
        long newbit = (state ^ (state >> 3)) & 1;
        state = ((state >> 1) | (newbit << 3)) & 0xf;
        if(state == 0){
            state = (1 << 3) | 1;
        }
        return (byte) state;
    }
    
    private static byte LSFR8(){
        long newbit = ((state >> 7) ^ (state >> 5) ^ (state >> 4) ^ (state >> 3)) & 1;
        state = ((state >> 1) | (newbit << 7)) & 0xff;
        if(state == 0){
            state = (1 << 7) | 1;
        }
        return (byte) state;
    }
    
    private static short LSFR16(){
        long newbit = ((state >> 15) ^ (state >> 13) ^ (state >> 12) ^ (state >> 10)) & 1;
        state = ((state >> 1) | (newbit << 15)) & 0xffff;
        if (state == 0){
            state = (1 << 15) | 1;
        }
        return (short) state;
    }
    
    private static int LSFR32(){
        long newbit = ((state >> 31) ^ (state >> 21) ^ (state >> 1) ^ state) & 1;
        state = ((state >> 1) | (newbit << 31)) & 0xffffffff;
        if(state == 0){
            state = (1 << 31) | 1;
        }
        return (int) state;
    }
    
    public static void main(String[] args){
        for (int i = 0; i < 20; i++){
            System.out.println(LSFR4());
        }
        for (int i = 0; i < 40; i++){
            System.out.println(LSFR8());
        }
        for(int i = 0; i < 100; i++){
            System.out.println(LSFR16());
        }
        for(int i = 0; i < 200; i++){
            System.out.println(LSFR32());
        }
    }
}