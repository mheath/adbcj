#!/bin/sh

mvn compile
mvn -e exec:java -Dexec.mainClass="org.adbcj.perf.PipelineTest" -Dexec.args="$1" -Dexec.classpathScope=test 
