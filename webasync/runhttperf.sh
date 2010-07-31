#!/bin/sh

for i in 1 500 1000 1500 2000 2500 3000 3500 4000 4500 5000 5500 6000 6500 7000 7500 8000 8500 9000 9500 10000; do
	echo "Configuring the web server for $i connections"
	date
	curl http://$1:8080/count/$i
	httperf --server $1 --port 8080 --uri $2 --num-calls 100 --num-conns $i >> $3.log
	echo "-----" >> $3.log
done
curl http://$1:8080/count/10001