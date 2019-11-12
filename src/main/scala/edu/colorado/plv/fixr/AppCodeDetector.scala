package edu.colorado.plv.fixr

import java.io.{BufferedReader, FileReader}
import collection.JavaConverters._

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit

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

}
