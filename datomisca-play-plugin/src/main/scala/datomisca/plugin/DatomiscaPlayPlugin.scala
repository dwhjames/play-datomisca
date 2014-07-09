/*
 * Copyright 2012-2014 Pellucid and Zenexity
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datomisca
package plugin

import scala.util.Try

import play.api.{Application, Logger, PlayException, Plugin}


class DatomiscaPlayPlugin(app: Application) extends Plugin {

  val conf = {
    val conf0 = app.configuration
    conf0.getConfig("datomisca.uri") match {
      case None => conf0
      case Some(conf1) => conf0 ++ conf1 // conf1 withFallback conf0
    }
  }

  /** Retrieves URI using ID from configuration.  
    * It crashes with runtime exception if not found 
    */
  def uri(id: String): String =
    conf.getString(id) getOrElse { throw new IllegalArgumentException(s"$id not found") }
  /** Retrieves URI from configuration in safe mode.  
    * @return Some(uri) if found and None if not found 
    */
  def safeUri(id: String): Option[String] = conf.getString(id)

  /** Creates a Datomic connection (or throws a RuntimeException):
    * - if ID is found in configuration, it retrieves corresponding URI
    * - if ID is not found, it considers ID is an URI
    * @param id the id to search or an URI
    * @return created Connection (or throws RuntimeException)
    */
  def connect(id: String): Connection = Datomic.connect(
    if (id startsWith "datomic:")
      id
    else
      uri(id)
  )

  /** Safely creates a Datomic connection :
    * - if ID is found in configuration, it retrieves corresponding URI
    * - if ID is not found, it considers ID is an URI
    * @param id the id to search or an URI
    * @return a Try[Connection] embedding potential detected exception
    */
  def safeConnect(id: String): Try[Connection] = Try(connect(id))


  override def onStart: Unit = {
    import scala.collection.JavaConversions._
    app.configuration.getObject("datomisca.uri") foreach { obj =>
        obj.toMap foreach { case (k, v) =>
          if (v.valueType == com.typesafe.config.ConfigValueType.STRING) {
            val uriStr = v.unwrapped.toString
            assert {
              uriStr startsWith "datomic:"
            }
            val uri = new java.net.URI(uriStr drop 8)
            Logger.info(s"""DatomiscaPlayPlugin found datomisca.uri config with,
            |{
            |  config key:      $k
            |  storage service: ${uri.getScheme}
            |  db URI path:     ${uri.getAuthority}${uri.getPath}
            |}""".stripMargin
            )
          }
        }
    }
  }
}

/**
 * Datomic access methods.
 */
object DatomiscaPlayPlugin {

  def uri(id: String)(implicit app: Application): String = current.uri(id)
  def safeUri(id: String)(implicit app: Application): Option[String] = current.safeUri(id)

  def connect(id: String)(implicit app: Application): Connection = current.connect(id)
  def safeConnect(id: String)(implicit app: Application): Try[Connection] = current.safeConnect(id)

  /**
    * returns the current instance of the plugin.
    */
  def current(implicit app: Application): DatomiscaPlayPlugin = app.plugin[DatomiscaPlayPlugin] match {
    case Some(plugin) => plugin
    case _ => throw new PlayException("DatomiscaPlayPlugin Error", "The DatomiscaPlayPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:datomisca.plugin.DatomiscaPlayPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }

}

