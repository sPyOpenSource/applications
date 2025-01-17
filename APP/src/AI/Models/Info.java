package AI.Models;

import java.io.Serializable;

/**
 *
 * @author X. Wang
 */
public class Info implements Serializable{
    private final String name;
    private final long t = System.currentTimeMillis();
    
    public Info(String name){
        this.name = name;
    }
    
    public String getPayload(){
        return name;
    }
    
    public long getTime(){
        return t;
    }
}
