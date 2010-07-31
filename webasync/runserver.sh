#!/bin/sh

mvn compile

MAVEN_OPTS=-Xmx1024m mvn -e exec:java -Dexec.mainClass="org.adbcj.webasync.Main"
