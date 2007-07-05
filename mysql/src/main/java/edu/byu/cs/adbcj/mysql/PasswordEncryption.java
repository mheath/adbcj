/*
 Copyright (C) 2002-2007 MySQL AB

 This program is free software; you can redistribute it and/or modify
 it under the terms of version 2 of the GNU General Public License as 
 published by the Free Software Foundation.

 There are special exceptions to the terms and conditions of the GPL 
 as it is applied to this software. View the full text of the 
 exception in file EXCEPTIONS-CONNECTOR-J in the directory of this 
 software distribution.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA



 */
package edu.byu.cs.adbcj.mysql;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class PasswordEncryption {

	static byte[] encryptPassword(String password, byte[] salt) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		
		byte[] hash1 = md.digest(password.getBytes());
		
		md.reset();
		byte[] hash2 = md.digest(hash1);
		
		md.reset();
		md.update(salt);
		md.update(hash2);
		
		byte[] digest = md.digest();
		for (int i = 0; i < digest.length; i++) {
			digest[i] = (byte)(digest[i] ^ hash1[i]);
		}
		return digest;
	}
	
	private static long CRYPT_MAX = 0x03FFFFFFFL;
	private static long OLD_CRYPT_MAX = 0x01FFFFFFL;
	
	public static byte[] passwordCrypt(String password, byte[] salt) {
		if (password == null) {
			throw new IllegalArgumentException("password cannot be null");
		}
		if (password.length() == 0) {
			return new byte[0];
		}
		if (salt == null) {
			throw new IllegalArgumentException("seed cannot be null");
		}

		int[] saltHash = passwordHash(new String(salt));
		int[] passwordHash = passwordHash(password);
		
		long x = (saltHash[0] ^ passwordHash[0]) % CRYPT_MAX;
		long y = (saltHash[1] ^ passwordHash[1]) % CRYPT_MAX;
		byte[] cipherText = new byte[salt.length];

		for (int i = 0; i < salt.length; i++) {
			x = ((x * 3) + y) % CRYPT_MAX;
			y = (x + y + 33) % CRYPT_MAX;
			cipherText[i] = (byte)Math.floor((((double)x / CRYPT_MAX) * 31) + 64);
		}

		x = ((x * 3) + y) % CRYPT_MAX;
		y = (x + y + 33) % CRYPT_MAX;
		byte mask = (byte)Math.floor(((double)x / CRYPT_MAX) * 31);

		for (int i = 0; i < salt.length; i++) {
			cipherText[i] ^= mask;
		}

		return cipherText;
		
	}
	
	public static byte[] oldPasswordCrypt(String password, byte[] salt) {
		if (password == null) {
			throw new IllegalArgumentException("password cannot be null");
		}
		if (password.length() == 0) {
			return new byte[0];
		}
		if (salt == null) {
			throw new IllegalArgumentException("salt cannot be null");
		}

		long mixedHash = (oldPasswordHash(new String(salt)) ^ oldPasswordHash(password)) % OLD_CRYPT_MAX;
		long x = mixedHash;
		long y = mixedHash / 2;

		byte[] cipherText = new byte[salt.length];

		for (int i = 0; i < salt.length; i++) {
			x = ((x * 3) + y) % OLD_CRYPT_MAX;
			y = (x + y + 33) % OLD_CRYPT_MAX;
			cipherText[i] = (byte)Math.floor(((x / OLD_CRYPT_MAX) * 31) + 64);
		}

		return cipherText;
	
	}
	
	private static int[] passwordHash(String password) {
		long x = 1345345333L;
		long y = 7;
		long z = 0x12345671L;

		for (int i = 0; i < password.length(); i++) {
			char c = (char)(0xFF & password.charAt(i)); // Only use lower byte of character
			if (!Character.isWhitespace(c)) {
				x ^= ((((x & 0x3f) + y) * c) + (x << 8));
				y += c;
				z += ((z << 8) ^ x);
			}

		}

		int[] result = new int[2];
		// Mask out negative numbers and values higher than MAX_VALUE
		result[0] = (int)(x & Integer.MAX_VALUE);
		result[1] = (int)(z & Integer.MAX_VALUE);

		return result;
	}
	
	private static int oldPasswordHash(String password) {
		long x = 1345345333;
		long y = 7;
		
		for (int i = 0; i < password.length(); i++) {
			char c = (char)(0xFF & password.charAt(i)); // Only use lower byte of character
			if (!Character.isWhitespace(c)) {
				x ^= ((((x & 0x3f) + y) * c) + (x << 8));
				y += c;
			}
		}
		return (int)(x & Integer.MAX_VALUE); // Mask out negative numbers and values higher than MAX_VALUE
	}
	
}
