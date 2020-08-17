import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;



public class ImageResizer extends JFrame implements Runnable {
	
	private File directory;
	private List<File> files;
	private int fileIndex;
	private JPanel imagePanel;
	private JButton previousButton;
	private JButton nextButton;
	private JButton saveButton;
	private ImageComponent imageComponent;

	ImageResizer() {
		File directory = new File(System.getProperty("user.dir"));
		activateDirectory(directory); 
	}

	private void activateDirectory(File directory) {
		this.directory = directory;
		this.files = Arrays.asList(directory.listFiles(new FilenameFilter(){

			@Override
			public boolean accept(File dir, String filename) {
				filename = filename.toLowerCase();
				return filename.endsWith(".jpg") || filename.endsWith(".jpeg");
			}
			
		}));
		fileIndex = files.isEmpty() ? Integer.MIN_VALUE : 0;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new ImageResizer());
	}

	@Override
	public void run() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Image Resizer");
		setLayout(new GridBagLayout());
		
		imagePanel = new JPanel();
		imagePanel.setLayout(new GridBagLayout());
		add(imagePanel, new GridBagConstraints(0,0,3,1,1.0,1.0,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,2,2,2),0,0));
		loadImage();
		previousButton = new JButton(new AbstractAction("<"){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				fileIndex--;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						loadImage();
					}
				});
				setButtonStates();
			}
		});
		nextButton = new JButton(new AbstractAction(">"){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				fileIndex++;
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						loadImage();
					}
				});
				setButtonStates();
			}
		});
		saveButton = new JButton(new AbstractAction("Save"){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					ImageIO.write(imageComponent.getCroppedImage(), "jpeg", files.get(fileIndex));
					loadImage();
				} catch (IOException e) { }
			}
		});
		
		add(previousButton, new GridBagConstraints(0,1,1,1,0.0,0.0,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2,2,2,2),0,0));
		add(saveButton, new GridBagConstraints(1,1,1,1,1.0,0.0,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2,2,2,2),0,0));
		add(nextButton, new GridBagConstraints(2,1,1,1,0.0,0.0,GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(2,2,2,2),0,0));
		setButtonStates();
		
		JMenuBar menubar = new JMenuBar();
		JMenu menu = new JMenu("Settings");
		JMenuItem directoryMenuItem = new JMenuItem(new AbstractAction("Select Directory...") {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser directoryChooser = new JFileChooser(directory);
				directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				directoryChooser.setMultiSelectionEnabled(false);
				if (directoryChooser.showOpenDialog(ImageResizer.this) == JFileChooser.APPROVE_OPTION) {
					activateDirectory(directoryChooser.getSelectedFile());
					loadImage();
					setButtonStates();
				}
			}
			
		});
		menu.add(directoryMenuItem );
		menubar.add(menu );
		setJMenuBar(menubar );
		setPreferredSize(new Dimension(1000,700));
		
		pack();
		setVisible(true);
	}

	private void setButtonStates() {
		previousButton.setEnabled(fileIndex > 0);
		nextButton.setEnabled(fileIndex >= 0 && fileIndex < files.size() - 1);
		saveButton.setEnabled(imagePanel.getComponentCount() == 1);
	}

	private void loadImage() {
		imagePanel.removeAll();
		imageComponent = null;
		if (fileIndex >= 0) {
			try {
				imageComponent = new ImageComponent(files.get(fileIndex), 480, 800);
				imagePanel.add(imageComponent, new GridBagConstraints(0,0,1,1,1.0,1.0,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2,2,2,2),0,0));
				imagePanel.getLayout().layoutContainer(imagePanel);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						imagePanel.repaint();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
