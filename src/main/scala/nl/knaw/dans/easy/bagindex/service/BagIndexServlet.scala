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
package nl.knaw.dans.easy.bagindex.service

import java.net.URI

import nl.knaw.dans.easy.bagindex.BagIndexApp
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.scalatra.{ Ok, ScalatraServlet }

case class BagIndexServlet(app: BagIndexApp) extends ScalatraServlet with DebugEnhancedLogging {
  import app._
  val externalBaseUri = new URI(properties.getString("bag-index.daemon.external-base-uri"))

  get("/") {
    Ok("Hello world")
  }

  // get: http://bag-index/bag-sequence?contains=<bagId>
  // zoeken naar lijst in bag-sequence,
  // gegeven bagId, geef alles met dezelfde baseId, gesorteerd op datum
  // voorlopig newline separated (text/plain) String

  // put: http://bag-index/bags/<bagId>
  // get the bag with <bagId> from the bag-store, read bag-info.txt and get the base and timestamp properties
  // based on this, add a record to the index/database

  // get: http://bag-index/bags/<bagId>
  // returns record in database met base and timestamp (evt. in JSON format)

  // zelfde interface in servlet als cmd



  // TODO add servlet handlers
}
