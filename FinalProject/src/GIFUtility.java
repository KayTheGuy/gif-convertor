import java.io.*;
import java.awt.*;
import java.awt.image.*;

/**
 * @author Kayhan Dehghani Mohammadi CMPT 365 Spring 2017 Final Project GIF
 *         Maker
 **/

public class GIFUtility {
	private int imageWidth, imageHeight, transparentIdx, rpt = -1, mainDelay = 0, colorDepth, paletSize = 7, dis = -1,
			smpl = 10;
	private Color transColor = null;
	private boolean strted = false, closedStream = false, _1stFrame = true, setSize = false;
	private OutputStream result;
	private BufferedImage img;
	private byte[] pixs, idxPixs, clrTable;
	private boolean[] usedEntry = new boolean[256];
	private String format = "GIF89a";
	
	public boolean addFrame(BufferedImage image) {
		if ((image == null) || !strted) {
			return false;
		}
		boolean ok = true;
		try {
			if (!setSize) {
				setSize(image.getWidth(), image.getHeight());
			}
			img = image;
			retrievePixs();
			computePixs();
			if (_1stFrame) {
				outLSD();
				outPal();
				if (rpt >= 0) {
					outNetsExt();
				}
			}
			outControlExternals();
			outImage();
			if (!_1stFrame) {
				outPal();
			}
			outPixs();
			_1stFrame = false;
		} catch (IOException e) {
			ok = false;
		}
		
		return ok;
	}

	public void setDelay(int delay) {
		mainDelay = Math.round(delay / 10.0f);
	}

	public void setRepeat(int repeat) {
		if (repeat >= 0) {
			rpt = repeat;
		}
	}

	public boolean finish() {
		if (!strted)
			return false;
		boolean ok = true;
		strted = false;
		try {
			result.write(0x3b);
			result.flush();
			if (closedStream) {
				result.close();
			}
		} catch (IOException e) {
			ok = false;
		}

		transparentIdx = 0;
		result = null;
		img = null;
		pixs = null;
		idxPixs = null;
		clrTable = null;
		closedStream = false;
		_1stFrame = true;

		return ok;
	}

	public void setSize(int width, int height) {
		if (strted && !_1stFrame)
			return;
		imageWidth = width;
		imageHeight = height;
		if (imageWidth < 1)
			imageWidth = 320;
		if (imageHeight < 1)
			imageHeight = 240;
		setSize = true;
	}

	public boolean begin(OutputStream outputStream) {
		if (outputStream == null)
			return false;
		boolean ok = true;
		closedStream = false;
		result = outputStream;
		try {
			outSTRG(format);
		} catch (IOException e) {
			ok = false;
		}
		return strted = ok;
	}

	public boolean begin(String fileName) {
		boolean ok = true;
		try {
			result = new BufferedOutputStream(new FileOutputStream(fileName));
			ok = begin(result);
			closedStream = true;
		} catch (IOException e) {
			ok = false;
		}
		return strted = ok;
	}

	private void computePixs() {
		int length = pixs.length, numberOfPixels = length / 3;
		idxPixs = new byte[numberOfPixels];
		NeuQuant nq = new NeuQuant(pixs, length, smpl);
		clrTable = nq.process();
		for (int i = 0; i < clrTable.length; i += 3) {
			byte temp = clrTable[i];
			clrTable[i] = clrTable[i + 2];
			clrTable[i + 2] = temp;
			usedEntry[i / 3] = false;
		}
		int m = 0;
		for (int idx = 0; idx < numberOfPixels; idx++) {
			int index = nq.map(pixs[m++] & 0xff, pixs[m++] & 0xff, pixs[m++] & 0xff);
			usedEntry[index] = true;
			idxPixs[idx] = (byte) index;
		}
		pixs = null;
		colorDepth = 8;
		paletSize = 7;
		if (transColor != null) {
			transparentIdx = getNearest(transColor);
		}
	}

	private int getNearest(Color color) {
		if (clrTable == null)
			return -1;
		int r = color.getRed(), g = color.getGreen(), b = color.getBlue();
		int minimumPosition = 0, distanceMin = 256 * 256 * 256;
		int len = clrTable.length;
		for (int i = 0; i < len;) {
			int redDistance = r - (clrTable[i++] & 0xff), greenDistance = g - (clrTable[i++] & 0xff), blueDistance = b - (clrTable[i] & 0xff);
			int totalDistance = redDistance * redDistance + greenDistance * greenDistance + blueDistance * blueDistance;
			int idx = i / 3;
			if (usedEntry[idx] && (totalDistance < distanceMin)) {
				distanceMin = totalDistance;
				minimumPosition = idx;
			}
			i++;
		}
		return minimumPosition;
	}

	private void retrievePixs() {
		int width = img.getWidth(), height = img.getHeight(), T = img.getType();
		if ((width != imageWidth) || (height != imageHeight) || (T != BufferedImage.TYPE_3BYTE_BGR)) {
			BufferedImage tmpImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
			Graphics2D graphic = tmpImage.createGraphics();
			graphic.drawImage(img, 0, 0, null);
			img = tmpImage;
		}
		pixs = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
	}
	
	private void outLSD() throws IOException {
		outSH(imageWidth);
		outSH(imageHeight);
		result.write((0x80 | 0x70 | 0x00 | paletSize));
		result.write(0);
		result.write(0);
	}
	
	private void outControlExternals() throws IOException {
		result.write(0x21);
		result.write(0xf9);
		result.write(4);
		int transp, disp;
		if (transColor == null) {
			transp = 0;
			disp = 0;
		} else {
			transp = 1;
			disp = 2;
		}
		if (dis >= 0) {
			disp = dis & 7;
		}
		disp <<= 2;

		result.write(0 | disp | 0 | transp);
		outSH(mainDelay);
		result.write(transparentIdx);
		result.write(0);
	}
	
	private void outNetsExt() throws IOException {
		result.write(0x21);
		result.write(0xff);
		result.write(11);
		outSTRG("NETSCAPE" + "2.0");
		result.write(3);
		result.write(1);
		outSH(rpt);
		result.write(0);
	}
	
	private void outSH(int val) throws IOException {
		result.write(val & 0xff);
		result.write((val >> 8) & 0xff);
	}

	private void outSTRG(String string) throws IOException {
		for (int i = 0; i < string.length(); i++) {
			result.write((byte) string.charAt(i));
		}
	}

	private void outImage() throws IOException {
		result.write(0x2c);
		outSH(0);
		outSH(0);
		outSH(imageWidth);
		outSH(imageHeight);
		if (_1stFrame) {
			result.write(0);
		} else {
			result.write(0x80 | 0 | 0 | 0 | paletSize);
		}
	}

	private void outPal() throws IOException {
		result.write(clrTable, 0, clrTable.length);
		int n = (3 * 256) - clrTable.length;
		for (int i = 0; i < n; i++) {
			result.write(0);
		}
	}

	private void outPixs() throws IOException {
		LZWEncoder encoder = new LZWEncoder(imageWidth, imageHeight, idxPixs, colorDepth);
		encoder.ENC(result);
	}
}
