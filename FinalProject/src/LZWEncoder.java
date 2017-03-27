import java.io.IOException;
import java.io.OutputStream;

public class LZWEncoder {
	int BITS = 12;
	int HSIZE = 5003;
	private int EOF = -1;
	int currentAccumulator = 0, currentBit = 0;
	private int initialCodingSize;
	private int imageWidth, imageHeight;
	int hsize = HSIZE;
	int freeEntry = 0;
	boolean clearFlg = false;
	int gInitBits;
	int CC, EOFC, nBits, maximumCode;;
	int maximumBits = BITS;
	int maxOfMaxCode = 1 << BITS;
	int[] headerTable = new int[HSIZE];
	int[] codeTable = new int[HSIZE];
	private byte[] pixelsArray;
	private int remainingPart, currentPixel;
	int MASKS[] = { 0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF, 0x01FF, 0x03FF, 0x07FF,
			0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF };
	int ACC;
	byte[] ACCUM = new byte[256];

	LZWEncoder(int width, int height, byte[] pixels, int color_depth) {
		imageWidth = width;
		imageHeight = height;
		pixelsArray = pixels;
		initialCodingSize = Math.max(2, color_depth);
	}

	void char_out(byte c, OutputStream outs) throws IOException {
		ACCUM[ACC++] = c;
		if (ACC >= 254)
			flushCharcter(outs);
	}

	void cl_block(OutputStream outs) throws IOException {
		cl_hash(hsize);
		freeEntry = CC + 2;
		clearFlg = true;

		export(CC, outs);
	}

	void cl_hash(int hsize) {
		for (int i = 0; i < hsize; ++i)
			headerTable[i] = -1;
	}

	void comp(int init_bits, OutputStream outs) throws IOException {
		int FC, idx, constant, entry, disposal, HSreg, HSshift;

		gInitBits = init_bits;
		clearFlg = false;
		nBits = gInitBits;
		maximumCode = maximumCode(nBits);
		CC = 1 << (init_bits - 1);
		EOFC = CC + 1;
		freeEntry = CC + 2;
		ACC = 0;
		entry = nexPix();
		HSshift = 0;
		for (FC = hsize; FC < 65536; FC *= 2)
			++HSshift;
		HSshift = 8 - HSshift;
		HSreg = hsize;
		cl_hash(HSreg);
		export(CC, outs);
		outer_loop: while ((constant = nexPix()) != EOF) {
			FC = (constant << maximumBits) + entry;
			idx = (constant << HSshift) ^ entry;
			if (headerTable[idx] == FC) {
				entry = codeTable[idx];
				continue;
			} else if (headerTable[idx] >= 0) {
				disposal = HSreg - idx;
				if (idx == 0)
					disposal = 1;
				do {
					if ((idx -= disposal) < 0)
						idx += HSreg;

					if (headerTable[idx] == FC) {
						entry = codeTable[idx];
						continue outer_loop;
					}
				} while (headerTable[idx] >= 0);
			}
			export(entry, outs);
			entry = constant;
			if (freeEntry < maxOfMaxCode) {
				codeTable[idx] = freeEntry++;
				headerTable[idx] = FC;
			} else
				cl_block(outs);
		}
		export(entry, outs);
		export(EOFC, outs);
	}

	void ENC(OutputStream os) throws IOException {
		os.write(initialCodingSize);
		remainingPart = imageWidth * imageHeight;
		currentPixel = 0;
		comp(initialCodingSize + 1, os);
		os.write(0);
	}

	void flushCharcter(OutputStream outs) throws IOException {
		if (ACC > 0) {
			outs.write(ACC);
			outs.write(ACCUM, 0, ACC);
			ACC = 0;
		}
	}

	final int maximumCode(int n_bits) {
		return (1 << n_bits) - 1;
	}

	private int nexPix() {
		if (remainingPart == 0)
			return EOF;
		--remainingPart;
		byte pix = pixelsArray[currentPixel++];
		return pix & 0xff;
	}

	void export(int code, OutputStream outs) throws IOException {
		currentAccumulator &= MASKS[currentBit];
		if (currentBit > 0)
			currentAccumulator |= (code << currentBit);
		else
			currentAccumulator = code;
		currentBit += nBits;
		while (currentBit >= 8) {
			char_out((byte) (currentAccumulator & 0xff), outs);
			currentAccumulator >>= 8;
			currentBit -= 8;
		}

		if (freeEntry > maximumCode || clearFlg) {
			if (clearFlg) {
				maximumCode = maximumCode(nBits = gInitBits);
				clearFlg = false;
			} else {
				++nBits;
				if (nBits == maximumBits)
					maximumCode = maxOfMaxCode;
				else
					maximumCode = maximumCode(nBits);
			}
		}

		if (code == EOFC) {
			while (currentBit > 0) {
				char_out((byte) (currentAccumulator & 0xff), outs);
				currentAccumulator >>= 8;
				currentBit -= 8;
			}
			flushCharcter(outs);
		}
	}
}