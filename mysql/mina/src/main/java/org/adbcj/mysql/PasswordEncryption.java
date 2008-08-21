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
package org.adbcj.mysql;

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
	
}
