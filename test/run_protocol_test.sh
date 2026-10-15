#!/bin/bash
set -e
export JAVA_HOME=~/android/jdk-17.0.20.1+1; J=$JAVA_HOME/bin
T=~/android/test; D=/tmp/tvtest; rm -rf $D; mkdir -p $D $T/out
$J/javac -encoding UTF-8 -d $T/out ~/android/app/src/com/mj/tvremote/proto/*.java $T/ProtocolTest.java
$J/java -cp $T/out ProtocolTest $D gen
python3 $T/mock/mock_tv.py $D & PID=$!
for i in $(seq 1 50); do [ -f $D/tv_ready ] && break; sleep 0.2; done
$J/java -cp $T/out ProtocolTest $D run
RC=$?
sleep 0.5; kill $PID 2>/dev/null || true
echo "--- mock TV log (decoded by independent protobuf definitions) ---"; cat $D/tv_log.jsonl
python3 $T/mock/assert_log.py $D/tv_log.jsonl
