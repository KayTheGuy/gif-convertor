import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * @author Kayhan Dehghani Mohammadi CMPT 365 Spring 2017 Final Project GIF
 *         Maker
 **/

public class UI extends JFrame implements ActionListener {
	private static final long serialVersionUID = 1L;
	private JFrame mainFrame;
	private JPanel txtPanel;
	private JFileChooser fc;
	private static String videoFilePath;
	JMenuBar menuBar;
	JMenu menu, fileMenu;
	JMenuItem selectVideo, exitItem, convertGIF;
	JLabel label;
	JProgressBar progressBar;

	public static void main(String[] args) {
		new UI();
	}

	public UI() {
		mainFrame = new JFrame("Video to GIF Convertor [CMPT 365]");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setResizable(false);
		mainFrame.setSize(600, 400);
		mainFrame.setLocationRelativeTo(null);
		txtPanel = new JPanel(new GridLayout(4, 3));
		mainFrame.add(txtPanel, BorderLayout.CENTER);
		menuBar = new JMenuBar();
		menu = new JMenu("Window");
		fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		menuBar.add(menu);
		selectVideo = new JMenuItem("Select Video...");
		selectVideo.addActionListener(this);
		convertGIF = new JMenuItem("Convert to GIF");
		convertGIF.setVisible(false);
		convertGIF.addActionListener(this);
		exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(this);
		fileMenu.add(selectVideo);
		fileMenu.add(convertGIF);
		menu.add(exitItem);
		mainFrame.setJMenuBar(menuBar);
		progressBar = new JProgressBar();
		progressBar.setVisible(false);
		txtPanel.add(progressBar);
		label = new JLabel("<html><br>Welcome!</html>");
		txtPanel.add(label);

		txtPanel.setVisible(true);
		mainFrame.setVisible(true);
	}

	private void selectFile() {
		JDialog dialog = new JDialog(mainFrame, "Select Image", ModalityType.APPLICATION_MODAL);
		fc = new JFileChooser();
		fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
		fc.setMultiSelectionEnabled(true);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setVisible(true);
		int result = fc.showOpenDialog(dialog);
		
		progressBar.setVisible(true);
		if (result == JFileChooser.APPROVE_OPTION) {
			File[] files = fc.getSelectedFiles();
			if (files.length != 1) {
				label.setText("<html>Please select only one file<br>");
				return;
			}
			videoFilePath = files[0].getAbsolutePath();
			
			progressBar.setValue(20);
			
			label.setText("<html><br>Video has been loaded.<br> Please choose File -> Convert to GIF! </html>");
			selectVideo.setText("Select a New Video...");
			convertGIF.setVisible(true);
			progressBar.setValue(50);
		}
	}

	private void convertVidToGIF(String filename) {
		int numOfFrames = 20;
		BufferedImage[] frames = new BufferedImage[numOfFrames];
		VideoUtility vidUtil = new VideoUtility();
		try {
			frames = vidUtil.getNFrames(filename, numOfFrames);
		} catch (java.lang.Exception e) {
			label.setText("<html><br>Unable to read to video. Please try again. </html>");
			e.printStackTrace();
		}
		GIFUtility util = new GIFUtility();
		util.begin(new File("").getAbsoluteFile().toString() + File.pathSeparator +"result.gif");
		util.setDelay(100);
		util.setRepeat(15);
		for (int i = 0; i < frames.length; i++) {
			util.addFrame(frames[i]);
		}
		progressBar.setValue(75);
		util.finish();
		progressBar.setValue(100);
		label.setText("<html><br>Video has been converted to GIF format.<br> You can find the file in the current directory.</html>");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == exitItem) {
			System.exit(0);
		} else if (e.getSource() == selectVideo) {
			selectFile();
		} else if (e.getSource() == convertGIF) {
			convertVidToGIF(videoFilePath);
		}
	}

}
