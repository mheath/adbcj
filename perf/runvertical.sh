#!/bin/sh

mvn compile

for i in $(seq 0 10 500); do
	mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.VerticalBenchmark" -Dexec.args="$i adbcj:mysqlnetty://localhost/adbcjtck" -Dexec.classpathScope=test
	mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.VerticalBenchmark" -Dexec.args="$i adbcj:jdbc:mysql://localhost/adbcjtck" -Dexec.classpathScope=test
	mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.VerticalBenchmark" -Dexec.args="$i adbcj:postgresql-netty://localhost/adbcjtck" -Dexec.classpathScope=test
	mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.VerticalBenchmark" -Dexec.args="$i adbcj:jdbc:postgresql://localhost/adbcjtck" -Dexec.classpathScope=test
done