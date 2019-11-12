package test;

import AI.AIBaseMemory;
import AI.Models.Info;
import AI.util.PID;

import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import org.junit.Test;

/**
 *
 * @author X. Wang
 */
public class JUnitTest extends TestCase{
    private final static AIBaseMemory MEMORY = new AIBaseMemory();

    @Test
    public void testIncomingMessages(){
        System.out.println("* JUnitTest: memoryTestIncomingMessages()");
        MEMORY.addInfo(new Info("configure:test"), "incomingMessages");
        assertEquals("configure:test", MEMORY.dequeFirst("incomingMessages").getPayload());
    }
    
    @Test
    public void testOutgoingMessages2ArduinoDequeLast(){
        System.out.println("* JUnitTest: memoryTestOutgoingMessages2ArduinoDequeLast()");
        MEMORY.addInfo(new Info("test1"), "outgoingMessages2Arduino");
        assertEquals("test1", MEMORY.dequeLast("outgoingMessages2Arduino").getPayload());
    }
    
    @Test
    public void testConfiguresGetLast(){
        System.out.println("* JUnitTest: memoryTestConfiguresGetLast()");
        MEMORY.addInfo(new Info("test2"), "configures");
        assertEquals("test2", MEMORY.dequeLast("configures").getPayload());
    }
    
    @Test
    public void testEmotions(){
        System.out.println("* JUnitTest: testEmotions()");
        MEMORY.addEmotion();
        assertEquals(MEMORY.getEmotion(), 0.1);
    }
    
    @Test
    public void testLogpath(){
        System.out.println("* JUnitTest: testLogpath()");
        assertEquals(MEMORY.getLogPath(), null);
        MEMORY.setLogPath("test");
        assertEquals(MEMORY.getLogPath(), "test");
    }
    
    @Test
    public void testPID(){
        PID pid = new PID(1, 1, 1);
        System.out.println("* JUnitTest: testPID()");
        double y = pid.Compute(1, 1, 1);
        assertEquals(y, - 3.0);
        y = pid.Compute(10, 20, 1);
        assertEquals(y, - 31.0);
        y = pid.Compute(20, 30, 1);
        assertEquals(y, -71.0);
        y = pid.Compute(50, 20, 1);
        assertEquals(y, - 150.0);
        y = pid.Compute(- 150, 20, 1);
        assertEquals(y, 150.0);
        y = pid.Compute(0, 20, 1);
        assertEquals(y, 69.0);
        y = pid.Compute(10, 10, 10);
        assertEquals(y, - 51.0);
        y = pid.Compute(10, 10, 10);
        assertEquals(y, - 150.0);
    }
}
