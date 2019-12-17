package edu.colorado.plv.fixr

import java.io.{BufferedReader, File, FileReader}
import java.nio.file.Files

import collection.JavaConverters._
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit

import scala.io.Source
import sys.process._
import com.simontuffs.onejar.Boot


object AppCodeDetector {
  def packageListFromFileList(fileList : String): String = {
    val files = fileList.split(":").map(new FileReader(_))
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
    * @param apkFile
    * @return
    */
  def mainPackageFromApk(apkFile:String):String = {
    val resjarfile: String = getClass.getResource("/lib/apkinfo.jar").getPath

    val jarfileFile = new File(resjarfile)

    val jarfile = if (jarfileFile.exists()){
      resjarfile
    }else {
      "/home/biggroum/ApkInfo/target/scala-2.12/apkinfo_2.12-0.11-one-jar.jar"
    }

    val tmpfile = new File(Files.createTempDirectory("info_apk").toUri).getAbsolutePath + "/out"



    val res = s"java -jar ${jarfile} -f ${apkFile} -o ${tmpfile}" !;
//    val res = Boot.main(Array("-f",apkFile,"-o",tmpfile))
    if (res != 0) {
      println(s"Failed to extract main packaage for ${apkFile}")
      "" //no package filtering if failure
    } else {
      Source.fromFile(tmpfile).getLines().mkString(":")
    }
  }

}
