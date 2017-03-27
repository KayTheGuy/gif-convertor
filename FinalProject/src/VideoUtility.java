import java.awt.image.BufferedImage;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;


/**
 * @author Kayhan Dehghani Mohammadi CMPT 365 Spring 2017 Final Project GIF
 *         Maker
 **/

public class VideoUtility {
	public BufferedImage[] getNFrames(String filename, int N) throws Exception {
		BufferedImage[] tenFrames = new BufferedImage[N];
		BufferedImage bufferedImage;
		Frame frame;
		Java2DFrameConverter converter;
		FFmpegFrameGrabber g = new FFmpegFrameGrabber(filename);
		g.start();
		
		for (int i = 0; i < N; i++) {
			frame = g.grab();
			converter = new Java2DFrameConverter();
			bufferedImage = converter.convert(frame);
			tenFrames[i] = bufferedImage;
		}
		
		g.flush();
		g.stop();
		return tenFrames;
	}
}
