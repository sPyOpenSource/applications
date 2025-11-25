
package utilities;

import java.text.DecimalFormat;
import java.util.Date;

/**
 * @author Adrian
 *
 * I bet there's a system utility somewhere to do this...
 * 
 */
public class Timer extends Thread{
    
    /**
     * Time at the start
     */
    private long t0 = 0;
    
    /**
     * Time now
     */
    private long t1 = 0;
    
    /**
     * Time since last call
     */
    private long delta;
    
    /**
     * for 3 d.p.
     */
    private DecimalFormat threeDec;
    
    /**
     * Static single instance to hold all times
     */
    static private Timer timer = null;
    
    /**
     * What o'clock have you?
     *
     */
    static private void newTime()
    {
        long last = timer.t1;
        Date d = new Date();
        timer.t1 = d.getTime() - timer.t0;
        timer.delta = timer.t1 - last;
    }
    
    /**
     * Check if we've been initialised and initialise if needed
     * @param e
     */
    static private void initialiseIfNeedBe()
    {
        if(timer != null) return;
        timer = new Timer();
        
        newTime();
        timer.t0 = timer.t1;
        timer.threeDec = new DecimalFormat("0.000");
        timer.threeDec.setGroupingUsed(false);
    }
    
    /**
     * Get a double as a 3 d.p. string
     * @param v
     * @return
     */
    static private String d3dp(double v)
    {
        return timer.threeDec.format(v);
    }
    
    /**
     * Generate a timestamp
     * @return
     */
    static public String stamp()
    {
        initialiseIfNeedBe();
        newTime();
        return " [" + d3dp(timer.t1 * 0.001) + "s/" + timer.delta + "ms]";
    }
    
    /**
     * Get the time from the start in seconds
     * @return
     */
    static public double elapsed()
    {
        initialiseIfNeedBe();
        Date d = new Date();
        long e = d.getTime() - timer.t0;
        return 0.001 * (double)e;
    }
        
    @Override
    public void run() {
        stamp();
        while(true){
            System.out.println(elapsed());
        }
    }
}
