/*
 * Copyright 2012 Pascal Voitot
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
package play.modules.datomic

import play.api._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

import reactivedatomic.{Datomic, Connection}
import scala.util.{Try, Success, Failure}


class DatomicPlugin(app: Application) extends Plugin {

  val conf = DatomicPlugin.parseConf(app)

  /** Retrieves URI using ID from configuration.  
    * It crashes with runtime exception if not found 
    */
  def uri(id: String): String = conf(id)
  /** Retrieves URI from configuration in safe mode.  
    * @return Some(uri) if found and None if not found 
    */
  def safeUri(id: String): Option[String] = conf.get(id)

  /** Creates a Datomic connection (or throws a RuntimeException):
    * - if ID is found in configuration, it retrieves corresponding URI
    * - if ID is not found, it considers ID is an URI
    * @param id the id to search or an URI
    * @return created Connection (or throws RuntimeException)
    */
  def connect(id: String): Connection = Datomic.connect(safeUri(id).getOrElse(id))

  /** Safely creates a Datomic connection :
    * - if ID is found in configuration, it retrieves corresponding URI
    * - if ID is not found, it considers ID is an URI
    * @param id the id to search or an URI
    * @return a Try[Connection] embedding potential detected exception
    */
  def safeConnect(id: String): Try[Connection] = Try(connect(safeUri(id).getOrElse(id)))

  override def onStart {
    Logger.info("DatomicPlugin starting...")
    Logger.info(
      "DatomicPlugin successfully started with uris :\n%s".format(
        conf.map{ case(k, v) => s"  $k : $v" }.mkString("{\n", "\n", "\n}")
      )
    )
  }

  override def onStop {
    Logger.info("DatomicPlugin stops, closing connections...")
  }
}

/**
 * Datomic access methods.
 */
object DatomicPlugin {

  def uri(id: String)(implicit app: Application): String = current.uri(id)
  def safeUri(id: String)(implicit app: Application): Option[String] = current.safeUri(id)

  def connect(id: String)(implicit app: Application): Connection = current.connect(id)
  def safeConnect(id: String)(implicit app: Application): Try[Connection] = current.safeConnect(id)
  /*val DEFAULT_HOST = "localhost:27017"

  def connection(implicit app :Application) = current.connection
  def db(implicit app :Application) = current.db
  def collection(name :String)(implicit app :Application) = current.collection(name)
  def dbName(implicit app :Application) = current.dbName
  */

  /**
    * returns the current instance of the plugin.
    */
  def current(implicit app: Application): DatomicPlugin = app.plugin[DatomicPlugin] match {
    case Some(plugin) => plugin
    case _ => throw new PlayException("DatomicPlugin Error", "The DatomicPlugin has not been initialized! Please edit your conf/play.plugins file and add the following line: '400:play.modules.datomic.DatomicPlugin' (400 is an arbitrary priority and may be changed to match your needs).")
  }

  private def parseConf(app: Application): Map[String, String] = {
    import scala.collection.JavaConversions._
    app.configuration.getObject("datomiska.uri") match {
      case Some(obj) => obj.toMap.map{ case(k, v) => k -> v.unwrapped.toString }
      case None =>  throw app.configuration.globalError("Missing configuration key 'datomic.uri' (should be a list of servers)!")
    }
  }
}

