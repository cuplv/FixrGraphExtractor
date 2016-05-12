/*
import com.trueaccord.scalapb.{ScalaPbPlugin => PB}
import com.github.retronym.SbtOneJar._
*/

lazy val commonSettings = Seq(
	organization := "edu.colorado.plv.fixr",
	version := "0.1.0",
	scalaVersion := "2.10.2",
	name := "FixrGraphExtractor",
	javaOptions += "-Xmx2G"
)

organization := "edu.colorado.plv.fixr"

name := "fixrgraphextractor"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "org.eclipse.jdt" % "org.eclipse.jdt.core" % "3.10.0"
)

/*
PB.protobufSettings
oneJarSettings
*/
