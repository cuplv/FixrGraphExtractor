// import com.trueaccord.scalapb.{ScalaPbPlugin => PB}
import com.github.retronym.SbtOneJar

lazy val commonSettings = SbtOneJar.oneJarSettings ++ Seq(
	organization := "edu.colorado.plv.fixr",
	version := "0.1.0",
	scalaVersion := "2.10.2",
	name := "FixrGraphExtractor",
	javaOptions += "-Xmx2G",
  exportJars := true
)

organization := "edu.colorado.plv.fixr"

name := "fixrgraphextractor"

resolvers += Resolver.sonatypeRepo("public")

resolvers += "Local Maven Repository" at "file:///"+Path.userHome.absolutePath+"/.m2/repository"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

scalacOptions += "-target:jvm-1.7"

libraryDependencies ++= Seq(
  "org.eclipse.jdt" % "org.eclipse.jdt.core" % "3.10.0",
  "org.scalatest" %% "scalatest" % "2.2.2" % Test,
  "junit" % "junit" % "4.11" % Test,
  "commons-lang" % "commons-lang" % "2.6"
)
