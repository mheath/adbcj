#!/bin/sh

mvn compile

mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.HorizontalBenchmark" -Dexec.args="MYSQL 10 localhost" -Dexec.classpathScope=test
mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.HorizontalBenchmark" -Dexec.args="MYSQL_JDBC 10 localhost" -Dexec.classpathScope=test
mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.HorizontalBenchmark" -Dexec.args="POSTGRES 10 localhost" -Dexec.classpathScope=test
mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.HorizontalBenchmark" -Dexec.args="POSTGRES_JDBC 10 localhost" -Dexec.classpathScope=test
