package edu.colorado.plv.fixr

import java.io.{BufferedReader, File, FileReader, FileWriter}
import java.net.{HttpURLConnection, URL}
import java.nio.file.Files


import collection.JavaConverters._
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit

import scala.io.Source
import sys.process._


object AppCodeDetector {
  def packageListFromFileList(fileList : String): String = {
    val files = fileList.split(":").map(new FileReader(_))
    println(files)
    val out: Array[String] = files.map(packageListFromFile(_).mkString(":"))
    out.mkString(":")
  }
  def packageListFromFile(file: FileReader):Seq[String] = {
    val reader = new BufferedReader(file)
    try{
      val breader = new BufferedReader(reader)
      var out = new StringBuilder();
      var line: String = ""
      line = breader.readLine()
      while(line != null){
        out.append(line)
        out.append("\n")
        line = breader.readLine()
      }
      val p: CompilationUnit = StaticJavaParser.parse(out.mkString)
      val nodelist = p.getTypes.asScala.map(a => a.getFullyQualifiedName.get())
      nodelist
    }catch{
      case e => {
        println("-------------")
        println(s"Exception in reading source archive: ${file}")
        println(e.printStackTrace())
        println("-------------")
        List()}
    }finally{
      reader.close()
    }
  }

  /***
    * Note: this is a silly hack to avoid dexlib conflicts
    * apkinfo.jar is not included in resources file if using oneJar
    * TODO: find a better way to handle this than a list of hardcoded paths
    * TODO: find a way to not need extra jar
    * @param apkFile
    * @return
    */
  def mainPackageFromApk(apkFile:String):String = {
    val resource: URL = getClass.getResource("/apkinfo.jar")
    val homedir = System.getProperty("user.home")
    println(s"Homedir: ${homedir}")
    val searchLocations = List("apkinfo_2.12-0.11-one-jar.jar",
      "Documents/source/ApkInfo/target/scala-2.12/apkinfo_2.12-0.11-one-jar.jar",
      "ApkInfo/target/scala-2.12/apkinfo_2.12-0.11-one-jar.jar",
      "biggroumsetup/apkinfo_2.12-0.11-one-jar.jar"
    ).map(a => new File(s"${homedir}/${a}"))
    // ! in path means that it resolved a file inside the jar.  Jar resource resolution is currently broken.
    val jarfileFile: File = if (resource != null && (!resource.getPath.contains("!"))) {
      new File(resource.getPath)
    }else {
      searchLocations.find(_.exists()).getOrElse(???)
    }
    println(s"Using apk info jar: ${jarfileFile.getCanonicalPath} .")

    val tmpfile = new File(Files.createTempDirectory("info_apk").toUri).getAbsolutePath + "/out"

    val res = s"java -jar ${jarfileFile.getCanonicalPath} -f ${apkFile} -o ${tmpfile}" !;
    if (res != 0) {
      println(s"Failed to extract main package for ${apkFile}")
      "" //no package filtering if failure
    } else {
      val pkg = Source.fromFile(tmpfile).getLines().mkString(":")
      println(s"Using package:  ${pkg} -- for apk file ${apkFile}")
      pkg
    }
  }

}
