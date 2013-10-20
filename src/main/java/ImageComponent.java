import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JComponent;


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
	
	public ImageComponent(File file, int targetHeight, int targetWidth) throws IOException {
		this.targetHeight = targetHeight;
		this.targetWidth = targetWidth;
		BufferedImage fullScaleImage = ImageIO.read(file);
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
}
