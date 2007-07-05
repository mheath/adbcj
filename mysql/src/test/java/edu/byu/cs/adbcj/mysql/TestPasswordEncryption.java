package edu.byu.cs.adbcj.mysql;

import org.junit.Assert;
import org.junit.Test;

public class TestPasswordEncryption {

	@Test
	public void testPasswordEncryption() throws Exception {
		byte[] salt = new byte[] { 0x62, 0x5b, 0x24, 0x3b, 0x3e, 0x65, 0x60,
				0x36, 0x2a, 0x6f, 0x72, 0x6e, 0x53, 0x35, 0x4b, 0x59, 0x5e,
				0x4f, 0x4e, 0x7a };
		byte[] expectedCipherText = new byte[] { 0x52, 0x18, 0x9, 0x57, 0x6,
				(byte)0xbc, 0x5c, 0x25, 0x4, (byte)0x88, (byte)0x8c, 0x4a,
				(byte)0x81, 0x22, (byte)0xa6, (byte)0x90, (byte)0xcf,
				(byte)0x9a, (byte)0xe6, (byte)0xbb };
		String password = "cockroach";

		byte[] cipherText = PasswordEncryption.encryptPassword(password, salt);
		Assert.assertEquals(expectedCipherText.length, cipherText.length);
		for (int i = 0; i < expectedCipherText.length; i++) {
			Assert.assertEquals(expectedCipherText[i], cipherText[i]);
		}
	}

}
