package cr0s.javara.resources;

import java.awt.image.BufferedImage;
import java.util.WeakHashMap;
import javafx.scene.paint.Color;

/**
 * Contains textures from .shp source with remapped team colors
 * 
 * @author Cr0s
 *
 */
public class RemappedTextureCache {
    private static RemappedTextureCache instance = null;
    private final WeakHashMap<String, BufferedImage> remappedTextures;

    private RemappedTextureCache() {
	this.remappedTextures = new WeakHashMap<>();
    }

    public static RemappedTextureCache getInstance() {
	if (instance == null) {
	    instance = new RemappedTextureCache();
	}

	return instance;
    }

    /**
     * Checks cache for specified texture and color
     * @param textureName name of texture, for example "mcv.shp"
     * @param remapColor remapping color (team color)
     * @param textureIndex index in texture sheet of .shp (number of frame) or -1 if we need all frames as one image
     * @return null if image is not cached
     */
    public BufferedImage checkInCache(String textureName, Color remapColor, int textureIndex) {
	// Generate key to search
	// searchKey = textureName + r + g + b + textureIndex
	if (remapColor == null) {
	    remapColor = Color.rgb(255, 255, 255);
	}

	String cacheKey = textureName + Double.toString(remapColor.getRed()) + Double.toString(remapColor.getGreen()) + Double.toString(remapColor.getBlue()) + textureIndex;

	return remappedTextures.get(cacheKey);
    }

    /**
     * Puts specified image as ImageBuffer in cache
     * @param image Image of remapped image
     * @param textureName texture name
     * @param remapColor remapping color (team color)
     * @param textureIndex index of texture, or -1 if we need all .SHP frames as one whole image
     */
    public void putInCache(BufferedImage image, String textureName, Color remapColor, int textureIndex) {
	if (remapColor == null) {
	    remapColor = Color.rgb(255, 255, 255);
	}

	String cacheKey = textureName + Double.toString(remapColor.getRed()) + Double.toString(remapColor.getGreen()) + Double.toString(remapColor.getBlue()) + textureIndex;	

	if (!this.remappedTextures.containsKey(cacheKey)) {
	    this.remappedTextures.put(cacheKey, image);
	} else {
	    System.err.println("[Warning] Remapped textures cache warning: trying to put in cache image that already cached.");
	}
    }
}
