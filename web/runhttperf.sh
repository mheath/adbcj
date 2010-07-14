#!/bin/sh

for i in '1 50 100 150 200 250 300 350 400 450 500 550 600 650 700 750 800 850 900 950 1000'; do
	echo "Configuring the web server for $i connections"
	curl http://$1:8080/poolsize?pool_size=$i
	httperf --server $1 --port 8080 --uri $2 --num-calls 100 --num-cons $i >> $3.log
	echo "-----" >> $3.log
done
curl http://$1:8080/poolsize?pool_size=1000