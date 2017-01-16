/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.bagstoreindex.service

import nl.knaw.dans.easy.bagstoreindex.{ BagStoreIndexApp, CONTEXT_ATTRIBUTE_KEY_BAGSTOREINDEX_APP => appKey }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.eclipse.jetty.ajp.Ajp13SocketConnector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.scalatra.servlet.ScalatraListener

import scala.util.Try

class BagStoreIndexService extends BagStoreIndexApp with DebugEnhancedLogging {
  import logger._

  // TODO info's for base directory, etc.
  info(s"database connection: $dbUrl")
  validateSettings()

  private val port = properties.getInt("bag-store-index.daemon.http.port")
  val server = new Server(port)
  val context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS)
  context.setAttribute(appKey, this)
  context.addEventListener(new ScalatraListener)
  server.setHandler(context)
  info(s"HTTP port is $port")

  if (properties.containsKey("bag-store-index.daemon.ajp.port")) {
    val ajp = new Ajp13SocketConnector
    val ajpPort = properties.getInt("bag-store-index.daemon.ajp.port")
    ajp.setPort(ajpPort)
    server.addConnector(ajp)
    info(s"AJP port is $ajpPort")
  }

  def start(): Try[Unit] = Try {
    info("Starting HTTP service ...")
    server.start()
    initConnection()
  }

  def stop(): Try[Unit] = Try {
    info("Stopping HTTP service ...")
    closeConnection()
    server.stop()
  }

  def destroy: Try[Unit] = Try {
    server.destroy()
  }
}

object BagStoreIndexService extends DebugEnhancedLogging {

  def apply(): BagStoreIndexService = new BagStoreIndexService

  def main(args: Array[String]): Unit = {
    import logger._
    val service = BagStoreIndexService()
    Runtime.getRuntime.addShutdownHook(new Thread("service-shutdown") {
      override def run(): Unit = {
        info("Stopping service ...")
        service.stop()
        info("Cleaning up ...")
        service.destroy
        info("Service stopped.")
      }
    })
    service.start()
    info("Service started ...")
  }
}
