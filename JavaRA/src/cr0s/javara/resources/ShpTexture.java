package cr0s.javara.resources;

import java.nio.ByteBuffer;
import java.awt.image.BufferedImage;
import javafx.scene.paint.Color;

import cr0s.javara.util.Pos;
import redhorizon.filetypes.pal.PalFile;
import redhorizon.filetypes.shp.ShpFileCnc;

public class ShpTexture {
    public int width, height; // size of one single frame
    public int numImages;

    private final int[] remapIndexes = { 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95 };
    private final boolean[] isRemap = new boolean[256];
    private Pos sheetPos;
    private final ShpFileCnc shp;

    public String palleteName;
    public Color forcedColor;

    public ShpTexture(ShpFileCnc shp) {
	this.width = shp.width();
	this.height = shp.height();
	this.numImages = shp.numImages();

	for (int i = 0; i < remapIndexes.length; i++) {
	    isRemap[remapIndexes[i]] = true;
	}

	this.shp = shp;
    }

    /**
     * Gets ImageBuffer for single .SHP frame with specified remapping color
     * @param index index of .SHP frame
     * @param remapColor team color
     * @return ImageBuffer for remapped texture
     */
    public BufferedImage getAsImage(int index, Color remapColor) {
	// Check image in cache
	BufferedImage res = RemappedTextureCache.getInstance().checkInCache(shp.getFileName(), remapColor, index);
	if (res != null) {
	    return res;
	}

	// Image is not cached, remap palette, generate image and cache it
	BufferedImage imgbuf = remapShpFrame(index, remapColor);

	// Cache image and return
	//Image img = SwingFXUtils.toFXImage(imgbuf, null);
	RemappedTextureCache.getInstance().putInCache(imgbuf, shp.getFileName(), remapColor, index);
	return imgbuf;
    }

    private BufferedImage remapShpFrame(int index, Color remapColor) {
	PalFile pal = ResourceManager.getInstance().getPaletteByName(this.palleteName == null ? "temperat.pal" : this.palleteName);
	Color[] colors = new Color[256];

	// Remap pallete only if remapping color is specified
	remapPallete(colors, pal, remapColor);

	ByteBuffer bb = shp.getImage(index);
	BufferedImage imgbuf = new BufferedImage(shp.width(), shp.height(), BufferedImage.TYPE_INT_ARGB);
	for (int y = 0; y < shp.height(); y++) {
	    for (int x = 0; x < shp.width(); x++) {
		int colorValue = bb.get() & 0xFF;

		// Check for shadow color
		if (colorValue != 0x04 && colorValue != 0x03) {
                    int r,g,b,a;
		    if (forcedColor == null) {
                        r = (int)(colors[colorValue].getRed() * 255);
                        g = (int)(colors[colorValue].getGreen() * 255);
                        b = (int)(colors[colorValue].getBlue() * 255);
                        a = (colorValue == 0) ? 0 : 255;
			imgbuf.setRGB(x, y, (r << 16) | (g << 8) | b | (a << 24));
		    } else {
                        r = (int)(forcedColor.getRed() * 255);
                        g = (int)(forcedColor.getGreen() * 255);
                        b = (int)(forcedColor.getBlue() * 255);
                        a = (colorValue == 0) ? 0 : (int)(forcedColor.getOpacity() * 255);
			imgbuf.setRGB(x, y, (r << 16) | (g << 8) | b | (a << 24));
		    }
		} else {
		    // Shadows
		    imgbuf.setRGB(x, y, 128 << 24); // Replace shadow color with black color with 3/4 transparency
		}
	    }
	}

	bb.rewind();

	return imgbuf;
    }
    
    private BufferedImage remapShpShadowFrame(int index) {
	Color shadowColors[] = new Color[] {
		new Color(0, 0, 0, 0), Color.GREEN,
		Color.BLUE, Color.YELLOW,
		Color.BLACK,
		new Color(0, 0, 0, 160f/255),
		new Color(0, 0, 0, 128f/255),
		new Color(0, 0, 0, 64f/255),
	};
	
	
	ByteBuffer bb = shp.getImage(index);
	BufferedImage imgbuf = new BufferedImage(shp.width(), shp.height(), BufferedImage.TYPE_INT_ARGB);
	for (int y = 0; y < shp.height(); y++) {
	    for (int x = 0; x < shp.width(); x++) {
		int colorValue = bb.get() & 0xFF;
                int r = (int)(shadowColors[colorValue % 8].getRed() * 255);
                int g = (int)(shadowColors[colorValue % 8].getGreen() * 255);
                int b = (int)(shadowColors[colorValue % 8].getBlue() * 255);
                int a = (int)(shadowColors[colorValue % 8].getOpacity() * 255);
		imgbuf.setRGB(x, y, (r << 16) | (g << 8) | b | (a << 24));
	    }
	}

	bb.rewind();

	return imgbuf;
    }
    

    public BufferedImage getAsCombinedImage(Color remapColor) {
	return getAsCombinedImage(remapColor, false, 0, 0);
    }
    /**
     * Gets combined image by height of all .SHP frames
     * @param remapColor
     * @return
     */
    public BufferedImage getAsCombinedImage(Color remapColor, boolean isShadowSprite, int startFrame, int endFrame) {
	int combinedHeight = this.height * this.numImages;
	int combinedWidth = this.width;

	BufferedImage img = RemappedTextureCache.getInstance().checkInCache(shp.getFileName(), remapColor, -1);
	if (img != null) {
	    return img;
	}

	// Image is not cached
	// Create big sized common image, which will combine all frames of source .SHP
	BufferedImage imgBuf = new BufferedImage(combinedWidth, combinedHeight, BufferedImage.TYPE_INT_ARGB);

	if (endFrame == 0) {
	    endFrame = this.numImages;
	}
	
	for (int i = startFrame; i < endFrame; i++) {
	    BufferedImage frameBuf;
	    if (!isShadowSprite) { 
		frameBuf = remapShpFrame(i, remapColor); 
	    } else {
		frameBuf = remapShpShadowFrame(i);
	    }
	    
	    int shiftX = 0;
	    int shiftY = i * height;

	    for (int y = 0; y < height; y++) {
		for (int x = 0; x < width; x++) {
		    imgBuf.setRGB(shiftX + x, shiftY + y, frameBuf.getRGB(x, y));
		}
	    }
	}

	// Cache result and return
	RemappedTextureCache.getInstance().putInCache(imgBuf, shp.getFileName(), remapColor, -1);
	return imgBuf;
    }

    /**
     * Apply remapping rules to source pallete
     * @param colors new palette with remapped team colors
     * @param pal source palette with default team colors
     * @param remapColor remapping (team) color
     */
    public void remapPallete(Color[] colors, PalFile pal, Color remapColor) {
	double remapR = 1, remapG = 1, remapB = 1;
	
	if (remapColor != null) { // If need image without remapping
	    remapR = remapColor.getRed();
	    remapG = remapColor.getGreen();
	    remapB = remapColor.getBlue();		
	}
	ByteBuffer bBuffer = pal.getPaletteDataByteBuffer();

	// Remap palette
	for (int i = 0; i < 256; i++) {
	    int r = bBuffer.get() & 0xFF;
	    int g = bBuffer.get() & 0xFF;
	    int b = bBuffer.get() & 0xFF;

	    colors[i] = Color.rgb(r, g, b);

	    // Check in remap table, this color in source palette needs to be changed to team-related color or not
	    if (isRemap[i] && remapColor != null) {
		// We need sustain source brightness and only change color from default to remapped
		float[] hsbRemap = java.awt.Color.RGBtoHSB((int)(remapR * 255), (int)(remapG * 255), (int)(remapB * 255), null);
		float[] hsbSource = java.awt.Color.RGBtoHSB(r, g, b, null);

		// Applying changes, using remapped H, S values and source B value
		java.awt.Color newColor = java.awt.Color.getHSBColor(hsbRemap[0], hsbRemap[1], hsbSource[2]);
		colors[i] = Color.rgb(newColor.getRed(), newColor.getGreen(), newColor.getBlue());
	    }
	}

	bBuffer.rewind();
    }

    public void setSheetPos(int x, int y) {
	this.sheetPos = new Pos(x, y);
    }

    public Pos getSpriteSheetCoords() {
	return this.sheetPos;
    }

    public String getTextureName() {
	return this.shp.getFileName();
    }
}
