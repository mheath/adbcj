package org.adbcj.support;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.Charset;

/**
 * @author Mike Heath
 */
public class DecoderInputStream extends InputStream {

	private final InputStream in;
	private int limit;

	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	public DecoderInputStream(InputStream in) {
		this(in, Integer.MAX_VALUE);
	}

	public DecoderInputStream(InputStream in, int limit) {
		this.in = in;
		this.limit = limit;
	}

	public int available() throws IOException {
		return in.available();
	}

	public void close() throws IOException {
		in.close();
	}

	public void mark(int readlimit) {
		in.mark(readlimit);
	}

	public boolean markSupported() {
		return in.markSupported();
	}

	@Override
	public int read() throws IOException {
		int i = in.read();
		if (i >= 0) {
			limit--;
		}
		if (limit < 0) {
			throw new IllegalStateException("Read too many bytes");
		}
		return i;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int i = in.read(b, off, len);
		limit -= i;
		if (limit < 0) {
			throw new IllegalStateException("Read too many bytes");
		}
		return i;
	}

	@Override
	public long skip(long n) throws IOException {
		long i = in.skip(n);
		limit -= i;
		if (limit < 0) {
			throw new IllegalStateException("Read too many bytes");
		}
		return i;
	}

	public void reset() throws IOException {
		in.reset();
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	public void setByteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
	}

	public byte readByte() throws IOException {
		int ch = read();
		if (ch < 0) {
			throw new EOFException();
		}
		return (byte) (ch);
	}

	public int readInt() throws IOException {
	    int ch1 = read();
	    int ch2 = read();
	    int ch3 = read();
	    int ch4 = read();
	    if ((ch1 | ch2 | ch3 | ch4) < 0) {
	        throw new EOFException();
	    }
	    return (byteOrder == ByteOrder.BIG_ENDIAN) ?
			    ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0)) :
			    ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0));
	}

	public int readUnsignedShort() throws IOException {
	    int ch1 = in.read();
	    int ch2 = in.read();
	    if ((ch1 | ch2) < 0) {
	        throw new EOFException();
	    }
		return (byteOrder == ByteOrder.BIG_ENDIAN) ?
		    (ch1 << 8) + (ch2 << 0) :
			(ch2 << 8) + (ch1 << 0);
	}

	public final short readShort() throws IOException {
		int ch1 = in.read();
		int ch2 = in.read();
		if ((ch1 | ch2) < 0) {
			throw new EOFException();
		}
		return (byteOrder == ByteOrder.BIG_ENDIAN) ?
			(short)((ch1 << 8) + (ch2 << 0)) :
			(short)((ch2 << 8) + (ch1 << 0));
	}

	/**
	 * Read a null terminated string or until the end of the stream.
	 * 
	 * @param charset
	 * @return
	 */
	public String readString(Charset charset) throws IOException {
		// TODO: Add support for UTF-16
		byte[] buffer = new byte[getLimit()];

		int pos = 0;
		int c;
		while ((c = read()) > 0) {
			buffer[pos++] = (byte)c;
		}
		return new String(buffer, 0, pos, charset);
	}

	/**
	 * Reads a string of the specified length.
	 *
	 * @param length
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public String readString(int length, Charset charset) throws IOException {
		// TODO: Add support for UTF-16
		byte[] buffer = new byte[length];
		int pos = 0;
		while (pos < length) {
			int b = read();
			if (b < 0) {
				throw new IOException("End of stream, expected " + (length - pos) + " characters");
			}
			buffer[pos++] = (byte)b;
		}
		return new String(buffer, 0, pos - 1, charset);
	}

}