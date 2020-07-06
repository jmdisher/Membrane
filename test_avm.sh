#!/bin/bash

# A test for deploying AVM contracts and creating programmable topics.
# The general design of this is to use the CounterProgram in order to automatically assign an incrementing number to a given user, which can then be observed in the composite document.
# This is a very trivial case but does expose how data originating within AVM can become part of a composite document.
# NOTE:  Because the AVM isn't fully modified for Laminar, keys MUST be exactly 32 bytes long.

# First, we crudely package the test class as a JAR.
mkdir temp_build
cd temp_build
mkdir META-INF
mkdir -p com/jeffdisher/membrane/avm
cp ../rest-server/target/test-classes/com/jeffdisher/membrane/avm/CounterProgram.class com/jeffdisher/membrane/avm
echo -e "Manifest-Version: 1.0\nMain-Class: com.jeffdisher.membrane.avm.CounterProgram\n" > META-INF/MANIFEST.MF
zip ../program.jar -r .
cd ../
rm -rf temp_build

# Post the AVM program and then delete the temporary resource.
curl -XPOST "localhost:8080/counter" -F "type=integer" -F "arguments=" -F "code=@program.jar"
rm -f program.jar

curl -XPOST "localhost:8080/name" -F "type=String" -F "code=" -F "arguments="

# These writes to the counter topic will assign numbers for these keys, even though they aren't given any data.
curl -XPUT "localhost:8080/counter/12345678901234567890123456789012" -d ""
curl -XPUT "localhost:8080/counter/12345678901234567890123456789013" -d ""
curl -XPUT "localhost:8080/name/12345678901234567890123456789012" -d "First character name"
curl -XPUT "localhost:8080/name/12345678901234567890123456789013" -d "Second character name"
curl -XGET "localhost:8080/12345678901234567890123456789012"
curl -XGET "localhost:8080/12345678901234567890123456789013"
curl -XDELETE "localhost:8080/exit"
