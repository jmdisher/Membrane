#!/bin/bash

curl -XPOST "localhost:8080/topic1"
curl -XPOST "localhost:8080/topic2"
curl -XPUT "localhost:8080/topic1/key1" -d "TESTING"
curl -XPUT "localhost:8080/topic1/key2" -d "TESTING2
next line"
curl -XPUT "localhost:8080/topic2/key1" -d "topic1 key1"
curl -XGET "localhost:8080/key1"
curl -XGET "localhost:8080/key2"
curl -XDELETE "localhost:8080/exit"
