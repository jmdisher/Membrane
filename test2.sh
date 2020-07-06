#!/bin/bash

# Intended to be used after test.sh to show what happens when a partial-overlap projection is being created on the same cluster.
# For full effect, this should be run after restarting Membrane in order to see how this different instance would view the shared data.

curl -XPOST "localhost:8080/new_topic" -F "type=String" -F "code=" -F "arguments=" -F "allowExisting="
curl -XPOST "localhost:8080/employee_number" -F "type=integer" -F "code=" -F "arguments=" -F "allowExisting="
printf "\x00\x00\x00\x03" > number.temp
curl -XPUT "localhost:8080/employee_number/new_guy" --data-binary "@number.temp"
rm -f number.temp
curl -XPUT "localhost:8080/new_topic/key1" -d "Existing user's story"
curl -XPUT "localhost:8080/new_topic/new_guy" -d "New guy's story"
curl -XGET "localhost:8080/key1"
curl -XGET "localhost:8080/new_guy"
curl -XDELETE "localhost:8080/exit"
