#!/bin/bash

# NOTE: The use of "integer" means that it will be expecting the data to be encoded as a 4-byte signed big-endian value.

# Create the topics.
curl -XPOST "localhost:8080/topic1" -F "type=String" -F "code=" -F "arguments="
curl -XPOST "localhost:8080/topic2" -F "type=String" -F "code=" -F "arguments="
curl -XPOST "localhost:8080/employee_number" -F "type=integer" -F "code=" -F "arguments="

# Populate the topics for key1 and key2.
printf "\x00\x00\x00\x01" > number.temp
curl -XPUT "localhost:8080/employee_number/key1" --data-binary "@number.temp"
printf "\x00\x00\x00\x02" > number.temp
curl -XPUT "localhost:8080/employee_number/key2" --data-binary "@number.temp"
rm -f number.temp
curl -XPUT "localhost:8080/topic1/key1" -d "TESTING"
curl -XPUT "localhost:8080/topic1/key2" -d "TESTING2
next line"
curl -XPUT "localhost:8080/topic2/key1" -d "topic1 key1"

# Get the combined JSON documents.
curl -XGET "localhost:8080/json/key1"
curl -XGET "localhost:8080/json/key2"

# Shut down.
curl -XDELETE "localhost:8080/exit"
