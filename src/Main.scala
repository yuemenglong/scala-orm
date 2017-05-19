
import scala.reflect.runtime.universe
import scala.reflect.ManifestFactory

object Main extends App {
  var cls = Class.forName("scala.Person")
  println(cls.getAnnotations().length)
//  for (anno <- cls.getAnnotations()) {
//    println(anno)
//  }
  for (field <- cls.getDeclaredFields()) {
    println(field)
  }
}
