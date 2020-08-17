import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;


public class ImageComponent extends JComponent implements ImageObserver {

	private BufferedImage image;
	private Orientation orientation;
	private int imageX;
	private int imageY;
	private int targetWidth;
	private int targetHeight;
	private double cropXRatio;
	private double cropYRatio;
	private int imageHeight;
	private int imageWidth;
	private int scaledWidth;
	private int scaledHeight;
	private double scaleInFrame = -1.0;
	private BufferedImage frameImage;
	private int scaledTargetHeight;
	private int scaledTargetWidth;
	private int maxRectY;
	private int maxRectX;
	
	public ImageComponent(File file, int targetHeight, int targetWidth) throws Exception {
		this.targetHeight = targetHeight;
		this.targetWidth = targetWidth;
        ImageInformation info = readImageInformation(file);
        BufferedImage fullScaleImage = transformImage(ImageIO.read(file), getExifTransformation(info));
        double heightScale = fullScaleImage.getHeight() / (double)targetHeight;
		double widthScale = fullScaleImage.getWidth() / (double) targetWidth;
		double scale = Math.min(heightScale, widthScale);
		orientation = scale == widthScale ? Orientation.PORTRAIT : Orientation.LANDSCAPE;
		
		int scaledWidth = (int)(fullScaleImage.getWidth()/scale);
		int scaledHeight = (int)(fullScaleImage.getHeight()/scale);

		image = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance(1/scale, 1/scale);
		g.drawImage(fullScaleImage, at, this);
		
        this.imageWidth = image.getWidth(this);
        this.imageHeight = image.getHeight(this);
        
        cropXRatio = (((double)imageWidth - (double)targetWidth) / 2)/imageWidth;
        cropYRatio = (((double)imageHeight - (double)targetHeight) / 2)/imageHeight;

		DragHandler dragHandler = new DragHandler();
		this.addMouseListener(dragHandler);
		this.addMouseMotionListener(dragHandler);
	}
	
	@Override
    protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		double heightScale = image.getHeight() / (double)getHeight();
		double widthScale = image.getWidth() / (double)getWidth();
		double noScale = 1.0;
		double newScale = Math.max(noScale, Math.max(heightScale, widthScale));
		if (newScale != noScale && scaleInFrame != newScale) {
			scaleInFrame = newScale;
			scaledWidth = (int)(image.getWidth()/scaleInFrame);
			scaledHeight = (int)(image.getHeight()/scaleInFrame);
			scaledTargetWidth = (int)(targetWidth/scaleInFrame);
			scaledTargetHeight = (int)(targetHeight/scaleInFrame);
			maxRectX = scaledWidth - scaledTargetWidth;
			maxRectY = scaledHeight - scaledTargetHeight;
			
			frameImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = frameImage.createGraphics();
			AffineTransform at = AffineTransform.getScaleInstance(1/scaleInFrame, 1/scaleInFrame);
			g2d.drawImage(image, at, this);
		} else if (scaleInFrame != newScale){
			frameImage = image;
			scaleInFrame = newScale;
			scaledWidth = image.getWidth();
			scaledHeight = image.getHeight();
			scaledTargetWidth = targetWidth;
			scaledTargetHeight = targetHeight;
			maxRectX = scaledWidth - scaledTargetWidth;
			maxRectY = scaledHeight - scaledTargetHeight;
		}

		int x = getWidth()/2 - frameImage.getWidth(this)/2;
		int y = getHeight()/2 - frameImage.getHeight(this)/2;

        if (y < 0) {
            y = 0;
        }

        if (x < 0) {
            x = 0;
        }
        
        this.imageX = x;
        this.imageY = y;
        g.drawImage(frameImage, imageX, imageY, this);
        g.setColor(Color.RED);
        
        g.drawRect(imageX+(int)(cropXRatio*scaledWidth), imageY+(int)(cropYRatio*scaledHeight), scaledTargetWidth - 1, scaledTargetHeight - 1);
    }

	public BufferedImage getCroppedImage() {
		return image.getSubimage((int)(cropXRatio*imageWidth), (int)(cropYRatio*imageHeight), targetWidth, targetHeight);
	}

	private enum Orientation {
		LANDSCAPE,PORTRAIT;
	}
	
	private class DragHandler extends MouseAdapter {
		private int dragStartY;
		private int dragStartX;
		private int dragStartRectY;
		private int dragStartRectX;
		
		@Override
		public void mousePressed(MouseEvent e) {
			this.dragStartX = e.getX();
			this.dragStartY = e.getY();
			this.dragStartRectX = (int)(cropXRatio*scaledWidth);
			this.dragStartRectY = (int)(cropYRatio*scaledHeight);
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			switch(orientation) {
			case LANDSCAPE:
				int xChange = e.getX() - dragStartX;
				int newX = dragStartRectX + xChange;
				int cropX = newX < 0 ? 0 : newX > maxRectX ? maxRectX : newX;
				cropXRatio = (double)cropX / scaledWidth;
				break;
			case PORTRAIT:
				int yChange = e.getY() - dragStartY;
				int newY = dragStartRectY + yChange;
				int cropY = newY < 0 ? 0 : newY > maxRectY ? maxRectY : newY;
				cropYRatio = (double)cropY / scaledHeight;
				break;
			}
			ImageComponent.this.paintImmediately(ImageComponent.this.getBounds());
		}
	}

    // Inner class containing image information
    public static class ImageInformation {
        public final int orientation;
        public final int width;
        public final int height;

        public ImageInformation(int orientation, int width, int height) {
            this.orientation = orientation;
            this.width = width;
            this.height = height;
        }

        public String toString() {
            return String.format("%dx%d,%d", this.width, this.height, this.orientation);
        }
    }


    public static ImageInformation readImageInformation(File imageFile)  throws IOException, MetadataException, ImageProcessingException {
        Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
        Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);

        int orientation = 1;
        try {
            orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        } catch (MetadataException | NullPointerException me) {
            System.out.println("Could not get orientation");
        }
        int width = jpegDirectory.getImageWidth();
        int height = jpegDirectory.getImageHeight();

        return new ImageInformation(orientation, width, height);
    }

    // Look at http://chunter.tistory.com/143 for information
    public static AffineTransform getExifTransformation(ImageInformation info) {

        AffineTransform t = new AffineTransform();

        switch (info.orientation) {
        case 1:
            break;
        case 2: // Flip X
            t.scale(-1.0, 1.0);
            t.translate(-info.width, 0);
            break;
        case 3: // PI rotation
            t.translate(info.width, info.height);
            t.rotate(Math.PI);
            break;
        case 4: // Flip Y
            t.scale(1.0, -1.0);
            t.translate(0, -info.height);
            break;
        case 5: // - PI/2 and Flip X
            t.rotate(-Math.PI / 2);
            t.scale(-1.0, 1.0);
            break;
        case 6: // -PI/2 and -width
            t.translate(info.height, 0);
            t.rotate(Math.PI / 2);
            break;
        case 7: // PI/2 and Flip
            t.scale(-1.0, 1.0);
            t.translate(-info.height, 0);
            t.translate(0, info.width);
            t.rotate(  3 * Math.PI / 2);
            break;
        case 8: // PI / 2
            t.translate(0, info.width);
            t.rotate(  3 * Math.PI / 2);
            break;
        }

        return t;
    }

    public static BufferedImage transformImage(BufferedImage image, AffineTransform transform) throws Exception {
        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);

        BufferedImage destinationImage = op.createCompatibleDestImage(image, (image.getType() == BufferedImage.TYPE_BYTE_GRAY) ? image.getColorModel() : null );
        Graphics2D g = destinationImage.createGraphics();
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, destinationImage.getWidth(), destinationImage.getHeight());
        destinationImage = op.filter(image, destinationImage);
        return destinationImage;
    }
}
