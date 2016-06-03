import scala.xml.{Node, Elem}
import scala.xml.transform.RewriteRule
import scalaz.Scalaz._
import com.typesafe.sbteclipse.core.EclipsePlugin.EclipseTransformerFactory
import com.typesafe.sbteclipse.core.Validation
import sbt.{ProjectRef,State}

// sbteclipse rewrite rules - exclude the java file in the test resources from the compilation
object ClasspathentryRewriteRule extends RewriteRule {
  override def transform(parent: Node): Seq[Node] = {
    parent match {
      case c @ <classpathentry/> if (c \ "@path").toString().endsWith("src/test/resources") =>
        <classpathentry excluding="**" kind="src" path={ "src/test/resources" }/>
      case other => other
    }
  }
}

// sbteclipse transformer
object ClasspathentryTransformer extends EclipseTransformerFactory[RewriteRule] {
  override def createTransformer(ref: ProjectRef, state: State): Validation[RewriteRule] = {
    ClasspathentryRewriteRule.success
  }
}
