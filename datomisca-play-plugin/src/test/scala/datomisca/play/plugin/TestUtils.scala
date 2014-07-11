
import play.api.{Application, WithDefaultConfiguration, WithDefaultGlobal, WithDefaultPlugins, Mode, Play}
import play.core.SourceMapper

case class FakeApplicationWithConf(
    override val path: java.io.File = new java.io.File("."),
    override val classloader: ClassLoader = classOf[FakeApplicationWithConf].getClassLoader,
    val additionalPlugins: Seq[String] = Nil,
    val additionalConfiguration: com.typesafe.config.Config, 
    override val sources: Option[SourceMapper] = None,
    override val mode: Mode.Mode = Mode.Test
) extends Application with WithDefaultConfiguration with WithDefaultGlobal with WithDefaultPlugins {
  override def pluginClasses = {
    additionalPlugins
  }

  override def configuration = {
    super.configuration ++ play.api.Configuration(additionalConfiguration)
  }
}
  

object Utils {
  def running[T](fakeApp: FakeApplicationWithConf)(block: => T): T = {
    synchronized {
      try {
        Play.start(fakeApp)
        block
      } finally {
        Play.stop()
      }
    }
  }
}
