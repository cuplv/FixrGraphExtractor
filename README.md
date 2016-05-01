# FixrGraphExtractor

Functionalities:
- Slice the control flow graph only w.r.t. calls to Android APIs (or some other user-defined API)
- Extract the control flow graph with data information from java source code (CDFG)
- Implement the isomorphism check (TOOO)

## Limitation of the features
1. Now the extraction is limited to a intraprocedural analysis (one graph per procedure)
2. Limitation of the current slicing technique (the slicing is not sound):
  * No aliasing
  * Method calls are opaque, and they do not change anything (e.g. memory content or have side effects on the parameters)


# Compile (-x test avoid to run the test while building)
gradle build -x test
gradle uploadArchives

# Run (java example)
Now the program takes as input a java file and a method name and builds the sliced CDFG graph using the android APIs as seeds.
It then creates a dot file named <method_name>_<sliced>.dot that represents the CDFG.

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