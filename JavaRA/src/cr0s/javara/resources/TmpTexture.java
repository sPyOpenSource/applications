package cr0s.javara.resources;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.awt.image.BufferedImage;

import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.embed.swing.SwingFXUtils;

import cr0s.javara.util.Pos;
import redhorizon.filetypes.pal.PalFile;
import redhorizon.filetypes.tmp.TmpFileRA;

public class TmpTexture {
    private final TmpFileRA tmp;
    public int width, height; // size of one single frame
    public int numImages;
    public String name;

    private final static int TILE_WIDTH = 24, TILE_HEIGHT = 24;

    private final int PALETTE_SIZE = 256;
    private final int BYTE_MASK = 0xFF;
    private final int SHADOW_COLOR = 0x04;
    private final int SHADOW_ALPHA_LEVEL = 64;
    private final int NON_TRANSPARENT_ALPHA = 255;

    private final String type;
    private Image combinedImage = null;

    private final HashMap<Integer, BufferedImage> frameCache = new HashMap<>();

    private boolean isInSpriteSheet;
    private Pos spriteSheetPos;
    
    public Rectangle boundingBox;
    
    public TmpTexture(TmpFileRA aTmp, String aType) {
	this.width = aTmp.width();
	this.height = aTmp.height();
	this.numImages = aTmp.numImages();

	this.tmp = aTmp;
	this.type = aType;
	
	this.name = tmp.getFileName();
    }

    private BufferedImage applyPalette(int index) {
	PalFile pal = ResourceManager.getInstance().getPaletteByName(
		type + ".pal");
	Color[] colors = new Color[PALETTE_SIZE];

	ByteBuffer bBuffer = pal.getPaletteDataByteBuffer();

	// Use palette data
	for (int i = 0; i < PALETTE_SIZE; i++) {
	    int r = bBuffer.get() & BYTE_MASK;
	    int g = bBuffer.get() & BYTE_MASK;
	    int b = bBuffer.get() & BYTE_MASK;

	    colors[i] = Color.rgb(r, g, b);
	}

	bBuffer.rewind();

	ByteBuffer bb = tmp.getImage(index);
	// Allocate zeros filled buffer if image is empty
	if (bb == null) {
	    bb = ByteBuffer.allocate(width * height);
	}
	
	BufferedImage imgbuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	for (int y = 0; y < width; y++) {
	    for (int x = 0; x < height; x++) {
		int colorValue = bb.get() & BYTE_MASK;

		// Check for shadow color
		if (colorValue != SHADOW_COLOR) {
                    int r = (int)(colors[colorValue].getRed() * 255);
                    int g = (int)(colors[colorValue].getGreen() * 255);
                    int b = (int)(colors[colorValue].getBlue() * 255);
		    imgbuf.setRGB(x, y, (r << 16) | (g << 8) | b | ((colorValue == 0) ? 0 : NON_TRANSPARENT_ALPHA << 24));
		} else {
		    // Replace shadow color with black color with transparency
		    imgbuf.setRGB(x, y, SHADOW_ALPHA_LEVEL << 24);
		}
	    }
	}

	bb.rewind();

	return imgbuf;
    }

    /**
     * Gets combined image by height of all .TMP tiles.
     * 
     * @param remapColor
     * @return big combined image
     */
    public Image getAsCombinedImage() {
	if (this.combinedImage != null) {
	    return this.combinedImage;
	}

	int combinedHeight, combinedWidth, tileHeight, tileWidth;
	
	if (this.tmp.getWidthInTiles() == 1 && this.tmp.getHeightInTiles() == 1) {
	    combinedHeight = this.height * numImages;
	    combinedWidth = this.width;
	    tileHeight = numImages;
	} else {
	    combinedHeight = this.height * this.tmp.getHeightInTiles();
	    combinedWidth = this.width * this.tmp.getWidthInTiles();
	    tileHeight = this.getHeightInTiles();
	}

	tileWidth = this.getWidthInTiles();
	
	// Image is not cached
	// Create big sized common image, which will combine all frames of
	// source .TMP
	BufferedImage imgBuf = new BufferedImage(combinedWidth, combinedHeight, BufferedImage.TYPE_INT_ARGB);

	for (int tileY = 0; tileY < tileHeight; tileY++) {
	    for (int tileX = 0; tileX < tileWidth; tileX++) {
		int imgIndex = tmp.getWidthInTiles() * tileY + tileX;

		BufferedImage frameBuf;
		if (tmp.getImage(imgIndex) == null) {
		    // Create empty image
		    frameBuf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		} else {
		    frameBuf = applyPalette(imgIndex);
		}

		//frame.setFilter(Image.FILTER_LINEAR);

		for (int y = 0; y < height; y++) {
		    for (int x = 0; x < width; x++) {
			imgBuf.setRGB(tileX * width + x, tileY * height + y,
				frameBuf.getRGB(x, y));
		    }
		}
	    }
	}

	// Cache result and return
	this.combinedImage = SwingFXUtils.toFXImage(imgBuf, null);
	return this.combinedImage;
    }

    public BufferedImage getByIndex(int index) {
	if (this.frameCache.containsKey(index)) {
	    return frameCache.get(index);
	} else {
	    BufferedImage img = applyPalette(index);
	    //img.setFilter(Image.FILTER_LINEAR);
	    frameCache.put(index, img);

	    return img;
	}
    }
    
    public boolean isInSpriteSheet() {
	return this.isInSpriteSheet;
    }
    
    public void setSpriteSheetCoords(Pos coord) {
	this.isInSpriteSheet = true;
	this.spriteSheetPos = coord;
    }
    
    public Pos getSpriteSheetCoords() {
	return this.spriteSheetPos;
    }
    
    public int getWidthInTiles() {
	return this.tmp.getWidthInTiles();
    }
    
    public int getHeightInTiles() {
	return this.tmp.getHeightInTiles();
    }    
}
