#!/bin/bash

# NOTE: The use of "integer" means that it will be expecting the data to be encoded as a 4-byte signed big-endian value.

curl -XPOST "localhost:8080/topic1" -d "type=String&code=&arguments="
curl -XPOST "localhost:8080/topic2" -d "type=String&code=&arguments="
curl -XPOST "localhost:8080/employee_number" -d "type=integer&code=&arguments="
printf "\x00\x00\x00\x01" > number.temp
curl -XPUT "localhost:8080/employee_number/key1" --data-binary "@number.temp"
printf "\x00\x00\x00\x02" > number.temp
curl -XPUT "localhost:8080/employee_number/key2" --data-binary "@number.temp"
rm -f number.temp
curl -XPUT "localhost:8080/topic1/key1" -d "TESTING"
curl -XPUT "localhost:8080/topic1/key2" -d "TESTING2
next line"
curl -XPUT "localhost:8080/topic2/key1" -d "topic1 key1"
curl -XGET "localhost:8080/key1"
curl -XGET "localhost:8080/key2"
curl -XDELETE "localhost:8080/exit"
