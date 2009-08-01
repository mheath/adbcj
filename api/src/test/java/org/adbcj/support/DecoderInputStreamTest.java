package org.adbcj.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Mike Heath
 */
public class DecoderInputStreamTest {

	@Test
	public void boundingTest() throws IOException {
		InputStream in = new ByteArrayInputStream(new byte[] {0, 1});
		InputStream din = new DecoderInputStream(in, 1);
		din.read(); // Should pass
		try {
			din.read();
			Assert.fail("Should have thrown indicating limit exceeded");
		} catch (IllegalStateException e) {
			// Pass
		}
	}

	@Test
	public void readString() throws IOException {
		final String charset = "UTF-8";
		final String s = "Have a nice day!";
		byte[] input = (s + "\0").getBytes(charset);
		InputStream bais = new ByteArrayInputStream(input);
		DecoderInputStream in = new DecoderInputStream(bais);
		in.setLimit(input.length);
		String i = in.readString(Charset.forName(charset));
		Assert.assertEquals(s, i);
	}
}
