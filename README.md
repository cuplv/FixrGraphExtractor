# FixrGraphExtractor

- Extract control data and flow graph from java source code
- Implement slicing for Android APIs
- Implement the isomorphism check among two annotated graphs

# Compile
gradle build
gradle uploadArchives

# Run
Now the program just print a dot file of a CFG (in ~/test.dot)

java -cp lib/soot-2.5.0.jar:repos/FixrGraphExtractor-1.0.jar edu.colorado.plv.fixr.Main <path_to_rt.jar>:./src/test/resources simple.Simple main

were <path_to_rt.jar> is the path to the file rt.jar in the jdk directory (e.g. /usr/lib/jvm/jdk1.7.0/jre/lib/rt.jar)
