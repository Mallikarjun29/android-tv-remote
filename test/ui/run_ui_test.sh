#!/bin/bash
# Robolectric UI smoke test: drives the real activity against the mock TV on 127.0.0.1:6466/6467.
H=~/android; export JAVA_HOME=$H/jdk-17.0.20.1+1; J=$JAVA_HOME/bin; R=$H/rob; D=/tmp/tvui
rm -rf $D $R/out; mkdir -p $D $R/out
CP="$R/lib/*:$H/build/classes"
$J/javac -encoding UTF-8 -d $R/out -cp "$CP:$H/sdk/platforms/android-34/android.jar" $R/UiSmokeTest.java || exit 1
# generate the client identity the test pre-installs in the app, using the app's own Identity class
$J/javac -encoding UTF-8 -d $H/test/out $H/app/src/com/mj/tvremote/proto/*.java $H/test/ProtocolTest.java && $J/java -cp $H/test/out ProtocolTest $D gen
python3 $H/test/mock/mock_tv.py $D real & PID=$!
for i in $(seq 1 50); do [ -f $D/tv_ready ] && break; sleep 0.2; done
$J/java -Xmx700m -Drobolectric.logging.enabled=false -cp "$R/out:$R/rt:$CP:$H/sdk/platforms/android-34/android.jar" org.junit.runner.JUnitCore UiSmokeTest 2>&1 | grep -v "^\s*at \|WARNING\|^$" | tail -${TAIL:-40}
kill $PID 2>/dev/null
