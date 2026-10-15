#!/bin/bash
# Builds, aligns and signs the APK without Gradle. Output: build/TVRemote.apk
set -e
H=~/android; JDK=$H/jdk-17.0.20.1+1/bin; SDK=$H/sdk; BT=$SDK/build-tools/34.0.0; AJ=$SDK/platforms/android-34/android.jar
B=$H/build; rm -rf $B; mkdir -p $B/gen $B/classes $B/res $B/dex
$BT/aapt2 compile --dir $H/app/res -o $B/res
$BT/aapt2 link -o $B/base.apk -I $AJ --manifest $H/app/AndroidManifest.xml --java $B/gen --min-sdk-version 24 --target-sdk-version 34 \
  --version-code ${VCODE:-1} --version-name ${VNAME:-1.0} $B/res/*.flat
$JDK/javac -encoding UTF-8 --release 8 -Xlint:-options -classpath $AJ -d $B/classes $(find $H/app/src $B/gen -name '*.java')
$JDK/java -cp $BT/lib/d8.jar com.android.tools.r8.D8 --release --min-api 24 --lib $AJ --output $B/dex $(find $B/classes -name '*.class')
cp $B/base.apk $B/unsigned.apk
(cd $B/dex && zip -q -j $B/unsigned.apk classes.dex)
$BT/zipalign -f -p 4 $B/unsigned.apk $B/aligned.apk
KS=$H/keystore/release.jks
: "${KS_PASS:?set KS_PASS to your keystore password (used to create or open keystore/release.jks)}"
if [ ! -f $KS ]; then mkdir -p $H/keystore; $JDK/keytool -genkeypair -keystore $KS -storepass $KS_PASS -keypass $KS_PASS -alias tvremote -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=TV Remote, O=Mallikarjun" ; fi
$JDK/java -jar $BT/lib/apksigner.jar sign --ks $KS --ks-pass pass:$KS_PASS --key-pass pass:$KS_PASS --out $B/TVRemote.apk $B/aligned.apk
$JDK/java -jar $BT/lib/apksigner.jar verify --verbose $B/TVRemote.apk
ls -l $B/TVRemote.apk
