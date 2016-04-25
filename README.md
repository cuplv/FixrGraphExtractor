# FixrGraphExtractor

Functionalities:
- Slice the control flow graph only w.r.t. calls to Android APIs (or some other user-defined API)
- Extract the control flow graph with data information from java source code (CFDG)
Now the extraction is limited to a graph per procedure.
- Check the approximate isomorphism between two CDFG

# Implementation plan
List of tasks:
- Slicing:
  - Find relevant variable (DONE)
  - Slice the CFG accordingly
- CDFG:
  - Define CDFG structure
  - Extract the CDFG from a CFG and dataflow information
- Isomorphism

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


# Test
Run the test cases: `gradle test`

The test uses the jimple representation that can be generated calling soot:
`java -jar ./lib/soot-2.5.0.jar --f J -src-prec java -cp <rt.jar>:<full path to FixrGraphExtractor/src/test/resources> slice.TestSlice`