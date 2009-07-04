package org.adbcj.support;

import java.io.OutputStream;
import java.io.IOException;

/**
 * @author Mike Heath
 */
public class EncoderOutputStream extends OutputStream {

	private final OutputStream out;

	private int limit;

	private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	public EncoderOutputStream(OutputStream out) {
		this.out = out;
	}

	@Override
	public void close() throws IOException {
		out.close();
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void write(byte[] b) throws IOException {
		out.write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		limit -= len;
		assertLimit();
		out.write(b, off, len);
	}

	private void assertLimit() {
		if (limit < 0) {
			throw new IllegalStateException("Exceeded write limit");
		}
	}

	@Override
	public void write(int b) throws IOException {
		limit--;
		out.write(b);
	}

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	public void setByteOrder(ByteOrder byteOrder) {
		this.byteOrder = byteOrder;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public void writeInt(int v) throws IOException {
		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			out.write((v >>> 24) & 0xFF);
			out.write((v >>> 16) & 0xFF);
			out.write((v >>>  8) & 0xFF);
			out.write(v & 0xFF);
		} else {
			out.write(v & 0xFF);
			out.write((v >>>  8) & 0xFF);
			out.write((v >>> 16) & 0xFF);
			out.write((v >>> 24) & 0xFF);
		}
	}

	public void writeShort(short v) throws IOException {
		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			out.write((v >>> 8) & 0xFF);
			out.write(v & 0xFF);
		} else {
			out.write(v & 0xFF);
			out.write((v >>> 8) & 0xFF);
		}

	}
}
