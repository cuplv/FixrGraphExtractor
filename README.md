# FixrGraphExtractor

- Extract control data and flow graph from java source code
- Implement slicing for Android APIs
- Implement the isomorphism check among two annotated graphs

# Compile
gradle build
gradle uploadArchives

# Run (java example)
Now the program just print a dot file of a CFG (in ~/test.dot)

java -cp lib/soot-2.5.0.jar:repos/FixrGraphExtractor-1.0.jar edu.colorado.plv.fixr.Main <path_to_rt.jar>:./src/test/resources simple.Simple main

were <path_to_rt.jar> is the path to the file rt.jar in the jdk directory (e.g. /usr/lib/jvm/jdk1.7.0/jre/lib/rt.jar)


# Run (android example)
Need the android.jar file

java -cp lib/soot-2.5.0.jar:repos/FixrGraphExtractor-1.0.jar edu.colorado.plv.fixr.Main rt.jar:android.jar:./src/test/resources androidtests.HelloWorldActivity onCreate

rt.jar: jar file of the runtime environment for java
android.jar: jar file of the android framework.
You can obtain a version of the jar here http://repository.grepcode.com/java/ext/com/google/android/android/4.4.2_r1/android-4.4.2_r1.jar or downloading the Android SDK
