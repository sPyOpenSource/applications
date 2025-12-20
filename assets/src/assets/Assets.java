package assets;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import com.formdev.flatlaf.extras.FlatSVGIcon;

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
        } else {
            return new ImageIcon(getURL(resourceName));
        }
    }
    
    public Icon getIcon(String resourceName) {
        return getIcon(resourceName, 24, 24);
    }
    
    public RandomAccessFile getRandomAccessFile(String path) throws IOException 
    {
        File tmpFile = File.createTempFile("isc", "tmp");
        tmpFile.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(tmpFile, "rwd");
        byte[] buffer = new byte[2048];
        int    tmp;
        InputStream in = getInputStream(path);
        while ((tmp = in.read(buffer)) != -1) 
        {
          raf.write(buffer, 0, tmp);
        }
         
        raf.seek(0);
         
        return raf;
    }
}