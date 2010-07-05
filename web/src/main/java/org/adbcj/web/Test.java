//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.web;

import java.net.Socket;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Test {

	public static void main(String[] args) throws Exception {
		int i = 0;
		while (true) {
			System.out.println(i++);
			new Socket("ip-10-244-203-49.ec2.internal", 3306);
		}
	}

}
