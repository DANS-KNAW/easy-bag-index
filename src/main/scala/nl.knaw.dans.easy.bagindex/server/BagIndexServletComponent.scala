/**
 * Copyright (C) 2017 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.bagindex.server

import nl.knaw.dans.easy.bagindex._
import nl.knaw.dans.easy.bagindex.access.DatabaseAccessComponent
import nl.knaw.dans.easy.bagindex.command.Command.database.getAllBagsWithOtherIdVersion
import nl.knaw.dans.easy.bagindex.components.{ DatabaseComponent, IndexBagComponent }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.logging.servlet._
import nl.knaw.dans.lib.string._
import org.joda.time.DateTime
import org.json4s.JValue
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.scalatra._

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Node, PrettyPrinter, Text }

trait BagIndexServletComponent {
  this: IndexBagComponent with DatabaseComponent with DatabaseAccessComponent =>

  val bagIndexServlet: BagIndexServlet

  trait BagIndexServlet extends ScalatraServlet
    with ServletLogger
    with PlainLogFormatter
    with LogResponseBodyOnError
    with DebugEnhancedLogging {

    val version: String

    private def toXml(bagInfo: BagInfo): Node = {
      <bag-info>
        <bag-id>{bagInfo.bagId.toString}</bag-id>
        <base-id>{bagInfo.baseId.toString}</base-id>
        <created>{bagInfo.created.toString(dateTimeFormatter)}</created>
        <doi>{bagInfo.doi}</doi>
        <urn>{bagInfo.urn}</urn>
        {
          bagInfo.otherId.id.map { id =>
            <otherid>
              <id>{ id }</id>
              {
                bagInfo.otherId.version.map { version =>
                  <version>{ version }</version>
                }.getOrElse(Text(""))
              }
            </otherid>
          }.getOrElse(Text(""))
        }
      </bag-info>
    }

    private def toJson(bagInfo: BagInfo): JValue = {
      "bag-info" -> {
        ("bag-id" -> bagInfo.bagId.toString) ~
          ("base-id" -> bagInfo.baseId.toString) ~
          ("created" -> bagInfo.created.toString(dateTimeFormatter)) ~
          ("doi" -> bagInfo.doi) ~
          ("urn" -> bagInfo.urn) ~
          ("otherId" -> bagInfo.otherId.id) ~
          ("otherIdVersion" -> bagInfo.otherId.version)
      }
    }

    private def createResponse[T](toXml: T => Node)(toJson: T => JValue): T => String = {
      request.getHeader("Accept") match {
        case accept @ ("application/xml" | "text/xml") =>
          contentType = accept
          (new PrettyPrinter(80, 4).format(_: Node)) compose toXml
        case _ =>
          contentType = "application/json"
          pretty _ compose (render _ compose toJson)
      }
    }

    get("/") {
      Ok(s"EASY Bag Index running v$version.")
    }


    get("/search") {
      def searchWithIdentifier(identifier: Identifier, identifierType: String): Try[String] = {
        databaseAccess.doTransaction(implicit c => {
          database.getBagsWithIdentifier(identifier, identifierType)
            .map(createResponse[Seq[BagInfo]](relations => <result>{relations.map(toXml)}</result>)(relations => "result" -> relations.map(toJson)))
        })
      }
      def searchWithVersionedOtherId(id: Identifier, version: String): Try[String] = {
        databaseAccess.doTransaction(implicit c => {
          database.getAllBagsWithOtherIdVersion(id, version)
            .map(createResponse[Seq[BagInfo]](relations => <result>{relations.map(toXml)}</result>)(relations => "result" -> relations.map(toJson)))
        })
      }
      def searchWithOtherIdVersion(version: String): Try[String] = {
        params.get("otherId").map(searchWithVersionedOtherId(version, _))
          .getOrElse(Failure(new IllegalArgumentException("otherIdVersion for search query specified but no otherId")))
      }

      Option(params)
        .filter(_.size > 0)
        .map { params =>
          params.get("otherIdVersion").map(searchWithOtherIdVersion)
            .orElse(params.get("doi").map(searchWithIdentifier(_, "doi")))
            .orElse(params.get("urn").map(searchWithIdentifier(_, "urn")))
            .orElse(params.get("otherId").map(searchWithIdentifier(_, "otherId")))
            .getOrElse(Failure(new IllegalArgumentException("query parameter not supported")))
        }
        .getOrElse(Failure(new IllegalArgumentException("no search query specified")))
        .map(Ok(_))
        .doIfFailure { case e => logger.error(e.getMessage, e) }
        .getOrRecover(defaultErrorHandling)
    }

    // GET: http://bag-index/bag-sequence?contains=<bagId>
    // given a bagId, return a list of bagIds that have the same baseId, ordered by the 'created' timestamp
    // the data is returned as a newline separated (text/plain) String
    get("/bag-sequence") {
      contentType = "text/plain"
      params.get("contains")
        .map(uuidStr => {
          uuidStr.toUUID.toTry
            .flatMap(uuid => databaseAccess.doTransaction(implicit c => index.getBagSequence(uuid)))
            .recoverWith {
              case BagIdNotFoundException(_) => Success(List.empty)
            }
        })
        .getOrElse(Failure(new IllegalArgumentException("query parameter 'contains' not found")))
        .map(ids => Ok(ids.mkString("\n")))
        .doIfFailure { case e => logger.error(e.getMessage, e) }
        .getOrRecover(defaultErrorHandling)
    }

    // GET: http://bag-index/bags/<bagId>
    // given a bagId, return the relation data corresponding to this bagId
    // the data is returned as JSON by default or XML when specified (content-type application/xml or text/xml)
    get("/bags/:bagId") {
      val uuidStr = params("bagId")
      uuidStr.toUUID.toTry
        .doIfSuccess(bagId => logger.info(s"get relation data corresponding to bag $bagId"))
        .flatMap(uuid => databaseAccess.doTransaction(implicit c => database.getBagInfo(uuid)))
        .map(createResponse[BagInfo](bagInfo => <result>{toXml(bagInfo)}</result>)(bagInfo => "result" -> toJson(bagInfo)))
        .map(Ok(_))
        .recoverWith {
          case BagIdNotFoundException(_) => Success(NotFound(s"bag with id $uuidStr could not be found"))
        }
        .doIfFailure { case e => logger.error(e.getMessage, e) }
        .getOrRecover(defaultErrorHandling)
    }

    // PUT: http://bag-index/bags/<bagId>
    // get the bag with the given bagId from the bag-store, read bag-info.txt and get the base and 'created' timestamp properties
    // based on this, add a record to the index/database
    put("/bags/:bagId") {
      val uuidStr = params("bagId")
      uuidStr.toUUID.toTry
        .flatMap(uuid => databaseAccess.doTransaction(implicit c => index.addFromBagStore(uuid)))
        .map(_ => Created())
        .doIfFailure { case e => logger.error(e.getMessage, e) }
        .getOrRecover(defaultErrorHandling)
    }

    // TODO (low prio) zelfde interface in cmd als in servlet

    private def defaultErrorHandling(t: Throwable): ActionResult = {
      t match {
        case e: IllegalArgumentException => BadRequest(e.getMessage)
        case e: UUIDError => BadRequest(e.getMessage)
        case e: BagReaderException => BadRequest(e.getMessage)
        case e: BagIdNotFoundException => NotFound(e.getMessage)
        case e: NotABagDirException => NotFound(e.getMessage)
        case e: InvalidIsVersionOfException => BadRequest(e.getMessage)
        case e: BagNotFoundException => NotFound(e.getMessage)
        case e: NoIdentifierFoundException => BadRequest(e.getMessage)
        case e: BagAlreadyInIndexException => BadRequest(e.getMessage)
        case e =>
          logger.error("Unexpected type of failure", e)
          InternalServerError(s"[${ DateTime.now }] Unexpected type of failure. Please consult the logs")
      }
    }
  }
}
