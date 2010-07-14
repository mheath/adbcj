//*********************************************************************
//Copyright 2010 Intellectual Reserve, Inc. All rights reserved.
//This notice may not be removed.
//*********************************************************************
package org.adbcj.webasync;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.InetSocketAddress;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class Main {

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = new ClassPathXmlApplicationContext("META-INF/spring/applicationContext.xml");

		ServerBootstrap bootstrap = applicationContext.getBean("bootstrap", ServerBootstrap.class);
		bootstrap.bind(new InetSocketAddress(8080));
		System.out.println("Server ready");
		
	}

}
