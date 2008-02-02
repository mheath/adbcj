/*
 *   Copyright (c) 2007 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.adbcj.postgresql;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordHash {

	public static byte[] hashPasswordMD5(String user, String password, byte[] salt) {
		try {
			Charset charset = Charset.forName("US-ASCII");
			MessageDigest md = MessageDigest.getInstance("MD5");
			
			md.update(password.getBytes(charset));
			md.update(user.getBytes(charset));
			byte[] userPasswordDigest = md.digest();

			byte[] userPassword = new byte[userPasswordDigest.length * 2];
			digestToHex(userPasswordDigest, userPassword, 0);
			
			md.update(userPassword);
			md.update(salt);
			
			byte[] digest = md.digest();
			byte[] hex = new byte[userPasswordDigest.length * 2 + 3];
			hex[0] = 'm';
			hex[1] = 'd';
			hex[2] = '5';
			digestToHex(digest, hex, 3);
			return hex;
			
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static final byte[] HEX_VALUES =
			{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	
	/**
	 * @param digest
	 * @param hex
	 * @param offset
	 */
	private static void digestToHex(byte[] digest, byte[] hex, int offset) {
		for (int j = 0; j < digest.length; j++) {
			int b = digest[j] & 0xff;
			int i = j * 2 + offset;
			hex[i] = HEX_VALUES[b >> 4];
			hex[i + 1] = HEX_VALUES[b & 0x0F];
		}
	}
	
}
