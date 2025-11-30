package test;
        
import javafx.scene.image.Image;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author xuyi
 */
public class testClass {
    @Test
    public void test() {
//        Image calculator = new Image("/Users/xuyi/Desktop/horse.jpeg");
        int expected = 4;
        //int actual = calculator.add(2, 2);
        // The message is optional and displayed if the test fails
        assertEquals(expected, 4);
    }
}
