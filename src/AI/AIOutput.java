package AI;

/**
 * This is the output of AI.
 * 
 * @author X. Wang
 * @version 1.0
 */
public class AIOutput extends AIZeroOutput
{
    //private Writer w;
 
    /**
     * Constructor for objects of class AIOutput
     * @param mem
     */
    public AIOutput(AIMemory mem)
    {
	super(mem);
        /*try {
            w = new OutputStreamWriter(mem.getSerialPort().getOutputStream(),"UTF-8");
        } catch (IOException | NullPointerException ex) {
            Logger.getLogger(AIOutput.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }
    
    private void Send(){
        /*Info info = mem.dequeFirst("outgoingMessages2Arduino");
        if(info == null)
            return;*/
        /*try {
            w.write(info.getPayload());
            w.flush();
        } catch (IOException ex) {
            Logger.getLogger(AIOutput.class.getName()).log(Level.SEVERE, null, ex);
        }*/
    }

    @Override
    protected void loop() {
        Send();
    }
}