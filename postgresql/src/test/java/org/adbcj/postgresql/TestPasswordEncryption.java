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

import java.security.NoSuchAlgorithmException;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestPasswordEncryption {

	@Test
	public void testMD5Hash() throws NoSuchAlgorithmException {
		String username = "mheath";
		String password = "testpassword";
		byte[] salt = {(byte)0x42, (byte)0xfd, (byte)0xbc, (byte)0xd1};
		byte[] expectedDigest = {0x6d, 0x64, 0x35,
				0x39, 0x39, 0x34, 0x37, 0x34, 0x62, 0x63, 0x65,
				0x32, 0x64, 0x32, 0x33, 0x35, 0x63, 0x39, 0x66,
				0x35, 0x37, 0x33, 0x36, 0x30, 0x30, 0x38, 0x32,
				0x64, 0x64, 0x61, 0x64, 0x61, 0x34, 0x34, 0x66};
		
		byte[] digest = PasswordHash.hashPasswordMD5(username, password, salt);
		Assert.assertEquals(digest, expectedDigest);
	}	
}
