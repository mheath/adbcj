package org.adbcj.mysql.codec;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.testng.Assert;
import org.testng.annotations.Test;

public class IoUtilsTest {

	@Test
	public void testSafeRead() throws IOException {
		InputStream in = new ByteArrayInputStream(new byte[] {0,1,2});
		Assert.assertEquals(IoUtils.safeRead(in), 0);
		Assert.assertEquals(IoUtils.safeRead(in), 1);
		Assert.assertEquals(IoUtils.safeRead(in), 2);
		try {
			IoUtils.safeRead(in);
			Assert.fail("Did not throw EOF exception");
		} catch (EOFException e) {
			// Pass
		}
	}

	@Test(dependsOnMethods="testSafeRead")
	public void testReadShort() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short)0).putShort((short)1).putShort((short)2).putShort((short)-1).putShort((short)-2);
		InputStream in = new ByteArrayInputStream(buffer.array(), 0, buffer.position());
		Assert.assertEquals(IoUtils.readShort(in), 0);
		Assert.assertEquals(IoUtils.readShort(in), 1);
		Assert.assertEquals(IoUtils.readShort(in), 2);
		Assert.assertEquals(IoUtils.readShort(in), -1);
		Assert.assertEquals(IoUtils.readShort(in), -2);
	}

	@Test(dependsOnMethods="testSafeRead")
	public void testReadUnsignedShort() throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putShort((short)0).putShort((short)1).putShort((short)2).putShort((short)-1).putShort((short)-2);
		InputStream in = new ByteArrayInputStream(buffer.array(), 0, buffer.position());
		Assert.assertEquals(IoUtils.readUnsignedShort(in), 0);
		Assert.assertEquals(IoUtils.readUnsignedShort(in), 1);
		Assert.assertEquals(IoUtils.readUnsignedShort(in), 2);
		Assert.assertEquals(IoUtils.readUnsignedShort(in), 0xffff);
		Assert.assertEquals(IoUtils.readUnsignedShort(in), 0xfffe);
	}

	@Test
	public void readString() throws IOException {
		final String first = "Hi there";
		final String second = "Have a nice day!";
		byte[] firstBytes = first.getBytes();
		byte[] secondBytes = second.getBytes();
		byte[] newBytes = new byte[firstBytes.length + secondBytes.length + 1];
		System.arraycopy(firstBytes, 0, newBytes, 0, firstBytes.length);
		System.arraycopy(secondBytes, 0, newBytes, firstBytes.length + 1, secondBytes.length);
		InputStream in = new ByteArrayInputStream(newBytes);

		Assert.assertEquals(IoUtils.readString(in, "UTF-8"), first);
		Assert.assertEquals(IoUtils.readString(in, "UTF-8"), second);
	}

}
