#!/bin/sh

mvn compile

for i in $(seq 0 10 500); do
	mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.VerticalBenchmark" -Dexec.args="$i adbcj:mysqlnetty://$1/adbcjtck" -Dexec.classpathScope=test
	mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.VerticalBenchmark" -Dexec.args="$i adbcj:jdbc:mysql://$1/adbcjtck" -Dexec.classpathScope=test
	mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.VerticalBenchmark" -Dexec.args="$i adbcj:postgresql-netty://$1/adbcjtck" -Dexec.classpathScope=test
	mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.VerticalBenchmark" -Dexec.args="$i adbcj:jdbc:postgresql://$1/adbcjtck" -Dexec.classpathScope=test
done