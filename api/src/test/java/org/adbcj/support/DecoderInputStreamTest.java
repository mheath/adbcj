package org.adbcj.support;

import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.Charset;

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
		InputStream bais = new ByteArrayInputStream((s + "\0").getBytes(charset));
		DecoderInputStream in = new DecoderInputStream(bais);
		String i = in.readString(Charset.forName(charset));
		Assert.assertEquals(s, i);
	}
}
