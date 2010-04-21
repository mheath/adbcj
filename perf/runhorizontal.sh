#!/bin/sh

COUNT=50000
HOSTS="localhost"

mvn compile

mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.HorizontalBenchmark" -Dexec.args="MYSQL $COUNT $HOSTS" -Dexec.classpathScope=test
mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.HorizontalBenchmark" -Dexec.args="MYSQL_JDBC $COUNT $HOSTS" -Dexec.classpathScope=test
mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.HorizontalBenchmark" -Dexec.args="POSTGRES $COUNT $HOSTS" -Dexec.classpathScope=test
mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.HorizontalBenchmark" -Dexec.args="POSTGRES_JDBC $COUNT $HOSTS" -Dexec.classpathScope=test
