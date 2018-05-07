import com.github.retronym.SbtOneJar._

import sbtprotobuf.{ProtobufPlugin=>PB}

enablePlugins(ProtobufPlugin)

oneJarSettings

version := "0.1.0"
scalaVersion := "2.12.1"
organization := "edu.colorado.plv.fixr"
name := "fixrgraphextractor"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

resolvers += "Maven Central Repository" at "https://repo1.maven.org/maven2/"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

scalacOptions += "-target:jvm-1.7"

// Remove the parallel execution of tests - soot is not happy with that
concurrentRestrictions in Global += Tags.limit(Tags.Test, 1)


libraryDependencies ++= Seq(
  "junit" % "junit" % "4.11" % Test,
  "commons-lang" % "commons-lang" % "2.6",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "com.google.googlejavaformat" % "google-java-format" % "1.0",
  "com.google.guava" %  "guava" % "19.0",
  "args4j" % "args4j" % "2.0.23",
  "ch.qos.logback" % "logback-classic" % "1.0.10",
  "ch.qos.logback" % "logback-core" % "1.0.10",
  "net.sf.jgrapht" % "jgrapht" % "0.8.3",
  "org.apache.commons" % "commons-lang3" % "3.1",
  "org.apache.commons" % "commons-collections4" % "4.1",
  "com.google.code.findbugs" % "annotations" % "2.0.1",
  "com.github.scopt" %% "scopt" % "3.7.0",
  "com.lihaoyi" %% "scalatags" % "0.6.7",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.ow2.asm" % "asm-debug-all" % "5.1"
)

