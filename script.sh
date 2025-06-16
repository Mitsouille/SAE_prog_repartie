#!/bin/bash

cd "$(dirname "$0")"

pkill -f Main
sleep 2

mvn -f MainServeur exec:java -Dexec.mainClass=Main &
PID_MAIN=$!

for i in {1..10}; do
  nc -z localhost 1099 && break
  sleep 1
done

if ! nc -z localhost 1099; then
  kill $PID_MAIN
  exit 1
fi

java -cp "DB_restaurant/target/classes:MainServeur/target/classes:DB_restaurant/lib/ojdbc11.jar" Main &
PID_DB=$!

sleep 1

java -cp "HTTPService/target/classes:MainServeur/target/classes:HTTPService/lib/json.jar" Main &

PID_HTTP=$!

wait $PID_MAIN $PID_DB $PID_HTTP

