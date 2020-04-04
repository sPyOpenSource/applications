package test;

import AI.AIMemory;
import AI.Models.InfoZero;

import junit.framework.TestCase;
import static junit.framework.TestCase.assertEquals;
import org.jnode.fs.jfat.CacheElement;
import org.jnode.fs.jfat.CacheKey;
import org.jnode.fs.jfat.CacheMap;
import org.junit.Test;

/**
 *
 * @author X. Wang
 */
public class JUnitTest extends TestCase{
    private final static AIMemory MEMORY = new AIMemory();

    @Test
    public void testIncomingMessages(){
        System.out.println("* JUnitTest: memoryTestIncomingMessages()");
        MEMORY.addInfo(new InfoZero("configure:test"), "incomingMessages");
        assertEquals("configure:test", MEMORY.dequeFirst("incomingMessages").getPayload());
    }
    
    @Test
    public void testOutgoingMessages2ArduinoDequeLast(){
        System.out.println("* JUnitTest: memoryTestOutgoingMessages2ArduinoDequeLast()");
        MEMORY.addInfo(new InfoZero("test1"), "outgoingMessages2Arduino");
        assertEquals("test1", MEMORY.dequeLast("outgoingMessages2Arduino").getPayload());
    }
    
    @Test
    public void testConfiguresGetLast(){
        System.out.println("* JUnitTest: memoryTestConfiguresGetLast()");
        MEMORY.addInfo(new InfoZero("test2"), "configures");
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
    public void testCacheMap(){
        System.out.println("* JUnitTest: testCachMap()");
        CacheMap map = new CacheMap( 11);
        assertEquals(map.getCacheSize(),11);
        map.get(1);
    }
    
    @Test
    public void testCacheKey(){
        System.out.println("* JUnitTest: testCacheKey()");
        CacheKey key = new CacheKey();
        assertEquals(key.get(),-1);
        assertEquals(key.isFree(), true);
        key.set(1);
        assertEquals(key.get(),1);
        assertEquals(key.hashCode(),1);
        assertEquals(key.toString(),"1");
        assertEquals(key.isFree(), false);
        key.set(0xff00ff);
        assertEquals(key.hashCode(),16711680);
    }
    
    @Test
    public void testCacheElement(){
        System.out.println("* JUnitTest: testCacheElement()");
        CacheElement element = new CacheElement();
        assertEquals(element.isFree(), true);
    }
}
