# Fixr Graph Extractor


[![Build Status](https://travis-ci.com/cuplv/FixrGraphExtractor.svg?token=8yApKhj2WXmbEPSYZsqa&branch=master)](https://travis-ci.com/cuplv/FixrGraphExtractor)

Sriram Sankaranarayanan, Sergio Mover and Rhys Braginton Pettee Olsen

University of Colorado at Boulder CUPLV Lab

Boulder, Colorado

Part of CUPLV's DARPA MUSE Effort

## Functionalities
- Slice the a Soot Jimple control data flow graph only w.r.t. calls to Android APIs (or some other user-defined API)
- Extract the control flow graph with data information from java source code (CDFG) to produce an abstract control data flow graph (ACDFG) (IN PROGRESS)
- Implement the isomorphism check (TOOO)

## Limitations
1. Presently, the extraction is limited to a intraprocedural analysis (one graph per procedure)
2. Limitations of the current slicing technique (the slicing is not sound):
  * No aliasing (due to this, we cannot handle arrays of arrays correctly, since in jimple they are encoded with intermeditate variables)
  * Method calls are opaque, and they do not change anything (e.g. memory content or have side effects on the parameters)
  * Exceptions are not currently handled correctly (need to deal with them in the PDG)

## Building and Execution

### Requirements

Building and execution requires:

- Working versions of the JDK and JRE for Java 1.7
- A suitable version of SBT (found to work on 0.13.6)
- A suitable version of Scala (found to work on 2.11.4)

### Manual Configuraiton

The users's `.profile` must be set to run a version of Java for 1.7. To do this, copy and replace code from the `.profile` in [https://github.com/effervescentFibration/SetJdkVersion]. Similar code is available in the CUPLV Slack channel.

The user must find a suitable version of `rt.jar` for the 1.7 JDK and set the variable `RT` in the Makefile accordingly. Several defaults are commented out in the Makefile. If the location of your system's `rt.jar` is among these, uncomment it.

If the location of your system's `rt.jar` isn't among these, take the latest version from the list given by:
`find / -name "rt.jar" 2>/dev/null | grep 1.7`
and set `RT` to this in the Makefile. Because this may take a while, we suggest commiting a new version of the Makefile with your new assignment commented out to cut down on future work.

### Compilation

To clean the environment, run `sbt clean`.

To compile and generate the .jar files needed for execution, run `sbt package`.

### Execution
The extractor has two modes of execution.

In the first mode, the tool expects as input a class name and a method name (relative to the class).

The output of the extraction is an ACDFG saved in the output folder in protobuf format (with the naming convention <class_name>_<method_name>.acdfg.bin

An example of run in these settings through sbt is:
`sbt  "run -l /usr/lib/jvm/jdk1.7.0/jre/lib/rt.jar:./src/test/resources/javasources -c slice.TestSlice -m m1 -f java.lang.Math -o ./"`

List of packages: java.lang.Math:java.util

Dot representation on of the cdfg and of the intermediate results is produced in the provenance-dir, if this parameter is given as input.

The classpath argument ('-l') must contain the source file of the class and method of interest.

In the second mode the extractor is run on all the classes and methods found in the subfolder of a specific path (set with the `-p` option).

In this case, the extractor generates a file per class,method pair.

To run the extraction of all the classes and methods in a specific path run (note that the filter here is on android classes, by default):
`sbt  "run -l /usr/lib/jvm/jdk1.7.0/jre/lib/rt.jar:./src/test/resources/libs/android-17.jar:./src/test/resources/javasources  -p ./src/test/resources/javasources  -o ./"`

Execution is simplifed by invoking commands in the Makefile, which can be referenced and extended for writing new invokations.

To perform the graph extraction on a test Android method, run `make`.

Note that running the graph extraction against the Android API requires a copy of `android.jar`, which can be obtained from `http://repository.grepcode.com/java/ext/com/google/android/android/4.4.2_r1/android-4.4.2_r1.jar` or by downloading the Android SDK.

To test the graph extraction on a rudimentary program (that doesn't use any Android API methods), run `make test`.


### Unit Tests
To perform the unit tests, run `sbt test`.

### Jimple represenation
The test uses the jimple representation that can be generated calling soot:
`java -jar ./lib/soot-2.5.0.jar --f J -src-prec java -cp <rt.jar>:<full path to FixrGraphExtractor/src/test/resources> slice.TestSlice`.

## One-jar
We can get a single jar file by running `sbt oneJar`

Then, we can run the program as:
```java -jar ./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT-one-jar.jar  <soot-classpath> slice.TestSlice m1```


## SOOT issues
We use the commit version fe313b0 of soot to avoid later bugs still not solved (https://github.com/Sable/soot/issues/618)