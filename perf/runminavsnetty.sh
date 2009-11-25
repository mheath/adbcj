#!/bin/sh

mvn exec:java -Dexec.mainClass="org.adbcj.perf.MinaVsNettyBenchmark" -Dexec.args="$1" -Dexec.classpathScope=test