package assets;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.io.InputStream;
import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;

public class Assets{
    public String get(String assets){
        return getURL(assets).toString();
    }
    public InputStream getInputStream(String assets){
        return getClass().getResourceAsStream(assets);
    }
    public URL getURL(String assets){
        return getClass().getResource(assets);
    }
    public Icon getIcon(String resourceName, int width, int height) {
        if (resourceName.endsWith(".svg")) {
            return new FlatSVGIcon(resourceName.substring(1), width, height);
        }
        else {
            return new ImageIcon(getURL(resourceName));
        }
    }
    public Icon getIcon(String resourceName) {
        return getIcon(resourceName, 24, 24);
    }
}