# Makefile for Test Execution of Fixr Graph Extractor
# Author: Rhys Braginton Pettee Olsen <rhol9958@colorado.edu>
# CUPLV Lab, Boulder, Colorado
#
# Standing proble:
#		`java.lang.RuntimeException: couldn't find class: java.lang.Object``
#
# RULED OUT AS CAUSE:
#   * Version of Java
#   * Order of classpath entries for every classpath variable

# TODO:
#		* Experiment with different RT .jar's

# JAVA = /Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home/bin/java

JAVA = java
# Should be good in general

SOOT = ./lib/soot-2.5.0.jar

EXTRACTOR = ./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT.jar

MAIN_CLASS = edu.colorado.plv.fixr.Main

RT = ./classes.jar

TEST = ./src/test/resources

TEST_CLASS = simple.Simple

TEST_METHOD = main

default:
	$(JAVA) -cp $(SOOT):$(EXTRACTOR) $(MAIN_CLASS) $(RT):$(TEST) $(TEST_CLASS) $(TEST_METHOD)

hedy:
	java -cp ./lib/soot-2.5.0.jar:./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT.jar edu.colorado.plv.fixr.Main ./classes.jar:./src/test/resources simple.Simple main

hedy-old:
	java -cp ./lib/soot-2.5.0.jar:./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT.jar edu.colorado.plv.fixr.Main  /scanr_run/bediacamera_repos/TwidereProject/Twidere-Android/twidere/build/intermediates/exploded-aar/com.pnikosis/materialish-progress/1.7/jars/classes.jar:./src/test/resources simple.Simple main

full:
	java -cp ./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT.jar:./lib/soot-2.5.0.jar edu.colorado.plv.fixr.Main /cygdrive/c/Program\ Files/Java/jdk1.7.0_79/jre/lib/rt.java:./src/test/resources simple.Simple main

old:
	java -cp lib/soot-2.5.0.jar:repos/FixrGraphExtractor-1.0.jar edu.colorado.plv.fixr.Main <path_to_rt.jar>:./src/test/resources simple.Simple main

inspect:
	jar tf ./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT.jar
