default:
	java -cp ./lib/soot-2.5.0.jar:./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT.jar edu.colorado.plv.fixr.Main

drunken:
	java -cp ./lib/soot-2.5.0.jar:./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT.jar edu.colorado.plv.fixr.Main

full:
	java -cp ./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT.jar:./lib/soot-2.5.0.jar edu.colorado.plv.fixr.Main /cygdrive/c/Program\ Files/Java/jdk1.7.0_79/jre/lib/rt.java:./src/test/resources simple.Simple main

old:
	java -cp lib/soot-2.5.0.jar:repos/FixrGraphExtractor-1.0.jar edu.colorado.plv.fixr.Main <path_to_rt.jar>:./src/test/resources simple.Simple main

inspect:
	jar tf ./target/scala-2.10/fixrgraphextractor_2.10-0.1-SNAPSHOT.jar
